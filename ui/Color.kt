package lt.skautai.android.ui.theme

import androidx.compose.ui.graphics.Color

object ScoutPalette {
    val Forest = Color(0xFF214D34)
    val ForestDeep = Color(0xFF0C1A11)
    val ForestSoft = Color(0xFFBFD9BF)
    val ForestMist = Color(0xFFE4EEDF)

    val Moss = Color(0xFF3E5B43)
    val MossDeep = Color(0xFF102016)
    val MossSoft = Color(0xFFD6E4D2)
    val MossMist = Color(0xFFEAF1E6)
    val Lichen = Color(0xFFE1EAD1)

    val Khaki = Color(0xFFF1E3C8)
    val Gold = Color(0xFF8B6A2B)
    val GoldDeep = Color(0xFF2B1D00)
    val GoldSoft = Color(0xFFF0E0AE)
    val GoldWarning = Color(0xFFFFE08A)
    val GoldWarningText = Color(0xFF4A3700)

    val Earth = Color(0xFFE9DDCF)
    val White = Color(0xFFFFFFFF)
    val BlackScrim = Color(0xFF000000)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
}

object ScoutSurfaces {
    val LightBackground = Color(0xFFF1F4EE)
    val LightSurface = Color(0xFFF8FBF5)
    val LightSurfaceDim = Color(0xFFE0E6DC)
    val LightSurfaceBright = Color(0xFFF8FBF5)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFF2F6EF)
    val LightSurfaceContainer = Color(0xFFECEFE8)
    val LightSurfaceContainerHigh = Color(0xFFE5EBE1)
    val LightSurfaceContainerHighest = Color(0xFFDDE5DA)
    val LightSurfaceVariant = Color(0xFFD8E0D5)
    val LightOutline = Color(0xFF68786B)
    val LightOutlineVariant = Color(0xFFBCC7BA)
    val LightOnSurface = Color(0xFF161C17)
    val LightOnSurfaceVariant = Color(0xFF3F4A41)
    val LightInverseSurface = Color(0xFF2B322C)
    val LightInverseOnSurface = Color(0xFFEFF4EC)

    val DarkBackground = Color(0xFF0D120E)
    val DarkSurface = Color(0xFF141A15)
    val DarkSurfaceDim = Color(0xFF0D120E)
    val DarkSurfaceBright = Color(0xFF343B35)
    val DarkSurfaceContainerLowest = Color(0xFF080C09)
    val DarkSurfaceContainerLow = Color(0xFF171D18)
    val DarkSurfaceContainer = Color(0xFF1C231E)
    val DarkSurfaceContainerHigh = Color(0xFF273028)
    val DarkSurfaceContainerHighest = Color(0xFF323B33)
    val DarkSurfaceVariant = Color(0xFF3F4A41)
    val DarkOutline = Color(0xFF859487)
    val DarkOutlineVariant = Color(0xFF3F4A41)
    val DarkOnSurface = Color(0xFFE0E4DD)
    val DarkOnSurfaceVariant = Color(0xFFBEC9BE)
    val DarkInverseSurface = Color(0xFFE0E6DC)
    val DarkInverseOnSurface = Color(0xFF2B322C)
}

object ScoutGradients {
    val LoginBackground = listOf(ScoutPalette.MossMist, ScoutSurfaces.LightSurfaceContainerHigh)
    val LoginHero = listOf(Color(0xFF23452A), Color(0xFF315F38))
    val HomeHero = listOf(ScoutPalette.MossMist, Color(0xFFDDE9D4))
    val HeroTextMuted = Color(0xFFD6E5D3)
}

object ScoutStatusColors {
    val PendingContainer = ScoutPalette.GoldWarning
    val OnPendingContainer = ScoutPalette.GoldWarningText
}

data class ScoutUnitPalette(
    val cardTone: Color,
    val iconTone: Color,
    val accent: Color
)

object ScoutUnitColors {
    val PatyreSkautai = ScoutUnitPalette(
        cardTone = Color(0xFFF4D7D2),
        iconTone = Color(0xFFE9B8B0),
        accent = Color(0xFF8E2F25)
    )
    val Skautai = ScoutUnitPalette(
        cardTone = Color(0xFFF5E7B6),
        iconTone = Color(0xFFE7CB69),
        accent = Color(0xFF6F5412)
    )
    val Vilkai = ScoutUnitPalette(
        cardTone = Color(0xFFF7DDC4),
        iconTone = Color(0xFFEAB27C),
        accent = Color(0xFF8A4A16)
    )
    val Gildija = ScoutUnitPalette(
        cardTone = Color(0xFFE1E5E0),
        iconTone = Color(0xFFC6CDC4),
        accent = Color(0xFF465149)
    )
    val VyrSkautai = ScoutUnitPalette(
        cardTone = Color(0xFFE6DDF0),
        iconTone = Color(0xFFCBB7DE),
        accent = Color(0xFF5C3E76)
    )
    val VyrSkautes = ScoutUnitPalette(
        cardTone = Color(0xFFD8E1EF),
        iconTone = Color(0xFFB4C6DD),
        accent = Color(0xFF263F63)
    )
    val Default = ScoutUnitPalette(
        cardTone = ScoutPalette.Khaki,
        iconTone = ScoutPalette.GoldSoft,
        accent = Color(0xFF7A5A2E)
    )
}

val ForestPrimary = ScoutPalette.Forest
val ForestOnPrimary = ScoutPalette.White
val ForestPrimaryContainer = ScoutPalette.ForestSoft
val ForestOnPrimaryContainer = ScoutPalette.ForestDeep

val MossSecondary = ScoutPalette.Moss
val MossOnSecondary = ScoutPalette.White
val MossSecondaryContainer = ScoutPalette.MossSoft
val MossOnSecondaryContainer = ScoutPalette.MossDeep

val GoldTertiary = ScoutPalette.Gold
val GoldOnTertiary = ScoutPalette.White
val GoldTertiaryContainer = ScoutPalette.GoldSoft
val GoldOnTertiaryContainer = ScoutPalette.GoldDeep

val ErrorRed = ScoutPalette.Error
val OnErrorRed = ScoutPalette.OnError
val ErrorContainer = ScoutPalette.ErrorContainer
val OnErrorContainer = ScoutPalette.OnErrorContainer

val White = ScoutPalette.White
