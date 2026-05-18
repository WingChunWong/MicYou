package com.lanrhyme.micyou

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS DeviceDiscoveryManager stub.
 * iOS is a client, not a server, so device discovery is not needed.
 */
actual class DeviceDiscoveryManager actual constructor() {
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    actual val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    actual fun startDiscovery() {
        // No-op on iOS — iOS is the client, not the server
    }

    actual fun stopDiscovery() {
        _discoveredDevices.value = emptyList()
        _isDiscovering.value = false
    }
}
