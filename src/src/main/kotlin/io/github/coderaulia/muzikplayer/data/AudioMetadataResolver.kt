package io.github.coderaulia.muzikplayer.data

import org.jaudiotagger.audio.AudioFileIO
import java.nio.file.Path
import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension

data class SongAudioInfo(
    val format: String,
    val sampleRate: String,
    val sampleRateKHz: String,
    val bitDepth: String,
    val channels: String,
    val encoding: String,
    val bitRate: String,
    val isLossless: Boolean,
)

object AudioMetadataResolver {
    private val cache = ConcurrentHashMap<Path, SongAudioInfo>()

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
                SongAudioInfo(
                    format = ext,
                    sampleRate = rateStr,
                    sampleRateKHz = rateKHzStr,
                    bitDepth = bitsStr,
                    channels = chStr,
                    encoding = enc,
                    bitRate = brStr,
                    isLossless = h.isLossless,
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
                )
            }
        }
    }
}
