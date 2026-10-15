# krfr

基于开源项目 **Nrfr** 二次开发的 SIM 卡国家码与运营商配置工具，免 Root（经 Shizuku 授权）。

> 原上游：https://github.com/Ackites/Nrfr （Apache-2.0）

## 功能

- **配置页**：经 Shizuku 修改 SIM 卡国家码（ISO 2 位）与运营商名称，双卡独立覆盖，支持自定义国家码 / 自定义运营商名称，一键保存、一键还原
- **SIM 状态**：查看每张卡的网络类型、信号强度、MCC/MNC、国家/地区、数据漫游、ICCID（脱敏显示）等实时信息
- **快捷方案**：18 个常用「国家码 + 运营商」组合，选定 SIM 后一键应用
- **备份与恢复**：把当前各卡槽覆盖配置导出为 JSON 文件，换机/重装后一键恢复
- **设置**：应用内主题切换（跟随系统 / 浅色 / 深色）、检查更新、关于
- 开机自动恢复已保存的覆盖配置（Shizuku 授权后）

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Shizuku（`rikka.shizuku:api:13.1.5`）+ HiddenApiBypass
- 包名：`com.github.krfr`　应用名：`krfr`　版本：1.0.0
- minSdk 26 / targetSdk 35 / compileSdk 36

## 构建

### 环境要求

- Android Studio（Koala 或更新）
- **JDK 21**：本项目 Gradle 8.9 不支持 JDK 25。本机可用的 JDK 21 已解压在 `~/jdk21/Contents/Home`。
  - Android Studio 中设置：`File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK`，选择 `~/jdk21/Contents/Home`（或通过 `Download JDK` 安装 21）。
  - 命令行构建：

```bash
cd /Users/cai/Doubao/chats/2026-10-15/new-chat-1/krfr
ANDROID_HOME=$HOME/Library/Android/sdk JAVA_HOME=$HOME/jdk21/Contents/Home ./gradlew :app:assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

> 注意：如果环境里存在 `ANDROID_SDK_ROOT` 且指向错误路径，请先 `unset ANDROID_SDK_ROOT`，避免与 `ANDROID_HOME` 冲突。

## 安装与授权

1. 安装 APK（需要允许「安装未知来源应用」）。
2. **包名已变更（新包名 `com.github.krfr`）**，与旧 Nrfr 是不同应用：需先安装 Shizuku（[RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)）并启动，再打开 krfr 按引导完成授权。
3. 授权后即可使用全部功能。

## 发布检查更新

应用内「检查更新」读取 GitHub Release：`app/src/main/java/com/github/krfr/manager/AppUpdateManager.kt` 中 `API_URL` 已指向 **kinsonstudio/krfr**。仓库发布 Release（`v1.0.0` 等 tag，并附带 APK 资产）后，应用内即可直接检查并下载更新。

## 图标

自适应图标为 krfr 原创设计（青蓝渐变 + SIM 卡 + 信号弧）：
- 矢量前景：`app/src/main/res/drawable/ic_launcher_foreground.xml`
- 渐变背景：`app/src/main/res/drawable/ic_launcher_background.xml`
- 源文件与预览：`docs/krfr-icon.svg` / `docs/krfr-icon-preview.png`

## 目录结构（与上游差异）

```
app/src/main/java/com/github/krfr/
├── data/
│   ├── CountryPresets.kt       # 国家码预设
│   ├── PresetCarriers.kt       # 运营商预设
│   ├── QuickPresets.kt         # [新增] 快捷方案预设
│   └── SettingsStore.kt        # [新增] 主题模式等设置持久化
├── manager/
│   ├── BackupManager.kt        # [新增] 配置备份/恢复
│   ├── SimStatusReader.kt      # [新增] SIM 实时状态读取（Shizuku shell 身份）
│   └── ...（CarrierConfig 核心逻辑沿用上游，仅改名）
├── ui/
│   ├── MainScaffold.kt         # [新增] 底部导航容器（配置/SIM状态/快捷方案/设置）
│   ├── components/KrfrWidgets.kt # [新增] 公共 UI 组件
│   └── screens/
│       ├── ConfigScreen.kt     # [重做] 配置页
│       ├── SimStatusScreen.kt  # [新增] SIM 状态页
│       ├── QuickPresetScreen.kt# [新增] 快捷方案页
│       ├── SettingsScreen.kt   # [新增] 设置页
│       └── AboutScreen.kt      # [重做] 关于页
└── theme/                      # [重做] 清爽浅色简约风主题（青蓝主色 + 主题切换）
```

## 许可证

本项目基于 **[Ackites/Nrfr](https://github.com/Ackites/Nrfr)** 开发，遵循 **Apache-2.0** 许可证。上游许可证见 [Ackites/Nrfr LICENSE](https://github.com/Ackites/Nrfr/blob/master/LICENSE)。

## 免责声明

本工具仅供学习研究。修改运营商配置可能影响网络、漫游与区域识别，请自行承担风险。
