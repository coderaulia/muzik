package io.github.coderaulia.muzikplayer.mpris

import io.github.coderaulia.muzikplayer.audio.PlayerController
import io.github.coderaulia.muzikplayer.data.SongKey
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

object DesktopNotifier {
    @Volatile
    private var lastNotifiedSongKey: SongKey? = null

    @Synchronized
    fun onPlaybackStateChanged(state: PlayerController.State) {
        val currentSong = state.currentlyPlaying?.queue?.currentSong
        val currentKey = state.currentlyPlaying?.queue?.currentSongKey

        if (currentSong == null || currentKey == null) {
            lastNotifiedSongKey = null
            return
        }

        if (state.pause) {
            return
        }

        if (currentKey == lastNotifiedSongKey) {
            return
        }
        lastNotifiedSongKey = currentKey

        val coverPathOrIcon = currentSong.cover?.getMprisFile(currentSong)?.toString() ?: "io.github.coderaulia.MuzikPlayer"
        val title = currentSong.title
        val artistAndAlbum = "${currentSong.artist.name} — ${currentSong.album.title}"

        sendNotification(title, artistAndAlbum, coverPathOrIcon)
    }

    internal fun sendNotification(title: String, artistAndAlbum: String, coverPathOrIcon: String) {
        try {
            ProcessBuilder(
                "notify-send",
                "-a", "MuzikPlayer",
                "-i", coverPathOrIcon,
                title,
                artistAndAlbum
            ).start()
        } catch (e: Exception) {
            logger.debug { "Could not send desktop notification via notify-send: ${e.message}" }
        }
    }

    @Synchronized
    fun reset() {
        lastNotifiedSongKey = null
    }
}
