package io.github.coderaulia.muzikplayer.data

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

object LyricsRepository {

    var cacheDir: Path = Path.of(System.getProperty("user.home"), ".cache", "muzikplayer", "lyrics")

    val memoryCache = ConcurrentHashMap<SongKey, Lyrics>()
    val cache: ConcurrentHashMap<SongKey, Lyrics> get() = memoryCache

    internal var httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun clearMemoryCache() {
        memoryCache.clear()
    }

    suspend fun resolveLyrics(song: Song, allowNetworkFetch: Boolean = true): Lyrics? {
        // 0. In-memory cache
        memoryCache[song.uniqueKey]?.let { return it }

        // 1. Embedded lyrics in song metadata
        if (song.lyrics != null) {
            memoryCache[song.uniqueKey] = song.lyrics
            return song.lyrics
        }

        return withContext(Dispatchers.IO) {
            // 2. Local sidecar .lrc file next to song.file
            checkSidecarLrc(song)?.let {
                memoryCache[song.uniqueKey] = it
                return@withContext it
            }

            // 3. Disk cache (~/.cache/muzikplayer/lyrics/)
            checkDiskCache(song)?.let {
                memoryCache[song.uniqueKey] = it
                return@withContext it
            }

            // 4. LRCLIB online auto-fetch
            if (allowNetworkFetch) {
                fetchFromLrclib(song)?.let {
                    memoryCache[song.uniqueKey] = it
                    saveToDiskCache(song, it)
                    return@withContext it
                }
            }

            null
        }
    }

