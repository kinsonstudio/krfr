<div align="center">

# 📶 krfr

### SIM 卡國家碼與電訊商配置工具 · 免 Root · 由 Shizuku 驅動

<img src="docs/krfr-icon-preview.png" width="96" alt="krfr logo" />

[![Release](https://img.shields.io/github/v/release/kinsonstudio/krfr?label=Latest&color=2F6B5E)](https://github.com/kinsonstudio/krfr/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/kinsonstudio/krfr/total?label=Downloads&color=8A7A3C)](https://github.com/kinsonstudio/krfr/releases)
[![License](https://img.shields.io/badge/License-Apache--2.0-2F6B5E)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208%2B-2F6B5E)](https://github.com/kinsonstudio/krfr)
[![Shizuku](https://img.shields.io/badge/Powered%20by-Shizuku-8A7A3C)](https://github.com/RikkaApps/Shizuku)

</div>

<div align="center">

**🌐 [English](./README.md) | [简体中文](./README.zh-CN.md) | [繁體中文](./README.zh-TW.md)**

</div>

---

### 📥 下載

> 點擊下方按鈕，從官網下載頁或 GitHub Releases 取得最新版本

<div align="center">

### [🌐 前往官網下載頁下載](https://taiai.asia/krfr.html)

### [🚀 前往 GitHub Releases 下載](https://github.com/kinsonstudio/krfr/releases/latest)

</div>

### ✨ 功能亮點

| | 功能 | 說明 |
|---|---|---|
| 🌍 | **國家碼修改** | 經 Shizuku 修改 SIM 卡國家碼（MCC/MNC）與電訊商名稱，雙卡獨立覆蓋，支援自訂值 |
| 📡 | **SIM 實時狀態** | 每張卡的網絡類型、訊號強度、MCC/MNC、國家/地區、漫遊狀態、ICCID（脫敏）實時可查 |
| ⚡ | **快捷方案** | 18 個常用「國家碼 + 電訊商」組合，選定 SIM 一鍵套用 |
| 💾 | **備份與還原** | 當前所有卡槽的覆蓋配置匯出為 JSON，換機 / 重裝後一鍵還原 |
| 🎨 | **個人化** | 4 套配色主題（墨綠 / 海藍 / 暖橙 / 玫紅）、明暗模式、上傳圖片作為應用程式背景 |
| 🪪 | **SIM 顯示名稱** | 可修改每張 SIM 卡的顯示名稱（狀態欄卡名），支援一鍵還原預設 |
| 🌐 | **三語介面** | 英文 / 简体中文 / 繁體中文，全部介面文字本地化，首次進入自動匹配系統語言 |
| 🔄 | **檢查更新** | 內建更新檢測，優先走國內伺服器（GitHub API 自動兜底） |

### 📱 應用截圖

<div align="center">

<img src="docs/krfr-screen-zhTW.jpg" width="280" alt="krfr 配置頁（繁體）：雙卡獨立配置國家碼與電訊商" style="border-radius: 34px; border: 9px solid #23262a; box-shadow: 0 18px 40px rgba(20, 32, 28, 0.28);" />

<br/>

*配置頁 · 雙卡獨立配置國家碼與電訊商（SIM1 中國廣電 / SIM2 中國移動）*

</div>

### 🚀 快速開始

1. 安裝並啟動 **[Shizuku](https://github.com/RikkaApps/Shizuku)**（無線調試或 ADB 方式啟用）
2. 下載安裝 krfr APK（需允許「安裝未知來源應用」）
3. 開啟 krfr，選擇語言（自動匹配系統語言，預設英文），按引導完成 Shizuku 授權
4. 選擇 SIM 卡 → 修改國家碼 / 電訊商 → 儲存
5. 重新啟動手機或等待配置生效

### 🛠 從原始碼建置

**環境要求：** Android Studio（Koala 或更新）· **JDK 17**（Gradle 8.9 不支援 JDK 25）· Android SDK

```bash
cd krfr
export JAVA_HOME=$HOME/jdk17/Contents/Home
./gradlew :app:assembleRelease
```

APK 輸出：`app/build/outputs/apk/release/app-release.apk`

> 若環境存在 `ANDROID_SDK_ROOT` 且指向錯誤路徑，先 `unset ANDROID_SDK_ROOT`，避免與 `ANDROID_HOME` 衝突。

### 📁 專案結構

```
app/src/main/java/com/github/krfr/
├── data/          # 預設資料：國家碼、電訊商、快捷方案、設定持久化
├── manager/       # 核心邏輯：CarrierConfig 覆蓋、SIM 狀態讀取、備份還原、更新檢測
├── ui/
│   ├── components/  # 共用元件（漸變頁頭、狀態徽章、結果橫幅等）
│   ├── screens/     # 配置 / SIM 狀態 / 快捷方案 / 設定 / 關於 / 個人化
│   ├── theme/       # 主題色板與切換
│   └── MainScaffold.kt  # 底部導覽容器
└── MainActivity.kt
```

### 🧩 技術棧

- **Kotlin + Jetpack Compose**（Material 3）
- **Shizuku**（`rikka.shizuku:api:13.1.5`）+ HiddenApiBypass，全程免 Root
- 套件名稱 `com.github.krfr` · 最新版本 v1.0.9
- minSdk 26 / targetSdk 35 / compileSdk 36

### 📄 授權條款

本專案基於 **[Ackites/Nrfr](https://github.com/Ackites/Nrfr)** 開發，遵循 **Apache-2.0** 授權。

上游授權：[Ackites/Nrfr LICENSE](https://github.com/Ackites/Nrfr/blob/master/LICENSE)

### ⚠️ 免責聲明

本工具僅供學習研究。修改電訊商配置可能影響網絡、漫遊與地區識別，請自行承擔使用風險。

---

<div align="center">

**Made by [KinsonStudio](https://github.com/kinsonstudio)** · 基於 Nrfr 開源 · Apache-2.0

</div>
