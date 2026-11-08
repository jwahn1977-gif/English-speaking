package com.jwahn.englishspeaking.ui.theme

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

val Blue = Color(0xFF2F6FED)
val Amber = Color(0xFFFFB800)
val Green = Color(0xFF22A55B)
val Red = Color(0xFFE5484D)

private val Light = lightColorScheme(
    primary = Blue, secondary = Amber, tertiary = Green, error = Red,
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8AB0FF), secondary = Amber, tertiary = Color(0xFF5BD68E), error = Color(0xFFFF8A8E),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current).copy(primary = Color(0xFF8AB0FF))
            else dynamicLightColorScheme(LocalContext.current).copy(primary = Blue, secondary = Amber)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
