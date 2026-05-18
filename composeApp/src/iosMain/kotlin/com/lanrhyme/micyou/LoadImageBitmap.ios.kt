package com.lanrhyme.micyou

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create

actual fun loadImageBitmap(path: String): ImageBitmap? {
    return try {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) {
            Logger.w("BackgroundImage", "Image file not found: $path")
            return null
        }
        val data = NSData.create(contentsOfFile = path) ?: return null
        val bytes = ByteArray(data.length.toInt()).apply {
            data.getBytes(this.refTo(0), data.length)
        }
        val image = Image.makeFromEncoded(bytes)
        image.toComposeImageBitmap()
    } catch (e: Exception) {
        Logger.e("BackgroundImage", "Failed to load image: $path", e)
        null
    }
}
