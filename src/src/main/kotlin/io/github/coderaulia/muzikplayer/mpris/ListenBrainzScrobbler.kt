package io.github.coderaulia.muzikplayer.mpris

import io.github.coderaulia.muzikplayer.audio.PlayerController
import io.github.coderaulia.muzikplayer.data.SongKey
import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

object ListenBrainzScrobbler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build()
    }

    private var currentSongKey: SongKey? = null
    private var playingNowSent: Boolean = false
    private var scrobbleSent: Boolean = false
    private var accumulatedListened: Duration = Duration.ZERO
    private var lastReportedPosition: Duration = Duration.ZERO

    fun shouldScrobble(trackDuration: Duration, listenedDuration: Duration): Boolean {
        if (trackDuration < 30.seconds) return false
        val requiredDuration = minOf(trackDuration / 2, 240.seconds)
        return listenedDuration >= requiredDuration
    }

    fun escapeJson(text: String): String {
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

    fun buildListenPayload(
        listenType: String,
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Long?,
        listenedAtEpochSeconds: Long? = null,
    ): String {
        val escapedType = escapeJson(listenType)
        val escapedTitle = escapeJson(title)
        val escapedArtist = escapeJson(artist)
        val releasePart = if (album != null) {
            ""","release_name":"${escapeJson(album)}""""
        } else ""
        val durationPart = if (durationSeconds != null) {
            ""","duration":$durationSeconds"""
        } else ""
        val listenedAtPart = if (listenedAtEpochSeconds != null) {
            """"listened_at":$listenedAtEpochSeconds,"""
        } else ""

        return """{"listen_type":"$escapedType","payload":[{${listenedAtPart}"track_metadata":{"artist_name":"$escapedArtist","track_name":"$escapedTitle"$releasePart,"additional_info":{"listening_from":"MuzikPlayer"$durationPart}}}]}"""
    }

    @Synchronized
    fun onPlaybackStateChanged(state: PlayerController.State, position: Duration) {
        val token = Preferences.listenBrainzToken.get()
        if (token.isBlank()) {
            return
        }

        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val currentKey = state.currentlyPlaying?.queue?.currentSongKey

        if (currentSong == null || currentKey == null) {
            resetTracking()
            return
        }

        if (currentKey != currentSongKey) {
            currentSongKey = currentKey
            playingNowSent = false
            scrobbleSent = false
            accumulatedListened = Duration.ZERO
            lastReportedPosition = position
        } else if (!state.pause) {
            val delta = position - lastReportedPosition
            // Advance accumulated time only if progressing forward naturally (ignore backwards seek or huge forward jumps)
            if (delta > Duration.ZERO && delta < 5.seconds) {
                accumulatedListened += delta
            }
            lastReportedPosition = position
        } else {
            lastReportedPosition = position
        }

        if (!state.pause && !playingNowSent) {
            playingNowSent = true
            val payload = buildListenPayload(
                listenType = "playing_now",
                title = currentSong.title,
                artist = currentSong.artist.name,
                album = currentSong.album.title,
                durationSeconds = currentSong.length.inWholeSeconds,
                listenedAtEpochSeconds = null
            )
            submitListenAsync(token, payload)
        }

        if (!scrobbleSent && shouldScrobble(currentSong.length, accumulatedListened)) {
            scrobbleSent = true
            val nowEpochSeconds = Clock.System.now().epochSeconds
            val payload = buildListenPayload(
                listenType = "single",
                title = currentSong.title,
                artist = currentSong.artist.name,
                album = currentSong.album.title,
                durationSeconds = currentSong.length.inWholeSeconds,
                listenedAtEpochSeconds = nowEpochSeconds
            )
            submitListenAsync(token, payload)
        }
    }

    private fun submitListenAsync(token: String, jsonPayload: String) {
        scope.launch {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.listenbrainz.org/1/submit-listens"))
                    .header("Authorization", "Token $token")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 200..299) {
                    logger.warn { "ListenBrainz submission returned HTTP ${response.statusCode()}: ${response.body()}" }
                } else {
                    logger.debug { "ListenBrainz listen submitted successfully" }
                }
            } catch (e: Exception) {
                logger.warn { "Failed to submit listen to ListenBrainz: ${e.message}" }
            }
        }
    }

    @Synchronized
    fun resetTracking() {
        currentSongKey = null
        playingNowSent = false
        scrobbleSent = false
        accumulatedListened = Duration.ZERO
        lastReportedPosition = Duration.ZERO
    }
}
