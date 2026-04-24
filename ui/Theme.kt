package lt.skautai.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = MossSecondary,
    onSecondary = MossOnSecondary,
    secondaryContainer = MossSecondaryContainer,
    onSecondaryContainer = MossOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = ScoutSurfaces.LightBackground,
    onBackground = ScoutSurfaces.LightOnSurface,
    surface = ScoutSurfaces.LightSurface,
    onSurface = ScoutSurfaces.LightOnSurface,
    surfaceVariant = ScoutSurfaces.LightSurfaceVariant,
    onSurfaceVariant = ScoutSurfaces.LightOnSurfaceVariant,
    surfaceTint = ScoutPalette.Forest,
    surfaceDim = ScoutSurfaces.LightSurfaceDim,
    surfaceBright = ScoutSurfaces.LightSurfaceBright,
    surfaceContainerLowest = ScoutSurfaces.LightSurfaceContainerLowest,
    surfaceContainerLow = ScoutSurfaces.LightSurfaceContainerLow,
    surfaceContainer = ScoutSurfaces.LightSurfaceContainer,
    surfaceContainerHigh = ScoutSurfaces.LightSurfaceContainerHigh,
    surfaceContainerHighest = ScoutSurfaces.LightSurfaceContainerHighest,
    inverseSurface = ScoutSurfaces.LightInverseSurface,
    inverseOnSurface = ScoutSurfaces.LightInverseOnSurface,
    inversePrimary = ScoutPalette.ForestSoft,
    outline = ScoutSurfaces.LightOutline,
    outlineVariant = ScoutSurfaces.LightOutlineVariant,
    scrim = ScoutPalette.BlackScrim,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = ForestPrimaryContainer,
    onPrimary = ForestOnPrimaryContainer,
    primaryContainer = ForestPrimary,
    onPrimaryContainer = ForestPrimaryContainer,
    secondary = MossSecondaryContainer,
    onSecondary = MossOnSecondaryContainer,
    secondaryContainer = MossSecondary,
    onSecondaryContainer = MossSecondaryContainer,
    tertiary = GoldTertiaryContainer,
    onTertiary = GoldOnTertiaryContainer,
    tertiaryContainer = GoldTertiary,
    onTertiaryContainer = GoldTertiaryContainer,
    background = ScoutSurfaces.DarkBackground,
    onBackground = ScoutSurfaces.DarkOnSurface,
    surface = ScoutSurfaces.DarkSurface,
    onSurface = ScoutSurfaces.DarkOnSurface,
    surfaceVariant = ScoutSurfaces.DarkSurfaceVariant,
    onSurfaceVariant = ScoutSurfaces.DarkOnSurfaceVariant,
    surfaceTint = ScoutPalette.ForestSoft,
    surfaceDim = ScoutSurfaces.DarkSurfaceDim,
    surfaceBright = ScoutSurfaces.DarkSurfaceBright,
    surfaceContainerLowest = ScoutSurfaces.DarkSurfaceContainerLowest,
    surfaceContainerLow = ScoutSurfaces.DarkSurfaceContainerLow,
    surfaceContainer = ScoutSurfaces.DarkSurfaceContainer,
    surfaceContainerHigh = ScoutSurfaces.DarkSurfaceContainerHigh,
    surfaceContainerHighest = ScoutSurfaces.DarkSurfaceContainerHighest,
    inverseSurface = ScoutSurfaces.DarkInverseSurface,
    inverseOnSurface = ScoutSurfaces.DarkInverseOnSurface,
    inversePrimary = ScoutPalette.Forest,
    outline = ScoutSurfaces.DarkOutline,
    outlineVariant = ScoutSurfaces.DarkOutlineVariant,
    scrim = ScoutPalette.BlackScrim,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun SkautuInventoriusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
