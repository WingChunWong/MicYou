package com.lanrhyme.micyou

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual object BackgroundImagePicker {
    actual fun pickImage(scope: CoroutineScope, onResult: (String?) -> Unit) {
        scope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.Image
                )
                val savedPath = file?.let { copyToDocuments(it) }
                onResult(savedPath)
            } catch (e: Exception) {
                Logger.e("BackgroundImagePicker", "Failed to pick image", e)
                onResult(null)
            }
        }
    }

    private suspend fun copyToDocuments(file: PlatformFile): String? {
        return try {
            val bytes = file.readBytes()

            val paths = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            )
            val documentsDir = paths.firstOrNull() as? String ?: return null
            val backgroundDir = "$documentsDir/backgrounds"

            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(backgroundDir)) {
                fileManager.createDirectoryAtPath(backgroundDir, true, null, null)
            }

            val extension = file.extension
            val fileName = "custom_background.$extension"
            val outputPath = "$backgroundDir/$fileName"

            bytes.toNSData()?.writeToFile(outputPath, true)
            outputPath
        } catch (e: Exception) {
            Logger.e("BackgroundImagePicker", "Failed to copy image", e)
            null
        }
    }
}

private fun ByteArray.toNSData(): platform.Foundation.NSData? {
    return platform.Foundation.NSData.create(bytes = this, length = this.size.toULong())
}
