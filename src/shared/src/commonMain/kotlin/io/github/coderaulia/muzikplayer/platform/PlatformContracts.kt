package io.github.coderaulia.muzikplayer.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * Platform-agnostic URI/path identifier for a song or playlist resource.
 * - Desktop (Linux / Windows): Canonical filesystem path string
 * - Android: Content URI (`content://media/...` or SAF document URI)
 */
@JvmInline
value class MediaUri(val value: String)

/**
 * Shared audio playback & DSP contract across Desktop (FFSampledSP + Java Sound)
 * and Android (Media3 ExoPlayer + AudioProcessor).
 */
interface PlatformAudioEngine {
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Duration>
    val outputLevel: StateFlow<Float>

    suspend fun prepare(uri: MediaUri, startPosition: Duration = Duration.ZERO)
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(position: Duration)
    suspend fun setVolume(level: Float)
    suspend fun setEqualizer(enabled: Boolean, bandGainsDb: List<Float>)
}

/**
 * Shared media session / OS transport integration:
 * - Linux: MPRIS D-Bus (`org.mpris.MediaPlayer2.MuzikPlayer`)
 * - Windows: SystemMediaTransportControls (SMTC) / media keys
 * - Android: Media3 `MediaSessionService` notification & lockscreen controls
 */
interface PlatformMediaSession {
    fun start()
    fun updatePlaybackState(isPlaying: Boolean, position: Duration)
    fun updateMetadata(title: String, artist: String, album: String, duration: Duration, artworkBytes: ByteArray?)
    fun release()
}

/**
 * Shared preferences / key-value store abstraction:
 * - Desktop (Linux / Windows): `java.util.prefs.Preferences`
 * - Android: Jetpack `DataStore<Preferences>` / `SharedPreferences`
 */
interface PlatformPreferencesStore {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)
    fun flush()
}

/**
 * Shared window & shell integration contract:
 * - Desktop (Linux / Windows): Compose `WindowState` (minimize, maximize/restore, close, floating size/pos)
 * - Android: Activity / system bars / back handler (window chrome buttons hidden on mobile)
 */
interface PlatformShellActions {
    val supportsCustomWindowDecoration: Boolean
    val isMaximized: Boolean
    fun minimize()
    fun toggleMaximize()
    fun close()
    fun pickFolders(onResult: (List<MediaUri>) -> Unit)
}

/**
 * Platform capability descriptor used by the shared UI & Settings page
 * to maintain 1:1 feature parity while adapting OS-specific labels.
 */
enum class HostPlatform {
    LINUX_DESKTOP,
    WINDOWS_DESKTOP,
    ANDROID_MOBILE,
}
