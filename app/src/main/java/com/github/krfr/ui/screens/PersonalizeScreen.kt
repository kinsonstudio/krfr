package com.github.krfr.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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

/** 个性化页：软件外观（语言/配色/明暗/背景图）+ SIM 卡个性化（显示名） */
@Composable
fun PersonalizeScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    onThemeStyleChange: (ThemeStyle) -> Unit,
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
                .padding(bottom = 96.dp)
        ) {
            PageHeader(
                title = stringResource(R.string.personalize_title),
                subtitle = stringResource(R.string.personalize_subtitle),
                logoText = "PE",
                onRefresh = { refreshTrigger += 1 }
            )

            // ============ 软件外观 ============
            SectionLabel(stringResource(R.string.sec_app_style))

            // 语言
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
            Surface(
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
            } else if (simCards.isEmpty()) {
                Surface(
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
                                    LogStore.e("SIM 显示名保存失败 subId=${card.subId}: ${it.message.orEmpty()}")
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
                                    LogStore.e("SIM 显示名重置失败 subId=${card.subId}: ${it.message.orEmpty()}")
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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 78.dp)
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) KrfrAdaptive.linkSoft() else MaterialTheme.colorScheme.surfaceVariant)
            .clickablePressScale(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.onSurface,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
