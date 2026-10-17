package io.github.coderaulia.muzikplayer.android

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import io.github.coderaulia.muzikplayer.platform.MediaUri
import io.github.coderaulia.muzikplayer.platform.SharedPlaylist
import io.github.coderaulia.muzikplayer.platform.SharedTrack
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Scans Android `MediaStore.Audio.Media` and manages `.m3u` playlists in app storage.
 * Includes built-in showcase tracks if the device/emulator has no local audio files yet.
 */
object AndroidMediaScanner {

    fun scanDeviceLibrary(context: Context): List<SharedTrack> {
        val tracks = mutableListOf<SharedTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Title"
                    val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val rawTrackNum = cursor.getInt(trackCol)
                    val discNumber = if (rawTrackNum >= 1000) rawTrackNum / 1000 else 1
                    val trackNumber = if (rawTrackNum > 0) rawTrackNum % 1000 else null
                    val year = cursor.getInt(yearCol).takeIf { it > 1900 }
                    val mime = cursor.getString(mimeCol)
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val sidecarLyrics = readSidecarLrcIfAvailable(filePath)

                    tracks += SharedTrack(
                        id = id.toString(),
                        uri = MediaUri(contentUri.toString()),
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = durationMs.milliseconds,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        year = year,
                        mimeType = mime,
                        sampleRateHz = 48000,
                        bitrateKbps = if (mime?.contains("flac") == true) 1411 else 320,
                        folderPath = filePath.substringBeforeLast('/', ""),
                        lyricsRaw = sidecarLyrics,
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        } catch (_: Exception) {
        }

        return tracks.ifEmpty { fallbackShowcaseTracks() }
    }

    private fun readSidecarLrcIfAvailable(audioFilePath: String): String? {
        if (audioFilePath.isBlank()) return null
        return try {
            val audioFile = File(audioFilePath)
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) lrcFile.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun playlistsDir(context: Context): File =
        File(context.filesDir, "playlists").apply { mkdirs() }

    fun loadM3uPlaylists(context: Context): List<SharedPlaylist> {
        val dir = playlistsDir(context)
        val files = dir.listFiles { f -> f.extension.equals("m3u", ignoreCase = true) }.orEmpty()
        if (files.isEmpty()) {
            val defaultPlaylist = SharedPlaylist(
                id = "pl_favorites_mix",
                name = "Late Night Slate Mix",
                uri = MediaUri(File(dir, "Late Night Slate Mix.m3u").absolutePath),
                trackIds = listOf("demo_1", "demo_2"),
            )
            saveM3uPlaylist(context, defaultPlaylist)
            return listOf(defaultPlaylist)
        }
        return files.map { file ->
            val trackIds = file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
            SharedPlaylist(
                id = "pl_${file.nameWithoutExtension.lowercase().replace(' ', '_')}",
                name = file.nameWithoutExtension,
                uri = MediaUri(file.absolutePath),
                trackIds = trackIds,
            )
        }
    }

    fun saveM3uPlaylist(context: Context, playlist: SharedPlaylist) {
        val dir = playlistsDir(context)
        val file = File(dir, "${playlist.name}.m3u")
        val content = buildString {
            appendLine("#EXTM3U")
            playlist.trackIds.forEach { id ->
                appendLine(id)
            }
        }
        file.writeText(content)
    }

    fun deleteM3uPlaylist(context: Context, playlist: SharedPlaylist) {
        val dir = playlistsDir(context)
        File(dir, "${playlist.name}.m3u").delete()
    }

    private fun fallbackShowcaseTracks(): List<SharedTrack> = listOf(
        SharedTrack(
            id = "demo_1",
            uri = MediaUri("demo://midnight_slate.flac"),
            title = "Midnight Slate (Studio Master)",
            artist = "Aulia & The Synthetics",
            album = "Libadwaita Sessions",
            duration = 218.seconds,
            trackNumber = 1,
            discNumber = 1,
            year = 2026,
            mimeType = "audio/flac",
            bitrateKbps = 1411,
            sampleRateHz = 96000,
            lyricsRaw = """
                [00:00.00]♪ Instrumental Intro (96kHz Slate Master) ♪
                [00:12.00]City lights reflect across the rain-soaked glass
                [00:24.50]Every frequency aligned in crystal phase
                [00:38.00]Drifting through the midnight slate horizon
                [00:54.00]Ten bands of warmth beneath the bassline
            """.trimIndent(),
        ),
        SharedTrack(
            id = "demo_2",
            uri = MediaUri("demo://cobalt_pulse.flac"),
            title = "Cobalt Pulse",
            artist = "Aulia & The Synthetics",
            album = "Libadwaita Sessions",
            duration = 194.seconds,
            trackNumber = 2,
            discNumber = 1,
            year = 2026,
            mimeType = "audio/flac",
            bitrateKbps = 1411,
            sampleRateHz = 96000,
            lyricsRaw = """
                [00:00.00]♪ Sub-bass pulse @ 31Hz ♪
                [00:15.00]Signal locked inside the copper wire
                [00:30.00]Zero-gap transition into dawn
            """.trimIndent(),
        ),
        SharedTrack(
            id = "demo_3",
            uri = MediaUri("demo://emerald_echoes.flac"),
            title = "Emerald Echoes",
            artist = "Kora Vane",
            album = "Northern Resonances",
            duration = 252.seconds,
            trackNumber = 1,
            discNumber = 1,
            year = 2025,
            mimeType = "audio/flac",
            bitrateKbps = 1120,
            sampleRateHz = 48000,
        ),
        SharedTrack(
            id = "demo_4",
            uri = MediaUri("demo://amber_transients.mp3"),
            title = "Amber Transients",
            artist = "Kora Vane",
            album = "Northern Resonances",
            duration = 179.seconds,
            trackNumber = 2,
            discNumber = 1,
            year = 2025,
            mimeType = "audio/mpeg",
            bitrateKbps = 320,
            sampleRateHz = 44100,
        ),
    )
}
