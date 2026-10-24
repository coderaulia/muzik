package io.github.coderaulia.muzikplayer.audio

import io.github.coderaulia.muzikplayer.data.AudioMetadataResolver
import io.github.coderaulia.muzikplayer.data.SongAudioInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class ReplayGainAndSleepTimerTest : FunSpec({

    fun createInfo(
        trackGain: Float? = null,
        albumGain: Float? = null,
        trackPeak: Float? = null,
        albumPeak: Float? = null,
        isLossless: Boolean = true,
        isHiRes: Boolean = false,
    ) = SongAudioInfo(
        format = "FLAC",
        sampleRate = "44,100 Hz",
        sampleRateKHz = "44.1 kHz",
        bitDepth = "16-bit",
        channels = "2 (Stereo L/R)",
        encoding = "Lossless FLAC",
        bitRate = "VBR Lossless",
        isLossless = isLossless,
        trackGainDb = trackGain,
        albumGainDb = albumGain,
        trackPeak = trackPeak,
        albumPeak = albumPeak,
        isHiRes = isHiRes,
    )

    fun approx(a: Float, b: Float, tol: Float = 0.002f): Boolean = abs(a - b) <= tol

    test("AudioMetadataResolver.parseGainDb handles standard ReplayGain strings") {
        AudioMetadataResolver.parseGainDb("-6.42 dB") shouldBe -6.42f
        AudioMetadataResolver.parseGainDb("+1.2 dB") shouldBe 1.2f
        AudioMetadataResolver.parseGainDb("-3.50") shouldBe -3.50f
        AudioMetadataResolver.parseGainDb("+0.50 dB") shouldBe 0.50f
        AudioMetadataResolver.parseGainDb("-12.5 LUFS") shouldBe -12.5f
        AudioMetadataResolver.parseGainDb("  -2.1 dB  ") shouldBe -2.1f
        AudioMetadataResolver.parseGainDb("invalid") shouldBe null
        AudioMetadataResolver.parseGainDb("") shouldBe null
        AudioMetadataResolver.parseGainDb(null) shouldBe null
    }

    test("AudioMetadataResolver.computeReplayGainScale returns 1.0 when disabled or tags missing") {
        val info = createInfo(trackGain = -6.0f, albumGain = -4.0f)
        AudioMetadataResolver.computeReplayGainScale("Disabled", info, peakProtection = true) shouldBe 1.0f
        AudioMetadataResolver.computeReplayGainScale("Off", info, peakProtection = true) shouldBe 1.0f
        AudioMetadataResolver.computeReplayGainScale("", info, peakProtection = true) shouldBe 1.0f

        val untaggedInfo = createInfo(trackGain = null, albumGain = null)
        AudioMetadataResolver.computeReplayGainScale("Track Gain", untaggedInfo, peakProtection = true) shouldBe 1.0f
        AudioMetadataResolver.computeReplayGainScale("Album Gain", null, peakProtection = true) shouldBe 1.0f
    }

    test("AudioMetadataResolver.computeReplayGainScale selects Track Gain vs Album Gain correctly") {
        val info = createInfo(trackGain = -6.02f, albumGain = -2.0f) // -6.02 dB ~= 0.5x, -2.0 dB ~= 0.794x
        val trackScale = AudioMetadataResolver.computeReplayGainScale("Track Gain (-1.4 dB target)", info, peakProtection = false)
        approx(trackScale, 0.50f) shouldBe true

        val albumScale = AudioMetadataResolver.computeReplayGainScale("Album Gain (-1.4 dB target)", info, peakProtection = false)
        approx(albumScale, 0.794f) shouldBe true
    }

    test("AudioMetadataResolver.computeReplayGainScale falls back between track and album tags if one is missing") {
        val trackOnly = createInfo(trackGain = -6.02f, albumGain = null)
        val albumModeFallback = AudioMetadataResolver.computeReplayGainScale("Album Gain", trackOnly, peakProtection = false)
        approx(albumModeFallback, 0.50f) shouldBe true

        val albumOnly = createInfo(trackGain = null, albumGain = -6.02f)
        val trackModeFallback = AudioMetadataResolver.computeReplayGainScale("Track Gain", albumOnly, peakProtection = false)
        approx(trackModeFallback, 0.50f) shouldBe true
    }

    test("AudioMetadataResolver.computeReplayGainScale clamps boost when peak protection is active") {
        // +6.02 dB corresponds to 2.0x linear gain.
        // With peak = 0.8, linear * peak = 1.6 > 1.0 -> clamped to 1.0 / 0.8 = 1.25.
        val boostedInfo = createInfo(trackGain = 6.02f, trackPeak = 0.8f)

        val withProtection = AudioMetadataResolver.computeReplayGainScale("Track Gain", boostedInfo, peakProtection = true)
        approx(withProtection, 1.25f) shouldBe true

        val withoutProtection = AudioMetadataResolver.computeReplayGainScale("Track Gain", boostedInfo, peakProtection = false)
        approx(withoutProtection, 2.0f) shouldBe true
    }

    test("SleepTimerController.computeFadeMultiplier ramps linearly during the fade window") {
        SleepTimerController.computeFadeMultiplier(45.seconds, 30.seconds) shouldBe 1.0f
        SleepTimerController.computeFadeMultiplier(30.seconds, 30.seconds) shouldBe 1.0f
        SleepTimerController.computeFadeMultiplier(15.seconds, 30.seconds) shouldBe 0.5f
        SleepTimerController.computeFadeMultiplier(0.seconds, 30.seconds) shouldBe 0.0f
        SleepTimerController.computeFadeMultiplier((-5).seconds, 30.seconds) shouldBe 0.0f
    }
})
