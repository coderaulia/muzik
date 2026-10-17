package io.github.coderaulia.muzikplayer.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Center frequencies (Hz) of the 10-band graphic equalizer. */
val SHARED_EQ_BANDS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
const val SHARED_EQ_MAX_GAIN_DB = 12f

data class EqualizerPreset(
    val name: String,
    val gainsDb: List<Float>,
)

val DEFAULT_EQ_PRESETS = listOf(
    EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("Bass Boost", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("Vocal / Podcast", listOf(-2f, -2f, -1f, 2f, 4f, 5f, 3f, 1f, 0f, -1f)),
    EqualizerPreset("Electronic", listOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 2f, 4f, 5f)),
    EqualizerPreset("Acoustic", listOf(4f, 3f, 2f, 1f, 2f, 2f, 3f, 4f, 3f, 2f)),
    EqualizerPreset("Rock", listOf(5f, 3f, -1f, -2f, -1f, 2f, 4f, 5f, 5f, 5f)),
)

/**
 * Pure multiplatform 10-band biquad peaking equalizer engine.
 * Shared across Desktop (`javax.sound.sampled`) and Android (`Media3 AudioProcessor`).
 */
class BiquadEqualizerProcessor(
    val sampleRate: Double,
    val channels: Int,
    val sampleSizeInBits: Int,
    val bigEndian: Boolean,
) {
    private val bytesPerSample = sampleSizeInBits / 8
    private val frameSize = channels * bytesPerSample
    val isSupported: Boolean = sampleSizeInBits in setOf(16, 24, 32) && channels > 0

    // Per band: b0, b1, b2, a1, a2 (normalized)
    private val coefficients = Array(SHARED_EQ_BANDS.size) { DoubleArray(5) }
    // Per channel, per band: transposed direct form II state
    private val state = Array(channels.coerceAtLeast(1)) { Array(SHARED_EQ_BANDS.size) { DoubleArray(2) } }
    private var appliedGains: List<Float>? = null
    private var active = false
    private var scratch = ByteArray(0)

    fun resetState() {
        for (ch in state.indices) {
            for (band in state[ch].indices) {
                state[ch][band][0] = 0.0
                state[ch][band][1] = 0.0
            }
        }
    }

    fun updateConfiguration(enabled: Boolean, gains: List<Float>) {
        if (gains == appliedGains && enabled == active) return
        appliedGains = gains
        active = enabled && gains.any { it != 0f }
        for (band in SHARED_EQ_BANDS.indices) {
            val f0 = SHARED_EQ_BANDS[band].toDouble()
            if (f0 >= sampleRate / 2.0) {
                coefficients[band] = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)
                continue
            }
            val gainDb = gains.getOrElse(band) { 0f }
                .coerceIn(-SHARED_EQ_MAX_GAIN_DB, SHARED_EQ_MAX_GAIN_DB)
                .toDouble()
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2 * PI * f0 / sampleRate
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

    fun processPcmBytes(
        data: ByteArray,
        offset: Int,
        length: Int,
        enabled: Boolean,
        gains: List<Float>,
    ): ByteArray? {
        if (!isSupported) return null
        updateConfiguration(enabled, gains)
        if (!active) return null
        if (scratch.size < length) scratch = ByteArray(length)
        val out = scratch
        val full = (1L shl (sampleSizeInBits - 1)).toDouble()
        val frames = length / frameSize
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val pos = offset + (frame * channels + ch) * bytesPerSample
                var x = readSample(data, pos) / full
                for (band in SHARED_EQ_BANDS.indices) {
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
        val processedLength = frames * frameSize
        if (processedLength < length) {
            data.copyInto(
                destination = out,
                destinationOffset = processedLength,
                startIndex = offset + processedLength,
                endIndex = offset + length,
            )
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
