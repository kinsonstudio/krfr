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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.krfr.data.LanguageManager
import com.github.krfr.data.descFor
import com.github.krfr.data.nameFor
import com.github.krfr.data.QuickPresets
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.LogTexts
import com.github.krfr.manager.OperationState
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.model.SimCardInfo
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.R
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.KrfrPill
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.clickablePressScale
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
                errorMessage = context.getString(R.string.no_sim_shizuku)
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
        val lang = LanguageManager.currentLang(appContext)
        val simCard = selectedSimCard ?: return
        if (isApplying || !OperationState.begin()) return
        coroutineScope.launch {
            isApplying = true
            applyingId = preset.id
            try {
                val result = withContext(Dispatchers.IO) {
                    OperationState.markRunning()
                    CarrierConfigManager.setCarrierConfig(
                        appContext,
                        simCard.subId,
                        preset.countryCode,
                        preset.carrierName
                    )
                }
                statusIsError = false
                statusMessage = if (result.warnings.isEmpty()) {
                    context.getString(R.string.preset_applied, simCard.slot, preset.nameFor(lang))
                } else {
                    context.getString(R.string.preset_applied_warn, simCard.slot, preset.nameFor(lang), result.warnings.joinToString("；")).take(320)
                }
            } catch (e: Exception) {
                statusIsError = true
                statusMessage = if (OperationState.wasCancelled()) {
                    context.getString(R.string.operation_cancelled)
                } else {
                    context.getString(R.string.apply_failed, e.message?.lineSequence()?.firstOrNull()?.take(120) ?: context.getString(R.string.unknown_error))
                }
                LogStore.e(LogTexts.t("preset_apply_failed", e.message.orEmpty()))
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
                    title = stringResource(R.string.quick_title),
                    subtitle = stringResource(R.string.quick_subtitle),
                    logoText = "QP",
                    onRefresh = { refreshTrigger += 1 }
                )
            }

            item {
                SectionLabel(stringResource(R.string.apply_to))
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
                            stringResource(R.string.reading_sim),
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
                    SectionLabel(stringResource(R.string.presets), modifier = Modifier.padding(top = 12.dp))
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
                        stringResource(R.string.preset_count_note, QuickPresets.presets.size),
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
                    .clickablePressScale(onClick = { onSelected(simCard) })
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
    val lang = LanguageManager.currentLang(LocalContext.current)
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
                .clickablePressScale(onClick = onClick)
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
                    preset.nameFor(lang),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    preset.descFor(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                if (applying) stringResource(R.string.applying) else stringResource(R.string.apply),
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
