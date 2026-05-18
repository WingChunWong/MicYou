package com.lanrhyme.micyou

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun setAppLocale(languageCode: String) {
    // iOS locale is system-level; individual app locale switching
    // requires custom infrastructure. For now, this is a no-op.
    // The app relies on system locale by default.
}

actual fun readResourceFile(path: String): String? {
    return try {
        val fullPath = "composeResources/micyou.composeapp.generated.resources/files/$path"
        val bundlePath = NSBundle.mainBundle.pathForResource(fullPath, ofType = null)
            ?: return null
        NSString.stringWithContentsOfFile(bundlePath, NSUTF8StringEncoding, null) as? String
    } catch (e: Exception) {
        Logger.e("Localization", "Failed to read resource file: $path - ${e.message}")
        null
    }
}
