package com.lanrhyme.micyou

import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplication
import platform.Foundation.NSURL

/**
 * iOS UpdateChecker platform-specific implementations.
 *
 * iOS distributes unsigned and cannot auto-install updates.
 * Instead, the update dialog redirects users to the GitHub release page.
 */
actual suspend fun writeToFile(path: String, writer: suspend ((ByteArray, Int, Int) -> Unit) -> Unit) {
    val fileManager = NSFileManager.defaultManager
    val parentDir = path.substringBeforeLast("/")
    if (parentDir.isNotEmpty() && !fileManager.fileExistsAtPath(parentDir)) {
        fileManager.createDirectoryAtPath(parentDir, true, null, null)
    }
    // Accumulate bytes and write to file
    val chunks = mutableListOf<ByteArray>()
    writer { bytes, offset, length ->
        chunks.add(bytes.copyOfRange(offset, offset + length))
    }
    val totalSize = chunks.sumOf { it.size }
    val allBytes = ByteArray(totalSize)
    var pos = 0
    for (chunk in chunks) {
        chunk.copyInto(allBytes, pos)
        pos += chunk.size
    }
    NSData.create(bytes = allBytes, length = totalSize.toULong())
        ?.writeToFile(path, true)
}

actual fun findPlatformAsset(assets: List<GitHubAsset>): GitHubAsset? {
    // Look for iOS IPA or any iOS-related asset
    return assets.find { it.name.contains("ios", ignoreCase = true) }
        ?: assets.find { it.name.endsWith(".ipa", ignoreCase = true) }
}

actual fun getUpdateDownloadPath(fileName: String): String {
    val tmpDir = NSTemporaryDirectory() ?: ""
    return "$tmpDir/$fileName"
}

actual fun installUpdate(filePath: String) {
    // iOS cannot auto-install updates (unsigned distribution).
    // Open the GitHub release page as a reminder instead.
    Logger.i("UpdateInstaller", "iOS update downloaded to $filePath — opening release page as reminder")
    val releaseUrl = "https://github.com/LanRhyme/MicYou/releases/latest"
    NSURL.URLWithString(releaseUrl)?.let { url ->
        UIApplication.sharedApplication.openURL(url)
    }
}

actual fun getMirrorOs(): String = "ios"

actual fun getMirrorArch(): String = "arm64"

actual fun getPlatformName(): String = "iOS"

/**
 * iOS always acts like a portable app — users are redirected to
 * the release page instead of downloading/installing in-app.
 */
actual fun isPortableApp(): Boolean = true
