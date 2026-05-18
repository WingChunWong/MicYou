# MicYou PROJECT KNOWLEDGE BASE

## OVERVIEW

Kotlin Multiplatform app that turns Android devices into PC microphones. Uses Compose Multiplatform/Material 3. Supports Wi-Fi and USB (ADB) connections. Cross-platform: Android client + Desktop server (Windows/Linux/macOS).

- **Kotlin**: 2.2.20 | **JVM Target**: 11 | **Compose Multiplatform**: 1.10.1 | **AGP**: 8.11.2
- **License**: GPL-3.0 | **Version**: 1.3.2 (app) / 1.0.0 (plugin API) — see `gradle.properties`

## STRUCTURE

```
MicYou/
├── composeApp/          # Main app module (KMP)
│   ├── src/
│   │   ├── commonMain/  # Shared UI + logic (Compose, ViewModels)
│   │   ├── androidMain/ # Android-specific (MainActivity, AudioService, plugin impl)
│   │   └── jvmMain/     # Desktop-specific (main.kt, audio effects, network server)
│   └── build.gradle.kts # 610 lines - KMP config, packaging, NSIS, icon generation
├── buildSrc/            # Custom Gradle tasks (CheckLocalizationTask)
├── plugin-api/          # Plugin interface definitions (separate module)
├── exampleplugins/      # Sample plugin implementations
├── docs/                # FAQ, plugin API docs ([docs/](docs/))
└── gradle/libs.versions.toml  # Version catalog
```

## WHERE TO LOOK

| Task                     | Location                                                                     | Notes                                                   |
| ------------------------ | ---------------------------------------------------------------------------- | ------------------------------------------------------- |
| Entry point (Desktop)    | composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/main.kt                    | Window setup, tray, ViewModel init                      |
| Entry point (Android)    | composeApp/src/androidMain/kotlin/com/lanrhyme/micyou/MainActivity.kt        | Permission handling, quick start                        |
| Main UI composition      | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/App.kt                  | Theme, dialogs, platform routing                        |
| Core state management    | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/MainViewModel.kt        | Facade for AudioStream/Settings/Plugin ViewModels       |
| Audio stream logic       | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/AudioStreamViewModel.kt | Connection modes, config, stream control                |
| Audio engine interface   | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/AudioEngine.kt          | expect class - platform implementations                 |
| Network server (Desktop) | composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/network/NetworkServer.kt   | TCP/UDP server, ConnectionHandler                       |
| Audio effects pipeline   | composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/audio/                     | Noise reduction, AGC, VAD, Dereverb, Amplifier          |
| Plugin interfaces        | plugin-api/src/commonMain/kotlin/com/lanrhyme/micyou/plugin/                 | Plugin, PluginHost, PluginManifest, AudioEffectPlugin   |
| Plugin impl (Desktop)    | composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/plugin/                    | PluginManager, PluginClassLoader, PluginSecurityManager |
| Plugin impl (Android)    | composeApp/src/androidMain/kotlin/com/lanrhyme/micyou/plugin/                | AndroidPluginManager, AndroidPluginHostImpl             |
| Platform abstraction     | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/Platform.kt             | expect fun getPlatform(), Logger, VB-Cable              |
| Settings storage         | composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/util/Settings.kt           | File-based settings (desktop)                           |
| Localization             | composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/Localization.kt         | AppStrings, AppLanguage enum                            |

## CODE MAP

| Symbol               | Type         | Location                               | Role                                                                 |
| -------------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------- |
| MainViewModel        | class        | commonMain/MainViewModel.kt            | Facade ViewModel, coordinates AudioStream/Settings/Plugin/Update VMs |
| AudioStreamViewModel | class        | commonMain/AudioStreamViewModel.kt     | Handles connection modes, audio config, stream start/stop            |
| AudioEngine          | expect class | commonMain/AudioEngine.kt              | Platform-specific audio engine interface                             |
| NetworkServer        | class        | jvmMain/network/NetworkServer.kt       | TCP/UDP server for desktop                                           |
| ConnectionHandler    | class        | jvmMain/network/ConnectionHandler.kt   | Protocol handling for incoming connections                           |
| Plugin               | interface    | plugin-api/plugin/Plugin.kt            | Base plugin interface                                                |
| PluginHost           | interface    | plugin-api/plugin/PluginHost.kt        | Host API for plugins                                                 |
| AudioEffectPlugin    | interface    | plugin-api/plugin/AudioEffectPlugin.kt | Audio processing plugin interface                                    |
| App                  | @Composable  | commonMain/App.kt                      | Root UI, theme, dialog handling                                      |
| Platform             | interface    | commonMain/Platform.kt                 | Platform abstraction (Android/Desktop)                               |
| Logger               | object       | commonMain/Platform.kt                 | Cross-platform logging                                               |

## CONVENTIONS

- **Kotlin code style**: official (kotlin.code.style=official in gradle.properties)
- **JVM target**: JVM 11 for all modules
- **Compose**: Material 3, Material You dynamic colors (Android only)
- **State management**: ViewModel + StateFlow pattern, combine() for merged state
- **Settings**: SettingsFactory.getSettings() - platform-specific implementations
- **Logging**: Logger.i/d/w/e with tag + message, platform-specific LoggerImpl
- **expect/actual**: Platform-specific code uses expect/actual pattern (Platform.kt, AudioEngine.kt)
- **Localization**: getStrings(language) returns AppStrings, LocalAppStrings CompositionLocal
- **Plugin system**: Separate plugin-api module, platform-specific implementations

