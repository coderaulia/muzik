package io.github.coderaulia.muzikplayer.mpris

import io.github.coderaulia.muzikplayer.data.SongKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.mpris.MediaPlayer2.LoopStatus
import org.mpris.MediaPlayer2.PlaybackStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MprisAndIntegrationTest : FunSpec({

    test("SongKey.mprisTrackId() contains only valid D-Bus object path characters and no minus sign") {
        val testPaths = listOf(
            "/music/test-negative-hash.flac",
            "/home/user/Music/track-01.mp3",
            "/var/media/some_artist/some_album/song.ogg",
            "/tmp/nested/dir/with-hyphens/song.flac",
            "relative/path/test.wav",
        )

        for (pathStr in testPaths) {
            val key = SongKey(Path.of(pathStr))
            val trackId = key.mprisTrackId().path

            trackId.matches(Regex("^/[A-Za-z0-9_/]+$")) shouldBe true
            trackId shouldNotContain "-"
            trackId shouldContain "/io/github/coderaulia/MuzikPlayer/track_"
        }
    }

    test("MPRISPlayerState.diff excludes Position from PropertiesChanged per MPRIS 2.2 spec") {
        val baseState = MPRISPlayerState(
            playbackStatus = PlaybackStatus.Playing,
            loopStatus = LoopStatus.None,
            rate = 1.0,
            shuffle = false,
            metadata = null,
            volume = 1.0,
            position = Duration.ZERO,
            minimumRate = 1.0,
            maximumRate = 1.0,
            canGoNext = true,
            canGoPrevious = true,
            canPlay = true,
            canPause = true,
            canSeek = true,
            canControl = true,
        )

        // Only position changes: diff must be null because Position is never sent in PropertiesChanged
        val positionChangedState = baseState.copy(position = 10.seconds)
        val positionDiff = positionChangedState.diff(baseState, skipPosition = false)
        positionDiff shouldBe null

        // Volume and position change: diff must contain Volume but exclude Position
        val volumeChangedState = baseState.copy(volume = 0.5, position = 12.seconds)
        val volumeDiff = volumeChangedState.diff(baseState, skipPosition = false)
        volumeDiff?.propertiesChanged?.containsKey("Position") shouldBe false
        volumeDiff?.propertiesChanged?.containsKey("Volume") shouldBe true
    }

    test("ListenBrainzScrobbler.shouldScrobble implements 30s min, 50% rule, and 4-minute cap") {
        // Tracks under 30 seconds must never scrobble
        ListenBrainzScrobbler.shouldScrobble(29.seconds, 29.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(15.seconds, 15.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(0.seconds, 0.seconds) shouldBe false

        // 50% rule for tracks under 8 minutes (480s)
        ListenBrainzScrobbler.shouldScrobble(30.seconds, 14.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(30.seconds, 15.seconds) shouldBe true
        ListenBrainzScrobbler.shouldScrobble(100.seconds, 49.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(100.seconds, 50.seconds) shouldBe true
        ListenBrainzScrobbler.shouldScrobble(240.seconds, 119.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(240.seconds, 120.seconds) shouldBe true

        // 4-minute (240s) cap rule for tracks >= 8 minutes (480s)
        ListenBrainzScrobbler.shouldScrobble(600.seconds, 239.seconds) shouldBe false
        ListenBrainzScrobbler.shouldScrobble(600.seconds, 240.seconds) shouldBe true
        ListenBrainzScrobbler.shouldScrobble(900.seconds, 240.seconds) shouldBe true
        ListenBrainzScrobbler.shouldScrobble(1200.seconds, 300.seconds) shouldBe true
    }

    test("ListenBrainzScrobbler.buildListenPayload escapes JSON and structures payload properly") {
        val playingNow = ListenBrainzScrobbler.buildListenPayload(
            listenType = "playing_now",
            title = "Track \"Quotes\" & \\Slash\\",
            artist = "Artist Name",
            album = "Album Title",
            durationSeconds = 180,
            listenedAtEpochSeconds = null,
        )

        playingNow shouldContain """"listen_type":"playing_now""""
        playingNow shouldContain """"track_name":"Track \"Quotes\" & \\Slash\\""""
        playingNow shouldContain """"artist_name":"Artist Name""""
        playingNow shouldContain """"release_name":"Album Title""""
        playingNow shouldContain """"duration":180"""
        playingNow shouldNotContain """"listened_at""""

        val single = ListenBrainzScrobbler.buildListenPayload(
            listenType = "single",
            title = "Simple Track",
            artist = "Simple Artist",
            album = null,
            durationSeconds = null,
            listenedAtEpochSeconds = 1700000000L,
        )

        single shouldContain """"listen_type":"single""""
        single shouldContain """"listened_at":1700000000"""
        single shouldContain """"track_name":"Simple Track""""
        single shouldContain """"artist_name":"Simple Artist""""
        single shouldNotContain """"release_name""""
        single shouldNotContain """"duration""""
    }

    test("DiscordRpcClient.encodeIpcFrame encodes 8-byte little-endian header correctly") {
        val payload = """{"cmd":"SET_ACTIVITY","args":{}}"""
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val frame = DiscordRpcClient.encodeIpcFrame(1, payload)

        frame.size shouldBe 8 + payloadBytes.size

        val buffer = ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN)
        val opcode = buffer.getInt()
        val length = buffer.getInt()

        opcode shouldBe 1
        length shouldBe payloadBytes.size

        val readPayload = ByteArray(length)
        buffer.get(readPayload)
        String(readPayload, Charsets.UTF_8) shouldBe payload

        // Opcode 0 (Handshake) test
        val handshakeJson = """{"v":1,"client_id":"1219986431951405116"}"""
        val handshakeFrame = DiscordRpcClient.encodeIpcFrame(0, handshakeJson)
        val handshakeBuffer = ByteBuffer.wrap(handshakeFrame).order(ByteOrder.LITTLE_ENDIAN)

        handshakeBuffer.getInt() shouldBe 0
        handshakeBuffer.getInt() shouldBe handshakeJson.toByteArray(Charsets.UTF_8).size
    }
})
