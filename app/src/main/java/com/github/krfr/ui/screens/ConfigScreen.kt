package com.github.krfr.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.data.CountryPresets
import com.github.krfr.data.PresetCarriers
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.OperationState
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.model.SimCardInfo
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.KrfrPill
import com.github.krfr.ui.theme.KrfrSelectShape
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.clickablePressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SlotFormDraft(
    val countryCode: String,
    val customCountryCode: String,
    val isCustomCountryCode: Boolean,
    val carrier: PresetCarriers.CarrierPreset?,
    val customCarrierName: String
)

private fun draftFromSimConfig(sim: SimCardInfo): SlotFormDraft {
    val countryCode = sim.currentConfig["国家码"]?.trim()?.uppercase().orEmpty()
    val carrierName = sim.currentConfig["运营商名称"]?.trim().orEmpty()
    if (countryCode.isEmpty() && carrierName.isEmpty()) {
        return SlotFormDraft("", "", false, null, "")
    }
    val knownCountry = CountryPresets.countries.any { it.code.equals(countryCode, ignoreCase = true) }
    val matched = PresetCarriers.presets.firstOrNull { preset ->
        preset.name != "自定义" && (preset.name == carrierName || preset.displayName == carrierName)
    }
    val customPreset = PresetCarriers.presets.firstOrNull { it.name == "自定义" }
    return SlotFormDraft(
        countryCode = countryCode,
        customCountryCode = if (countryCode.isNotEmpty() && !knownCountry) countryCode else "",
        isCustomCountryCode = countryCode.isNotEmpty() && !knownCountry,
        carrier = matched ?: if (carrierName.isNotEmpty()) customPreset else null,
        customCarrierName = when {
            matched != null -> matched.displayName
            else -> carrierName
        }
    )
}