## ANTI-PATTERNS (THIS PROJECT)

- **DO NOT**: Ignore firewall dialog on desktop - port may be blocked (AudioStreamViewModel handles this)
- **NEVER**: Use hardcoded IP/port - use settings for persistence
- **ALWAYS**: Use Logger instead of println for cross-platform logging
- **ALWAYS**: Call updateAudioEngineConfig() after changing audio processing settings

## UNIQUE STYLES

- **Pocket mode**: Compact 600x250 window for minimal UI (toggle via settings)
- **Enhanced mode**: Full 850x650 window with visualizers and background settings
- **Haze effect**: Uses dev.chrisbanes.haze:haze library for glass blur effects
- **Audio processing chain**: AudioProcessorPipeline chains effects (NoiseReducer, AGCEffect, VADEffect, DereverbEffect, AmplifierEffect)
- **Plugin security**: PluginSecurityManager validates plugins before loading, PluginClassLoader isolates plugin code
- **VB-Cable integration**: Windows-only, auto-detect and install virtual audio device
- **Connection modes**: enum ConnectionMode { Wifi, Usb } - auto-config adjusts sample rate/channel count

## GRADLE CONFIG

- `gradle.properties`: `configuration-cache=true`, `caching=true`, `daemon.jvmargs=-Xmx3072M`
- `android.nonTransitiveRClass=true` — R classes not transitive across modules
- **Version catalog** in `gradle/libs.versions.toml` — all dependency versions centralized here
- **Material icons pinned to 1.7.3** via `resolutionStrategy` in `composeApp/build.gradle.kts` — do not bump without testing for conflicts with Material3
- Compose Material3 uses `1.10.0-alpha05` (separate from main Compose version)

## UI LIBRARIES

- **Haze** (`dev.chrisbanes.haze:haze`): Glass blur / glassmorphism effects
- **MaterialKolor** (`com.materialkolor:material-kolor`): Dynamic color palette generation (Android Material You)

## COMMANDS

```bash
# Build (Gradle wrapper)
./gradlew build                # Full build

# Android
./gradlew :composeApp:assembleDebug    # Debug APK
./gradlew :composeApp:assembleRelease  # Release APK (requires signing env vars)

# Desktop JVM run
./gradlew :composeApp:jvmRun           # Run desktop app

# Desktop packaging
./gradlew :composeApp:createDistributable       # Create distributable
./gradlew :composeApp:packageWindowsZip          # Windows ZIP
./gradlew :composeApp:packageWindowsNsis         # Windows NSIS installer
./gradlew :composeApp:packageExe                 # Windows EXE
./gradlew :composeApp:packageDmg                 # macOS DMG
./gradlew :composeApp:packageDeb                 # Linux DEB
./gradlew :composeApp:packageRpm                 # Linux RPM

# No-JRE packaging (requires system Java)
./gradlew :composeApp:packageNoJreAll            # All platforms without bundled JRE

# Plugin API
./gradlew :plugin-api:build                      # Build plugin API JAR

# Localization / code quality
./gradlew checkLocalization                      # Validate string key consistency
./gradlew installGitHooks                         # Install pre-commit hook
```

## CI/CD

- **Development**: `.github/workflows/development.yml` — push/PR to main/master/develop → builds Android APK + Windows (ZIP+NSIS)
- **Release**: `.github/workflows/release.yml` — manual trigger, creates GitHub Release with signed artifacts
- **Mirror**: `.github/workflows/mirrorchyan_release.yml` — MirrorChyan distribution for China
- **Pre-commit**: `.githooks/pre-commit` runs `checkLocalization` — install via `./gradlew installGitHooks`
- **Android signing**: environment-driven via `ANDROID_KEYSTORE_*` env vars (see `composeApp/build.gradle.kts`)

## DEEP DIVES

Sub-directory AGENTS.md files for focused domains:

| Area                   | File                                                                                          |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| Shared UI & ViewModels | [commonMain/AGENTS.md](composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/AGENTS.md)        |
| Android platform       | [androidMain/AGENTS.md](composeApp/src/androidMain/kotlin/com/lanrhyme/micyou/AGENTS.md)      |
| Audio effects pipeline | [audio/AGENTS.md](composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/audio/AGENTS.md)          |
| Network server         | [network/AGENTS.md](composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/network/AGENTS.md)      |
| Desktop plugin system  | [plugin/AGENTS.md](composeApp/src/jvmMain/kotlin/com/lanrhyme/micyou/plugin/AGENTS.md)        |
| Plugin API contracts   | [plugin-api/AGENTS.md](plugin-api/src/commonMain/kotlin/com/lanrhyme/micyou/plugin/AGENTS.md) |

## NOTES

- **VB-Cable**: Windows-only virtual audio device for system microphone output
- **NSIS packaging**: Requires NSIS installed or `nsis.makensis` Gradle property / `NSIS_MAKENSIS` env var
- **Update mechanism**: GitHub releases, auto-check on startup, mirror download option for China
- **Quick Start**: Android intent ACTION_QUICK_START for tile service auto-connect
- **macOS**: Requires BlackHole (virtual audio driver) and SwitchAudioOSX
- **Android minSdk**: 24 | **compileSdk/targetSdk**: 36
- **Translations**: Crowdin-based community translations, see `crowdin.yml`