package com.lanrhyme.micyou

import platform.Foundation.NSLog

/**
 * iOS Logger implementation using NSLog.
 */
class IosLogger : LoggerImpl {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val levelStr = when (level) {
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
        }
        val logMessage = "[$levelStr][$tag] $message"
        NSLog("%s", logMessage)
        throwable?.let {
            NSLog("%s", it.stackTraceToString())
        }
    }

    override fun getLogFilePath(): String? = null
}
