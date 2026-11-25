package dev.roxy.kryptonplayer.core.ui.theme

import android.os.Build
import android.util.Base64
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.toHct
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.roxy.kryptonplayer.core.common.playback.PlaybackController

val LocalPlaybackController = staticCompositionLocalOf<PlaybackController?> { null }

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Immutable
data class AppColors(
    val favorite: Color = favoriteColor,
    val lyricActive: Color = lyricActiveColor,
    val shimmerBackground: Color = dev.roxy.kryptonplayer.core.ui.theme.shimmerBackground,
    val shimmerLine: Color = dev.roxy.kryptonplayer.core.ui.theme.shimmerLine,
    val overlay: Color = dev.roxy.kryptonplayer.core.ui.theme.overlay,
    val overlayHeavy: Color = dev.roxy.kryptonplayer.core.ui.theme.blackMoreOverlay,
    val desktopPanelDark: Color = dev.roxy.kryptonplayer.core.ui.theme.desktopPanelDark,
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }
val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalMeshBackgroundEnabled = staticCompositionLocalOf { false }


private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KryptonPlayerTheme(
    darkTheme: Boolean = true,
    highContrastDarkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    customThemeValue: String = "",
    pureBlack: Boolean = true,
    disableAnimations: Boolean = false,
    useSystemFont: Boolean = false,
    dynamicThemeColor: Color? = null,
    enableMeshBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val baseColorScheme = remember(
        dynamicColor,
        customThemeValue,
        dynamicThemeColor,
        pureBlack,
        highContrastDarkTheme,
        context,
    ) {
        when {
            dynamicThemeColor != null -> {
                dynamicColorScheme(
                    seedColor = dynamicThemeColor,
                    isDark = true,
                    isAmoled = true,
                    style = paletteStyleFor(dynamicThemeColor),
                )
            }
            customThemeValue.isNotBlank() && customThemeValue != ThemePalettes.Default.id -> {
                val decodedSeed = ThemeSeedPaletteCodec.decodeFromPreference(customThemeValue)
                val namedPalette = ThemePalettes.findById(customThemeValue)
                when {
                    decodedSeed != null -> exactPaletteColorScheme(
                        palette = decodedSeed,
                        isDark = true,
                        isAmoled = true,
                    )
                    namedPalette != null -> exactPaletteColorScheme(
                        palette = namedPalette.toSeedPalette(),
                        isDark = true,
                        isAmoled = true,
                    )
                    else -> darkScheme
                }
            }
            dynamicColor && supportsDynamicTheming() -> {
                dynamicDarkColorScheme(context)
            }
            else -> darkScheme
        }
    }

    // Pure OLED / AMOLED Pitch Black enforced everywhere across the app with Mochi obsidian cards
    val appliedColorScheme = remember(baseColorScheme, highContrastDarkTheme, pureBlack) {
        baseColorScheme.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceDim = Color(0xFF000000),
            surfaceBright = Color(0xFF1A1B20),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF0B0B0D),
            surfaceContainer = Color(0xFF121215),
            surfaceContainerHigh = Color(0xFF191A1F),
            surfaceContainerHighest = Color(0xFF23242B),
            surfaceVariant = Color(0xFF141519),
            onBackground = Color(0xFFF5F5F7),
            onSurface = Color(0xFFF5F5F7),
            onSurfaceVariant = Color(0xFFA1A4AE),
            outline = Color.White.copy(alpha = 0.16f),
            outlineVariant = Color.White.copy(alpha = 0.08f),
        )
    }

    val finalColorScheme = if (disableAnimations) {
        appliedColorScheme
    } else {
        animateColorScheme(appliedColorScheme)
    }

    MaterialExpressiveTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
        content = {
            CompositionLocalProvider(
                LocalAppColors provides AppColors(
                    shimmerBackground = shimmerBackground,
                    shimmerLine = shimmerLine,
                ),
                LocalIsDarkTheme provides true,
                LocalMeshBackgroundEnabled provides enableMeshBackground,
                content = content,
            )
        },
    )
}

