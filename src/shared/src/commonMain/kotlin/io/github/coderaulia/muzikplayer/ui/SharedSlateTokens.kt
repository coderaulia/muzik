package io.github.coderaulia.muzikplayer.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shared Libadwaita Slate design tokens used across Desktop (Linux/Windows) and Android Mobile.
 * Matches `design/home-page/DESIGN.md` and `design/settings-page/DESIGN.md`.
 */
object SharedSlatePalette {
    val BgCanvas = Color(0xFF1E1E24)
    val BgWindow = Color(0xFF24242C)
    val BgSurface = Color(0xFF2C2C36)
    val BgElevated = Color(0xFF343440)
    val BgHeaderbar = Color(0xFF202028)
    val AccentPrimary = Color(0xFF78AEFF)
    val AccentPurple = Color(0xFFC084FC)
    val AccentGreen = Color(0xFF4ADE80)
    val AccentAmber = Color(0xFFFBBF24)
    val AccentCoral = Color(0xFFF87171)
    val TextPrimary = Color(0xFFF2F2F7)
    val TextSecondary = Color(0xFFA6A6B8)
    val TextMuted = Color(0xFF6E6E82)
    val BorderSubtle = Color(0x18FFFFFF)

    val LightBgCanvas = Color(0xFFF4F4F8)
    val LightBgWindow = Color(0xFFFAFAFC)
    val LightBgSurface = Color(0xFFFFFFFF)
    val LightBgElevated = Color(0xFFEBEBF2)
    val LightAccentPrimary = Color(0xFF1C68D8)
    val LightTextPrimary = Color(0xFF1A1A22)
    val LightTextSecondary = Color(0xFF525266)
    val LightTextMuted = Color(0xFF828296)

    fun darkScheme(accent: Color = AccentPrimary) = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF0A192F),
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = TextPrimary,
        secondary = AccentPurple,
        tertiary = AccentGreen,
        background = BgCanvas,
        onBackground = TextPrimary,
        surface = BgWindow,
        onSurface = TextPrimary,
        surfaceVariant = BgSurface,
        onSurfaceVariant = TextSecondary,
        surfaceContainerHigh = BgElevated,
        outline = BorderSubtle,
    )

    fun lightScheme(accent: Color = LightAccentPrimary) = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.14f),
        onPrimaryContainer = LightTextPrimary,
        secondary = AccentPurple,
        tertiary = AccentGreen,
        background = LightBgCanvas,
        onBackground = LightTextPrimary,
        surface = LightBgWindow,
        onSurface = LightTextPrimary,
        surfaceVariant = LightBgSurface,
        onSurfaceVariant = LightTextSecondary,
        surfaceContainerHigh = LightBgElevated,
        outline = Color(0x1F000000),
    )
}
