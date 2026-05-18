package com.lanrhyme.micyou.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * iOS fallback: returns default Material 3 color scheme using the seed color as primary.
 * material-kolor is not available on iOS (compiled with incompatible Kotlin version).
 */
actual fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle,
    contrastLevel: Double
): ColorScheme {
    // iOS falls back to a simple Material 3 scheme using the seed as primary
    return if (isDark) {
        darkColorScheme(primary = keyColor)
    } else {
        lightColorScheme(primary = keyColor)
    }
}
