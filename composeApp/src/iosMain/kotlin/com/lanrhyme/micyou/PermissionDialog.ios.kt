package com.lanrhyme.micyou

import androidx.compose.runtime.Composable

/**
 * iOS doesn't use Android-style runtime permissions for microphone.
 * Microphone permission is handled via Info.plist privacy description
 * (NSMicrophoneUsageDescription).
 */
@Composable
actual fun AndroidPermissionManagementSection(cardOpacity: Float) {
    // No implementation needed for iOS
}

actual fun hasAllRequiredPermissions(permissions: List<PermissionState>): Boolean = true

@Composable
actual fun PermissionDialog(
    permissions: List<PermissionState>,
    onDismiss: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit
) {
    // iOS handles permissions via system dialogs triggered by first use
}
