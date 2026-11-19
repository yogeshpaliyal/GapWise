package com.yogeshpaliyal.gapwise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Brand = Color(0xFF2F6B5E)

private val Light = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8E0),
    onPrimaryContainer = Color(0xFF0C2B24),
    background = Color(0xFFFBF8F3),
    surface = Color(0xFFFBF8F3),
    surfaceContainer = Color(0xFFF2EEE7),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD1C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1C4F44),
    background = Color(0xFF141614),
    surface = Color(0xFF141614),
)

@Composable
fun GapwiseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
