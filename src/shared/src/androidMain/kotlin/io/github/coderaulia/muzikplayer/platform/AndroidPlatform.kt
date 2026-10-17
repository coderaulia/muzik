package io.github.coderaulia.muzikplayer.platform

/**
 * Android platform bindings for [HostPlatform.ANDROID_MOBILE].
 *
 * Implementations in `:androidApp` provide:
 * - Media3 `ExoPlayer` + custom 10-band biquad `AudioProcessor` (ported from `audio/Equalizer.kt`)
 * - `MediaSessionService` for background playback, lockscreen controls, and Android Auto / Wear OS transport
 * - `MediaStore.Audio` + Storage Access Framework (SAF) folder scanning and `.m3u` playlist sync
 */
fun currentAndroidPlatform(): HostPlatform = HostPlatform.ANDROID_MOBILE
