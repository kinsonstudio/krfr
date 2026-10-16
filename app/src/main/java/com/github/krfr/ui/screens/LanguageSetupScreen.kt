package com.github.krfr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.ui.theme.KrfrCardShape
import com.github.krfr.ui.theme.KrfrPill

/**
 * 首次启动语言选择页：请求 Shizuku 权限后展示。
 * 语言名以各语言自写展示，无需翻译。
 */
@Composable
fun LanguageSetupScreen(
    initialLang: String = "en",
    onSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.2f))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(KrfrAdaptive.linkSoft()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "K",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KrfrAdaptive.link()
                    )
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "krfr",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "SIM Card Country Code · Carrier Config",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))

            LanguageOption(
                title = "English",
                subtitle = "English",
                selected = initialLang == "en",
                onClick = { onSelected("en") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LanguageOption(
                title = "简体中文",
                subtitle = "简体中文",
                selected = initialLang == "zh-CN",
                onClick = { onSelected("zh-CN") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LanguageOption(
                title = "繁體中文",
                subtitle = "繁體中文",
                selected = initialLang == "zh-TW",
                onClick = { onSelected("zh-TW") }
            )

            Spacer(modifier = Modifier.weight(1.2f))
            Text(
                "Select language · 選擇語言",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KrfrCardShape,
        color = if (selected) KrfrAdaptive.linkSoft() else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) KrfrAdaptive.link() else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .clip(KrfrCardShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(KrfrPill)
                    .background(KrfrAdaptive.linkSoft()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "→",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KrfrAdaptive.link()
                    )
                )
            }
        }
    }
}
