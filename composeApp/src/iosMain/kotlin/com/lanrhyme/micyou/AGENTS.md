# iOS Platform Implementation

## OVERVIEW
iOS-specific implementations for the MicYou KMP project. iOS acts as a client
(same as Android) — it captures microphone audio via AVAudioEngine and streams
to a desktop server over TCP/UDP using POSIX sockets.

## WHERE TO LOOK
| File | Role |
|------|------|
| Platform.ios.kt | Platform actual: getPlatform(), Logger, dynamic colors, audio sources |
| IosLogger.kt | LoggerImpl using NSLog |
| Settings.ios.kt | SettingsFactory using NSUserDefaults |
| AudioEngine.ios.kt | AVAudioEngine capture + POSIX socket streaming (TCP handshake, UDP audio, FEC) |
| BackHandlerCompat.ios.kt | No-op (no hardware back button) |
| DeviceDiscovery.ios.kt | No-op (iOS is client, not server) |
| Localization.ios.kt | setAppLocale, readResourceFile |
| LoadImageBitmap.ios.kt | Image loading via Skia + NSData |
| BackgroundImagePicker.ios.kt | Image picker via FileKit + NSFileManager |
| PluginFileChooser.ios.kt | Plugin file picker via FileKit |
| PermissionDialog.ios.kt | No-op (iOS handles permissions via Info.plist) |
| PlatformAdaptor.ios.kt | Stub PlatformAdaptor |
| UpdateChecker.ios.kt | Update check + redirect to GitHub release (no auto-install) |
| PluginSettingsContent.ios.kt | OpenPluginWindow + OpenPluginSettings (Dialog-based) |
| PluginHostProvider.ios.kt | createPluginHost with IosPluginHostImpl |
| PluginManagerProvider.ios.kt | createPluginManager with IosPluginManager stub |
| MainViewController.kt | iOS app entry point (ComposeUIViewController) |
| plugin/IosPluginHostImpl.kt | Minimal PluginHost for iOS |
| plugin/IosPluginManager.kt | Stub PluginManager (full plugin system not migrated) |
| plugin/IosPluginDataChannel.kt | No-op PluginDataChannel for iOS |
| theme/ExpressiveColorScheme.ios.kt | dynamicColorScheme fallback (default M3 colors) |

## CONVENTIONS
- iOS compilation REQUIRES macOS with Xcode installed
- Targets: iosArm64 (device), iosSimulatorArm64 (simulator)
- Microphone: AVAudioEngine input tap → float32 → 16-bit PCM → ProtoBuf → POSIX socket
- Protocol: matches Android client — TCP handshake ("MicYouCheck1"/"MicYouCheck2"),
  PACKET_MAGIC (0x4D696359) framing, UDP audio with UDP_PACKET_MAGIC (0x4D696355),
  FEC via XOR of 12 packets, heartbeat every 5s
- Settings: NSUserDefaults
- Logging: NSLog
- Plugin system: stub (full JVM runtime not applicable to iOS)

## KNOWN ISSUES
- Ktor client Darwin and FileKit klibs require Kotlin >= 2.3.x (ABI mismatch with 2.2.20)
- MaterialKolor unavailable on iOS; falls back to default M3 colors
- Audio conversion uses float32→PCM16 via manual loop (could be optimized with Accelerate)
- FEC group size hardcoded to 12 (matches Android)
