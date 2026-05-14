package lt.skautai.android.ui.theme

import androidx.compose.ui.graphics.Color

object ScoutPalette {
    // Primary - forest family
    val Forest = Color(0xFF1F3A2A)
    val ForestInk = Color(0xFF0B1710)
    val ForestSoft = Color(0xFFAFC7AE)   // container
    val ForestMist = Color(0xFFDDE8DA)   // faint tonal fill

    // Secondary - moss
    val Moss = Color(0xFF415E47)
    val MossSoft = Color(0xFFC9D8C6)
    val Lichen = Color(0xFFD5E3D0)

    // Tertiary - gold / khaki (warm contrast)
    val Gold = Color(0xFF7A5C1E)
    val GoldSoft = Color(0xFFE9DEAF)
    val Khaki = Color(0xFFE2D7BB)

    // Accent - clay (warn states, dots, subtle alerts)
    val Clay = Color(0xFFB8704A)
    val ClaySoft = Color(0xFFF0D8CA)

    // Ink
    val Ink = Color(0xFF0D120E)
    val InkBody = Color(0xFF1A1F1B)
    val InkMuted = Color(0xFF56645C)
    val InkFaint = Color(0xFF869188)
    val BlackScrim = Color(0xFF000000)

    // Aliases kept for backward compatibility with existing screens
    val ForestDeep = ForestInk
    val MossMist = Color(0xFFE3ECE0)
    val MossDeep = Color(0xFF102016)
    val GoldDeep = Color(0xFF2B1D00)
    val GoldWarning = Color(0xFFFFE08A)
    val GoldWarningText = Color(0xFF4A3700)
    val Earth = Color(0xFFE0D7C6)

    // Neutrals
    val Paper = Color(0xFFF0F5EE)        // main background - pale sage paper
    val PaperDeep = Color(0xFFE4ECE1)    // deeper moss-tinted contrast zone
    val White = Color(0xFFF9FCF8)
    val Line = Color(0xFFC8D3C7)
    val LineSoft = Color(0xFFDCE5DA)

    // Semantics
    val Ok = Color(0xFF406B3F)
    val Warn = Color(0xFFC18A1A)
    val Error = Color(0xFF9B2F27)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF5D7D3)
    val OnErrorContainer = Color(0xFF3A0F0B)
}

object ScoutSurfaces {
    // Light
    val LightBackground = ScoutPalette.Paper
    val LightSurface = ScoutPalette.Paper
    val LightSurfaceDim = ScoutPalette.PaperDeep
    val LightSurfaceBright = Color(0xFFF7FBF5)
    val LightSurfaceContainerLowest = ScoutPalette.White
    val LightSurfaceContainerLow = Color(0xFFEBF2E8)
    val LightSurfaceContainer = Color(0xFFE2EBDD)
    val LightSurfaceContainerHigh = Color(0xFFD8E4D4)
    val LightSurfaceContainerHighest = Color(0xFFCEDCCB)
    val LightSurfaceVariant = Color(0xFFD3DECF)
    val LightOutline = Color(0xFF647663)
    val LightOutlineVariant = ScoutPalette.Line
    val LightOnSurface = ScoutPalette.Ink
    val LightOnSurfaceVariant = ScoutPalette.InkMuted
    val LightInverseSurface = Color(0xFF263128)
    val LightInverseOnSurface = Color(0xFFEAF2E7)

    // Dark
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
    // Hero surfaces intentionally use the brand palette rather than raw Material roles.
    val LoginBackground = listOf(ScoutPalette.ForestMist, ScoutPalette.PaperDeep)
    val LoginHero = listOf(ScoutPalette.Forest, ScoutPalette.Moss)
    val HomeHero = listOf(ScoutPalette.Forest, Color(0xFF274832))
    val HeroTextMuted = ScoutPalette.ForestMist
}

object ScoutStatusColors {
    // Status colors are part of the custom Scout design language.
    val PendingContainer = ScoutPalette.GoldSoft
    val OnPendingContainer = ScoutPalette.GoldWarningText
    val OkContainer = ScoutPalette.Lichen
    val OnOkContainer = ScoutPalette.MossDeep
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
    val Skautai = ScoutUnitPalette(Color(0xFFF1E2B0), Color(0xFFE3C664), Color(0xFF6F5412))
    val Vilkai = ScoutUnitPalette(Color(0xFFF2D8BE), Color(0xFFE4AC76), Color(0xFF8A4A16))
    val Gildija = ScoutUnitPalette(Color(0xFFDEE2DD), Color(0xFFC3CAC1), Color(0xFF465149))
    val VyrSkautai = ScoutUnitPalette(Color(0xFFE2D9EB), Color(0xFFC6B2D8), Color(0xFF5C3E76))
    val VyrSkautes = ScoutUnitPalette(Color(0xFFD4DCE9), Color(0xFFB0C1D6), Color(0xFF263F63))
    val Default = ScoutUnitPalette(ScoutPalette.Khaki, ScoutPalette.GoldSoft, ScoutPalette.Gold)
}

// Role-color aliases consumed by Theme.kt
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