/**
 * Ensures immersive media screens (Player, Video Detail, etc.) render pure dark/high contrast
 * even if the device is currently in light theme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForceDarkContent(content: @Composable () -> Unit) {
    val darkScheme = darkScheme.copy(
        background = backgroundPureBlack,
        surface = surfacePureBlack,
        surfaceContainer = surfaceContainerPureBlack,
        surfaceContainerHigh = surfaceContainerHighPureBlack,
        surfaceContainerHighest = surfaceContainerHighestPureBlack,
    )
    SystemBarAppearanceEffect(isDark = true)
    MaterialExpressiveTheme(
        colorScheme = darkScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
        content = {
            CompositionLocalProvider(
                LocalIsDarkTheme provides true,
                LocalContentColor provides darkScheme.onSurfaceVariant,
                LocalAppColors provides AppColors(),
                content = content,
            )
        },
    )
}


/**
 * Keeps wallpaper & theme-derived accents while replacing stark/blinding near-white
 * full-screen surfaces with comfortably blended, harmonized tonal surfaces.
 * Ported & refined from the SimpMusic / mpvrx reference architecture using Material 3 color harmonization.
 */
fun ColorScheme.withComfortableLightSurfaces(): ColorScheme {
    val base = Color(0xFFF8F9FA)
    fun tint(amount: Float): Color = lerp(base, primary, amount)

    return copy(
        background = tint(0.015f),
        onBackground = Color(0xFF1B1B1B),
        surface = tint(0.015f),
        onSurface = Color(0xFF1B1B1B),
        surfaceDim = tint(0.075f),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8E8E8),
        onSurfaceVariant = Color(0xFF474747),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = tint(0.025f),
        surfaceContainer = tint(0.045f),
        surfaceContainerHigh = tint(0.070f),
        surfaceContainerHighest = tint(0.095f),
        outline = Color(0xFF777777),
        outlineVariant = Color(0xFFC7C7C7),
    )
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

@Serializable
data class ThemeExportV1(
    val version: Int = 1,
    val name: String? = null,
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val neutral: String,
)

object ThemeSeedPaletteCodec {
    private const val PreferencePrefix = "seedPalette:"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encodeForPreference(palette: ThemeSeedPalette, name: String? = null): String {
        val payload = json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            )
        )
        val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return PreferencePrefix + b64
    }

    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded = runCatching {
            val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
            bytes.toString(Charsets.UTF_8)
        }.getOrNull() ?: return null
        return decodeFromJson(decoded)
    }

    fun extractNameFromPreference(value: String): String? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded = runCatching {
            val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
            bytes.toString(Charsets.UTF_8)
        }.getOrNull() ?: return null
        return runCatching<String?> {
            val obj = json.parseToJsonElement(decoded)
            obj.jsonObject["name"]?.jsonPrimitive?.content
        }.getOrNull()
    }

    fun decodeFromJson(jsonStr: String): ThemeSeedPalette? {
        return runCatching {
            val export = json.decodeFromString<ThemeExportV1>(jsonStr)
            ThemeSeedPalette(
                primary = Color(android.graphics.Color.parseColor(export.primary)),
                secondary = Color(android.graphics.Color.parseColor(export.secondary)),
                tertiary = Color(android.graphics.Color.parseColor(export.tertiary)),
                neutral = Color(android.graphics.Color.parseColor(export.neutral)),
            )
        }.getOrNull()
    }

    fun extractNameFromJsonOrNull(jsonStr: String): String? {
        return runCatching<String?> {
            val obj = json.parseToJsonElement(jsonStr)
            obj.jsonObject["name"]?.jsonPrimitive?.content
        }.getOrNull()
    }


    fun encodeAsJson(palette: ThemeSeedPalette, name: String? = null): String {
        return json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            )
        )
    }
}

fun Color.toHexArgbString(): String {
    return String.format("#%08X", toArgb())
}

val ColorSaver = object : androidx.compose.runtime.saveable.Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun androidx.compose.runtime.saveable.SaverScope.save(value: Color): Int = value.toArgb()
}

data class ThemePalette(
    val id: String,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
) {
    fun toSeedPalette() = ThemeSeedPalette(primary, secondary, tertiary, neutral)
}

