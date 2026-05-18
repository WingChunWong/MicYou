package com.lanrhyme.micyou

import com.lanrhyme.micyou.plugin.IosPluginManager
import com.lanrhyme.micyou.plugin.Plugin
import com.lanrhyme.micyou.plugin.PluginHost
import com.lanrhyme.micyou.plugin.PluginInfo
import com.lanrhyme.micyou.plugin.PluginSettingsProvider
import com.lanrhyme.micyou.plugin.PluginUIProvider
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

class IosPluginManagerProvider(private val manager: IosPluginManager) : PluginManagerProvider {
    override val plugins: StateFlow<List<PluginInfo>> = manager.plugins

    override fun scanPlugins() = manager.scanPlugins()

    override fun importPlugin(pluginFilePath: String): Result<PluginInfo> = manager.importPlugin(pluginFilePath)

    override fun enablePlugin(pluginId: String): Result<Unit> = manager.enablePlugin(pluginId)

    override fun disablePlugin(pluginId: String): Result<Unit> = manager.disablePlugin(pluginId)

    override fun deletePlugin(pluginId: String): Result<Unit> = manager.deletePlugin(pluginId)

    override fun getPlugin(pluginId: String): Plugin? = manager.getPlugin(pluginId)

    override fun getPluginSettingsProvider(pluginId: String): PluginSettingsProvider? = manager.getPluginSettingsProvider(pluginId)

    override fun getPluginUIProvider(pluginId: String): PluginUIProvider? = manager.getPluginUIProvider(pluginId)
}

actual fun createPluginManager(
    pluginsDirPath: String,
    pluginHost: PluginHost,
    appLanguageProvider: () -> String,
    appStringProvider: ((String) -> String)?
): PluginManagerProvider? {
    return IosPluginManagerProvider(
        IosPluginManager(pluginHost)
    )
}

actual fun getPluginsDirPath(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    )
    val documentsDir = paths.firstOrNull() as? String ?: return ""
    val pluginDir = "$documentsDir/plugins"

    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(pluginDir)) {
        fileManager.createDirectoryAtPath(pluginDir, true, null, null)
    }

    return pluginDir
}
