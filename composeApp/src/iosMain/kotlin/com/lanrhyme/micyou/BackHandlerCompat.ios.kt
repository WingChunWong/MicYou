package com.lanrhyme.micyou

import androidx.compose.runtime.Composable

/**
 * iOS has no hardware back button. This is a no-op.
 * Navigation is handled by UINavigationController swipe gestures
 * or in-app back buttons.
 */
@Composable
actual fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) {
    // No hardware back button on iOS
}
