package io.github.coderaulia.muzikplayer.data

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

data class CachedSongEntry(
    val path: String,
    val lastModifiedMs: Long,
    val fileSize: Long,
    val disk: Int?,
    val track: Int?,
    val lengthSeconds: Double,
    val formattedLength: String,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    val dateStr: String?,
    val lyricsRaw: String?,
    val hasArtwork: Boolean,
)

class PersistentLibraryCache(
    val cacheFile: Path = defaultCacheFile,
    val coversDir: Path = defaultCoversDir,
) {
    companion object {
        val defaultCacheDir: Path = Path.of(System.getProperty("user.home"), ".cache", "muzikplayer")
        val defaultCacheFile: Path = defaultCacheDir.resolve("library_cache.bin")
        val defaultCoversDir: Path = defaultCacheDir.resolve("covers")

        private val MAGIC = "MZKC".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1

        private fun hashPath(path: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(path.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    private val entries = ConcurrentHashMap<String, CachedSongEntry>()
    private val dirty = AtomicBoolean(false)

    val isDirty: Boolean get() = dirty.get()
    val size: Int get() = entries.size

    init {
        load()
    }

    fun get(path: Path): CachedSongEntry? {
        return entries[path.toAbsolutePath().normalize().pathString]
    }

    fun get(pathString: String): CachedSongEntry? {
        return entries[pathString]
    }

    fun get(path: Path, lastModifiedMs: Long, fileSize: Long): CachedSongEntry? {
        return get(path.toAbsolutePath().normalize().pathString, lastModifiedMs, fileSize)
    }

    fun get(pathString: String, lastModifiedMs: Long, fileSize: Long): CachedSongEntry? {
        val entry = entries[pathString] ?: return null
        if (entry.lastModifiedMs != lastModifiedMs || entry.fileSize != fileSize) {
            return null
        }
        return entry
    }

    fun put(entry: CachedSongEntry) {
        val old = entries.put(entry.path, entry)
        if (old != entry) {
            dirty.set(true)
        }
    }

    fun remove(path: Path) {
        val pathStr = path.toAbsolutePath().normalize().pathString
        val toRemove = entries.keys.filter { it == pathStr || it.startsWith(pathStr + java.io.File.separator) }
        for (key in toRemove) {
            remove(key)
        }
    }

    fun remove(pathString: String) {
        if (entries.remove(pathString) != null) {
            dirty.set(true)
            try {
                Files.deleteIfExists(coverPath(pathString))
            } catch (_: Exception) {}
        }
    }

    fun clear() {
        entries.clear()
        dirty.set(true)
    }

    fun coverPath(songPath: String): Path {
        return coversDir.resolve("${hashPath(songPath)}.img")
    }

    fun saveCover(songPath: String, artworkBytes: ByteArray) {
        try {
            Files.createDirectories(coversDir)
            val file = coverPath(songPath)
            val tmp = file.resolveSibling("${file.fileName}.tmp")
            Files.write(tmp, artworkBytes)
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save cover art for $songPath" }
        }
    }

    fun getCover(songPath: String): ByteArray? {
        val file = coverPath(songPath)
        return if (Files.isRegularFile(file)) {
            try {
                Files.readAllBytes(file)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to read cover art for $songPath" }
                null
            }
        } else {
            null
        }
    }

    @Synchronized
    fun load() {
        if (!cacheFile.exists()) return
        try {
            BufferedInputStream(Files.newInputStream(cacheFile)).use { bis ->
                DataInputStream(bis).use { dis ->
                    val magic = ByteArray(4)
                    dis.readFully(magic)
                    if (!magic.contentEquals(MAGIC)) {
                        logger.warn { "Corrupted cache header in $cacheFile" }
                        return
                    }
                    val version = dis.readInt()
                    if (version != VERSION) {
                        logger.warn { "Cache version mismatch: $version != $VERSION" }
                        return
                    }
                    val count = dis.readInt()
                    for (i in 0 until count) {
                        val path = dis.readString()
                        val lastModifiedMs = dis.readLong()
                        val fileSize = dis.readLong()
                        val disk = dis.readNullableInt()
                        val track = dis.readNullableInt()
                        val lengthSeconds = dis.readDouble()
                        val formattedLength = dis.readString()
                        val title = dis.readNullableString()
                        val album = dis.readNullableString()
                        val artist = dis.readNullableString()
                        val albumArtist = dis.readNullableString()
                        val dateStr = dis.readNullableString()
                        val lyricsRaw = dis.readNullableString()
                        val hasArtwork = dis.readBoolean()

                        val entry = CachedSongEntry(
                            path = path,
                            lastModifiedMs = lastModifiedMs,
                            fileSize = fileSize,
                            disk = disk,
                            track = track,
                            lengthSeconds = lengthSeconds,
                            formattedLength = formattedLength,
                            title = title,
                            album = album,
                            artist = artist,
                            albumArtist = albumArtist,
                            dateStr = dateStr,
                            lyricsRaw = lyricsRaw,
                            hasArtwork = hasArtwork,
                        )
                        entries[entry.path] = entry
                    }
                    dirty.set(false)
                    logger.debug { "Loaded ${entries.size} cached song entries from $cacheFile" }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load cache from $cacheFile, resetting" }
            entries.clear()
            dirty.set(true)
        }
    }

    @Synchronized
    fun save() {
        val parent = cacheFile.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        val tempFile = cacheFile.resolveSibling("${cacheFile.fileName}.tmp")
        try {
            BufferedOutputStream(Files.newOutputStream(tempFile)).use { bos ->
                DataOutputStream(bos).use { dos ->
                    dos.write(MAGIC)
                    dos.writeInt(VERSION)
                    val snapshot = entries.values.toList()
                    dos.writeInt(snapshot.size)
                    for (entry in snapshot) {
                        dos.writeString(entry.path)
                        dos.writeLong(entry.lastModifiedMs)
                        dos.writeLong(entry.fileSize)
                        dos.writeNullableInt(entry.disk)
                        dos.writeNullableInt(entry.track)
                        dos.writeDouble(entry.lengthSeconds)
                        dos.writeString(entry.formattedLength)
                        dos.writeNullableString(entry.title)
                        dos.writeNullableString(entry.album)
                        dos.writeNullableString(entry.artist)
                        dos.writeNullableString(entry.albumArtist)
                        dos.writeNullableString(entry.dateStr)
                        dos.writeNullableString(entry.lyricsRaw)
                        dos.writeBoolean(entry.hasArtwork)
                    }
                    dos.flush()
                }
            }
            try {
                Files.move(tempFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING)
            }
            dirty.set(false)
            logger.debug { "Saved ${entries.size} cached song entries to $cacheFile" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save library cache to $cacheFile" }
            try {
                Files.deleteIfExists(tempFile)
            } catch (_: Exception) {}
        }
    }
}

private fun DataOutputStream.writeString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}

private fun DataInputStream.readString(): String {
    val len = readInt()
    val bytes = ByteArray(len)
    readFully(bytes)
    return String(bytes, Charsets.UTF_8)
}

private fun DataOutputStream.writeNullableString(value: String?) {
    if (value == null) {
        writeBoolean(false)
    } else {
        writeBoolean(true)
        writeString(value)
    }
}

private fun DataInputStream.readNullableString(): String? {
    return if (readBoolean()) readString() else null
}

private fun DataOutputStream.writeNullableInt(value: Int?) {
    if (value == null) {
        writeBoolean(false)
    } else {
        writeBoolean(true)
        writeInt(value)
    }
}

private fun DataInputStream.readNullableInt(): Int? {
    return if (readBoolean()) readInt() else null
}
