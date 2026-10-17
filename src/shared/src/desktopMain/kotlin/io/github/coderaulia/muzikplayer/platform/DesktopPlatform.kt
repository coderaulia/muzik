package io.github.coderaulia.muzikplayer.platform

import java.util.prefs.Preferences

/**
 * Detects whether the current JVM Desktop runtime is Windows or Linux.
 */
fun currentDesktopPlatform(): HostPlatform {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        osName.contains("win") -> HostPlatform.WINDOWS_DESKTOP
        else -> HostPlatform.LINUX_DESKTOP
    }
}

/**
 * JVM Desktop implementation of [PlatformPreferencesStore] backed by `java.util.prefs.Preferences`.
 * Works identically on Linux (`~/.java/.userPrefs`) and Windows (Registry `HKCU\Software\JavaSoft\Prefs`).
 */
class JvmPreferencesStore(
    nodePath: String = "/io/github/music-player",
) : PlatformPreferencesStore {
    private val prefs: Preferences = Preferences.userRoot().node(nodePath)

    override fun getString(key: String, defaultValue: String?): String? = prefs.get(key, defaultValue)
    override fun putString(key: String, value: String) = prefs.put(key, value)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)
    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) = prefs.putInt(key, value)
    override fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    override fun putFloat(key: String, value: Float) = prefs.putFloat(key, value)
    override fun flush() = prefs.flush()
}
