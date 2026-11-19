package com.yogeshpaliyal.gapwise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.R

/*
 * Gapwise palette: cool and calm, with one warm accent reserved for "around now".
 *   Ink     #10404A  primary — deep teal ink
 *   Mist    #EEF3F1  background — cool, never cream
 *   Moss    #4E7D57  "fresh" — you did it recently
 *   Marigold#E9A93A  "around now" — the only warm colour in the app
 *   Plum    #7A4467  "past usual" — gentle, deliberately not red
 */
object GapColors {
    val Ink = Color(0xFF10404A)
    val Mist = Color(0xFFEEF3F1)
    val Moss = Color(0xFF4E7D57)
    val Marigold = Color(0xFFE9A93A)
    val Plum = Color(0xFF7A4467)
}

internal val Light = lightColorScheme(
    primary = GapColors.Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6E7EC),
    onPrimaryContainer = Color(0xFF00262D),
    secondary = GapColors.Moss,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3EBD3),
    onSecondaryContainer = Color(0xFF0E2A14),
    tertiary = Color(0xFF7A5A00),
    tertiaryContainer = Color(0xFFFFDFA3),
    onTertiaryContainer = Color(0xFF261A00),
    background = GapColors.Mist,
    onBackground = Color(0xFF151D1E),
    surface = GapColors.Mist,
    onSurface = Color(0xFF151D1E),
    onSurfaceVariant = Color(0xFF3F4A4C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6FAF8),
    surfaceContainer = Color(0xFFE6EDEB),
    surfaceContainerHigh = Color(0xFFDDE6E4),
    surfaceContainerHighest = Color(0xFFD3DDDB),
    outline = Color(0xFF6F7A7C),
    outlineVariant = Color(0xFFBEC9CB),
)

internal val Dark = darkColorScheme(
    primary = Color(0xFF8ED0DB),
    onPrimary = Color(0xFF00363E),
    primaryContainer = Color(0xFF1C4E58),
    onPrimaryContainer = Color(0xFFC6E7EC),
    secondary = Color(0xFFA3D1A6),
    onSecondary = Color(0xFF0E3818),
    secondaryContainer = Color(0xFF2F5135),
    onSecondaryContainer = Color(0xFFD3EBD3),
    tertiary = Color(0xFFF3C56B),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDFA3),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDDE4E4),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDDE4E4),
    onSurfaceVariant = Color(0xFFBEC9CB),
    surfaceContainerLowest = Color(0xFF091011),
    surfaceContainerLow = Color(0xFF161D1E),
    surfaceContainer = Color(0xFF1A2122),
    surfaceContainerHigh = Color(0xFF242B2C),
    surfaceContainerHighest = Color(0xFF2F3637),
    outline = Color(0xFF889496),
    outlineVariant = Color(0xFF3F4A4C),
)

/** Status colours that sit outside the Material roles. */
@Immutable
data class StatusColors(val fresh: Color, val due: Color, val past: Color, val onDue: Color)

private val LightStatus = StatusColors(fresh = GapColors.Moss, due = GapColors.Marigold, past = GapColors.Plum, onDue = Color(0xFF2A1C00))
private val DarkStatus = StatusColors(fresh = Color(0xFF8FC597), due = Color(0xFFF3C56B), past = Color(0xFFE3A9CF), onDue = Color(0xFF2A1C00))

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

/* Type: Bitter (slab serif) carries numbers and titles; DM Sans does the work. */
private fun bitter(weight: Int) = Font(
    R.font.bitter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun dmSans(weight: Int) = Font(
    R.font.dm_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Bitter = FontFamily(bitter(400), bitter(500), bitter(600), bitter(700), bitter(800))
val DmSans = FontFamily(dmSans(400), dmSans(500), dmSans(600), dmSans(700))

private val base = Typography()

private fun TextStyle.sans() = copy(fontFamily = DmSans)
private fun TextStyle.slab() = copy(fontFamily = Bitter)

private val GapTypography = Typography(
    displayLarge = base.displayLarge.slab().copy(fontWeight = FontWeight(700), letterSpacing = (-0.02).em),
    displayMedium = base.displayMedium.slab().copy(fontWeight = FontWeight(700), letterSpacing = (-0.02).em),
    displaySmall = base.displaySmall.slab().copy(fontWeight = FontWeight(700), letterSpacing = (-0.01).em),
    headlineLarge = base.headlineLarge.slab().copy(fontWeight = FontWeight(700)),
    headlineMedium = base.headlineMedium.slab().copy(fontWeight = FontWeight(700)),
    headlineSmall = base.headlineSmall.slab().copy(fontWeight = FontWeight(600)),
    titleLarge = base.titleLarge.slab().copy(fontWeight = FontWeight(600)),
    titleMedium = base.titleMedium.sans().copy(fontWeight = FontWeight(600)),
    titleSmall = base.titleSmall.sans().copy(fontWeight = FontWeight(600)),
    bodyLarge = base.bodyLarge.sans(),
    bodyMedium = base.bodyMedium.sans(),
    bodySmall = base.bodySmall.sans(),
    labelLarge = base.labelLarge.sans().copy(fontWeight = FontWeight(600)),
    labelMedium = base.labelMedium.sans().copy(fontWeight = FontWeight(500)),
    labelSmall = base.labelSmall.sans().copy(fontWeight = FontWeight(500)),
)

/** The big "days since" numeral. */
val CountStyle = TextStyle(fontFamily = Bitter, fontWeight = FontWeight(700), fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.03).em)

/* Shapes: generous, and larger for bigger things. */
private val GapShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GapwiseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = if (dark) Dark else Light
    androidx.compose.runtime.CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = GapShapes,
            typography = GapTypography,
            content = content,
        )
    }
}
