package com.lanrhyme.micyou.plugin

import com.lanrhyme.micyou.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Minimal no-op PluginDataChannel for iOS.
 * The full plugin system is not yet migrated to iOS.
 */
class IosPluginDataChannel(
    override val id: String,
    override val config: DataChannelConfig = DataChannelConfig()
) : PluginDataChannel {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
    override val localPort: Int = 0

    override suspend fun connect(host: String, port: Int): Result<Unit> {
        Logger.w("IosPluginDataChannel", "Plugin data channel not supported on iOS")
        return Result.failure(UnsupportedOperationException("Plugin data channel not yet supported on iOS"))
    }

    override suspend fun bind(port: Int): Result<Unit> {
        Logger.w("IosPluginDataChannel", "Plugin data channel not supported on iOS")
        return Result.failure(UnsupportedOperationException("Plugin data channel not yet supported on iOS"))
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Plugin data channel not yet supported on iOS"))
    }

    override fun receive(): Flow<ByteArray> = emptyFlow()

    override suspend fun close() {
        _isConnected.value = false
    }
}

class IosPluginDataChannelProvider : PluginDataChannelProvider {
    private val channels = mutableMapOf<String, PluginDataChannel>()

    override fun createChannel(id: String, config: DataChannelConfig): PluginDataChannel {
        val channel = IosPluginDataChannel(id, config)
        channels[id] = channel
        return channel
    }

    override fun getChannel(id: String): PluginDataChannel? = channels[id]

    override fun closeChannel(id: String) {
        channels[id]?.let {
            channels.remove(id)
        }
    }

    override fun closeAllChannels() {
        channels.clear()
    }
}
