package com.lanrhyme.micyou

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import platform.AVFAudio.*
import platform.Foundation.NSLog
import platform.darwin.*
import platform.posix.*
import kotlin.math.sqrt
import kotlinx.cinterop.*

// ============================================================================
// POSIX socket helpers for Kotlin/Native iOS
// ============================================================================

private class PosixTcpSocket(private val fd: Int) {
    fun send(data: ByteArray): Int {
        return data.usePinned { pinned ->
            posix_send(fd, pinned.addressOf(0), data.size.toULong(), 0).toInt()
        }
    }

    fun recv(buffer: ByteArray): Int {
        return buffer.usePinned { pinned ->
            posix_recv(fd, pinned.addressOf(0), buffer.size.toULong(), 0).toInt()
        }
    }

    fun close() {
        posix_close(fd)
    }
}

private class PosixUdpSocket(private val fd: Int) {
    fun sendto(data: ByteArray, addr: sockaddr_in): Int {
        return data.usePinned { pinned ->
            val addrCopy = addr
            posix_sendto(
                fd, pinned.addressOf(0), data.size.toULong(), 0,
                addrCopy.readValue<sockaddr>().ptr, sizeOf<sockaddr_in>().toUInt()
            ).toInt()
        }
    }

    fun close() {
        posix_close(fd)
    }
}

private fun createSocket(type: Int): Int {
    val fd = posix_socket(AF_INET, type, 0)
    if (fd < 0) throw RuntimeException("Failed to create socket: ${strerror(posix_errno())?.toKString()}")
    // Set non-blocking
    val flags = fcntl(fd, F_GETFL, 0)
    fcntl(fd, F_SETFL, flags or O_NONBLOCK)
    return fd
}

private fun makeSockaddrIn(host: String, port: Int): sockaddr_in {
    val addr = nativeHeap.alloc<sockaddr_in>().apply {
        sin_family = AF_INET.toUShort()
        sin_port = htons(port.toUShort())
    }
    inet_pton(AF_INET, host, addr.sin_addr.ptr)
    return addr
}

private fun htons(value: UShort): UShort {
    return ((value.toInt() shr 8) or (value.toInt() shl 8)).toUShort()
}

private fun htonl(value: Int): Int {
    return ((value ushr 24) and 0xFF) or
            ((value ushr 8) and 0xFF00) or
            ((value shl 8) and 0xFF0000) or
            (value shl 24)
}

// ============================================================================
// iOS AudioEngine — AVAudioEngine capture + POSIX socket streaming
// ============================================================================

