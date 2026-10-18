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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.BuildConfig
import com.github.krfr.manager.AppUpdateManager
import com.github.krfr.manager.BackupManager
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.LogTexts
import com.github.krfr.manager.OperationState
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.ui.HongguoStyleUpdateDialog
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.components.StatusPill
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.R
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.clickableNoIndication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 设置页：配置备份/恢复、日志维护、关于与检查更新 */
@Composable
fun SettingsScreen(
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
                    } ?: error(context.getString(R.string.cannot_write_backup))
                }
            }.onSuccess {
                statusIsError = false
                statusMessage = context.getString(R.string.backup_exported)
            }.onFailure {
                statusIsError = true
                statusMessage = context.getString(R.string.export_failed, it.message ?: context.getString(R.string.unknown_error))
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
                    } ?: error(context.getString(R.string.cannot_read_backup))
                    BackupManager.parseJson(json)
                }
                withContext(Dispatchers.IO) {
                    OperationState.markRunning()
                    BackupManager.applyBackup(appContext, slots)
                }
            }.onSuccess { count ->
                statusIsError = false
                statusMessage = context.getString(R.string.restore_done, count)
                LogStore.i(LogTexts.t("backup_restored", count))
            }.onFailure {
                statusIsError = true
                statusMessage = if (OperationState.wasCancelled()) {
                    context.getString(R.string.restore_forced_stopped)
                } else {
                    context.getString(R.string.restore_failed, it.message ?: context.getString(R.string.invalid_file)).take(200)
                }
                LogStore.e(LogTexts.t("backup_restore_failed", it.message.orEmpty()))
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
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
                logoText = "SE"
            )

            SectionLabel(stringResource(R.string.sec_config_data))
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
                        icon = { IconBox(Icons.Filled.Upload, stringResource(R.string.export_cd)) },
                        title = stringResource(R.string.export_backup),
                        description = stringResource(R.string.export_backup_desc),
                        enabled = !isBusy,
                        onClick = {
                            if (!isBusy) backupLauncher.launch("krfr_backup_${System.currentTimeMillis() / 1000}.json")
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(
                        icon = { IconBox(Icons.Filled.Download, stringResource(R.string.import_cd)) },
                        title = stringResource(R.string.restore_backup),
                        description = stringResource(R.string.restore_backup_desc),
                        enabled = !isBusy,
                        onClick = { if (!isBusy) restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
            }

            SectionLabel(stringResource(R.string.sec_maintenance))
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
                        icon = { IconBox(Icons.Filled.BugReport, stringResource(R.string.log_cd)) },
                        title = stringResource(R.string.run_logs),
                        description = stringResource(R.string.run_logs_desc),
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
                                    context.getString(R.string.forced_stopped)
                                } else {
                                    context.getString(R.string.no_running_task)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBox(Icons.Filled.Close, stringResource(R.string.stop_cd), tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.force_clear_task),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                stringResource(R.string.force_clear_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            if (OperationState.isRunning()) stringResource(R.string.running) else stringResource(R.string.idle),
                            if (OperationState.isRunning()) "warn" else "plain"
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.sec_about))
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
                            Text(stringResource(R.string.current_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                updateMessage = context.getString(R.string.checking_update)
                                coroutineScope.launch {
                                    try {
                                        val release = withContext(Dispatchers.IO) {
                                            AppUpdateManager.fetchLatestRelease()
                                        }
                                        if (!AppUpdateManager.isNewerVersion(release.version, currentVersion)) {
                                            updateMessage = context.getString(R.string.already_latest)
                                        } else {
                                            updateMessage = null
                                            val activity = context as? Activity
                                            if (activity == null || activity.isFinishing) {
                                                updateMessage = context.getString(R.string.cannot_show_update)
                                            } else {
                                                HongguoStyleUpdateDialog.show(activity, currentVersion, release)
                                            }
                                        }
                                    } catch (error: Exception) {
                                        updateMessage = context.getString(R.string.update_check_failed, error.message ?: context.getString(R.string.network_error)).take(120)
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
                            Text(stringResource(R.string.check_update), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            updateMessage?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(
                            if (isCheckingUpdate) stringResource(R.string.checking) else stringResource(R.string.tap_to_check),
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
                        Text(stringResource(R.string.about_app), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(R.string.details_arrow),
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
            } ?: error(appContext.getString(R.string.cannot_write_export))
        }.onFailure {
            LogStore.e(LogTexts.t("log_export_failed", it.message.orEmpty()))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_logs), fontWeight = FontWeight.Bold) },
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
                                stringResource(R.string.no_logs),
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
                    stringResource(R.string.log_keep_note),
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
                                    "krfr run logs",
                                    LogStore.exportText()
                                )
                            )
                        }.onFailure { LogStore.e(LogTexts.t("log_copy_failed", it.message.orEmpty())) }
                    }) {
                        Text(stringResource(R.string.copy_logs))
                    }
                    TextButton(onClick = {
                        exportLauncher.launch("krfr_logs_${System.currentTimeMillis() / 1000}.txt")
                    }) {
                        Text(stringResource(R.string.export_logs))
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
                Text(stringResource(R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
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
        StatusPill(
            if (enabled) stringResource(R.string.status_available) else stringResource(R.string.status_processing),
            if (enabled) "info" else "plain"
        )
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
