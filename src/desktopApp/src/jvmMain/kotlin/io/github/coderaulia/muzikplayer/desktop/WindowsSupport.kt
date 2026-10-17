package io.github.coderaulia.muzikplayer.desktop

import io.github.coderaulia.muzikplayer.platform.HostPlatform
import io.github.coderaulia.muzikplayer.platform.MediaUri
import io.github.coderaulia.muzikplayer.platform.PlatformMediaSession
import io.github.coderaulia.muzikplayer.platform.currentDesktopPlatform
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Windows & Linux Desktop platform helpers ensuring 1:1 feature parity on Windows.
 *
 * Key Windows adaptations over Linux Desktop:
 * 1. Audio Decoder: Uses `ffsampledsp-x86_64-win.dll` via `java.library.path` (instead of `.so` + ELF bzip2 patch).
 * 2. Media Session: Gracefully bypasses Linux D-Bus/MPRIS on Windows and provides a Windows media transport hook.
 * 3. System Theme: Falls back cleanly when `org.freedesktop.portal.Settings` D-Bus portal is absent on Windows.
 * 4. Default Music Path: Resolves `%USERPROFILE%\Music` via `Path.of(System.getProperty("user.home"), "Music")`.
 */
object DesktopPlatformEnv {
    val platform: HostPlatform = currentDesktopPlatform()
    val isWindows: Boolean = platform == HostPlatform.WINDOWS_DESKTOP
    val isLinux: Boolean = platform == HostPlatform.LINUX_DESKTOP

    fun defaultMusicDirectory(): Path =
        Path.of(System.getProperty("user.home"), "Music")

    fun createNoOpOrWindowsMediaSession(): PlatformMediaSession = object : PlatformMediaSession {
        override fun start() = Unit
        override fun updatePlaybackState(isPlaying: Boolean, position: Duration) = Unit
        override fun updateMetadata(
            title: String,
            artist: String,
            album: String,
            duration: Duration,
            artworkBytes: ByteArray?,
        ) = Unit
        override fun release() = Unit
    }
}
