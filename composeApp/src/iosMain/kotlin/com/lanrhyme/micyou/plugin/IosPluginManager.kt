package com.lanrhyme.micyou.plugin

import com.lanrhyme.micyou.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal PluginManager for iOS.
 *
 * Since the full plugin JVM runtime (class loading, security manager, etc.)
 * is not applicable to iOS, this provides a stub that returns empty state.
 */
class IosPluginManager(
    private val pluginHost: PluginHost
) {
    private val _plugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val plugins: StateFlow<List<PluginInfo>> = _plugins.asStateFlow()

    fun scanPlugins() {
        Logger.d("IosPluginManager", "Plugin scanning not yet supported on iOS")
    }

    fun importPlugin(filePath: String): Result<PluginInfo> {
        Logger.w("IosPluginManager", "Plugin import not yet supported on iOS")
        return Result.failure(UnsupportedOperationException("Plugin import not yet supported on iOS"))
    }

    fun enablePlugin(pluginId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Plugin management not yet supported on iOS"))
    }

    fun disablePlugin(pluginId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Plugin management not yet supported on iOS"))
    }

    fun deletePlugin(pluginId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Plugin management not yet supported on iOS"))
    }

    fun getPlugin(pluginId: String): Plugin? = null

    fun getPluginSettingsProvider(pluginId: String): PluginSettingsProvider? = null

    fun getPluginUIProvider(pluginId: String): PluginUIProvider? = null
}
