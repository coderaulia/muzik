package io.github.coderaulia.muzikplayer.audio

import io.github.coderaulia.muzikplayer.utils.Preferences
import javax.sound.sampled.AudioFormat

/** Center frequencies (Hz) of the graphic equalizer bands. */
val EQ_BANDS = SHARED_EQ_BANDS
const val EQ_MAX_GAIN_DB = SHARED_EQ_MAX_GAIN_DB

/**
 * 10-band peaking equalizer applied to signed PCM before it reaches the output line.
 * Delegates DSP filtering to the multiplatform [BiquadEqualizerProcessor] in `:shared`.
 */
class Equalizer(private val format: AudioFormat) {
    private val channels = format.channels
    private val bytesPerSample = format.sampleSizeInBits / 8
    private val supported = format.encoding == AudioFormat.Encoding.PCM_SIGNED &&
            format.sampleSizeInBits in setOf(16, 24, 32) &&
            format.frameSize == channels * bytesPerSample

    private val processor = BiquadEqualizerProcessor(
        sampleRate = format.sampleRate.toDouble(),
        channels = channels,
        sampleSizeInBits = format.sampleSizeInBits,
        bigEndian = format.isBigEndian,
    )

    /** Returns a processed copy of the data, or null when the equalizer is bypassed. */
    fun process(data: ByteArray, offset: Int, length: Int): ByteArray? {
        if (!supported) return null
        return processor.processPcmBytes(
            data = data,
            offset = offset,
            length = length,
            enabled = Preferences.equalizerEnabled.get(),
            gains = Preferences.equalizerGains.get(),
        )
    }
}