object ThemePalettes {
    val Default = ThemePalette("default", "Obsidian Monochrome", Color(0xFFE8EAED), Color(0xFFC7C9D1), Color(0xFFA8C7FA), Color(0xFF16161B))
    val CoralRed = ThemePalette("coral_red", "Coral Red", Color(0xFFED5564), Color(0xFFED5564), Color(0xFFED5564), Color(0xFFED5564))
    val OceanBlue = ThemePalette("ocean_blue", "Ocean Blue", Color(0xFF4A90D9), Color(0xFF4A90D9), Color(0xFF4A90D9), Color(0xFF4A90D9))
    val ArcticBlue = ThemePalette("arctic_blue", "Arctic Blue", Color(0xFF00BFFF), Color(0xFF00BFFF), Color(0xFF00BFFF), Color(0xFF00BFFF))
    val MidnightNavy = ThemePalette("midnight_navy", "Midnight Navy", Color(0xFF2C3E50), Color(0xFF2C3E50), Color(0xFF2C3E50), Color(0xFF2C3E50))
    val SkyBlue = ThemePalette("sky_blue", "Sky Blue", Color(0xFF87CEEB), Color(0xFF87CEEB), Color(0xFF87CEEB), Color(0xFF87CEEB))
    val CobaltBlue = ThemePalette("cobalt_blue", "Cobalt Blue", Color(0xFF0047AB), Color(0xFF0047AB), Color(0xFF0047AB), Color(0xFF0047AB))
    val ElectricBlue = ThemePalette("electric_blue", "Electric Blue", Color(0xFF7DF9FF), Color(0xFF7DF9FF), Color(0xFF7DF9FF), Color(0xFF7DF9FF))
    val EmeraldGreen = ThemePalette("emerald_green", "Emerald Green", Color(0xFF2ECC71), Color(0xFF2ECC71), Color(0xFF2ECC71), Color(0xFF2ECC71))
    val TealWave = ThemePalette("teal_wave", "Teal Wave", Color(0xFF1ABC9C), Color(0xFF1ABC9C), Color(0xFF1ABC9C), Color(0xFF1ABC9C))
    val ForestGreen = ThemePalette("forest_green", "Forest Green", Color(0xFF228B22), Color(0xFF228B22), Color(0xFF228B22), Color(0xFF228B22))
    val SpotifyGreen = ThemePalette("spotify_green", "Spotify Green", Color(0xFF1DB954), Color(0xFF1DB954), Color(0xFF1DB954), Color(0xFF1DB954))
    val MintFresh = ThemePalette("mint_fresh", "Mint Fresh", Color(0xFF98FF98), Color(0xFF98FF98), Color(0xFF98FF98), Color(0xFF98FF98))
    val SageGreen = ThemePalette("sage_green", "Sage Green", Color(0xFF9CAF88), Color(0xFF9CAF88), Color(0xFF9CAF88), Color(0xFF9CAF88))
    val SunsetOrange = ThemePalette("sunset_orange", "Sunset Orange", Color(0xFFE67E22), Color(0xFFE67E22), Color(0xFFE67E22), Color(0xFFE67E22))
    val GoldenHour = ThemePalette("golden_hour", "Golden Hour", Color(0xFFF39C12), Color(0xFFF39C12), Color(0xFFF39C12), Color(0xFFF39C12))
    val WarmAmber = ThemePalette("warm_amber", "Warm Amber", Color(0xFFFFBF00), Color(0xFFFFBF00), Color(0xFFFFBF00), Color(0xFFFFBF00))
    val TangerineBlast = ThemePalette("tangerine_blast", "Tangerine Blast", Color(0xFFFF9800), Color(0xFFFF9800), Color(0xFFFF9800), Color(0xFFFF9800))
    val Peach = ThemePalette("peach", "Peach", Color(0xFFFFDAB9), Color(0xFFFFDAB9), Color(0xFFFFDAB9), Color(0xFFFFDAB9))
    val Mango = ThemePalette("mango", "Mango", Color(0xFFFF8243), Color(0xFFFF8243), Color(0xFFFF8243), Color(0xFFFF8243))
    val RoyalPurple = ThemePalette("royal_purple", "Royal Purple", Color(0xFF9B59B6), Color(0xFF9B59B6), Color(0xFF9B59B6), Color(0xFF9B59B6))
    val LavenderDream = ThemePalette("lavender_dream", "Lavender Dream", Color(0xFFB39DDB), Color(0xFFB39DDB), Color(0xFFB39DDB), Color(0xFFB39DDB))
    val YouTubeRed = ThemePalette("youtube_red", "YouTube Red", Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000))
    val Cyberpunk = ThemePalette("cyberpunk", "Cyberpunk", Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF), Color(0xFFFF00FF))
    val Synthwave = ThemePalette("synthwave", "Synthwave", Color(0xFFFF6EC7), Color(0xFFFF6EC7), Color(0xFFFF6EC7), Color(0xFFFF6EC7))
    val Charcoal = ThemePalette("charcoal", "Charcoal", Color(0xFF36454F), Color(0xFF36454F), Color(0xFF36454F), Color(0xFF36454F))

    val allPalettes = listOf(
        Default, CoralRed, OceanBlue, ArcticBlue, MidnightNavy, SkyBlue, CobaltBlue, ElectricBlue,
        EmeraldGreen, TealWave, ForestGreen, SpotifyGreen, MintFresh, SageGreen,
        SunsetOrange, GoldenHour, WarmAmber, TangerineBlast, Peach, Mango,
        RoyalPurple, LavenderDream, YouTubeRed, Cyberpunk, Synthwave, Charcoal
    )

    fun findById(id: String): ThemePalette? {
        return allPalettes.find { it.id == id }
    }
}

