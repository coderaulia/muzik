package io.github.coderaulia.muzikplayer.audio

import io.github.coderaulia.muzikplayer.platform.SharedLyricsParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class BiquadEqualizerProcessorTest {

    @Test
    fun bypassesWhenDisabledOrAllGainsZero() {
        val processor = BiquadEqualizerProcessor(
            sampleRate = 48000.0,
            channels = 2,
            sampleSizeInBits = 16,
            bigEndian = false,
        )
        val pcm = ByteArray(64) { (it % 17).toByte() }
        assertNull(processor.processPcmBytes(pcm, 0, pcm.size, enabled = false, gains = List(10) { 6f }))
        assertNull(processor.processPcmBytes(pcm, 0, pcm.size, enabled = true, gains = List(10) { 0f }))
    }

    @Test
    fun filtersNonZeroSignalWhenEnabled() {
        val processor = BiquadEqualizerProcessor(
            sampleRate = 44100.0,
            channels = 2,
            sampleSizeInBits = 16,
            bigEndian = false,
        )
        val pcm = ByteArray(128) { idx -> if (idx % 4 == 0) 40 else 10 }
        val out = processor.processPcmBytes(
            data = pcm,
            offset = 0,
            length = pcm.size,
            enabled = true,
            gains = listOf(6f, 4f, 2f, 0f, -2f, 0f, 3f, 4f, 2f, 1f),
        )
        assertNotNull(out)
    }

    @Test
    fun parsesSynchronizedLrcLinesAndFormatsDuration() {
        val lrc = """
            [00:12.50]First line
            [01:05.00]Second line
        """.trimIndent()
        val lines = SharedLyricsParser.parseSynchronized(lrc)
        assertEquals(2, lines.size)
        assertEquals("First line", lines[0].text)
        assertEquals("Second line", lines[1].text)
        assertEquals("1:05", SharedLyricsParser.formatDuration(65.seconds))
    }
}
