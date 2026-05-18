package com.lanrhyme.micyou

actual object PlatformAdaptor {
    actual fun configureAudioOutput(): Any? = null

    actual fun restoreAudioOutput(token: Any?) {
    }

    actual suspend fun runAdbReverse(port: Int): Boolean = false

    actual fun cleanupTempFiles() {
        // iOS manages temp files through the OS
    }

    actual val usesSystemAudioSinkForVirtualOutput: Boolean = false
}