    private fun checkSidecarLrc(song: Song): Lyrics? {
        return try {
            val sidecar = song.file.resolveSibling("${song.file.nameWithoutExtension}.lrc")
            if (Files.isRegularFile(sidecar)) {
                val content = Files.readString(sidecar, StandardCharsets.UTF_8)
                Lyrics.of(content)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to read sidecar LRC for ${song.file}" }
            null
        }
    }

    private fun checkDiskCache(song: Song): Lyrics? {
        return try {
            if (!Files.exists(cacheDir)) return null
            for (candidate in getCacheCandidates(song)) {
                if (Files.isRegularFile(candidate)) {
                    val content = Files.readString(candidate, StandardCharsets.UTF_8)
                    val lyrics = Lyrics.of(content)
                    if (lyrics != null) return lyrics
                }
            }
            null
        } catch (e: Exception) {
            logger.debug(e) { "Failed to read disk cache for ${song.title}" }
            null
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim().ifEmpty { "unknown" }
    }

    fun getCacheCandidates(song: Song): List<Path> {
        val list = mutableListOf<Path>()
        val artist = sanitizeFileName(song.artist.name)
        val title = sanitizeFileName(song.title)
        if (artist != "unknown" && title != "unknown") {
            list.add(cacheDir.resolve("$artist - $title.lrc"))
            list.add(cacheDir.resolve("$artist - $title.txt"))
        }
        val fileBase = sanitizeFileName(song.file.nameWithoutExtension)
        if (fileBase != "unknown") {
            list.add(cacheDir.resolve("$fileBase.lrc"))
            list.add(cacheDir.resolve("$fileBase.txt"))
        }
        return list
    }

    fun saveToDiskCache(song: Song, lyrics: Lyrics) {
        try {
            Files.createDirectories(cacheDir)
            val ext = if (lyrics is Lyrics.Synchronized) "lrc" else "txt"
            val artist = sanitizeFileName(song.artist.name)
            val title = sanitizeFileName(song.title)
            val fileName = if (artist != "unknown" && title != "unknown") {
                "$artist - $title.$ext"
            } else {
                "${sanitizeFileName(song.file.nameWithoutExtension)}.$ext"
            }
            val target = cacheDir.resolve(fileName)
            Files.writeString(
                target,
                lyrics.rawString,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save lyrics to disk cache for ${song.title}" }
        }
    }

    private fun fetchFromLrclib(song: Song): Lyrics? {
        val artist = song.artist.name.trim()
        val title = song.title.trim()
        if (artist.isEmpty() || title.isEmpty()) return null

        return try {
            val params = mutableListOf<String>()
            params.add("artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8).replace("+", "%20"))
            params.add("track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"))
            val album = song.album.title.trim()
            if (album.isNotEmpty() && !album.equals("Unknown", ignoreCase = true)) {
                params.add("album_name=" + URLEncoder.encode(album, StandardCharsets.UTF_8).replace("+", "%20"))
            }
            val durationSec = song.length.inWholeSeconds
            if (durationSec > 0) {
                params.add("duration=$durationSec")
            }

            val url = "https://lrclib.net/api/get?" + params.joinToString("&")
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "MuzikPlayer/1.5.6")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            if (response.statusCode() == 200) {
                parseLrclibJson(response.body())
            } else {
                logger.debug { "LRCLIB returned status ${response.statusCode()} for $artist - $title" }
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to fetch lyrics from LRCLIB for $artist - $title" }
            null
        }
    }

    fun parseLrclibJson(json: String): Lyrics? {
        if (json.isBlank()) return null
        return try {
            val obj = parseJsonObject(json)
            if (obj["instrumental"] == true) {
                return null
            }
            val synced = obj["syncedLyrics"] as? String
            if (!synced.isNullOrBlank()) {
                val parsed = Lyrics.of(synced)
                if (parsed != null) return parsed
            }
            val plain = obj["plainLyrics"] as? String
            if (!plain.isNullOrBlank()) {
                return Lyrics.of(plain)
            }
            null
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse LRCLIB JSON" }
            null
        }
    }

    private data class ParsedString(val value: String, val nextIndex: Int)

    private fun parseJsonString(json: String, openingQuoteIndex: Int): ParsedString {
        val sb = StringBuilder()
        var i = openingQuoteIndex + 1
        while (i < json.length) {
            val c = json[i]
            if (c == '"') {
                return ParsedString(sb.toString(), i + 1)
            }
            if (c == '\\') {
                i++
                if (i >= json.length) break
                when (val esc = json[i]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 < json.length) {
                            val hex = json.substring(i + 1, i + 5)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            } else {
                                sb.append("\\u").append(hex)
                            }
                        } else {
                            sb.append("\\u")
                        }
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return ParsedString(sb.toString(), i)
    }

    private fun parseJsonObject(json: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        var i = 0
        while (i < json.length && json[i] != '{') i++
        if (i >= json.length) return map
        i++ // skip '{'

        while (i < json.length) {
            while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
            if (i >= json.length || json[i] == '}') break

            if (json[i] != '"') {
                i++
                continue
            }

            val parsedKey = parseJsonString(json, i)
            val key = parsedKey.value
            i = parsedKey.nextIndex

            while (i < json.length && json[i].isWhitespace()) i++
            if (i >= json.length || json[i] != ':') break
            i++ // skip ':'

            while (i < json.length && json[i].isWhitespace()) i++
            if (i >= json.length) break

            when (json[i]) {
                '"' -> {
                    val parsedValue = parseJsonString(json, i)
                    map[key] = parsedValue.value
                    i = parsedValue.nextIndex
                }
                't' -> {
                    if (json.startsWith("true", i)) {
                        map[key] = true
                        i += 4
                    } else {
                        i++
                    }
                }
                'f' -> {
                    if (json.startsWith("false", i)) {
                        map[key] = false
                        i += 5
                    } else {
                        i++
                    }
                }
                'n' -> {
                    if (json.startsWith("null", i)) {
                        map[key] = null
                        i += 4
                    } else {
                        i++
                    }
                }
                else -> {
                    val startVal = i
                    while (i < json.length && json[i] != ',' && json[i] != '}') i++
                    map[key] = json.substring(startVal, i).trim()
                }
            }
        }
        return map
    }
}
