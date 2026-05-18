package com.lanrhyme.micyou

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun openPluginFileChooser(scope: CoroutineScope, onResult: (String?) -> Unit) {
    scope.launch {
        try {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(extensions = listOf("zip", "jar"))
            )
            val savedPath = file?.let { copyPluginToCache(it) }
            onResult(savedPath)
        } catch (e: Exception) {
            Logger.e("PluginFileChooser", "Failed to pick plugin file", e)
            onResult(null)
        }
    }
}

private suspend fun copyPluginToCache(file: PlatformFile): String? {
    return try {
        val bytes = file.readBytes()

        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory, NSUserDomainMask, true
        )
        val cacheDir = paths.firstOrNull() as? String ?: return null
        val pluginDir = "$cacheDir/plugin_imports"

        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(pluginDir)) {
            fileManager.createDirectoryAtPath(pluginDir, true, null, null)
        }

        val fileName = file.name
        val outputPath = "$pluginDir/$fileName"

        platform.Foundation.NSData.create(bytes = bytes, length = bytes.size.toULong())
            ?.writeToFile(outputPath, true)
        outputPath
    } catch (e: Exception) {
        Logger.e("PluginFileChooser", "Failed to copy plugin file", e)
        null
    }
}
