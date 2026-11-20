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
 * GapWise palette: plum and blush, taken from the app icon.
 *   Plum    #6B2A72  primary
 *   Blush   #FFB4D2  the icon's sun; tints containers
 *   Petal   #FBF6FA  background — near-white with a hint of pink
 *   Sky     #3E7CB1  "fresh" — you did it recently
 *   Rose    #E0569A  "around now" — the one loud colour, same family as the sun
 *   Violet  #7B4FC4  "past usual" — gentle, deliberately not red
 */
object GapColors {
    val Plum = Color(0xFF6B2A72)
    val Blush = Color(0xFFFFB4D2)
    val Petal = Color(0xFFFBF6FA)
    val Sky = Color(0xFF3E7CB1)
    val Rose = Color(0xFFE0569A)
    val Violet = Color(0xFF7B4FC4)
}

internal val Light = lightColorScheme(
    primary = GapColors.Plum,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6EC),
    onPrimaryContainer = Color(0xFF3E1546),
    secondary = Color(0xFF7A5868),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7DCEA),
    onSecondaryContainer = Color(0xFF2E1523),
    tertiary = Color(0xFF3E7CB1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E4FF),
    onTertiaryContainer = Color(0xFF001C38),
    background = GapColors.Petal,
    onBackground = Color(0xFF1F1A1E),
    surface = GapColors.Petal,
    onSurface = Color(0xFF1F1A1E),
    onSurfaceVariant = Color(0xFF4F4350),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F0F6),
    surfaceContainer = Color(0xFFF2E8F0),
    surfaceContainerHigh = Color(0xFFECE2EA),
    surfaceContainerHighest = Color(0xFFE6DBE4),
    outline = Color(0xFF807381),
    outlineVariant = Color(0xFFD3C2D1),
)

internal val Dark = darkColorScheme(
    primary = Color(0xFFF2B0EC),
    onPrimary = Color(0xFF4A1650),
    primaryContainer = Color(0xFF632C69),
    onPrimaryContainer = Color(0xFFFFD6EC),
    secondary = Color(0xFFE4BDD0),
    onSecondary = Color(0xFF442938),
    secondaryContainer = Color(0xFF5C3F4F),
    onSecondaryContainer = Color(0xFFF7DCEA),
    tertiary = Color(0xFFA2C9FF),
    onTertiary = Color(0xFF00315C),
    tertiaryContainer = Color(0xFF1E4876),
    onTertiaryContainer = Color(0xFFD3E4FF),
    background = Color(0xFF171216),
    onBackground = Color(0xFFEBDFE7),
    surface = Color(0xFF171216),
    onSurface = Color(0xFFEBDFE7),
    onSurfaceVariant = Color(0xFFD3C2D1),
    surfaceContainerLowest = Color(0xFF110D10),
    surfaceContainerLow = Color(0xFF1F1A1E),
    surfaceContainer = Color(0xFF241E22),
    surfaceContainerHigh = Color(0xFF2E282C),
    surfaceContainerHighest = Color(0xFF393337),
    outline = Color(0xFF9C8D9B),
    outlineVariant = Color(0xFF4F4350),
)

/** Status colours that sit outside the Material roles. */
@Immutable
data class StatusColors(val fresh: Color, val due: Color, val past: Color, val onDue: Color)

private val LightStatus = StatusColors(fresh = GapColors.Sky, due = GapColors.Rose, past = GapColors.Violet, onDue = Color.White)
private val DarkStatus = StatusColors(fresh = Color(0xFF8FC0EC), due = Color(0xFFFF8CC0), past = Color(0xFFC3A6FF), onDue = Color(0xFF3E0021))

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
