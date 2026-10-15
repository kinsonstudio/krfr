package com.github.krfr.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.BuildConfig
import com.github.krfr.manager.AppUpdateManager
import com.github.krfr.manager.BackupManager
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.ui.HongguoStyleUpdateDialog
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.components.StatusPill
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.clickableNoIndication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 设置页：主题切换、配置备份/恢复、关于与检查更新 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowAbout: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val currentVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { BuildConfig.VERSION_NAME }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isBusy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val cards = CarrierConfigManager.getSimCards(appContext)
                    val json = BackupManager.exportJson(cards)
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入备份文件")
                }
            }.onSuccess {
                statusIsError = false
                statusMessage = "备份已导出"
            }.onFailure {
                statusIsError = true
                statusMessage = "导出失败：${it.message ?: "未知错误"}"
            }
            isBusy = false
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isBusy = true
            runCatching {
                val slots = withContext(Dispatchers.IO) {
                    val json = appContext.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("无法读取备份文件")
                    BackupManager.parseJson(json)
                }
                withContext(Dispatchers.IO) {
                    BackupManager.applyBackup(appContext, slots)
                }
            }.onSuccess { count ->
                statusIsError = false
                statusMessage = "恢复完成，已应用 $count 个卡槽"
            }.onFailure {
                statusIsError = true
                statusMessage = "恢复失败：${it.message ?: "文件无效"}".take(200)
            }
            isBusy = false
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        delay(3000)
        if (statusMessage == msg) statusMessage = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
        ) {
            PageHeader(
                title = "设置",
                subtitle = "主题 · 数据 · 关于",
                logoText = "SE"
            )

            SectionLabel("外观")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = KrfrCardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    ThemeModeRow(
                        mode = ThemeMode.SYSTEM,
                        title = "跟随系统",
                        description = "随手机系统自动切换深浅色",
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeModeRow(
                        mode = ThemeMode.LIGHT,
                        title = "浅色",
                        description = "始终使用清爽浅色界面",
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    ThemeModeRow(
                        mode = ThemeMode.DARK,
                        title = "深色",
                        description = "始终使用深色界面",
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                }
            }

            SectionLabel("配置数据")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = KrfrCardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    ActionRow(
                        icon = { IconBox(Icons.Filled.Upload, "导出") },
                        title = "导出备份",
                        description = "把当前各卡槽的国家码/运营商配置保存为文件",
                        enabled = !isBusy,
                        onClick = {
                            if (!isBusy) backupLauncher.launch("krfr_backup_${System.currentTimeMillis() / 1000}.json")
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(
                        icon = { IconBox(Icons.Filled.Download, "导入") },
                        title = "恢复备份",
                        description = "从备份文件恢复覆盖配置（备份为空的卡槽会被还原）",
                        enabled = !isBusy,
                        onClick = { if (!isBusy) restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
            }

            SectionLabel("关于")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = KrfrCardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("当前版本", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "v$currentVersion",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KrfrAdaptive.link()
                                )
                            )
                        }
                        StatusPill("krfr", "info")
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoIndication {
                                if (isCheckingUpdate || isBusy) return@clickableNoIndication
                                isCheckingUpdate = true
                                updateMessage = "正在检查更新…"
                                coroutineScope.launch {
                                    try {
                                        val release = withContext(Dispatchers.IO) {
                                            AppUpdateManager.fetchLatestRelease()
                                        }
                                        if (!AppUpdateManager.isNewerVersion(release.version, currentVersion)) {
                                            updateMessage = "当前已是最新版本"
                                        } else {
                                            updateMessage = null
                                            val activity = context as? Activity
                                            if (activity == null || activity.isFinishing) {
                                                updateMessage = "无法弹出更新窗口"
                                            } else {
                                                HongguoStyleUpdateDialog.show(activity, currentVersion, release)
                                            }
                                        }
                                    } catch (error: Exception) {
                                        updateMessage = "更新检查失败：${error.message ?: "网络或更新源错误"}".take(120)
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("检查更新", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            updateMessage?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(
                            if (isCheckingUpdate) "检查中" else "点击检查",
                            if (isCheckingUpdate) "info" else "plain"
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoIndication(onClick = onShowAbout)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("关于本应用", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "详情 →",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = KrfrAdaptive.link()
                            )
                        )
                    }
                }
            }
        }

        SoftStatusBanner(
            message = statusMessage,
            isError = statusIsError,
            onDismiss = { statusMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 78.dp)
        )
    }
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = KrfrAdaptive.link(),
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            IconBox(Icons.Filled.Check, "选中", tint = KrfrAdaptive.link())
        }
    }
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusPill(if (enabled) "可用" else "处理中", if (enabled) "info" else "plain")
    }
}

@Composable
private fun IconBox(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color = KrfrAdaptive.link()
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = description,
        tint = tint,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KrfrAdaptive.linkSoft())
            .padding(8.dp)
    )
}
