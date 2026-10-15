package com.github.krfr.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.krfr.data.QuickPresets
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.OperationState
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.model.SimCardInfo
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.KrfrPill
import com.github.krfr.ui.theme.clickableNoIndication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 快捷方案页：选定 SIM 后一键应用常用「国家码+运营商」组合 */
@Composable
fun QuickPresetScreen() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    var simCards by remember { mutableStateOf<List<SimCardInfo>>(emptyList()) }
    var selectedSimCard by remember { mutableStateOf<SimCardInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    var applyingId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appContext, refreshTrigger) {
        isLoading = true
        errorMessage = null
        runCatching {
            withContext(Dispatchers.IO) {
                CarrierConfigManager.getSimCards(appContext)
            }
        }.onSuccess {
            simCards = it
            if (selectedSimCard == null || selectedSimCard?.subId !in it.map { c -> c.subId }) {
                selectedSimCard = it.minByOrNull { c -> c.slot }
            }
            if (it.isEmpty()) {
                errorMessage = "未检测到可用 SIM 卡，请确认 Shizuku 已授权"
            }
        }.onFailure {
            errorMessage = it.toSimCardsUserMessage()
        }
        isLoading = false
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        delay(2800)
        if (statusMessage == msg) statusMessage = null
    }

    fun applyPreset(preset: QuickPresets.QuickPreset) {
        val simCard = selectedSimCard ?: return
        if (isApplying || !OperationState.begin()) return
        coroutineScope.launch {
            isApplying = true
            applyingId = preset.id
            try {
                val result = withContext(Dispatchers.IO) {
                    CarrierConfigManager.setCarrierConfig(
                        appContext,
                        simCard.subId,
                        preset.countryCode,
                        preset.carrierName
                    )
                }
                statusIsError = false
                statusMessage = if (result.warnings.isEmpty()) {
                    "SIM${simCard.slot} 已应用：${preset.name}"
                } else {
                    "SIM${simCard.slot} 已应用：${preset.name}，${result.warnings.joinToString("；")}".take(320)
                }
            } catch (e: Exception) {
                statusIsError = true
                statusMessage = "应用失败：${e.message?.lineSequence()?.firstOrNull()?.take(120) ?: "未知错误"}"
            } finally {
                OperationState.end()
                isApplying = false
                applyingId = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
        ) {
            item {
                PageHeader(
                    title = "快捷方案",
                    subtitle = "一键应用常用国家码组合",
                    logoText = "QP",
                    onRefresh = { refreshTrigger += 1 }
                )
            }

            item {
                SectionLabel("应用到")
            }

            if (isLoading && simCards.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = KrfrCardShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            "正在读取 SIM…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else if (errorMessage != null && simCards.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = KrfrCardShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                item {
                    SimChipSelector(
                        simCards = simCards.sortedBy { it.slot },
                        selectedSimCard = selectedSimCard,
                        onSelected = { selectedSimCard = it }
                    )
                }

                item {
                    SectionLabel("方案", modifier = Modifier.padding(top = 12.dp))
                }

                items(QuickPresets.presets, key = { it.id }) { preset ->
                    QuickPresetRow(
                        preset = preset,
                        applying = isApplying && applyingId == preset.id,
                        enabled = selectedSimCard != null && !isApplying,
                        onClick = { applyPreset(preset) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Text(
                        "共 ${QuickPresets.presets.size} 个方案 · 点击卡片立即应用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
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
private fun SimChipSelector(
    simCards: List<SimCardInfo>,
    selectedSimCard: SimCardInfo?,
    onSelected: (SimCardInfo) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        simCards.forEach { simCard ->
            val selected = selectedSimCard?.subId == simCard.subId
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(KrfrPill)
                    .background(if (selected) KrfrAdaptive.linkSoft() else MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.outlineVariant,
                        shape = KrfrPill
                    )
                    .clickableNoIndication(onClick = { onSelected(simCard) })
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SIM${simCard.slot}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    simCard.carrierName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickPresetRow(
    preset: QuickPresets.QuickPreset,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .clip(KrfrCardShape)
                .clickableNoIndication(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KrfrAdaptive.linkSoft()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    preset.countryCode,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = KrfrAdaptive.link()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                if (applying) "应用中…" else "应用",
                modifier = Modifier
                    .clip(KrfrPill)
                    .background(if (enabled && !applying) KrfrAdaptive.linkSoft() else KrfrAdaptive.wash())
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled && !applying) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
