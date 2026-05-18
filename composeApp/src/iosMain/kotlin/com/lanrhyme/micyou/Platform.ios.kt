package com.lanrhyme.micyou

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lanrhyme.micyou.theme.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosPlatform : Platform {
    override val name: String = "iOS ${NSProcessInfo.processInfo.operatingSystemVersionString}"
    override val type: PlatformType = PlatformType.Ios
    override val ipAddress: String = "Client"
    override val ipAddresses: List<String> = listOf("Client")
}

actual fun getPlatform(): Platform = IosPlatform()

actual fun getAppVersion(): String {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
        ?: NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
        ?: "dev"
}

actual fun openUrl(url: String) {
    platform.Foundation.NSURL.URLWithString(url)?.let { nsUrl ->
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
    Logger.d("Platform", "Copied to clipboard: ${text.take(50)}...")
}

actual suspend fun isPortAllowed(port: Int, protocol: String): Boolean = true

actual suspend fun addFirewallRule(port: Int, protocol: String): Result<Unit> = Result.success(Unit)

@Composable
actual fun getDynamicColorScheme(isDark: Boolean, paletteStyle: PaletteStyle): ColorScheme? = null

actual fun isDynamicColorSupported(): Boolean = false

actual fun getDynamicSeedColor(): Long? = null

actual fun getAudioSourceOptions(): List<AudioSourceOption> {
    return listOf(
        AudioSourceOption(name = "Built-in Mic", label = "Built-in Microphone")
    )
}

actual fun isVirtualDeviceInstalled(): Boolean = false

actual suspend fun installVBCable() {
    // No-op on iOS
}

actual fun getVBCableInstallProgress(): Flow<String?> = flowOf(null)

actual fun isWindowsPlatform(): Boolean = false

actual fun isMacOSPlatform(): Boolean = false

@Composable
actual fun QrCodeImage(content: String, modifier: Modifier, sizeDp: Int) {
    // TODO: Generate QR code image for iOS
}
