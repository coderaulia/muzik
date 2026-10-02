package io.github.coderaulia.muzikplayer.audio

import io.github.coderaulia.muzikplayer.utils.Preferences
import javax.sound.sampled.AudioFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Center frequencies (Hz) of the graphic equalizer bands. */
val EQ_BANDS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
const val EQ_MAX_GAIN_DB = 12f

/**
 * 10-band peaking equalizer applied to signed PCM before it reaches the output line.
 * Settings are read from [Preferences] so changes apply live. One instance per [Player].
 */
class Equalizer(private val format: AudioFormat) {
    private val channels = format.channels
    private val bytesPerSample = format.sampleSizeInBits / 8
    private val bigEndian = format.isBigEndian
    private val supported = format.encoding == AudioFormat.Encoding.PCM_SIGNED &&
            format.sampleSizeInBits in setOf(16, 24, 32) &&
            format.frameSize == channels * bytesPerSample

    // Per band: b0, b1, b2, a1, a2 (normalized)
    private val coefficients = Array(EQ_BANDS.size) { DoubleArray(5) }
    // Per channel, per band: transposed direct form II state
    private val state = Array(channels) { Array(EQ_BANDS.size) { DoubleArray(2) } }
    private var appliedGains: List<Float>? = null
    private var active = false
    private var scratch = ByteArray(0)

    private fun refresh() {
        val enabled = Preferences.equalizerEnabled.get()
        val gains = Preferences.equalizerGains.get()
        if (gains === appliedGains && enabled == active) return
        appliedGains = gains
        active = enabled && gains.any { it != 0f }
        for (band in EQ_BANDS.indices) {
            val f0 = EQ_BANDS[band].toDouble()
            if (f0 >= format.sampleRate / 2.0) {
                coefficients[band] = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)
                continue
            }
            val gainDb = gains.getOrElse(band) { 0f }.coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB).toDouble()
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2 * PI * f0 / format.sampleRate
            val alpha = sin(w0) / (2 * Q)
            val a0 = 1 + alpha / a
            coefficients[band] = doubleArrayOf(
                (1 + alpha * a) / a0,
                -2 * cos(w0) / a0,
                (1 - alpha * a) / a0,
                -2 * cos(w0) / a0,
                (1 - alpha / a) / a0,
            )
        }
    }

    /** Returns a processed copy of the data, or null when the equalizer is bypassed. */
    fun process(data: ByteArray, offset: Int, length: Int): ByteArray? {
        if (!supported) return null
        refresh()
        if (!active) return null
        if (scratch.size < length) scratch = ByteArray(length)
        val out = scratch
        val full = (1L shl (format.sampleSizeInBits - 1)).toDouble()
        val frames = length / format.frameSize
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val pos = offset + (frame * channels + ch) * bytesPerSample
                var x = readSample(data, pos) / full
                for (band in EQ_BANDS.indices) {
                    val c = coefficients[band]
                    val z = state[ch][band]
                    val y = c[0] * x + z[0]
                    z[0] = c[1] * x - c[3] * y + z[1]
                    z[1] = c[2] * x - c[4] * y
                    x = y
                }
                val scaled = (x * full).roundToInt().coerceIn(-full.toInt(), (full - 1).toInt())
                writeSample(out, (frame * channels + ch) * bytesPerSample, scaled)
            }
        }
        val processedLength = frames * format.frameSize
        if (processedLength < length) {
            System.arraycopy(data, offset + processedLength, out, processedLength, length - processedLength)
        }
        return out
    }

    private fun readSample(data: ByteArray, pos: Int): Int {
        var v = 0
        for (i in 0 until bytesPerSample) {
            val b = data[pos + if (bigEndian) i else bytesPerSample - 1 - i].toInt()
            v = if (i == 0) b else (v shl 8) or (b and 0xFF)
        }
        return v
    }

    private fun writeSample(out: ByteArray, pos: Int, value: Int) {
        for (i in 0 until bytesPerSample) {
            val shift = 8 * (bytesPerSample - 1 - i)
            out[pos + if (bigEndian) i else bytesPerSample - 1 - i] = (value shr shift).toByte()
        }
    }

    private companion object {
        val Q = sqrt(2.0)
    }
}
