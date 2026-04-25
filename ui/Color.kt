package lt.skautai.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Skautai palette v2 — refined.
 *
 * What changed:
 * - Forest deepened slightly (#214D34 → #1F3A2A) — more confident primary.
 * - Saturation across neutrals reduced; off-whites read warmer, calmer.
 * - Added Clay accent (#B8704A) — warm complement to forest, used for alerts / new-item dots.
 * - Surfaces simplified: fewer near-identical tonal steps. You had 6, we really only need 3.
 *
 * Usage rule: forest/moss/gold are ACCENTS, not default fills. Most cards should sit on
 * `paper` with a 1px `line` border — reserve tonal fills for the hero and key CTAs.
 */
object ScoutPalette {
    // Primary — forest family
    val Forest = Color(0xFF1F3A2A)
    val ForestInk = Color(0xFF0B1710)
    val ForestSoft = Color(0xFFBFD0BA)   // container
    val ForestMist = Color(0xFFE6EEE1)   // faint tonal fill

    // Secondary — moss (slightly blue-green)
    val Moss = Color(0xFF3E5B43)
    val MossSoft = Color(0xFFD6E1D2)
    val Lichen = Color(0xFFDCE6D3)

    // Tertiary — gold / khaki (warm)
    val Gold = Color(0xFF7A5C1E)
    val GoldSoft = Color(0xFFEFE2B2)
    val Khaki = Color(0xFFE8DCC2)

    // New accent — clay (warm complement; use for alerts, dots, warn states)
    val Clay = Color(0xFFB8704A)
    val ClaySoft = Color(0xFFF2DCCB)

    // Ink
    val Ink = Color(0xFF0D120E)
    val InkBody = Color(0xFF1A1F1B)
    val InkMuted = Color(0xFF596560)
    val InkFaint = Color(0xFF8A938C)
    val BlackScrim = Color(0xFF000000)

    // Aliases kept for backward-compatibility with existing screens
    val ForestDeep = ForestInk
    val MossMist = Color(0xFFEAF1E6)
    val MossDeep = Color(0xFF102016)
    val GoldDeep = Color(0xFF2B1D00)
    val GoldWarning = Color(0xFFFFE08A)
    val GoldWarningText = Color(0xFF4A3700)
    val Earth = Color(0xFFE9DDCF)

    // Neutrals
    val Paper = Color(0xFFF6F7F2)        // main background — subtly warm off-white
    val PaperDeep = Color(0xFFECEEE5)    // slightly deeper for contrast zones
    val White = Color(0xFFFFFFFF)
    val Line = Color(0xFFD7DCD1)
    val LineSoft = Color(0xFFE5E9DF)

    // Semantics
    val Ok = Color(0xFF406B3F)
    val Warn = Color(0xFFC18A1A)
    val Error = Color(0xFF9B2F27)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF5D7D3)
    val OnErrorContainer = Color(0xFF3A0F0B)
}

/**
 * Surfaces — flattened from 6 near-identical tonal steps to 3 meaningful ones.
 * If you truly need surfaceContainerHighest etc., map them all to the closest of these.
 */
object ScoutSurfaces {
    // Light
    val LightBackground = ScoutPalette.Paper
    val LightSurface = ScoutPalette.Paper
    val LightSurfaceDim = ScoutPalette.PaperDeep
    val LightSurfaceBright = ScoutPalette.White
    val LightSurfaceContainerLowest = ScoutPalette.White
    val LightSurfaceContainerLow = ScoutPalette.Paper
    val LightSurfaceContainer = ScoutPalette.PaperDeep
    val LightSurfaceContainerHigh = Color(0xFFE5E8DE)
    val LightSurfaceContainerHighest = Color(0xFFDDE2D5)
    val LightSurfaceVariant = Color(0xFFDDE2D5)
    val LightOutline = Color(0xFF68786B)
    val LightOutlineVariant = ScoutPalette.Line
    val LightOnSurface = ScoutPalette.Ink
    val LightOnSurfaceVariant = ScoutPalette.InkMuted
    val LightInverseSurface = Color(0xFF2B322C)
    val LightInverseOnSurface = Color(0xFFEFF4EC)

    // Dark — rebalanced. Old dark mode swapped primary/container which muddied everything.
    val DarkBackground = Color(0xFF0C1410)
    val DarkSurface = Color(0xFF121A15)
    val DarkSurfaceDim = Color(0xFF0C1410)
    val DarkSurfaceBright = Color(0xFF2C342E)
    val DarkSurfaceContainerLowest = Color(0xFF060B08)
    val DarkSurfaceContainerLow = Color(0xFF151C17)
    val DarkSurfaceContainer = Color(0xFF1A221C)
    val DarkSurfaceContainerHigh = Color(0xFF232B25)
    val DarkSurfaceContainerHighest = Color(0xFF2D352E)
    val DarkSurfaceVariant = Color(0xFF3A443C)
    val DarkOutline = Color(0xFF7E8B80)
    val DarkOutlineVariant = Color(0xFF3A443C)
    val DarkOnSurface = Color(0xFFE0E4DD)
    val DarkOnSurfaceVariant = Color(0xFFB4BDB5)
    val DarkInverseSurface = Color(0xFFE0E6DC)
    val DarkInverseOnSurface = Color(0xFF2B322C)
}

object ScoutGradients {
    // Only one gradient now — the home hero. Everything else is flat.
    val LoginBackground = listOf(ScoutPalette.ForestMist, ScoutPalette.PaperDeep)
    val LoginHero = listOf(Color(0xFF1F3A2A), Color(0xFF2A4E35))
    val HomeHero = listOf(Color(0xFF1F3A2A), Color(0xFF274832))
    val HeroTextMuted = Color(0xFFD6E5D3)
}

object ScoutStatusColors {
    val PendingContainer = ScoutPalette.GoldSoft
    val OnPendingContainer = Color(0xFF4A3700)
    val OkContainer = ScoutPalette.Lichen
    val OnOkContainer = Color(0xFF1E3A1F)
    val WarnContainer = ScoutPalette.ClaySoft
    val OnWarnContainer = Color(0xFF5A2E13)
    val NeutralContainer = ScoutPalette.MossSoft
    val OnNeutralContainer = ScoutPalette.MossDeep
    val InfoContainer = ScoutPalette.ForestSoft
    val OnInfoContainer = ScoutPalette.ForestInk
}

data class ScoutUnitPalette(
    val cardTone: Color,
    val iconTone: Color,
    val accent: Color
)

object ScoutUnitColors {
    val PatyreSkautai = ScoutUnitPalette(Color(0xFFF0D4CE), Color(0xFFE4B3AB), Color(0xFF8E2F25))
    val Skautai      = ScoutUnitPalette(Color(0xFFF1E2B0), Color(0xFFE3C664), Color(0xFF6F5412))
    val Vilkai       = ScoutUnitPalette(Color(0xFFF2D8BE), Color(0xFFE4AC76), Color(0xFF8A4A16))
    val Gildija      = ScoutUnitPalette(Color(0xFFDEE2DD), Color(0xFFC3CAC1), Color(0xFF465149))
    val VyrSkautai   = ScoutUnitPalette(Color(0xFFE2D9EB), Color(0xFFC6B2D8), Color(0xFF5C3E76))
    val VyrSkautes   = ScoutUnitPalette(Color(0xFFD4DCE9), Color(0xFFB0C1D6), Color(0xFF263F63))
    val Default      = ScoutUnitPalette(ScoutPalette.Khaki, ScoutPalette.GoldSoft, ScoutPalette.Gold)
}

// Role-colour aliases consumed by Color.kt/Theme.kt
val ForestPrimary = ScoutPalette.Forest
val ForestOnPrimary = ScoutPalette.White
val ForestPrimaryContainer = ScoutPalette.ForestSoft
val ForestOnPrimaryContainer = ScoutPalette.ForestInk

val MossSecondary = ScoutPalette.Moss
val MossOnSecondary = ScoutPalette.White
val MossSecondaryContainer = ScoutPalette.MossSoft
val MossOnSecondaryContainer = Color(0xFF102016)

val GoldTertiary = ScoutPalette.Gold
val GoldOnTertiary = ScoutPalette.White
val GoldTertiaryContainer = ScoutPalette.GoldSoft
val GoldOnTertiaryContainer = Color(0xFF2B1D00)

val ErrorRed = ScoutPalette.Error
val OnErrorRed = ScoutPalette.OnError
val ErrorContainer = ScoutPalette.ErrorContainer
val OnErrorContainer = ScoutPalette.OnErrorContainer

val White = ScoutPalette.White