private fun paletteStyleFor(seedColor: Color): PaletteStyle {
    val chroma = seedColor.toHct().chroma
    return when {
        chroma < 4.0 -> PaletteStyle.Monochrome
        chroma < 12.0 -> PaletteStyle.Neutral
        else -> PaletteStyle.TonalSpot
    }
}

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
    isAmoled: Boolean = false,
): ColorScheme = mergedSeedColorScheme(
    primarySeed = palette.primary,
    secondarySeed = palette.secondary,
    tertiarySeed = palette.tertiary,
    neutralSeed = palette.neutral,
    isDark = isDark,
    isAmoled = isAmoled,
)

private fun mergedSeedColorScheme(
    primarySeed: Color,
    secondarySeed: Color,
    tertiarySeed: Color,
    neutralSeed: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    contrastLevel: Double = 0.0,
    style: PaletteStyle = paletteStyleFor(primarySeed),
): ColorScheme {
    val primaryScheme = dynamicColorScheme(seedColor = primarySeed, isDark = isDark, isAmoled = isAmoled, style = style, contrastLevel = contrastLevel)
    val secondaryScheme = dynamicColorScheme(seedColor = secondarySeed, isDark = isDark, isAmoled = isAmoled, style = paletteStyleFor(secondarySeed), contrastLevel = contrastLevel)
    val tertiaryScheme = dynamicColorScheme(seedColor = tertiarySeed, isDark = isDark, isAmoled = isAmoled, style = paletteStyleFor(tertiarySeed), contrastLevel = contrastLevel)
    val neutralScheme = dynamicColorScheme(seedColor = neutralSeed, isDark = isDark, isAmoled = isAmoled, style = paletteStyleFor(neutralSeed), contrastLevel = contrastLevel)


    return ColorScheme(
        primary = primaryScheme.primary,
        onPrimary = primaryScheme.onPrimary,
        primaryContainer = primaryScheme.primaryContainer,
        onPrimaryContainer = primaryScheme.onPrimaryContainer,
        inversePrimary = primaryScheme.inversePrimary,

        secondary = secondaryScheme.primary,
        onSecondary = secondaryScheme.onPrimary,
        secondaryContainer = secondaryScheme.primaryContainer,
        onSecondaryContainer = secondaryScheme.onPrimaryContainer,

        tertiary = tertiaryScheme.primary,
        onTertiary = tertiaryScheme.onPrimary,
        tertiaryContainer = tertiaryScheme.primaryContainer,
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer,

        background = neutralScheme.background,
        onBackground = neutralScheme.onBackground,
        surface = neutralScheme.surface,
        onSurface = neutralScheme.onSurface,
        surfaceVariant = neutralScheme.surfaceVariant,
        onSurfaceVariant = neutralScheme.onSurfaceVariant,
        inverseSurface = neutralScheme.inverseSurface,
        inverseOnSurface = neutralScheme.inverseOnSurface,

        surfaceBright = neutralScheme.surfaceBright,
        surfaceDim = neutralScheme.surfaceDim,
        surfaceContainer = neutralScheme.surfaceContainer,
        surfaceContainerLow = neutralScheme.surfaceContainerLow,
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest,
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh,
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest,

        outline = neutralScheme.outline,
        outlineVariant = neutralScheme.outlineVariant,

        error = primaryScheme.error,
        onError = primaryScheme.onError,
        errorContainer = primaryScheme.errorContainer,
        onErrorContainer = primaryScheme.onErrorContainer,

        scrim = neutralScheme.scrim,
        surfaceTint = primaryScheme.surfaceTint,
    )
}

@Composable
private fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = spring<Color>(stiffness = Spring.StiffnessLow)
    return ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value,
        surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceContainerLowest").value,
        surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceContainerHighest").value,
    )
}
