# ZBuilds ⚡

[![Build & Release APK](https://github.com/RANZN/ZBuilds/actions/workflows/release.yml/badge.svg)](https://github.com/RANZN/ZBuilds/actions/workflows/release.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.03.01-brightgreen.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI-blue.svg)](https://developer.android.com/topic/architecture)
[![DI](https://img.shields.io/badge/Dependency%20Injection-Koin-orange.svg?logo=koin)](https://insert-koin.io/)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-green.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**ZBuilds** is a modern, high-performance Android utility engineered to discover, track, update, and manage GitHub releases for Android apps (`.apk`) and Magisk/KernelSU root modules (`.zip`). 

Built strictly following **Clean Architecture** principles and **MVI (Model-View-Intent)** with **Jetpack Compose**, **Koin**, and an **AMOLED Pure-Black Design System**.

---

## 📱 Screenshots

| Home & Updates | Add App Source | GitHub Token Settings | Storage & Retention |
|:---:|:---:|:---:|:---:|
| <img src="screenshots/01_home_screen.png" width="220" alt="Home Screen"/> | <img src="screenshots/02_add_app_sheet.png" width="220" alt="Add Source Sheet"/> | <img src="screenshots/03_settings_token.png" width="220" alt="Token Settings"/> | <img src="screenshots/04_settings_cleanup.png" width="220" alt="Storage Cleanup"/> |

---

## ✨ Features

- **Multi-Format Build Support**: Track both standard Android APKs (`.apk`) and Magisk / KernelSU / APatch root modules (`.zip`) with dedicated badge indicators.
- **Dynamic Format Filtering**: Toggle visibility between APKs and Modules independently in Settings to filter tracked lists and the Add App Source screen.
- **Add App Source Screen**: Paste any GitHub repo or release URL, see every APK/ZIP with its full file name, architecture, size and Debug/Release badge, and load older releases to find files that newer releases no longer ship.
- **Smart Update Matching**: Releases that ship several apps or variants are handled by matching each tracked file's version-independent name, architecture and build variant. Each card shows that build's own version, and the Install button turns into a disabled "Installed" state when the installed version already matches the server.
- **Architecture Tagging**: Detects the architecture (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `all`) from each file name.
- **GitHub Rate Limit Bypass**:
  - Secure GitHub Personal Access Token integration to bypass GitHub's 60 req/hr anonymous limit (up to 5,000 req/hr).
  - **Generate** action: Directly opens GitHub token creation page with pre-filled scopes (`public_repo`).
  - **Paste** action: Instantly reads token from the clipboard.
  - **Masked Token Security**: Never displays the raw token in plain text; automatically masks input with bullet obfuscation, eye-toggle visibility, and `••••••••••••${last4}` verification.
- **Crash Tracking & Analytics**:
  - Global `UncaughtExceptionHandler` that intercepts all uncaught thread exceptions without crashing silently.
  - Generates comprehensive diagnostic reports with stack traces, rolling breadcrumbs, device ABI, OS version, and app version.
  - Thread-safe local persistence (`filesDir/crash_reports/`) with rolling retention limits (max 25 reports).
  - Built-in Settings card with the latest crash, copy/share as Markdown, a pre-filled GitHub issue, and clearing stored reports.
- **Storage Access Framework (SAF)**: Choose custom download folders anywhere on internal or external storage (`content://` URI tree persistence).
- **Storage Cleanup**:
  - Auto-clean older versions of an app or module when a new build is downloaded.
  - One-tap immediate storage cleanup with snackbar feedback.
  - Retention period setting (1, 7 or 30 days). The choice is saved, but automatic scheduled cleanup is not implemented yet.
- **AMOLED Pure-Black UI**: Beautiful Jetpack Compose Material 3 dark interface with neon emerald green accents (`#00E676`) and zero battery drain on OLED screens.

---

## 🏛️ Clean Architecture & Tech Stack

The project follows **Clean Architecture** with a unidirectional data flow (**MVI**):

```
                        ┌─────────────────────────────────────────┐
                        │              Presentation               │
                        │  (Jetpack Compose, ViewModels, MVI      │
                        │   State / Action / Effect contracts)    │
                        └────────────────────┬────────────────────┘
                                             │ depends on
                        ┌────────────────────▼────────────────────┐
                        │                 Domain                  │
                        │  (Pure Kotlin models, rules, use cases, │
                        │        repository interfaces)           │
                        └────────────────────▲────────────────────┘
                                             │ implements
                        ┌────────────────────┴────────────────────┐
                        │                  Data                   │
                        │  (OkHttp + kotlinx.serialization DTOs,  │
                        │   DataStore entities, SAF, packages,    │
                        │   local crash storage, mappers)         │
                        └─────────────────────────────────────────┘
```

- **Pure Domain Layer**: Plain Kotlin only, with no Android, serialization or networking imports. Business rules (asset matching, build versions, Debug/Release detection, file name parsing) live in `domain/rules`.
- **Data Layer Models**: Network responses are `GitHub*Dto` classes and stored tracked apps are `TrackedAppEntity`. Mappers convert them to domain models, so storage and network formats never reach the UI.
- **MVI Presentation**: Each screen has a contract with an immutable `UiState`, `UiAction`s sent by the screen and one-time `UiEffect`s (navigation, snackbars, system intents, clipboard). ViewModels only call use cases. Screens only render state and send actions; derived values such as filtered lists live on the state.
- **Strings**: All user-facing text lives in `strings.xml`. ViewModels send `UiText`, the data layer raises typed `AppError`s, and a `StringProvider` supplies text to non-UI classes.
- **Dependency Injection**: **Koin** (`koin-android`, `koin-androidx-compose`).
- **Networking**: **OkHttp 4** with **kotlinx.serialization**. The GitHub token is added to each request by `GitHubApiService`.
- **Persistence**: **Jetpack DataStore Preferences** for settings and tracked apps (stored as JSON).
- **Diagnostics**: Custom `ZBuildsCrashHandler` + `CrashReporter` with rolling breadcrumbs and local JSON report storage.

---

## 📁 Project Structure

```
com.ranzan.zbuilds/
├── core/
│   ├── base/            # BaseViewModel, UiState / UiAction / UiEffect, UiText
│   └── utils/           # Size/date formatting, StringProvider, crash report formatting
├── data/
│   ├── analytics/       # ZBuildsCrashHandler, CrashReporterImpl, CrashStorageDataSource
│   ├── local/           # DataStore (settings, tracked apps), AppDownloader, PackageInspectorImpl
│   │   └── model/       # TrackedAppEntity + mappers
│   ├── remote/          # GitHubApiService (OkHttp), SourceUrlParser
│   │   ├── mapper/      # DTO → domain mappers
│   │   └── model/       # GitHub DTOs
│   └── repository/      # AppRepositoryImpl, SettingsRepositoryImpl, DownloaderRepositoryImpl, SuggestionRepositoryImpl
├── domain/
│   ├── model/           # TrackedApp, SourceRelease, ReleaseAsset, AppError, AppSettings, CrashReport, ...
│   ├── repository/      # Repository interfaces
│   ├── rules/           # AssetMatcher, VersionRules, BuildVariantRules, AssetNameRules
│   └── usecase/         # Single-responsibility use cases
├── di/
│   └── KoinModules.kt   # Koin definitions (data, repositories, use cases, ViewModels)
└── ui/
    ├── addsource/       # AddAppSourceScreen
    ├── components/      # TrackedAppCard, UpdateBannerCard, TopSearchBar
    ├── home/            # HomeScreen, HomeViewModel, HomeContract
    ├── settings/        # SettingsScreen, SettingsViewModel, SettingsContract
    ├── theme/           # Color, Theme, Type (AMOLED palette)
    └── util/            # FileIntentHelper, ClipboardHelper, app icon loading, error text
```

---

## 🚀 Automated CI/CD (`.github/workflows/release.yml`)

The repository includes a production-grade GitHub Actions release workflow:
- **Triggers**: On push to `master`/`main`, git tags (`v*`), manual trigger (`workflow_dispatch`), or release publication.
- **JDK Toolchain**: Sets up Java 17 Temurin with Gradle build cache (`setup-gradle@v4`).
- **Keystore Detection**: Automatically detects if `KEYSTORE_BASE64` secret exists:
  - If present: compiles release variant and signs APK with `r0adkll/sign-android-release@v1`.
  - If absent: compiles debug variant for instant CI validation.
- **Automated GitHub Release**: Packages APK as `ZBuilds-v${VERSION}.apk` and attaches it directly to a new GitHub Release.

---

## 🛠️ Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17
- Android SDK 36 (minSdk 24, targetSdk 36)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/RANZN/ZBuilds.git
cd ZBuilds

# Build debug APK
./gradlew assembleDebug

# Install directly to connected device
./gradlew installDebug
```

---

## 📄 License

```text
Copyright 2026 Ranjan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
