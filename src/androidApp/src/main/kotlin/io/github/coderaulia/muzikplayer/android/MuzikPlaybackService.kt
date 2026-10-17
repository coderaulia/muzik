package io.github.coderaulia.muzikplayer.android

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Android background [MediaSessionService] for MuzikPlayer.
 *
 * Publishes playback state, metadata, and transport controls (Play/Pause, Skip Next,
 * Skip Previous, Seek) to the Android system media notification, lockscreen,
 * Bluetooth headsets, and Android Auto.
 */
class MuzikPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
