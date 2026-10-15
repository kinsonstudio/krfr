package com.github.krfr.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.ui.components.DetailGrid
import com.github.krfr.ui.components.PageHeader
import com.github.krfr.ui.components.StatusPill
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.SimStatusReader
import com.github.krfr.manager.toSimCardsUserMessage
import com.github.krfr.model.SimStatusInfo
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SimStatusScreen() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    var statuses by remember { mutableStateOf<List<SimStatusInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    fun load() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val cards = CarrierConfigManager.getSimCards(appContext)
                    SimStatusReader.readAll(appContext, cards)
                }
            }.onSuccess {
                statuses = it
                if (it.isEmpty()) {
                    errorMessage = "未检测到可用 SIM 卡，请确认 Shizuku 已授权"
                }
            }.onFailure {
                errorMessage = it.toSimCardsUserMessage()
            }
            isLoading = false
        }
    }

    LaunchedEffect(refreshTrigger) {
        load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        PageHeader(
            title = "SIM 状态",
            subtitle = if (isLoading) "正在读取…" else "每张卡的实时网络信息",
            logoText = "ST",
            onRefresh = { refreshTrigger += 1 }
        )

        when {
            errorMessage != null && statuses.isEmpty() -> {
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
            statuses.isEmpty() && !isLoading -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = KrfrCardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        "未检测到可用 SIM 卡",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                statuses.sortedBy { it.slot }.forEach { status ->
                    SimStatusCard(status)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    "ICCID 显示已脱敏，仅用于核对卡号后几位",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SimStatusCard(status: SimStatusInfo) {
    val signalColor = when {
        status.signalLevel >= 3 -> KrfrAdaptive.link()
        status.signalLevel == 2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val signalValue = if (status.signalDbm != 0) {
        "${SimStatusReader.signalText(status.signalLevel)} · ${status.signalDbm} dBm"
    } else {
        SimStatusReader.signalText(status.signalLevel)
    }
    val roamingText = if (status.dataRoaming) "漫游中" else "未漫游"
    val roamingColor = if (status.dataRoaming) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SIM${status.slot}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = KrfrAdaptive.link()
                        )
                        Text(
                            status.carrierName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                    Text(
                        "subId ${status.subId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = if (status.hasOverride) "已覆盖" else "默认",
                    tone = if (status.hasOverride) "info" else "plain"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailGrid(
                items = listOf(
                    "运营商" to (status.carrierName to MaterialTheme.colorScheme.onSurface),
                    "国家/地区" to (status.countryIso to MaterialTheme.colorScheme.onSurface),
                    "MCC / MNC" to ("${status.mcc} / ${status.mnc}" to MaterialTheme.colorScheme.onSurface),
                    "网络类型" to (status.networkType to KrfrAdaptive.link()),
                    "信号强度" to (signalValue to signalColor),
                    "数据漫游" to (roamingText to roamingColor),
                    "SIM 卡状态" to (status.simState to MaterialTheme.colorScheme.onSurface),
                    "ICCID" to (status.iccidMasked to KrfrAdaptive.codeAccent())
                )
            )
        }
    }
}
