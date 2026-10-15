package com.github.krfr.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.OperationState
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
    var showLogDialog by remember { mutableStateOf(false) }
    var logVersion by remember { mutableStateOf(0) }
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
                    OperationState.markRunning()
                    BackupManager.applyBackup(appContext, slots)
                }
            }.onSuccess { count ->
                statusIsError = false
                statusMessage = "恢复完成，已应用 $count 个卡槽"
                LogStore.i("恢复备份完成，已应用 $count 个卡槽")
            }.onFailure {
                statusIsError = true
                statusMessage = if (OperationState.wasCancelled()) {
                    "恢复已被强制终止"
                } else {
                    "恢复失败：${it.message ?: "文件无效"}".take(200)
                }
                LogStore.e("恢复备份异常: ${it.message.orEmpty()}")
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

            SectionLabel("维护")
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
                        icon = { IconBox(Icons.Filled.BugReport, "日志") },
                        title = "运行日志",
                        description = "查看读取、保存、还原等操作记录与错误",
                        enabled = true,
                        onClick = {
                            LogStore.restore(appContext)
                            logVersion += 1
                            showLogDialog = true
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoIndication {
                                val cancelled = OperationState.cancelAll()
                                statusIsError = cancelled
                                statusMessage = if (cancelled) {
                                    "已强制终止当前后台任务"
                                } else {
                                    "当前没有运行中的任务"
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBox(Icons.Filled.Close, "终止", tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "强制清除任务",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "保存/还原卡住时强制终止后台操作",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            if (OperationState.isRunning()) "运行中" else "空闲",
                            if (OperationState.isRunning()) "warn" else "plain"
                        )
                    }
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

    if (showLogDialog) {
        LogDialog(
            appContext = appContext,
            version = logVersion,
            onDismiss = { showLogDialog = false }
        )
    }
}

@Composable
private fun LogDialog(
    appContext: android.content.Context,
    version: Int,
    onDismiss: () -> Unit
) {
    var logs by remember(version) { mutableStateOf(LogStore.snapshot()) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            appContext.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(LogStore.exportText().toByteArray(Charsets.UTF_8))
            } ?: error("无法写入导出文件")
        }.onFailure {
            LogStore.e("导出日志失败: ${it.message.orEmpty()}")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("运行日志", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                "暂无日志",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(logs) { entry ->
                            val color = when (entry.level) {
                                "E" -> MaterialTheme.colorScheme.error
                                "W" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                "${entry.timeText()} [${entry.level}] ${entry.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    "最多保留 200 条，重启后仍可查看",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        runCatching {
                            val clipboard = appContext.getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE
                            ) as? android.content.ClipboardManager
                            clipboard?.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "krfr 运行日志",
                                    LogStore.exportText()
                                )
                            )
                        }.onFailure { LogStore.e("复制日志失败: ${it.message.orEmpty()}") }
                    }) {
                        Text("复制日志")
                    }
                    TextButton(onClick = {
                        exportLauncher.launch("krfr_logs_${System.currentTimeMillis() / 1000}.txt")
                    }) {
                        Text("导出日志")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                LogStore.clear()
                LogStore.persist(appContext)
                logs = emptyList()
            }) {
                Text("清空")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
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