actual class AudioEngine actual constructor() {
    private val _streamState = MutableStateFlow(StreamState.Idle)
    actual val streamState: Flow<StreamState> = _streamState.asStateFlow()

    private val _audioLevels = MutableStateFlow(0f)
    actual val audioLevels: Flow<Float> = _audioLevels.asStateFlow()

    private val _rawSpectrum = MutableStateFlow(FloatArray(0))
    actual val rawSpectrum: Flow<FloatArray> = _rawSpectrum.asStateFlow()

    private val _processedSpectrum = MutableStateFlow(FloatArray(0))
    actual val processedSpectrum: Flow<FloatArray> = _processedSpectrum.asStateFlow()

    private val _audioLevelData = MutableStateFlow(AudioLevelData.SILENT)
    actual val audioLevelData: Flow<AudioLevelData> = _audioLevelData.asStateFlow()

    private val _audioMetrics = MutableStateFlow<AudioMetrics?>(null)
    actual val audioMetrics: Flow<AudioMetrics?> = _audioMetrics.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    actual val lastError: Flow<String?> = _lastError.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    actual val isMuted: Flow<Boolean> = _isMuted.asStateFlow()

    private val _webUrl = MutableStateFlow("")
    actual val webUrl: Flow<String> = _webUrl.asStateFlow()

    private val _webClientCount = MutableStateFlow(0)
    actual val webClientCount: Flow<Int> = _webClientCount.asStateFlow()

    actual val installProgress: Flow<String?> = flowOf(null)

    private val proto = ProtoBuf {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val startStopMutex = Mutex()

    // Audio capture
    private var audioEngine: AVAudioEngine? = null
    private val audioPacketChannel = Channel<ByteArray>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Sequence numbering
    @Volatile
    private var sequenceNumber = 0

    // FEC
    private val fecGroupBuffer = mutableListOf<ByteArray>()
    private var fecGroupStartSeq = 0
    private val FEC_GROUP_SIZE = 12

    // Heartbeat
    @Volatile
    private var lastPingReceivedTime = 0L
    private val HEARTBEAT_TIMEOUT_MS = 5000L

    companion object {
        private const val MAX_UDP_CONSECUTIVE_FAILURES = 500
        private const val CHECK_1 = "MicYouCheck1"
        private const val CHECK_2 = "MicYouCheck2"
    }

    actual suspend fun start(
        ip: String,
        port: Int,
        mode: ConnectionMode,
        isClient: Boolean,
        sampleRate: SampleRate,
        channelCount: ChannelCount,
        audioFormat: AudioFormat
    ) {
        if (!isClient) return
        Logger.i("AudioEngine", "Starting iOS AudioEngine: mode=$mode, ip=$ip, port=$port")

        _lastError.value = null
        _streamState.value = StreamState.Connecting

        startStopMutex.withLock {
            job?.cancel()
            job = scope.launch {
                try {
                    startInternal(ip, port, mode, sampleRate, channelCount, audioFormat)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("AudioEngine", "Connection failed: ${e.message}", e)
                    _streamState.value = StreamState.Error
                    _lastError.value = e.message ?: "Connection failed"
                } finally {
                    cleanup()
                }
            }
        }
    }

    private suspend fun startInternal(
        ip: String,
        port: Int,
        mode: ConnectionMode,
        sampleRate: SampleRate,
        channelCount: ChannelCount,
        audioFormat: AudioFormat
    ) = withContext(Dispatchers.Default) {
        val targetIp = if (mode == ConnectionMode.Usb) "127.0.0.1" else ip

        // 1. Setup audio capture
        setupAudioCapture(sampleRate, channelCount)

        // 2. TCP connect + handshake
        Logger.i("AudioEngine", "Connecting TCP to $targetIp:$port")
        val tcpFd = createSocket(SOCK_STREAM)
        val tcpAddr = makeSockaddrIn(targetIp, port)
        val addrPtr = tcpAddr.ptr.reinterpret<sockaddr>()
        val connectResult = posix_connect(tcpFd, addrPtr, sizeOf<sockaddr_in>().toUInt())
        if (connectResult < 0) {
            posix_close(tcpFd)
            throw RuntimeException("TCP connect failed: ${strerror(posix_errno())?.toKString()}")
        }
        // Set blocking for handshake
        fcntl(tcpFd, F_SETFL, fcntl(tcpFd, F_GETFL, 0) and O_NONBLOCK.inv())
        val tcpSocket = PosixTcpSocket(tcpFd)

        // 3. Handshake
        Logger.d("AudioEngine", "Starting handshake")
        val check1Bytes = CHECK_1.encodeToByteArray()
        tcpSocket.send(check1Bytes)

        val responseBuffer = ByteArray(CHECK_2.length)
        val bytesRead = tcpSocket.recv(responseBuffer)
        if (bytesRead != CHECK_2.length || !responseBuffer.decodeToString().equals(CHECK_2)) {
            tcpSocket.close()
            throw RuntimeException("Handshake failed: received ${responseBuffer.decodeToString()}")
        }
        Logger.i("AudioEngine", "Handshake successful")

        // 4. Setup UDP (WiFi mode only)
        var udpSocket: PosixUdpSocket? = null
        var udpAddr: sockaddr_in? = null
        if (mode == ConnectionMode.Wifi) {
            val udpPort = calculateUdpPort(port)
            Logger.i("AudioEngine", "Setting up UDP to $targetIp:$udpPort")
            val udpFd = createSocket(SOCK_DGRAM)
            udpAddr = makeSockaddrIn(targetIp, udpPort)
            udpSocket = PosixUdpSocket(udpFd)
        }

        // 5. Start audio capture
        startAudioEngine()

        _streamState.value = StreamState.Streaming
        _lastError.value = null
        lastPingReceivedTime = currentTimeMillis()

        // 6. Send initial mute state
        val muteMsg = proto.encodeToByteArray(
            MessageWrapper.serializer(),
            MessageWrapper(mute = MuteMessage(_isMuted.value))
        )
        sendTcpPacket(tcpSocket, muteMsg)

        // 7. Start reader coroutine (handle control messages from server)
        val readerJob = launch {
            try {
                val readBuf = ByteArray(8192)
                while (isActive) {
                    val magicBytes = ByteArray(4)
                    var totalRead = 0
                    while (totalRead < 4) {
                        val n = tcpSocket.recv(ByteArray(4 - totalRead).also { buf ->
                            System.arraycopy(magicBytes, totalRead, buf, 0, 0) // unused
                        })
                        if (n <= 0) throw RuntimeException("Connection closed")
                        totalRead += n
                    }
                    // Read magic properly
                    val magic = (magicBytes[0].toInt() shl 24) or
                            (magicBytes[1].toInt() and 0xFF shl 16) or
                            (magicBytes[2].toInt() and 0xFF shl 8) or
                            (magicBytes[3].toInt() and 0xFF)

                    if (magic != PACKET_MAGIC) {
                        Logger.w("AudioEngine", "Invalid magic: ${magic.toString(16)}")
                        continue
                    }

                    val lenBytes = ByteArray(4)
                    totalRead = 0
                    while (totalRead < 4) {
                        val n = tcpSocket.recv(lenBytes.copyOfRange(totalRead, 4))
                        if (n <= 0) throw RuntimeException("Connection closed")
                        totalRead += n
                    }
                    val length = (lenBytes[0].toInt() and 0xFF shl 24) or
                            (lenBytes[1].toInt() and 0xFF shl 16) or
                            (lenBytes[2].toInt() and 0xFF shl 8) or
                            (lenBytes[3].toInt() and 0xFF)

                    if (length > 0) {
                        val packetBytes = ByteArray(length)
                        val n = tcpSocket.recv(packetBytes)
                        if (n > 0) {
                            try {
                                val wrapper = proto.decodeFromByteArray(
                                    MessageWrapper.serializer(),
                                    if (n == length) packetBytes else packetBytes.copyOf(n)
                                )
                                if (wrapper.mute != null) {
                                    _isMuted.value = wrapper.mute.isMuted
                                    Logger.i("AudioEngine", "Received mute: ${wrapper.mute.isMuted}")
                                }
                                if (wrapper.ping != null) {
                                    lastPingReceivedTime = currentTimeMillis()
                                    val pongMsg = proto.encodeToByteArray(
                                        MessageWrapper.serializer(),
                                        MessageWrapper(pong = PongMessage(wrapper.ping.timestamp))
                                    )
                                    sendTcpPacket(tcpSocket, pongMsg)
                                }
                            } catch (e: Exception) {
                                Logger.e("AudioEngine", "Error decoding message: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("AudioEngine", "Reader stopped: ${e.message}")
            }
        }

        // 8. Audio sending loop
        try {
            sequenceNumber = 0
            fecGroupBuffer.clear()
            fecGroupStartSeq = 0
            var udpConsecutiveFailures = 0

            while (isActive) {
                // Heartbeat check
                if (currentTimeMillis() - lastPingReceivedTime > HEARTBEAT_TIMEOUT_MS) {
                    throw RuntimeException("Heartbeat timeout — server unreachable")
                }

                // Check reader health
                if (readerJob.isCancelled || readerJob.isCompleted) {
                    throw RuntimeException("Reader job failed — connection lost")
                }

                // Receive audio data from capture tap
                val audioData = audioPacketChannel.receiveCatching().getOrNull() ?: continue

                if (audioData.isEmpty()) continue

                // Calculate levels
                val levelData = calculateAudioLevelDataFromPcm16(audioData)
                _audioLevels.value = levelData.rms
                _audioLevelData.value = levelData

                if (!_isMuted.value) {
                    val sr = sampleRate.value
                    val ch = if (channelCount == ChannelCount.Stereo) 2 else 1
                    val fmt = audioFormat.value

                    val packet = AudioPacketMessage(
                        buffer = audioData,
                        sampleRate = sr,
                        channelCount = ch,
                        audioFormat = fmt
                    )
                    val wrapper = MessageWrapper(
                        audioPacket = AudioPacketMessageOrdered(
                            sequenceNumber++,
                            packet,
                            currentTimeMillis()
                        )
                    )

                    if (udpSocket != null && udpAddr != null) {
                        // WiFi mode: send audio via UDP
                        try {
                            sendAudioViaUdp(udpSocket, udpAddr, wrapper)
                            udpConsecutiveFailures = 0

                            // FEC
                            fecGroupBuffer.add(audioData)
                            if (fecGroupBuffer.size >= FEC_GROUP_SIZE) {
                                val xorResult = xorBuffers(fecGroupBuffer)
                                val fecPacket = AudioPacketMessage(
                                    buffer = xorResult,
                                    sampleRate = sr,
                                    channelCount = ch,
                                    audioFormat = fmt
                                )
                                val fecWrapper = MessageWrapper(
                                    audioPacket = AudioPacketMessageOrdered(
                                        sequenceNumber++, fecPacket,
                                        currentTimeMillis(),
                                        fecSequenceNumber = fecGroupStartSeq
                                    )
                                )
                                sendAudioViaUdp(udpSocket, udpAddr, fecWrapper)
                                fecGroupBuffer.clear()
                                fecGroupStartSeq = sequenceNumber
                            }
                        } catch (e: Exception) {
                            udpConsecutiveFailures++
                            Logger.w("AudioEngine", "UDP send failed (${udpConsecutiveFailures}): ${e.message}")
                            if (udpConsecutiveFailures >= MAX_UDP_CONSECUTIVE_FAILURES) {
                                throw RuntimeException(
                                    "UDP send failed $udpConsecutiveFailures consecutive times"
                                )
                            }
                        }
                    } else {
                        // USB mode: send audio via TCP
                        val packetBytes = proto.encodeToByteArray(
                            MessageWrapper.serializer(), wrapper
                        )
                        sendTcpPacket(tcpSocket, packetBytes)
                    }
                }
            }
        } finally {
            readerJob.cancel()
            tcpSocket.close()
            udpSocket?.close()
        }
    }

    private fun setupAudioCapture(sampleRate: SampleRate, channelCount: ChannelCount) {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
        audioSession.setActive(true, null)

        audioEngine = AVAudioEngine()
        val inputNode = audioEngine!!.inputNode()
        val hardwareFormat = inputNode.outputFormatForBus(0u)
        val preferredSampleRate = sampleRate.value.toDouble()

        // Configure the tap format: 16-bit PCM, mono, target sample rate
        val targetFormat = AVAudioFormat(
            standardFormatWithSampleRate = preferredSampleRate,
            channels = 1u  // Force mono
        )

        inputNode.installTapOnBus(0u, 1024u, targetFormat) { [weak audioPacketChannel] buffer, _ ->
            // Called on real-time audio thread — must be fast!
            val pcmBuffer = buffer as? AVAudioPCMBuffer ?: return@installTapOnBus
            val frameLength = pcmBuffer.frameLength.toInt()
            if (frameLength <= 0) return@installTapOnBus

            // Convert float32 samples to 16-bit PCM
            val floatData = pcmBuffer.floatChannelData
            if (floatData == null) return@installTapOnBus
            val floats = floatData[0]!!.readValues<Float>(frameLength)

            val pcm16 = ByteArray(frameLength * 2) // 16-bit mono = 2 bytes/sample
            for (i in 0 until frameLength) {
                val sample = (floats[i] * 32767f).toInt().coerceIn(-32768, 32767)
                pcm16[i * 2] = (sample and 0xFF).toByte()
                pcm16[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
            }

            // Non-blocking send to channel
            audioPacketChannel?.trySend(pcm16)
        }
    }

    private fun startAudioEngine() {
        audioEngine?.prepare()
        audioEngine?.startAndReturnError(null)
        Logger.i("AudioEngine", "AVAudioEngine started")
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun sendAudioViaUdp(
        udpSocket: PosixUdpSocket,
        addr: sockaddr_in,
        wrapper: MessageWrapper
    ) {
        val packetBytes = proto.encodeToByteArray(MessageWrapper.serializer(), wrapper)
        val length = packetBytes.size
        val header = ByteArray(8).apply {
            this[0] = (UDP_PACKET_MAGIC shr 24).toByte()
            this[1] = (UDP_PACKET_MAGIC shr 16).toByte()
            this[2] = (UDP_PACKET_MAGIC shr 8).toByte()
            this[3] = UDP_PACKET_MAGIC.toByte()
            this[4] = (length shr 24).toByte()
            this[5] = (length shr 16).toByte()
            this[6] = (length shr 8).toByte()
            this[7] = length.toByte()
        }
        udpSocket.sendto(header + packetBytes, addr)
    }

    private fun sendTcpPacket(socket: PosixTcpSocket, data: ByteArray) {
        val length = data.size
        val header = ByteArray(8).apply {
            this[0] = (PACKET_MAGIC shr 24).toByte()
            this[1] = (PACKET_MAGIC shr 16).toByte()
            this[2] = (PACKET_MAGIC shr 8).toByte()
            this[3] = PACKET_MAGIC.toByte()
            this[4] = (length shr 24).toByte()
            this[5] = (length shr 16).toByte()
            this[6] = (length shr 8).toByte()
            this[7] = length.toByte()
        }
        socket.send(header + data)
    }

    private fun xorBuffers(buffers: List<ByteArray>): ByteArray {
        val maxLen = buffers.maxOf { it.size }
        val result = ByteArray(maxLen)
        for (buf in buffers) {
            for (i in buf.indices) {
                result[i] = (result[i].toInt() xor buf[i].toInt()).toByte()
            }
        }
        return result
    }

    private fun calculateAudioLevelDataFromPcm16(buffer: ByteArray): AudioLevelData {
        if (buffer.size < 2) return AudioLevelData.SILENT

        var sum = 0.0
        var maxSample = 0.0
        val sampleCount = buffer.size / 2

        for (i in 0 until sampleCount) {
            val lo = buffer[i * 2].toInt() and 0xFF
            val hi = buffer[i * 2 + 1].toInt()
            val sample = (hi shl 8) or lo
            val normalized = sample / 32768.0
            sum += normalized * normalized
            maxSample = maxOf(maxSample, kotlin.math.abs(normalized))
        }

        if (sampleCount == 0) return AudioLevelData.SILENT

        val rms = sqrt(sum / sampleCount).toFloat().coerceIn(0f, 1f)
        val peak = maxSample.toFloat().coerceIn(0f, 1f)

        return AudioLevelData.fromRmsAndPeak(rms, peak)
    }

    private fun currentTimeMillis(): Long {
        return (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    private fun cleanup() {
        audioEngine?.stop()
        audioEngine?.inputNode()?.removeTapOnBus(0u)
        audioEngine = null
        while (audioPacketChannel.tryReceive().isSuccess) { /* drain */ }
        _streamState.value = StreamState.Idle
        Logger.i("AudioEngine", "AudioEngine stopped")
    }

    actual fun stop() {
        job?.cancel()
        job = null
    }

    actual fun setMonitoring(enabled: Boolean) { /* No-op on iOS client */ }
    actual suspend fun installDriver() { /* No-op */ }
    actual suspend fun setMute(muted: Boolean) { _isMuted.value = muted }
    actual fun setStreamingNotificationEnabled(enabled: Boolean) { /* No silent push on iOS yet */ }
    actual fun setAudioSource(sourceName: String) { /* Built-in mic only */ }
    actual fun updatePerformanceConfig(config: PerformanceConfig) { /* No-op */ }

    actual fun updateConfig(
        enableNS: Boolean,
        nsType: NoiseReductionType,
        nsIntensity: Float,
        enableAGC: Boolean,
        agcTargetLevel: Int,
        agcAttackRate: Float,
        agcDecayRate: Float,
        enableVAD: Boolean,
        vadThreshold: Int,
        enableDereverb: Boolean,
        dereverbLevel: Float,
        amplification: Float,
        processingChain: List<AudioEffectType>,
        equalizerConfig: EqualizerConfig
    ) {
        // Audio processing is handled on the desktop server side.
        // iOS client sends raw audio; no local processing needed.
    }
}
