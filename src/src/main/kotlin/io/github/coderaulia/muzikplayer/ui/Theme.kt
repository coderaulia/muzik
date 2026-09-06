package io.github.coderaulia.muzikplayer.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mmarco94.klibportal.portals.Settings
import io.github.coderaulia.muzikplayer.LocalAppearanceSettings
import io.github.coderaulia.muzikplayer.generated.resources.Res
import io.github.coderaulia.muzikplayer.generated.resources.theme_auto
import io.github.coderaulia.muzikplayer.generated.resources.theme_dark
import io.github.coderaulia.muzikplayer.generated.resources.theme_light
import io.github.coderaulia.muzikplayer.utils.HSLColor
import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.coderaulia.muzikplayer.utils.hsb
import org.jetbrains.compose.resources.StringResource

// See Material3.ColorScheme
const val INACTIVE_ALPHA = .38f
private const val TOO_DARK_THRESHOLD = 0.15
private const val TOO_BRIGHT_THRESHOLD_LIGHT = 0.85
private const val TOO_BRIGHT_THRESHOLD_SATURATION = 0.3

object MuzikTheme {

    enum class UserPreference(val nameRes: StringResource) {
        AUTO(Res.string.theme_auto), LIGHT(Res.string.theme_light), DARK(Res.string.theme_dark),
    }

    val shapes: Shapes = Shapes(
        extraSmall = RoundedCornerShape(size = 4.dp),
        small = RoundedCornerShape(size = 6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

    private val defaultPalette = listOf(
        Color(red = 208, green = 188, blue = 255),
        Color(red = 204, green = 194, blue = 220),
        Color(red = 239, green = 184, blue = 200),
    ).map { it.hsb() }

    data class ColorSchemeContainer(
        val light: ColorScheme,
        val dark: ColorScheme,
    ) {
        @Composable
        fun auto(): ColorScheme {
            val theme by Preferences.theme.state
            return when (theme) {
                UserPreference.LIGHT -> light
                UserPreference.DARK -> dark
                UserPreference.AUTO -> when (LocalAppearanceSettings.current.colorScheme) {
                    Settings.Appearance.ColorScheme.LIGHT -> light
                    Settings.Appearance.ColorScheme.NO_PREFERENCE -> light
                    Settings.Appearance.ColorScheme.DARK -> dark
                }
            }
        }
    }

    private val slateDark = darkColorScheme(
        primary = Color(0xFFA7C8FF),
        onPrimary = Color(0xFF003061),
        primaryContainer = Color(0xFF4691F2),
        onPrimaryContainer = Color(0xFF002A55),
        secondary = Color(0xFFCABEFF),
        onSecondary = Color(0xFF31009A),
        secondaryContainer = Color(0xFF4A16D1),
        onSecondaryContainer = Color(0xFFBCAEFF),
        tertiary = Color(0xFF48E087),
        onTertiary = Color(0xFF00391B),
        tertiaryContainer = Color(0xFF00A65B),
        onTertiaryContainer = Color(0xFF003117),
        background = Color(0xFF131313),
        onBackground = Color(0xFFE4E2E1),
        surface = Color(0xFF131313),
        onSurface = Color(0xFFE4E2E1),
        surfaceVariant = Color(0xFF353535),
        onSurfaceVariant = Color(0xFFC1C6D4),
        outline = Color(0xFF8B919E),
        outlineVariant = Color(0xFF414752),
        inverseSurface = Color(0xFFE4E2E1),
        inverseOnSurface = Color(0xFF303030),
        inversePrimary = Color(0xFF005EB2),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

    private val slateLight = lightColorScheme(
        primary = Color(0xFF005EB2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD5E3FF),
        onPrimaryContainer = Color(0xFF001B3C),
        background = Color(0xFFF9F9F9),
        onBackground = Color(0xFF1A1B1E),
        surface = Color(0xFFF9F9F9),
        onSurface = Color(0xFF1A1B1E),
        surfaceVariant = Color(0xFFE1E2EC),
        onSurfaceVariant = Color(0xFF414752),
        outline = Color(0xFF717782),
        outlineVariant = Color(0xFFC1C6D4),
    )

    @Composable
    fun getDefaultScheme(): ColorScheme {
        // The app shell uses a stable neutral palette. Album artwork can still provide
        // a contextual scheme through colorScheme(palette) where appropriate.
        return ColorSchemeContainer(light = slateLight, dark = slateDark).auto()
    }

    fun colorScheme(palette: List<HSLColor>): ColorSchemeContainer {
        fun penalty(color: HSLColor): Float {
            return if (color.lightness < TOO_DARK_THRESHOLD) {
                1 - color.lightness
            } else if (color.lightness > TOO_BRIGHT_THRESHOLD_LIGHT && color.saturation < TOO_BRIGHT_THRESHOLD_SATURATION) {
                color.lightness
            } else 0f
        }

        val sorted = palette.sortedBy { color -> penalty(color) }
        val primary = sorted[0].pastel()
        val secondary = sorted.getOrNull(1)?.pastel() ?: primary
        val tertiary = sorted.getOrNull(2)?.pastel() ?: secondary
        return ColorSchemeContainer(
            dark = darkColorScheme(
                primary = primary.color,
                onPrimary = primary.contrast().color,
                primaryContainer = primary.darker().color,
                onPrimaryContainer = primary.darker().contrast().color,
                surface = primary.darker(2f).color,
                onSurface = primary.darker(2f).contrast().color,
                background = primary.darker(3f).color,
                onBackground = primary.darker(3f).contrast().color,
                secondary = secondary.color,
                onSecondary = secondary.contrast().color,
                secondaryContainer = secondary.darker().color,
                onSecondaryContainer = secondary.darker().contrast().color,
                tertiary = tertiary.color,
                onTertiary = tertiary.contrast().color,
                tertiaryContainer = tertiary.darker().color,
                onTertiaryContainer = tertiary.darker().contrast().color,
                outlineVariant = Color.White.copy(alpha = 0.2f),
            ),
            light = lightColorScheme(
                primary = primary.color,
                onPrimary = primary.contrast().color,
                primaryContainer = primary.lighter().color,
                onPrimaryContainer = primary.lighter().contrast().color,
                surface = primary.lighter(2f).color,
                onSurface = primary.lighter(2f).contrast().color,
                background = primary.lighter(3f).color,
                onBackground = primary.lighter(3f).contrast().color,
                secondary = secondary.color,
                onSecondary = secondary.contrast().color,
                secondaryContainer = secondary.lighter().color,
                onSecondaryContainer = secondary.lighter().contrast().color,
                tertiary = tertiary.color,
                onTertiary = tertiary.contrast().color,
                tertiaryContainer = tertiary.lighter().color,
                onTertiaryContainer = tertiary.lighter().contrast().color,
                outlineVariant = Color.Black.copy(alpha = 0.1f),
            )
        )
    }

    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.01).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.01.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.02.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.04.sp,
        ),
    )
}