// 配置页：双卡草稿独立，保存/还原/全部还原
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    var selectedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedSimCard by remember { mutableStateOf<SimCardInfo?>(null) }
    var selectedCountryCode by remember { mutableStateOf("") }
    var customCountryCode by remember { mutableStateOf("") }
    var isCustomCountryCode by remember { mutableStateOf(false) }
    var selectedCarrier by remember { mutableStateOf<PresetCarriers.CarrierPreset?>(null) }
    var customCarrierName by remember { mutableStateOf("") }
    var simCards by remember { mutableStateOf<List<SimCardInfo>>(emptyList()) }
    var isLoadingSimCards by remember { mutableStateOf(false) }
    var simCardsError by remember { mutableStateOf<String?>(null) }
    var isSimCardMenuExpanded by remember { mutableStateOf(false) }
    var isCountryCodeMenuExpanded by remember { mutableStateOf(false) }
    var isCarrierMenuExpanded by remember { mutableStateOf(false) }
    var isApplyingConfig by remember { mutableStateOf(false) }
    var activeAction by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val slotDrafts = remember { mutableStateMapOf<Int, SlotFormDraft>() }

    fun currentDraft() = SlotFormDraft(
        countryCode = selectedCountryCode,
        customCountryCode = customCountryCode,
        isCustomCountryCode = isCustomCountryCode,
        carrier = selectedCarrier,
        customCarrierName = customCarrierName
    )

    fun applyDraft(draft: SlotFormDraft) {
        selectedCountryCode = draft.countryCode
        customCountryCode = draft.customCountryCode
        isCustomCountryCode = draft.isCustomCountryCode
        selectedCarrier = draft.carrier
        customCarrierName = draft.customCarrierName
        isCountryCodeMenuExpanded = false
        isCarrierMenuExpanded = false
    }

    fun switchToSim(next: SimCardInfo) {
        val prev = selectedSimCard
        if (prev != null && prev.slot != next.slot) {
            slotDrafts[prev.slot] = currentDraft()
        }
        selectedSlot = next.slot
        selectedSimCard = next
        applyDraft(slotDrafts[next.slot] ?: draftFromSimConfig(next))
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        delay(2800)
        if (statusMessage == msg) statusMessage = null
    }

    LaunchedEffect(appContext, refreshTrigger) {
        isLoadingSimCards = true
        simCardsError = null
        runCatching {
            withContext(Dispatchers.IO) {
                CarrierConfigManager.getSimCards(appContext)
            }
        }.onSuccess {
            simCards = it
            if (it.isEmpty()) {
                simCardsError = "未检测到可用 SIM 卡，请确认 Shizuku 已授权"
            }
        }.onFailure {
            simCardsError = it.toSimCardsUserMessage()
        }
        isLoadingSimCards = false
    }

    LaunchedEffect(simCards, selectedSlot) {
        if (simCards.isEmpty()) {
            selectedSimCard = null
            return@LaunchedEffect
        }
        val slot = selectedSlot ?: simCards.minByOrNull { it.slot }?.slot
        if (selectedSlot == null && slot != null) {
            selectedSlot = slot
        }
        val next = simCards.find { it.slot == slot } ?: simCards.first()
        if (selectedSimCard == null || selectedSimCard?.slot != next.slot) {
            switchToSim(next)
        } else {
            selectedSimCard = next
        }
    }

    fun refreshSimCards() {
        refreshTrigger += 1
    }

    val effectiveSimCard = selectedSimCard ?: simCards.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
        ) {
            PageHeader(
                title = "krfr",
                subtitle = "SIM 卡国家码 · 运营商配置",
                logoText = "K",
                onRefresh = ::refreshSimCards
            )

            CurrentConfigsOverview(
                simCards = simCards,
                isLoading = isLoadingSimCards,
                errorMessage = simCardsError
            )

            if (simCards.isNotEmpty()) {
                ResetAllButton(
                    enabled = !isApplyingConfig,
                    isApplying = isApplyingConfig && activeAction == "resetAll",
                    onResetAll = {
                        if (!isApplyingConfig && OperationState.begin()) {
                            coroutineScope.launch {
                                isApplyingConfig = true
                                activeAction = "resetAll"
                                try {
                                    withContext(Dispatchers.IO) {
                                        OperationState.markRunning()
                                        CarrierConfigManager.restoreAllToUnset(appContext)
                                    }
                                    statusIsError = false
                                    statusMessage = "已还原全部 SIM 的覆盖配置（主卡 + 副卡）"
                                    delay(400)
                                    refreshSimCards()
                                    slotDrafts.clear()
                                    selectedCountryCode = ""
                                    selectedCarrier = null
                                    customCarrierName = ""
                                    isCustomCountryCode = false
                                    customCountryCode = ""
                                } catch (e: Exception) {
                                    statusIsError = true
                                    statusMessage = if (OperationState.wasCancelled()) {
                                        "操作已被强制终止"
                                    } else {
                                        "全部还原失败: ${formatErrorMessage(e)}"
                                    }
                                    LogStore.e("全部还原异常: ${e.message.orEmpty()}")
                                    delay(400)
                                    refreshSimCards()
                                } finally {
                                    OperationState.end()
                                    isApplyingConfig = false
                                    activeAction = null
                                }
                            }
                        }
                    }
                )
            }

            SectionLabel("目标 SIM 卡")
            SimCardSelector(
                simCards = simCards,
                selectedSimCard = effectiveSimCard,
                isExpanded = isSimCardMenuExpanded,
                onExpandedChange = { isSimCardMenuExpanded = it },
                onSimCardSelected = { switchToSim(it) }
            )

            SectionLabel("覆盖内容")
            CountryCodeSelector(
                selectedCountryCode = selectedCountryCode,
                isCustomCountryCode = isCustomCountryCode,
                customCountryCode = customCountryCode,
                isExpanded = isCountryCodeMenuExpanded,
                onExpandedChange = { isCountryCodeMenuExpanded = it },
                onCountryCodeSelected = { code ->
                    selectedCountryCode = code
                    isCustomCountryCode = false
                    if (selectedCarrier?.region != code) {
                        selectedCarrier = null
                        customCarrierName = ""
                    }
                },
                onCustomSelected = {
                    isCustomCountryCode = true
                    selectedCountryCode = customCountryCode
                }
            )

            if (isCustomCountryCode) {
                CustomCountryCodeInput(
                    value = customCountryCode,
                    onValueChange = {
                        if (it.length <= 2 && it.all { char -> char.isLetter() }) {
                            customCountryCode = it.uppercase()
                            selectedCountryCode = it.uppercase()
                        }
                    }
                )
            }

            CarrierSelector(
                selectedCarrier = selectedCarrier,
                selectedCountryCode = selectedCountryCode,
                isCustomCountryCode = isCustomCountryCode,
                isExpanded = isCarrierMenuExpanded,
                onExpandedChange = { isCarrierMenuExpanded = it },
                onCarrierSelected = { carrier ->
                    selectedCarrier = carrier
                    customCarrierName = carrier.displayName
                    if (carrier.region.length == 2) {
                        selectedCountryCode = carrier.region
                        isCustomCountryCode = false
                    }
                }
            )

            if (selectedCarrier?.name == "自定义") {
                CustomCarrierNameInput(
                    value = customCarrierName,
                    onValueChange = { customCarrierName = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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

        ActionButtons(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            effectiveSimCard = effectiveSimCard,
            selectedCountryCode = selectedCountryCode,
            isCustomCountryCode = isCustomCountryCode,
            customCountryCode = customCountryCode,
            selectedCarrier = selectedCarrier,
            customCarrierName = customCarrierName,
            isApplyingConfig = isApplyingConfig,
            activeAction = activeAction,
            onReset = { simCard ->
                if (!isApplyingConfig && OperationState.begin()) {
                    coroutineScope.launch {
                        isApplyingConfig = true
                        activeAction = "reset"
                        try {
                            val result = withContext(Dispatchers.IO) {
                                OperationState.markRunning()
                                CarrierConfigManager.resetCarrierConfig(appContext, simCard.subId)
                            }
                            statusIsError = false
                            statusMessage = buildOperationMessage("设置已还原", result.warnings)
                            delay(400)
                            refreshSimCards()
                            slotDrafts.remove(simCard.slot)
                            selectedCountryCode = ""
                            selectedCarrier = null
                            customCarrierName = ""
                            isCustomCountryCode = false
                            customCountryCode = ""
                        } catch (e: Exception) {
                            statusIsError = true
                            statusMessage = if (OperationState.wasCancelled()) {
                                "操作已被强制终止"
                            } else {
                                "还原失败: ${formatErrorMessage(e)}"
                            }
                            LogStore.e("还原异常: ${e.message.orEmpty()}")
                            delay(400)
                            refreshSimCards()
                        } finally {
                            OperationState.end()
                            isApplyingConfig = false
                            activeAction = null
                        }
                    }
                }
            },
            onSave = { simCard ->
                if (!isApplyingConfig && OperationState.begin()) {
                    val carrierName = if (selectedCarrier?.name == "自定义") {
                        customCarrierName.takeIf { it.isNotEmpty() }
                    } else {
                        selectedCarrier?.displayName
                    }
                    val countryCode = if (isCustomCountryCode) {
                        customCountryCode.takeIf { it.length == 2 }
                    } else {
                        selectedCountryCode
                    }

                    coroutineScope.launch {
                        isApplyingConfig = true
                        activeAction = "save"
                        try {
                            val result = withContext(Dispatchers.IO) {
                                OperationState.markRunning()
                                CarrierConfigManager.setCarrierConfig(
                                    appContext,
                                    simCard.subId,
                                    countryCode,
                                    carrierName
                                )
                            }
                            statusIsError = false
                            statusMessage = buildOperationMessage("设置已保存", result.warnings)
                            slotDrafts[simCard.slot] = currentDraft()
                            delay(400)
                            refreshSimCards()
                        } catch (e: Exception) {
                            statusIsError = true
                            statusMessage = if (OperationState.wasCancelled()) {
                                "操作已被强制终止"
                            } else {
                                "保存失败: ${formatErrorMessage(e)}"
                            }
                            LogStore.e("保存异常: ${e.message.orEmpty()}")
                            delay(400)
                            refreshSimCards()
                        } finally {
                            OperationState.end()
                            isApplyingConfig = false
                            activeAction = null
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimCardSelector(
    simCards: List<SimCardInfo>,
    selectedSimCard: SimCardInfo?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSimCardSelected: (SimCardInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    SelectField(
        label = "SIM 卡",
        value = selectedSimCard?.let { "SIM ${it.slot} · ${it.carrierName}" } ?: "",
        expanded = isExpanded,
        iconGlyph = "SIM",
        onClick = { onExpandedChange(true) }
    )

    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { onExpandedChange(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = KrfrCardShape,
            tonalElevation = 0.dp
        ) {
            SheetTitle("选择 SIM 卡")
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(horizontal = 8.dp)
            ) {
                items(simCards) { simCard ->
                    SelectorSheetOption(
                        text = "SIM ${simCard.slot} · ${simCard.carrierName}",
                        supportingText = formatConfigSummary(simCard),
                        selected = selectedSimCard?.subId == simCard.subId,
                        onClick = {
                            onSimCardSelected(simCard)
                            onExpandedChange(false)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CurrentConfigsOverview(
    simCards: List<SimCardInfo>,
    isLoading: Boolean,
    errorMessage: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "当前状态",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        StatusBusySpinner(color = KrfrAdaptive.link())
                    }
                }
            }

            when {
                errorMessage != null && simCards.isEmpty() -> {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                simCards.isEmpty() -> {
                    Text(
                        if (isLoading) "正在读取 SIM…" else "未检测到可用 SIM 卡",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                else -> {
                    simCards.sortedBy { it.slot }.forEach { simCard ->
                        SimConfigSummary(simCard = simCard)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBusySpinner(color: Color) {
    val spin = rememberInfiniteTransition(label = "status-busy")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "status-busy-angle"
    )
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = stroke
        )
    }
}

@Composable
private fun SimConfigSummary(simCard: SimCardInfo) {
    val hasOverride = simCard.currentConfig.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KrfrAdaptive.wash())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .widthIn(min = 52.dp)
                .clip(KrfrPill)
                .background(MaterialTheme.colorScheme.onSurface)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "SIM${simCard.slot}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.surface
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                simCard.carrierName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatConfigSummary(simCard),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (hasOverride) FontFamily.Monospace else FontFamily.SansSerif
                ),
                color = if (hasOverride) KrfrAdaptive.codeAccent() else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            if (hasOverride) "已覆盖" else "默认",
            modifier = Modifier
                .clip(KrfrPill)
                .background(if (hasOverride) KrfrAdaptive.linkSoft() else KrfrAdaptive.card())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (hasOverride) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatConfigSummary(simCard: SimCardInfo): String {
    if (simCard.currentConfig.isEmpty()) {
        return "无覆盖配置"
    }
    return simCard.currentConfig.entries.joinToString(" · ") { (key, value) ->
        "$key: $value"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodeSelector(
    selectedCountryCode: String,
    isCustomCountryCode: Boolean,
    customCountryCode: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCountryCodeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val displayText = when {
        isCustomCountryCode -> "自定义"
        selectedCountryCode.isEmpty() -> ""
        else -> CountryPresets.countries.find { it.code == selectedCountryCode }
            ?.let { "${it.name} (${it.code})" }
            ?: selectedCountryCode
    }

    SelectField(
        label = "国家码",
        value = displayText,
        expanded = isExpanded,
        iconGlyph = "CN",
        onClick = { onExpandedChange(true) }
    )

    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { onExpandedChange(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = KrfrCardShape,
            tonalElevation = 0.dp
        ) {
            SheetTitle("选择国家码")
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 8.dp)
            ) {
                items(CountryPresets.countries) { countryInfo ->
                    SelectorSheetOption(
                        text = "${countryInfo.name} (${countryInfo.code})",
                        selected = !isCustomCountryCode && selectedCountryCode == countryInfo.code,
                        onClick = {
                            onCountryCodeSelected(countryInfo.code)
                            onExpandedChange(false)
                        }
                    )
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    SelectorSheetOption(
                        text = "自定义",
                        selected = isCustomCountryCode,
                        onClick = {
                            onCustomSelected()
                            onExpandedChange(false)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CustomCountryCodeInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("自定义国家码（2 位字母）") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true,
        shape = KrfrSelectShape,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarrierSelector(
    selectedCarrier: PresetCarriers.CarrierPreset?,
    selectedCountryCode: String,
    isCustomCountryCode: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCarrierSelected: (PresetCarriers.CarrierPreset) -> Unit
) {
    val carrierMenuItems = remember(selectedCountryCode, isCustomCountryCode) {
        buildCarrierMenuItems(
            selectedCountryCode = selectedCountryCode,
            isCustomCountryCode = isCustomCountryCode
        )
    }
    val sheetState = rememberModalBottomSheetState()

    SelectField(
        label = "运营商名称",
        value = selectedCarrier?.name ?: "",
        expanded = isExpanded,
        iconGlyph = "OP",
        onClick = { onExpandedChange(true) }
    )

    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { onExpandedChange(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = KrfrCardShape,
            tonalElevation = 0.dp
        ) {
            SheetTitle("选择运营商")
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 8.dp)
            ) {
                items(carrierMenuItems) { item ->
                    when (item) {
                        is CarrierMenuItem.Header -> {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is CarrierMenuItem.Option -> {
                            SelectorSheetOption(
                                text = item.carrier.name,
                                selected = selectedCarrier == item.carrier,
                                onClick = {
                                    onCarrierSelected(item.carrier)
                                    onExpandedChange(false)
                                }
                            )
                        }
                        CarrierMenuItem.Divider -> {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SelectField(
    label: String,
    value: String,
    expanded: Boolean,
    iconGlyph: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KrfrSelectShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) KrfrAdaptive.link() else MaterialTheme.colorScheme.outlineVariant,
                shape = KrfrSelectShape
            )
            .clickablePressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KrfrAdaptive.linkSoft()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                iconGlyph,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = KrfrAdaptive.link()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value.ifEmpty { "点击选择" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (value.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (expanded) KrfrAdaptive.linkSoft() else KrfrAdaptive.wash()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

@Composable
private fun SelectorSheetOption(
    text: String,
    onClick: () -> Unit,
    supportingText: String? = null,
    selected: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) KrfrAdaptive.linkSoft() else Color.Transparent)
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildCarrierMenuItems(
    selectedCountryCode: String = "",
    isCustomCountryCode: Boolean = false
): List<CarrierMenuItem> {
    val carriersByRegion = PresetCarriers.presets
        .filter { it.region.isNotEmpty() }
        .groupBy { it.region }
    val countryNames = CountryPresets.countries.associate { it.code to it.name }
    val result = mutableListOf<CarrierMenuItem>()

    val regionOrder = when {
        !isCustomCountryCode && selectedCountryCode.length == 2 &&
            carriersByRegion.containsKey(selectedCountryCode.uppercase()) -> {
            listOf(selectedCountryCode.uppercase())
        }
        else -> CountryPresets.countries.map { it.code }
    }

    regionOrder.forEach { region ->
        val carriers = carriersByRegion[region] ?: return@forEach
        result.add(CarrierMenuItem.Header(countryNames[region] ?: region))
        carriers.sortedBy { it.name }.forEach { carrier ->
            result.add(CarrierMenuItem.Option(carrier))
        }
        if (regionOrder.size > 1) {
            result.add(CarrierMenuItem.Divider)
        }
    }

    if (result.lastOrNull() is CarrierMenuItem.Divider) {
        result.removeAt(result.lastIndex)
    }

    PresetCarriers.presets
        .filter { it.region.isEmpty() }
        .sortedBy { it.name }
        .forEach { carrier ->
            result.add(CarrierMenuItem.Option(carrier))
        }

    return result
}

private sealed class CarrierMenuItem {
    data class Header(val title: String) : CarrierMenuItem()
    data class Option(val carrier: PresetCarriers.CarrierPreset) : CarrierMenuItem()
    object Divider : CarrierMenuItem()
}

@Composable
private fun CustomCarrierNameInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("自定义运营商名称") },
        shape = KrfrSelectShape,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildOperationMessage(successMessage: String, warnings: List<String>): String {
    if (warnings.isEmpty()) return successMessage
    return "$successMessage，${warnings.joinToString("；")}".take(320)
}

private fun formatErrorMessage(error: Throwable): String {
    var current: Throwable = error
    while (current.cause != null && current.message.isNullOrBlank()) {
        current = current.cause!!
    }
    return current.message?.lineSequence()?.firstOrNull()?.take(160)
        ?: current.javaClass.simpleName.ifBlank { "未知错误" }
}

/** 列表内的「一键还原全部 SIM」入口，避免固定在底部遮挡内容 */
@Composable
private fun ResetAllButton(
    enabled: Boolean,
    isApplying: Boolean,
    onResetAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, KrfrAdaptive.linkSoft()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .clip(KrfrCardShape)
                .clickablePressScale(onClick = onResetAll)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(KrfrPill)
                    .background(KrfrAdaptive.linkSoft()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↺",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = KrfrAdaptive.link()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "一键还原全部 SIM",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "主卡 + 副卡一起恢复默认",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (isApplying) "还原中…" else "还原",
                modifier = Modifier
                    .clip(KrfrPill)
                    .background(if (enabled && !isApplying) KrfrAdaptive.linkSoft() else KrfrAdaptive.wash())
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled && !isApplying) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    modifier: Modifier = Modifier,
    effectiveSimCard: SimCardInfo?,
    selectedCountryCode: String,
    isCustomCountryCode: Boolean,
    customCountryCode: String,
    selectedCarrier: PresetCarriers.CarrierPreset?,
    customCarrierName: String,
    isApplyingConfig: Boolean,
    activeAction: String?,
    onReset: (SimCardInfo) -> Unit,
    onSave: (SimCardInfo) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { effectiveSimCard?.let(onReset) },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            enabled = !isApplyingConfig && effectiveSimCard != null,
            shape = KrfrPill,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                when {
                    isApplyingConfig && activeAction == "reset" -> "还原中…"
                    isApplyingConfig -> "请稍候…"
                    else -> "还原配置"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Button(
            onClick = { effectiveSimCard?.let(onSave) },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            enabled = !isApplyingConfig && effectiveSimCard != null && (
                (isCustomCountryCode && customCountryCode.length == 2) ||
                    (!isCustomCountryCode && selectedCountryCode.isNotEmpty()) ||
                    (selectedCarrier != null &&
                        (selectedCarrier.name != "自定义" || customCarrierName.isNotEmpty()))
                ),
            shape = KrfrPill,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KrfrAdaptive.link(),
                contentColor = KrfrAdaptive.onLink(),
                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Text(
                when {
                    isApplyingConfig && activeAction == "save" -> "保存中…"
                    isApplyingConfig -> "请稍候…"
                    else -> "保存配置"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
