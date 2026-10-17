package io.github.coderaulia.muzikplayer.platform

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shared multiplatform track, album, artist, playlist, and queue models
 * used across Desktop and Android UIs for 1:1 feature parity.
 */
data class SharedTrack(
    val id: String,
    val uri: MediaUri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0L,
    val duration: Duration,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val mimeType: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val folderPath: String = "",
    val lyricsRaw: String? = null,
)

data class SharedAlbum(
    val name: String,
    val artist: String,
    val year: Int?,
    val tracks: List<SharedTrack>,
) {
    val totalDuration: Duration
        get() = tracks.fold(Duration.ZERO) { acc, t -> acc + t.duration }
}

data class SharedArtist(
    val name: String,
    val albums: List<SharedAlbum>,
    val tracks: List<SharedTrack>,
)

data class SharedPlaylist(
    val id: String,
    val name: String,
    val uri: MediaUri?,
    val trackIds: List<String>,
)

enum class SharedRepeatMode {
    OFF,
    ALL,
    ONE,
}

data class SharedLyricsLine(
    val start: Duration,
    val text: String,
)

/**
 * Lightweight multiplatform LRC & plain lyrics parser for shared UI rendering.
 */
object SharedLyricsParser {
    private val lineRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")

    fun parseSynchronized(raw: String?, offsetMs: Int = 0): List<SharedLyricsLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val parsed = mutableListOf<SharedLyricsLine>()
        for (line in raw.lineSequence()) {
            val match = lineRegex.matchEntire(line.trim()) ?: continue
            val mm = match.groupValues[1].toLongOrNull() ?: continue
            val ss = match.groupValues[2].toLongOrNull() ?: continue
            val fracStr = match.groupValues[3]
            val fracMs = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> (fracStr.toLongOrNull() ?: 0L)
                else -> 0L
            }
            val text = match.groupValues[4].trim()
            val totalMs = (mm * 60_000L + ss * 1_000L + fracMs - offsetMs).coerceAtLeast(0L)
            parsed += SharedLyricsLine(start = totalMs.milliseconds, text = text)
        }
        return parsed.sortedBy { it.start }
    }

    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }
}
