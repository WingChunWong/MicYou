package com.lanrhyme.micyou

import com.lanrhyme.micyou.plugin.IosPluginHostImpl
import com.lanrhyme.micyou.plugin.PluginHost

actual fun createPluginHost(
    audioEngine: AudioEngine,
    showSnackbarCallback: (String) -> Unit,
    showNotificationCallback: (String, String) -> Unit
): PluginHost {
    return IosPluginHostImpl(
        audioEngine = audioEngine,
        showSnackbarCallback = showSnackbarCallback,
        showNotificationCallback = showNotificationCallback
    )
}
