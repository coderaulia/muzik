package io.github.coderaulia.muzikplayer.mpris

import io.github.coderaulia.muzikplayer.audio.PlayerController
import io.github.coderaulia.muzikplayer.data.SongKey
import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

object DiscordRpcClient {
    private const val CLIENT_ID = "1219986431951405116"

    private var channel: SocketChannel? = null
    private var lastSongKey: SongKey? = null
    private var lastPause: Boolean? = null
    private var lastPositionSec: Long = -1L

    fun encodeIpcFrame(opcode: Int, jsonPayload: String): ByteArray {
        val payloadBytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(8 + payloadBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(opcode)
        buffer.putInt(payloadBytes.size)
        buffer.put(payloadBytes)
        return buffer.array()
    }

    private fun escapeJson(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun buildSetActivityPayload(
        pid: Long,
        title: String?,
        artist: String?,
        album: String?,
        isPaused: Boolean,
        positionSeconds: Long,
        durationSeconds: Long,
    ): String {
        val nonce = UUID.randomUUID().toString()
        if (title == null || artist == null) {
            return """{"cmd":"SET_ACTIVITY","args":{"pid":$pid,"activity":null},"nonce":"$nonce"}"""
        }

        val escapedTitle = escapeJson(title)
        val escapedArtist = escapeJson(artist)
        val escapedAlbum = escapeJson(album ?: "")
        val smallImage = if (isPaused) "paused" else "playing"
        val smallText = if (isPaused) "Paused" else "Playing"

        val timestampsPart = if (!isPaused && durationSeconds > 0) {
            val nowSec = System.currentTimeMillis() / 1000
            val start = (nowSec - positionSeconds).coerceAtLeast(0)
            val end = start + durationSeconds
            ""","timestamps":{"start":$start,"end":$end}"""
        } else {
            ""
        }

        return """{"cmd":"SET_ACTIVITY","args":{"pid":$pid,"activity":{"type":2,"details":"$escapedTitle","state":"$escapedArtist"$timestampsPart,"assets":{"large_image":"muzikplayer","large_text":"$escapedAlbum","small_image":"$smallImage","small_text":"$smallText"}}},"nonce":"$nonce"}"""
    }

    private fun findDiscordSocket(): Path? {
        val candidates = mutableListOf<Path>()
        val xdg = System.getenv("XDG_RUNTIME_DIR")
        if (!xdg.isNullOrBlank()) {
            for (i in 0..9) {
                candidates.add(Path.of(xdg, "discord-ipc-$i"))
            }
        }
        val tmp = System.getenv("TMPDIR") ?: "/tmp"
        for (i in 0..9) {
            candidates.add(Path.of(tmp, "discord-ipc-$i"))
        }
        if (!xdg.isNullOrBlank()) {
            for (i in 0..9) {
                candidates.add(Path.of(xdg, "app/com.discordapp.Discord/discord-ipc-$i"))
            }
        }
        return candidates.firstOrNull { Files.exists(it) }
    }

    private fun connect(): Boolean {
        close()
        val socketPath = findDiscordSocket() ?: return false
        return try {
            val address = UnixDomainSocketAddress.of(socketPath)
            val ch = SocketChannel.open(StandardProtocolFamily.UNIX)
            ch.connect(address)
            val handshakeJson = """{"v":1,"client_id":"$CLIENT_ID"}"""
            val frame = encodeIpcFrame(0, handshakeJson)
            ch.write(ByteBuffer.wrap(frame))
            channel = ch
            true
        } catch (e: Exception) {
            logger.debug { "Failed to connect to Discord IPC socket: ${e.message}" }
            close()
            false
        }
    }

    @Synchronized
    fun onPlaybackStateChanged(state: PlayerController.State, position: Duration) {
        if (!Preferences.discordRpc.get()) {
            close()
            return
        }

        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val currentKey = state.currentlyPlaying?.queue?.currentSongKey
        val isPaused = state.pause
        val posSec = position.inWholeSeconds

        val songChanged = currentKey != lastSongKey
        val pauseChanged = isPaused != lastPause
        val seekOccurred = !songChanged && !isPaused && kotlin.math.abs(posSec - (lastPositionSec + 1)) > 3

        if (!songChanged && !pauseChanged && !seekOccurred) {
            lastPositionSec = posSec
            return
        }

        lastSongKey = currentKey
        lastPause = isPaused
        lastPositionSec = posSec

        if (channel == null || !channel!!.isOpen) {
            if (!connect()) return
        }

        val pid = ProcessHandle.current().pid()
        val payload = if (currentSong == null) {
            buildSetActivityPayload(pid, null, null, null, false, 0, 0)
        } else {
            buildSetActivityPayload(
                pid = pid,
                title = currentSong.title,
                artist = currentSong.artist.name,
                album = currentSong.album.title,
                isPaused = isPaused,
                positionSeconds = posSec,
                durationSeconds = currentSong.length.inWholeSeconds
            )
        }

        try {
            val frame = encodeIpcFrame(1, payload)
            channel?.write(ByteBuffer.wrap(frame))
        } catch (e: Exception) {
            logger.debug { "Failed to send activity to Discord: ${e.message}" }
            close()
        }
    }

    @Synchronized
    fun close() {
        try {
            channel?.close()
        } catch (_: Exception) {}
        channel = null
        lastSongKey = null
        lastPause = null
        lastPositionSec = -1L
    }
}
