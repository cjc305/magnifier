# 數位放大鏡 / Magnifier

[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-TBD-lightgrey)](#)

An Android magnifier app that turns your phone camera into a magnifying glass.

把手機鏡頭變成放大鏡的 Android App。

## Features / 功能

- 即時相機放大 1× 到 10× / Real-time camera zoom, 1× to 10×
- 內建手電筒 / Built-in flashlight for low-light reading
- 一鍵拍照存到相簿 / One-tap capture saves to gallery
- 內建相簿瀏覽 + 刪除 / In-app gallery with single/batch delete
- 完全離線、零追蹤、零個資收集 / Fully offline, zero tracking, zero data collection

## Tech stack

- **Kotlin** 2.0.21 + **Jetpack Compose** (BOM 2024.09)
- **CameraX** 1.3.4 — camera preview, capture, zoom, torch
- **Coil** 2.5.0 — image loading
- **MediaStore** — gallery integration
- **MVVM** — ViewModel + Repository pattern
- **MinSdk** 24 / **TargetSdk** 36

## Project structure

```
app/src/main/java/com/example/magnifier/
├── MainActivity.kt              # 29 lines, Compose entry point
├── MagnifierApplication.kt      # AppContainer DI host
├── di/AppContainer.kt           # Manual DI factory
├── data/
│   ├── camera/                  # CameraX wrapper behind CameraController interface
│   ├── media/                   # MediaStore wrapper behind MediaRepository interface
│   └── permission/              # Runtime permission gate
└── ui/
    ├── magnifier/               # Magnifier screen + ViewModel
    ├── gallery/                 # Gallery screen + ViewModel
    └── theme/                   # Material 3 theme
```

See [architecture audit](docs/audit-2026-05-11-architecture.md) for details on the modular / atomic / API-first / dependency-clean design pillars.

## Build

```bash
./gradlew bundleRelease         # Produces signed AAB if keystore.properties exists
./gradlew bundleDebug           # Debug build
./gradlew test                  # Unit tests (7 ViewModel + camera tests)
```

For release builds you need a `keystore.properties` file at the repo root — see [keystore.properties.example](keystore.properties.example).

## Privacy

This app is **fully offline**:

- Camera frames are processed in memory only, never uploaded
- Photos are saved to your device's MediaStore (`Pictures/Magnifier`), not to any server
- No analytics, no tracking SDKs, no ads, no account required

Full policy: <https://cjc305.github.io/magnifier/privacy.html>

## Status

Pre-release. Preparing for first Google Play submission.

- Play Store launch checklist: [docs/play-store/](docs/play-store/)
- Listing copy & ASO: [listing.md](docs/play-store/listing.md)
- Console form prefill: [console-prefill.md](docs/play-store/console-prefill.md)

## License

TBD — likely MIT or Apache 2.0. Decided before first Play release.

## Contact

Bug reports, feature requests, privacy concerns: [GitHub Issues](https://github.com/cjc305/magnifier/issues)
