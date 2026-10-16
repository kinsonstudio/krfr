package com.github.krfr.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.krfr.manager.ShizukuHelper
import com.github.krfr.ui.theme.KrfrAdaptive
import com.github.krfr.R
import com.github.krfr.ui.theme.KrfrPill

private const val SHIZUKU_RELEASES = "https://github.com/RikkaApps/Shizuku/releases"

enum class ShizukuBlockReason {
    NOT_INSTALLED,
    SERVICE_NOT_RUNNING,
    PERMISSION_NOT_GRANTED
}

@Composable
fun ShizukuNotReadyScreen(
    reason: ShizukuBlockReason,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val title = when (reason) {
        ShizukuBlockReason.NOT_INSTALLED -> stringResource(R.string.shizuku_not_installed)
        ShizukuBlockReason.SERVICE_NOT_RUNNING -> stringResource(R.string.shizuku_not_running)
        ShizukuBlockReason.PERMISSION_NOT_GRANTED -> stringResource(R.string.shizuku_need_permission)
    }
    val detail = when (reason) {
        ShizukuBlockReason.NOT_INSTALLED -> stringResource(R.string.shizuku_dl_first)
        ShizukuBlockReason.SERVICE_NOT_RUNNING -> stringResource(R.string.shizuku_start_first)
        ShizukuBlockReason.PERMISSION_NOT_GRANTED -> stringResource(R.string.shizuku_allow)
    }
    val actionLabel = when (reason) {
        ShizukuBlockReason.NOT_INSTALLED -> stringResource(R.string.go_download)
        ShizukuBlockReason.SERVICE_NOT_RUNNING -> stringResource(R.string.open_shizuku)
        ShizukuBlockReason.PERMISSION_NOT_GRANTED -> stringResource(R.string.request_permission)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    when (reason) {
                        ShizukuBlockReason.NOT_INSTALLED -> {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_RELEASES))
                            )
                        }
                        ShizukuBlockReason.SERVICE_NOT_RUNNING -> {
                            if (!ShizukuHelper.openApp(context)) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_RELEASES))
                                )
                            }
                        }
                        ShizukuBlockReason.PERMISSION_NOT_GRANTED -> onRequestPermission()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = KrfrPill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KrfrAdaptive.link(),
                    contentColor = KrfrAdaptive.onLink()
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun resolveShizukuBlockReason(context: Context): ShizukuBlockReason {
    return when {
        ShizukuHelper.isBinderAvailable() -> ShizukuBlockReason.PERMISSION_NOT_GRANTED
        ShizukuHelper.isInstalled(context) -> ShizukuBlockReason.SERVICE_NOT_RUNNING
        else -> ShizukuBlockReason.NOT_INSTALLED
    }
}
