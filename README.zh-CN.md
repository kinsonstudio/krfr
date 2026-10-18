<div align="center">

# 📶 krfr

### SIM 卡国家码与运营商配置工具 · 免 Root · 由 Shizuku 驱动

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

### 📥 下载

> 点击下方按钮，从官网下载页或 GitHub Releases 获取最新版本

<div align="center">

### [🌐 前往官网下载页下载](https://taiai.asia/krfr.html)

### [🚀 前往 GitHub Releases 下载](https://github.com/kinsonstudio/krfr/releases/latest)

</div>

### ✨ 功能亮点

| | 功能 | 说明 |
|---|---|---|
| 🌍 | **国家码修改** | 经 Shizuku 修改 SIM 卡国家码（MCC/MNC）与运营商名称，双卡独立覆盖，支持自定义值 |
| 📡 | **SIM 实时状态** | 每张卡的网络类型、信号强度、MCC/MNC、国家/地区、漫游状态、ICCID（脱敏）实时可查 |
| ⚡ | **快捷方案** | 18 个常用「国家码 + 运营商」组合，选定 SIM 一键应用 |
| 💾 | **备份与恢复** | 当前所有卡槽的覆盖配置导出为 JSON，换机 / 重装后一键恢复 |
| 🎨 | **个性化** | 4 套配色主题（墨绿 / 海蓝 / 暖橙 / 玫红）、明暗模式、上传图片作为应用背景 |
| 🪪 | **SIM 显示名** | 可修改每张 SIM 卡的显示名（状态栏卡名），支持一键还原默认 |
| 🌐 | **三语界面** | 英文 / 简体中文 / 繁體中文，全部界面文字本地化，首次进入自动匹配系统语言 |
| 🔄 | **检查更新** | 内置更新检测，优先走国内服务器（GitHub API 自动兜底） |

### 📱 应用截图

<div align="center">

<img src="docs/krfr-screen-1.jpg" width="280" alt="krfr 配置页：双卡独立配置国家码与运营商" style="border-radius: 34px; border: 9px solid #23262a; box-shadow: 0 18px 40px rgba(20, 32, 28, 0.28);" />

<br/>

*配置页 · 双卡独立配置国家码与运营商（SIM1 中国广电 / SIM2 中国移动）*

</div>

### 🚀 快速开始

1. 安装并启动 **[Shizuku](https://github.com/RikkaApps/Shizuku)**（无线调试或 ADB 方式激活）
2. 下载安装 krfr APK（需允许「安装未知来源应用」）
3. 打开 krfr，选择语言（自动匹配系统语言，默认英文），按引导完成 Shizuku 授权
4. 选择 SIM 卡 → 修改国家码 / 运营商 → 保存
5. 重启手机或等待配置生效

### 🛠 从源码构建

**环境要求：** Android Studio（Koala 或更新）· **JDK 17**（Gradle 8.9 不支持 JDK 25）· Android SDK

```bash
cd krfr
export JAVA_HOME=$HOME/jdk17/Contents/Home
./gradlew :app:assembleRelease
```

APK 输出：`app/build/outputs/apk/release/app-release.apk`

> 若环境存在 `ANDROID_SDK_ROOT` 且指向错误路径，先 `unset ANDROID_SDK_ROOT`，避免与 `ANDROID_HOME` 冲突。

### 📁 项目结构

```
app/src/main/java/com/github/krfr/
├── data/          # 预设数据：国家码、运营商、快捷方案、设置持久化
├── manager/       # 核心逻辑：CarrierConfig 覆盖、SIM 状态读取、备份恢复、更新检测
├── ui/
│   ├── components/  # 公共组件（渐变页头、状态徽章、结果横幅等）
│   ├── screens/     # 配置 / SIM 状态 / 快捷方案 / 设置 / 关于 / 个性化
│   ├── theme/       # 主题色板与切换
│   └── MainScaffold.kt  # 底部导航容器
└── MainActivity.kt
```

### 🧩 技术栈

- **Kotlin + Jetpack Compose**（Material 3）
- **Shizuku**（`rikka.shizuku:api:13.1.5`）+ HiddenApiBypass，全程免 Root
- 包名 `com.github.krfr` · 最新版本 v1.1.0
- minSdk 26 / targetSdk 35 / compileSdk 36

### 📄 许可证

本项目基于 **[Ackites/Nrfr](https://github.com/Ackites/Nrfr)** 开发，遵循 **Apache-2.0** 许可证。

上游许可证：[Ackites/Nrfr LICENSE](https://github.com/Ackites/Nrfr/blob/master/LICENSE)

### ⚠️ 免责声明

本工具仅供学习研究。修改运营商配置可能影响网络、漫游与区域识别，请自行承担使用风险。

---

<div align="center">

**Made by [KinsonStudio](https://github.com/kinsonstudio)** · 基於 Nrfr 開源 · Apache-2.0

</div>
