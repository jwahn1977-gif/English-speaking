package com.jwahn.englishspeaking.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
fun AppTheme(fontScale: Float = 1f, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current).copy(primary = Color(0xFF8AB0FF))
            else dynamicLightColorScheme(LocalContext.current).copy(primary = Blue, secondary = Amber)
        dark -> Dark
        else -> Light
    }
    // 앱 안의 글자 크기 설정(보통/크게/아주 크게)을 시스템 글자 크기 위에 곱한다
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
