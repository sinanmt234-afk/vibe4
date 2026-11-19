package com.aurelia.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ObsidianBg = Color(0xFF0A0A0E)
val PureBlackBg = Color(0xFF000000)
val GlassSurface = Color(0xFF15151E)
val GlassSurfaceElevated = Color(0xFF1F1F2B)
val ChampagneGold = Color(0xFFD4AF37)
val SoftGold = Color(0xFFEBD284)
val IvoryText = Color(0xFFF6F4EE)
val MutedSilver = Color(0xFF9C9AA6)
val SubtleBorder = Color(0x28FFFFFF)
val GoldBorder = Color(0x45D4AF37)

private val AureliaColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = Color(0xFF141106),
    secondary = SoftGold,
    onSecondary = Color(0xFF141106),
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = GlassSurface,
    onSurface = IvoryText,
    surfaceVariant = GlassSurfaceElevated,
    onSurfaceVariant = MutedSilver,
    outline = SubtleBorder
)

private val AureliaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        color = IvoryText
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        color = IvoryText
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        color = IvoryText
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = IvoryText
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = MutedSilver
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = ChampagneGold
    )
)

@Composable
fun AureliaTheme(
    oledPureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (oledPureBlack) {
        AureliaColorScheme.copy(background = PureBlackBg, surface = Color(0xFF0C0C10))
    } else {
        AureliaColorScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AureliaTypography,
        content = content
    )
}
