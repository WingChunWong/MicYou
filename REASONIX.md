# REASONIX.md — MicYou

## Stack
- Kotlin 2.2.20, Compose Multiplatform 1.10.1, Material 3 [libs.versions.toml](gradle/libs.versions.toml)
- Ktor 3.4.0 — client (OkHttp/Android, Java/JVM, Darwin/iOS) + server (Netty, WebSockets, CORS)
- kotlinx-serialization 1.8.1 (JSON + protobuf), kotlinx-datetime 0.7.1
- ONNX Runtime 1.14.0 + RNNoise 2.1.2 — ML audio processing
- Gradle 9.3.1 with version catalog, AGP 8.11.2, JVM target 11 [gradle.properties](gradle.properties)

## Layout
- `composeApp/` — main KMP module (commonMain, androidMain, jvmMain, iosMain) [composeApp/src/](composeApp/src/)
- `plugin-api/` — plugin interface definitions, standalone Gradle module [settings.gradle.kts](settings.gradle.kts)
- `exampleplugins/` — sample plugin implementations
- `buildSrc/` — custom Gradle task: `CheckLocalizationTask` [build.gradle.kts](build.gradle.kts)
- `docs/` — FAQ + plugin API reference
- `.githooks/` — pre-commit runs `./gradlew checkLocalization` [.githooks/pre-commit](.githooks/pre-commit)

## Commands
- **Android debug APK:** `./gradlew :composeApp:assembleDebug`
- **Desktop run:** `./gradlew :composeApp:run`
- **Tests:** `./gradlew test`
- **Package (with JRE):** `./gradlew :composeApp:packageDeb` / `:packageRpm` / `:packageDmg` / `:packageWindowsNsis` / `:packageWindowsZip`
- **Package (no JRE):** `./gradlew :composeApp:packageWindowsNoJreZip` / `:packageLinuxNoJreTarGz` / `:packageMacosNoJreTarGz`
- **Localization check:** `./gradlew checkLocalization`
- **Install git hooks:** `./gradlew installGitHooks`

No detekt / ktlint / editorconfig — no automated lint/format.

## Conventions
- Kotlin code style: official [gradle.properties](gradle.properties)
- Platform-specific code: `expect`/`actual` (AudioEngine, Platform) [Platform.kt](composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/Platform.kt)
- State: ViewModel + StateFlow [MainViewModel.kt](composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/MainViewModel.kt)
- Logging: `Logger.i/d/w/e`, not println [Platform.kt](composeApp/src/commonMain/kotlin/com/lanrhyme/micyou/Platform.kt)
- i18n: Compose Multiplatform Resources — `strings.xml` under `composeResources/values*/` [strings.xml](composeApp/src/commonMain/composeResources/values/strings.xml)
- Pre-commit validates localization files for missing/extra keys [build.gradle.kts](build.gradle.kts)

## Watch out for
- `configuration-cache=true` — invalidate with `./gradlew --no-configuration-cache` if build behaves oddly [gradle.properties](gradle.properties)
- Material icons-extended pinned to 1.7.3 via `resolutionStrategy` — Compose upgrades don't bump it [composeApp/build.gradle.kts](composeApp/build.gradle.kts:39-42)
- `AGENTS.md` module memory files live in source trees; keep in sync with code changes
- Desktop builds bundle a JRE by default; no-JRE variant scripts in `composeApp/scripts/no-jre/`
- iOS targets (iosArm64, iosSimulatorArm64) require macOS with Xcode to compile [composeApp/build.gradle.kts](composeApp/build.gradle.kts:58-67)
- iOS plugin system is a stub; full plugin runtime not yet migrated
- iOS AudioEngine is a stub; AVAudioEngine capture not yet implemented
- MaterialKolor unavailable on iOS (ABI mismatch); falls back to default M3 colors
