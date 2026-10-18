<div align="center">

# 📶 krfr

### SIM Card MCC/MNC & Carrier Configuration Tool · No Root · Powered by Shizuku

<img src="docs/krfr-icon-preview.png" width="96" alt="krfr logo" />

[![Release](https://img.shields.io/github/v/release/kinsonstudio/krfr?label=Latest&color=2F6B5E)](https://github.com/kinsonstudio/krfr/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/kinsonstudio/krfr/total?label=Downloads&color=8A7A3C)](https://github.com/kinsonstudio/krfr/releases)
[![License](https://img.shields.io/badge/License-Apache--2.0-2F6B5E)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208%2B-2F6B5E)](https://github.com/kinsonstudio/krfr)
[![Shizuku](https://img.shields.io/badge/Powered%20by-Shizuku-8A7A3C)](https://github.com/RikkaApps/Shizuku)

**Per-SIM independent config · Live SIM status · Quick presets · Backup & restore · Themes**

</div>

---

<div align="center">

### 🌐 Language / 语言

**English** · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md)

</div>

> **Tip:** New users get a language picker right after Shizuku authorization — it auto-matches your device system language (English by default). You can switch anytime in the app's Personalize tab.

---

## 📥 Download

> Tap a button below to get the latest version from the official download page or GitHub Releases

<div align="center">

### [🌐 Go to Official Download Page](https://taiai.asia/krfr.html)

### [🚀 Go to GitHub Releases](https://github.com/kinsonstudio/krfr/releases/latest)

</div>

## ✨ Features

| | Feature | Description |
|---|---|---|
| 🌍 | **MCC/MNC Override** | Change SIM card country code (MCC/MNC) and carrier name via Shizuku, per-SIM independent override with custom values |
| 📡 | **Live SIM Status** | Network type, signal strength, MCC/MNC, country/region, roaming state, masked ICCID for every SIM in real time |
| ⚡ | **Quick Presets** | 18 common "country code + carrier" combos, apply to any SIM with one tap |
| 💾 | **Backup & Restore** | Export all override configs as JSON; restore after switching devices or reinstalling |
| 🎨 | **Personalize** | 4 color themes (dark green / ocean blue / warm orange / rose), light & dark modes, upload your own background image |
| 🪪 | **SIM Display Name** | Edit each SIM's display name (status bar card name) with one-tap restore to default |
| 🌐 | **Trilingual UI** | English / 简体中文 / 繁體中文 — all interface texts localized; auto-matches system language on first launch |
| 🔄 | **Update Check** | Built-in update checker backed by a fast domestic server (GitHub API as fallback) |

## 📱 Screenshots

<div align="center">

<img src="docs/krfr-screen-1.jpg" width="280" alt="krfr config screen: per-SIM independent MCC/MNC and carrier override" style="border-radius: 34px; border: 9px solid #23262a; box-shadow: 0 18px 40px rgba(20, 32, 28, 0.28);" />

<br/>

*Config screen · per-SIM independent country code & carrier override (SIM1 China Broadnet / SIM2 China Mobile)*

</div>

## 🚀 Quick Start

1. Install and activate **[Shizuku](https://github.com/RikkaApps/Shizuku)** (via wireless debugging or ADB)
2. Download and install the krfr APK (allow "install unknown apps")
3. Open krfr, pick your language (auto-matched to system, English default), then follow the wizard to grant Shizuku permission
4. Select a SIM → change MCC/MNC or carrier name → save
5. Reboot the device or wait for the config to take effect

## 🛠 Build from Source

### Requirements

- Android Studio (Koala or newer)
- **JDK 17** (Gradle 8.9 does not support JDK 25)
- Android SDK

### Command Line Build

```bash
cd krfr
export JAVA_HOME=$HOME/jdk17/Contents/Home
./gradlew :app:assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

> If `ANDROID_SDK_ROOT` points to a wrong path, run `unset ANDROID_SDK_ROOT` first to avoid conflicts with `ANDROID_HOME`.

## 📁 Project Structure

```
app/src/main/java/com/github/krfr/
├── data/          # Preset data: country codes, carriers, quick presets, settings persistence
├── manager/       # Core logic: CarrierConfig override, SIM status, backup/restore, update check
├── ui/
│   ├── components/  # Shared components (gradient headers, status badges, result banners…)
│   ├── screens/     # Config / SIM status / Quick presets / Settings / About / Personalize
│   ├── theme/       # Theme palettes & switching
│   └── MainScaffold.kt  # Bottom navigation container
└── MainActivity.kt
```

## 🧩 Tech Stack

- **Kotlin + Jetpack Compose** (Material 3)
- **Shizuku** (`rikka.shizuku:api:13.1.5`) + HiddenApiBypass, fully rootless
- Package `com.github.krfr` · Latest version v1.0.9
- minSdk 26 / targetSdk 35 / compileSdk 36

## 📄 License

This project is developed based on **[Ackites/Nrfr](https://github.com/Ackites/Nrfr)** under the **Apache-2.0** license.

Upstream license: [Ackites/Nrfr LICENSE](https://github.com/Ackites/Nrfr/blob/master/LICENSE)

## ⚠️ Disclaimer

This tool is for learning and research purposes only. Modifying carrier configuration may affect network, roaming and region detection. Use at your own risk.

---

<div align="center">

**Made by [KinsonStudio](https://github.com/kinsonstudio)** · Independently developed · Based on Nrfr

</div>
