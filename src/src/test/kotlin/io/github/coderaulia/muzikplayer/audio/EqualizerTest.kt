package io.github.coderaulia.muzikplayer.audio

import io.github.coderaulia.muzikplayer.utils.Preferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import javax.sound.sampled.AudioFormat
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class EqualizerTest : FunSpec({
    val format = AudioFormat(44100f, 16, 2, true, false)

    fun sine(freq: Double, frames: Int): ByteArray {
        val out = ByteArray(frames * 4)
        for (i in 0 until frames) {
            val v = (sin(2 * PI * freq * i / 44100) * 8000).toInt()
            for (ch in 0..1) {
                out[i * 4 + ch * 2] = v.toByte()
                out[i * 4 + ch * 2 + 1] = (v shr 8).toByte()
            }
        }
        return out
    }

    fun rms(data: ByteArray, from: Int): Double {
        var sum = 0.0
        var n = 0
        var i = from
        while (i < data.size) {
            val v = (data[i].toInt() and 0xFF) or (data[i + 1].toInt() shl 8)
            sum += v.toDouble() * v
            n++
            i += 4
        }
        return sqrt(sum / n)
    }

    test("bypassed equalizer returns null, boost raises level at the band frequency") {
        val oldEnabled = Preferences.equalizerEnabled.get()
        val oldGains = Preferences.equalizerGains.get()
        try {
            val input = sine(1000.0, 44100)
            Preferences.equalizerEnabled.set(false)
            Equalizer(format).process(input, 0, input.size) shouldBe null

            Preferences.equalizerEnabled.set(true)
            Preferences.equalizerGains.set(List(10) { if (it == 5) 12f else 0f })
            val out = Equalizer(format).process(input, 0, input.size)!!
            // skip the first 1000 frames of filter settling
            (rms(out, 4000) / rms(input, 4000)) shouldBeGreaterThan 3.0
        } finally {
            Preferences.equalizerEnabled.set(oldEnabled)
            Preferences.equalizerGains.set(oldGains)
        }
    }
})
