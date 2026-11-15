package com.github.krfr.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.github.krfr.ui.theme.UiStyle
import com.github.krfr.ui.theme.isDarkSurface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.R
import com.github.krfr.data.SettingsStore
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.LogTexts
import com.github.krfr.manager.SubscriptionHiddenApi
import com.github.krfr.model.SimCardInfo
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.SectionLabel
import com.github.krfr.ui.components.SoftStatusBanner
import com.github.krfr.ui.components.StatusPill
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.KrfrPill
import com.github.krfr.ui.theme.OceanLink
import com.github.krfr.ui.theme.RoseLink
import com.github.krfr.ui.theme.SunsetLink
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.ThemeStyle
import com.github.krfr.ui.theme.clickableNoIndication
import com.github.krfr.ui.theme.clickablePressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.github.krfr.ui.theme.uiBottomInset
import com.github.krfr.ui.glass.GlassSurface

/** 个性化页：软件外观（语言/配色/明暗/背景图）+ SIM 卡个性化（显示名） */
@Composable
fun PersonalizeScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    backgroundPath: String?,
    onBackgroundPathChange: (String?) -> Unit,
    appLang: String?,
    onLangChange: (String) -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val coroutineScope = rememberCoroutineScope()
    var simCards by remember { mutableStateOf<List<SimCardInfo>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val displayNameDrafts = remember { mutableStateMapOf<Int, String>() }
    var loadingNames by remember { mutableStateOf(true) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val target = File(appContext.filesDir, "custom_bg.png")
                withContext(Dispatchers.IO) {
                    val input = appContext.contentResolver.openInputStream(uri)
                        ?: error("cannot open image")
                    input.use { ins ->
                        // 先采样再写盘，避免大图占满内存
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeStream(ins, null, bounds)
                        var sample = 1
                        while (bounds.outWidth / sample > 1440) sample *= 2
                        input.close()
                        val input2 = appContext.contentResolver.openInputStream(uri) ?: error("cannot open image")
                        input2.use { ins2 ->
                            val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                            val bitmap = android.graphics.BitmapFactory.decodeStream(ins2, null, decodeOpts)
                                ?: error("decode failed")
                            target.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }
                            bitmap.recycle()
                        }
                    }
                }
                SettingsStore.writeBackgroundPath(appContext, target.absolutePath)
                onBackgroundPathChange(target.absolutePath)
                statusIsError = false
                statusMessage = context.getString(R.string.bg_picked_ok)
            }.onFailure {
                statusIsError = true
                statusMessage = context.getString(R.string.bg_pick_failed)
            }
        }
    }

    LaunchedEffect(appContext, refreshTrigger) {
        loadingNames = true
        runCatching {
            withContext(Dispatchers.IO) {
                CarrierConfigManager.getSimCards(appContext)
            }
        }.onSuccess { cards ->
            simCards = cards
            cards.forEach { card ->
                if (!displayNameDrafts.containsKey(card.subId)) {
                    val name = withContext(Dispatchers.IO) {
                        CarrierConfigManager.getSimDisplayName(card.subId)
                    } ?: card.carrierName
                    displayNameDrafts[card.subId] = name
                }
            }
        }
        loadingNames = false
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        delay(2800)
        if (statusMessage == msg) statusMessage = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = uiBottomInset(96.dp))
        ) {
            PageHeader(
                title = stringResource(R.string.personalize_title),
                subtitle = stringResource(R.string.personalize_subtitle),
                logoText = "PE",
                onRefresh = { refreshTrigger += 1 }
            )

            // ============ 软件外观 ============
            SectionLabel(stringResource(R.string.sec_app_style))

            // 界面风格：液态玻璃（默认，New）/ 原生
            UiStyleCard(selected = uiStyle, onSelect = onUiStyleChange)
            Spacer(modifier = Modifier.height(10.dp))

            // 语言
            GlassSurface(
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.app_language),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                stringResource(R.string.app_language_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(currentLangLabel(appLang), "info")
                    }
                    LanguageRow("en", stringResource(R.string.lang_en), appLang == "en") {
                        onLangChange("en")
                    }
                    LanguageRow("zh-CN", stringResource(R.string.lang_zh_cn), appLang == "zh-CN") {
                        onLangChange("zh-CN")
                    }
                    LanguageRow("zh-TW", stringResource(R.string.lang_zh_tw), appLang == "zh-TW") {
                        onLangChange("zh-TW")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 配色风格
            GlassSurface(
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.color_style),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                stringResource(R.string.color_style_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleCard(
                            color = KrfrAdaptive.link(),
                            label = stringResource(R.string.style_paper_green),
                            selected = themeStyle == ThemeStyle.PAPER_GREEN,
                            onClick = { onThemeStyleChange(ThemeStyle.PAPER_GREEN) },
                            modifier = Modifier.weight(1f)
                        )
                        StyleCard(
                            color = OceanLink,
                            label = stringResource(R.string.style_ocean_blue),
                            selected = themeStyle == ThemeStyle.OCEAN_BLUE,
                            onClick = { onThemeStyleChange(ThemeStyle.OCEAN_BLUE) },
                            modifier = Modifier.weight(1f)
                        )
                        StyleCard(
                            color = SunsetLink,
                            label = stringResource(R.string.style_sunset_orange),
                            selected = themeStyle == ThemeStyle.SUNSET_ORANGE,
                            onClick = { onThemeStyleChange(ThemeStyle.SUNSET_ORANGE) },
                            modifier = Modifier.weight(1f)
                        )
                        StyleCard(
                            color = RoseLink,
                            label = stringResource(R.string.style_rose_pink),
                            selected = themeStyle == ThemeStyle.ROSE_PINK,
                            onClick = { onThemeStyleChange(ThemeStyle.ROSE_PINK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 明暗模式
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = KrfrCardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    ModeRow(
                        title = stringResource(R.string.theme_system),
                        description = stringResource(R.string.theme_system_desc),
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                    ModeRow(
                        title = stringResource(R.string.theme_light),
                        description = stringResource(R.string.theme_light_desc),
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    ModeRow(
                        title = stringResource(R.string.theme_dark),
                        description = stringResource(R.string.theme_dark_desc),
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 背景图
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = KrfrCardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                real = false
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.bg_mode),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                if (backgroundPath != null) stringResource(R.string.bg_custom)
                                else stringResource(R.string.bg_none),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            shape = KrfrPill,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.pick_image))
                        }
                        if (backgroundPath != null) {
                            TextButton(onClick = {
                                SettingsStore.writeBackgroundPath(appContext, null)
                                onBackgroundPathChange(null)
                                statusIsError = false
                                statusMessage = context.getString(R.string.bg_cleared)
                            }) {
                                Text(stringResource(R.string.clear_image))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            // ============ SIM 卡个性化 ============
            SectionLabel(stringResource(R.string.sec_sim_personalize))
            Text(
                stringResource(R.string.sim_settings_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (loadingNames && simCards.isEmpty()) {
                GlassSurface(
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
            } else if (simCards.isEmpty()) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = KrfrCardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        stringResource(R.string.no_sim_personalize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                simCards.sortedBy { it.slot }.forEach { card ->
                    SimNameCard(
                        card = card,
                        draft = displayNameDrafts[card.subId].orEmpty(),
                        onDraftChange = { displayNameDrafts[card.subId] = it },
                        onSave = {
                            coroutineScope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        SubscriptionHiddenApi.setSimDisplayName(
                                            card.subId,
                                            displayNameDrafts[card.subId].orEmpty().trim()
                                        )
                                    }
                                }.onSuccess {
                                    // 回读实际生效的名称并刷新草稿
                                    val current = withContext(Dispatchers.IO) {
                                        CarrierConfigManager.getSimDisplayName(card.subId) ?: card.carrierName
                                    }
                                    displayNameDrafts[card.subId] = current
                                    statusIsError = false
                                    statusMessage = context.getString(R.string.name_saved)
                                }.onFailure {
                                    statusIsError = true
                                    statusMessage = context.getString(
                                        R.string.name_save_failed,
                                        it.message ?: context.getString(R.string.unknown_error)
                                    )
                                    LogStore.e(LogTexts.t("sim_name_save_failed", card.subId, it.message.orEmpty()))
                                }
                            }
                        },
                        onReset = {
                            coroutineScope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        SubscriptionHiddenApi.setSimDisplayName(card.subId, null)
                                    }
                                    val restored = withContext(Dispatchers.IO) {
                                        CarrierConfigManager.getSimDisplayName(card.subId) ?: card.carrierName
                                    }
                                    displayNameDrafts[card.subId] = restored
                                }.onSuccess {
                                    statusIsError = false
                                    statusMessage = context.getString(R.string.name_reset)
                                }.onFailure {
                                    statusIsError = true
                                    statusMessage = context.getString(
                                        R.string.name_reset_failed,
                                        it.message ?: context.getString(R.string.unknown_error)
                                    )
                                    LogStore.e(LogTexts.t("sim_name_reset_failed", card.subId, it.message.orEmpty()))
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        SoftStatusBanner(
            message = statusMessage,
            isError = statusIsError,
            onDismiss = { statusMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = uiBottomInset(96.dp, 24.dp))
        )
    }
}

@Composable
private fun currentLangLabel(appLang: String?): String {
    return when (appLang) {
        "zh-CN" -> "简体中文"
        "zh-TW" -> "繁體中文"
        else -> "English"
    }
}

@Composable
private fun LanguageRow(
    value: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StyleCard(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) KrfrAdaptive.linkSoft() else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(220),
        label = "style-card-bg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220),
        label = "style-card-label"
    )
    val swatchScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "style-card-swatch"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickablePressScale(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer { scaleX = swatchScale; scaleY = swatchScale }
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ModeRow(
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
        val titleColor by animateColorAsState(
            targetValue = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(220),
            label = "mode-title"
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimNameCard(
    card: SimCardInfo,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        real = false
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SIM${card.slot}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KrfrAdaptive.link()
                    )
                )
                Text(
                    card.carrierName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                StatusPill("subId ${card.subId}", "plain")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.sim_display_name),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.sim_display_name_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = KrfrPill
                ) {
                    Text(stringResource(R.string.reset))
                }
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = KrfrPill
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun UiStyleCard(selected: UiStyle, onSelect: (UiStyle) -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                stringResource(R.string.ui_style_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.ui_style_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UiStyleOption(
                    style = UiStyle.LIQUID_GLASS,
                    label = stringResource(R.string.ui_style_liquid),
                    selected = selected == UiStyle.LIQUID_GLASS,
                    onClick = { onSelect(UiStyle.LIQUID_GLASS) },
                    modifier = Modifier.weight(1f)
                )
                UiStyleOption(
                    style = UiStyle.NATIVE,
                    label = stringResource(R.string.ui_style_native),
                    selected = selected == UiStyle.NATIVE,
                    onClick = { onSelect(UiStyle.NATIVE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UiStyleOption(
    style: UiStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    val primary = KrfrAdaptive.link()
    val radius = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(220),
        label = "ui-style-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = tween(220),
        label = "ui-style-border-width"
    )
    val optionBg by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = if (dark) 0.16f else 0.09f) else Color.Transparent,
        animationSpec = tween(220),
        label = "ui-style-bg"
    )
    Column(
        modifier = modifier
            .clip(radius)
            .background(optionBg)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = radius
            )
            .clickablePressScale(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 预览图单独裁剪，徽标放在裁剪层之外，才不会被圆角切掉
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(11.dp))
            ) {
                StylePreview(style = style, dark = dark)
            }
            if (style == UiStyle.LIQUID_GLASS) {
                NewBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-8).dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/** 两种风格的小样：玻璃版画悬浮胶囊，原生版画实心导航栏 */
@Composable
private fun StylePreview(style: UiStyle, dark: Boolean) {
    val cardRadius = RoundedCornerShape(6.dp)
    if (style == UiStyle.LIQUID_GLASS) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (dark) Color(0xFF102A4A) else Color(0xFFA9D4FF),
                            if (dark) Color(0xFF173A63) else Color(0xFF2F7BE8)
                        )
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(2) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                            .clip(cardRadius)
                            .background(Color.White.copy(alpha = if (dark) 0.14f else 0.38f))
                            .border(0.7.dp, Color.White.copy(alpha = if (dark) 0.20f else 0.55f), cardRadius)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 6.dp)
                    .fillMaxWidth()
                    .height(17.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (dark) 0.16f else 0.30f))
                    .border(0.8.dp, Color.White.copy(alpha = if (dark) 0.45f else 0.80f), CircleShape)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) {
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                    )
                }
            }
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(2) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                            .clip(cardRadius)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, cardRadius)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .size(if (index == 0) 9.dp else 4.dp)
                            .clip(if (index == 0) CircleShape else CircleShape)
                            .background(
                                if (index == 0) KrfrAdaptive.link().copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF4FA8FF), Color(0xFF0A7AFF)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 1.5.dp)
    ) {
        Text(
            stringResource(R.string.badge_new),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            maxLines = 1
        )
    }
}
