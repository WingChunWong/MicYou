package com.lanrhyme.micyou.plugin

import com.lanrhyme.micyou.AudioEngine
import com.lanrhyme.micyou.Settings
import com.lanrhyme.micyou.SettingsFactory

/**
 * iOS (mobile) PluginHost implementation.
 *
 * Extends BasePluginHostImpl with iOS-specific platform info and a no-op data channel provider,
 * since the full plugin system is not yet migrated to iOS.
 */
class IosPluginHostImpl(
    audioEngine: AudioEngine,
    private val showSnackbarCallback: (String) -> Unit,
    private val showNotificationCallback: (String, String) -> Unit
) : BasePluginHostImpl(audioEngine, SettingsFactory.getSettings()) {

    override val dataChannelProvider: PluginDataChannelProvider = IosPluginDataChannelProvider()

    override fun showSnackbar(message: String) {
        showSnackbarCallback(message)
    }

    override fun showNotification(title: String, message: String) {
        showNotificationCallback(title, message)
    }

    override val platform: PluginHost.PlatformInfo = PluginHost.PlatformInfo(
        name = "iOS",
        version = "",
        isDesktop = false,
        isMobile = true
    )
}
