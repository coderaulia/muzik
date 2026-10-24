package io.github.coderaulia.muzikplayer.data

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.Tag
import java.nio.file.Path
import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.math.pow

data class SongAudioInfo(
    val format: String,
    val sampleRate: String,
    val sampleRateKHz: String,
    val bitDepth: String,
    val channels: String,
    val encoding: String,
    val bitRate: String,
    val isLossless: Boolean,
    val trackGainDb: Float? = null,
    val albumGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumPeak: Float? = null,
    val isHiRes: Boolean = false,
)

object AudioMetadataResolver {
    private val cache = ConcurrentHashMap<Path, SongAudioInfo>()

    fun parseGainDb(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace("dB", "", ignoreCase = true)
            .replace("LUFS", "", ignoreCase = true)
            .trim()
        val normalized = if (cleaned.startsWith("+")) cleaned.substring(1).trim() else cleaned
        return normalized.toFloatOrNull()
    }

    private fun getTagValue(tag: Tag?, vararg keys: String): String? {
        if (tag == null) return null
        for (key in keys) {
            val v = try { tag.getFirst(key) } catch (_: Exception) { null }
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    fun computeReplayGainScale(
        replayGainMode: String,
        info: SongAudioInfo?,
        peakProtection: Boolean,
    ): Float {
        if (replayGainMode.isBlank() ||
            replayGainMode.equals("Disabled", ignoreCase = true) ||
            replayGainMode.equals("Off", ignoreCase = true)
        ) {
            return 1.0f
        }
        if (info == null || (info.albumGainDb == null && info.trackGainDb == null)) {
            return 1.0f
        }

        val isAlbum = replayGainMode.contains("Album", ignoreCase = true)
        val tagDb = (if (isAlbum) info.albumGainDb ?: info.trackGainDb else info.trackGainDb ?: info.albumGainDb) ?: 0f

        var linear = 10f.pow(tagDb / 20f)
        if (peakProtection) {
            val peak = (if (isAlbum) info.albumPeak ?: info.trackPeak else info.trackPeak ?: info.albumPeak) ?: 1.0f
            if (peak > 0f && linear * peak > 1.0f) {
                linear = 1.0f / peak
            }
        }
        return linear.coerceIn(0.05f, 4.0f)
    }

    fun resolve(path: Path): SongAudioInfo {
        return cache.computeIfAbsent(path) { p ->
            val ext = p.extension.uppercase()
            try {
                val f = AudioFileIO.read(p.toFile())
                val h = f.audioHeader
                val rate = h.sampleRateAsNumber
                val rateStr = if (rate > 0) "${NumberFormat.getIntegerInstance().format(rate)} Hz" else "${h.sampleRate} Hz"
                val rateKHzStr = if (rate > 0) {
                    val khz = rate / 1000.0
                    if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else "%.1f kHz".format(khz)
                } else "${h.sampleRate} Hz"
                val bits = h.bitsPerSample
                val bitsStr = if (bits > 0) "$bits-bit" else "16-bit"
                val ch = h.channels
                val chStr = when (ch) {
                    "1" -> "1 (Mono)"
                    "2" -> "2 (Stereo L/R)"
                    else -> if (ch.isNullOrEmpty()) "2 (Stereo)" else "$ch Channels"
                }
                val enc = if (h.isLossless) "Lossless ${h.format}" else h.format
                val br = h.bitRate
                val brStr = if (br.isNotEmpty() && br != "0") "$br kbps" else if (h.isLossless) "VBR Lossless" else "Standard"

                val tag = try { f.tag } catch (_: Exception) { null }
                val trackGain = parseGainDb(
                    getTagValue(tag, "REPLAYGAIN_TRACK_GAIN", "replaygain_track_gain", "R128_TRACK_GAIN")
                )
                val albumGain = parseGainDb(
                    getTagValue(tag, "REPLAYGAIN_ALBUM_GAIN", "replaygain_album_gain", "R128_ALBUM_GAIN")
                )
                val trackPeak = getTagValue(tag, "REPLAYGAIN_TRACK_PEAK", "replaygain_track_peak")?.trim()?.toFloatOrNull()
                val albumPeak = getTagValue(tag, "REPLAYGAIN_ALBUM_PEAK", "replaygain_album_peak")?.trim()?.toFloatOrNull()

                val isHiRes = h.isLossless && (rate >= 88200 || bits > 16)

                SongAudioInfo(
                    format = ext,
                    sampleRate = rateStr,
                    sampleRateKHz = rateKHzStr,
                    bitDepth = bitsStr,
                    channels = chStr,
                    encoding = enc,
                    bitRate = brStr,
                    isLossless = h.isLossless,
                    trackGainDb = trackGain,
                    albumGainDb = albumGain,
                    trackPeak = trackPeak,
                    albumPeak = albumPeak,
                    isHiRes = isHiRes,
                )
            } catch (_: Exception) {
                val isLossless = ext in setOf("FLAC", "WAV", "ALAC", "APE", "AIFF")
                SongAudioInfo(
                    format = ext,
                    sampleRate = "44,100 Hz",
                    sampleRateKHz = "44.1 kHz",
                    bitDepth = "16-bit",
                    channels = "2 (Stereo L/R)",
                    encoding = if (isLossless) "Lossless $ext" else "$ext Audio",
                    bitRate = if (isLossless) "Lossless" else "Compressed",
                    isLossless = isLossless,
                    trackGainDb = null,
                    albumGainDb = null,
                    trackPeak = null,
                    albumPeak = null,
                    isHiRes = false,
                )
            }
        }
    }
}
