package io.github.coderaulia.muzikplayer.android

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import io.github.coderaulia.muzikplayer.audio.BiquadEqualizerProcessor
import io.github.coderaulia.muzikplayer.audio.SHARED_EQ_BANDS
import io.github.coderaulia.muzikplayer.platform.SharedTrack
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Media3 [AudioProcessor] that runs the shared 10-band [BiquadEqualizerProcessor]
 * on 16-bit PCM audio frames decoded by ExoPlayer on Android.
 */
@UnstableApi
class AndroidEqualizerAudioProcessor : AudioProcessor {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var gainsDb: List<Float> = List(SHARED_EQ_BANDS.size) { 0f }

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var processor: BiquadEqualizerProcessor? = null
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded: Boolean = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioFormat.NOT_SET
            this.outputAudioFormat = AudioFormat.NOT_SET
            return AudioFormat.NOT_SET
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.processor = BiquadEqualizerProcessor(
            sampleRate = inputAudioFormat.sampleRate.toDouble(),
            channels = inputAudioFormat.channelCount,
            sampleSizeInBits = 16,
            bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN,
        )
        return outputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        val inputBytes = ByteArray(remaining)
        inputBuffer.get(inputBytes)

        val dsp = processor
        val processed = if (enabled && dsp != null) {
            dsp.processPcmBytes(
                data = inputBytes,
                offset = 0,
                length = remaining,
                enabled = enabled,
                gains = gainsDb,
            )
        } else {
            null
        }

        buffer.put(processed ?: inputBytes, 0, remaining)
        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        processor?.resetState()
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        processor = null
    }
}

/**
 * Android playback wrapper around Media3 [ExoPlayer] + [AndroidEqualizerAudioProcessor].
 */
@OptIn(UnstableApi::class)
class AndroidAudioEngine(
    context: Context,
    private val onTrackEnded: () -> Unit,
) {
    val eqProcessor = AndroidEqualizerAudioProcessor()

    val exoPlayer: ExoPlayer = run {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(eqProcessor))
                    .build()
            }
        }
        ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onTrackEnded()
                        }
                    }
                })
            }
    }

    fun playTrack(track: SharedTrack, startPosition: Duration = Duration.ZERO) {
        if (track.uri.value.startsWith("demo://")) {
            return
        }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(track.uri.value))
            .setMediaId(track.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()
        exoPlayer.setMediaItem(mediaItem, startPosition.inWholeMilliseconds)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setPlaying(playing: Boolean) {
        if (playing) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    fun seekTo(position: Duration) {
        exoPlayer.seekTo(position.inWholeMilliseconds)
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun updateEqualizer(enabled: Boolean, gainsDb: List<Float>) {
        eqProcessor.enabled = enabled
        eqProcessor.gainsDb = gainsDb
    }

    fun currentPosition(): Duration = exoPlayer.currentPosition.coerceAtLeast(0L).milliseconds

    fun release() {
        exoPlayer.release()
    }
}
