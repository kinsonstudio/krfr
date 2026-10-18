package com.github.krfr

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.krfr.data.LanguageManager
import com.github.krfr.data.SettingsStore
import com.github.krfr.manager.CarrierConfigManager
import com.github.krfr.manager.LogStore
import com.github.krfr.manager.OperationState
import com.github.krfr.manager.ShizukuHelper
import com.github.krfr.ui.MainScaffold
import com.github.krfr.ui.screens.AboutScreen
import com.github.krfr.ui.screens.LanguageSetupScreen
import com.github.krfr.ui.screens.ShizukuBlockReason
import com.github.krfr.ui.screens.ShizukuNotReadyScreen
import com.github.krfr.ui.screens.resolveShizukuBlockReason
import com.github.krfr.ui.theme.KrfrTheme
import com.github.krfr.ui.theme.ThemeMode
import com.github.krfr.ui.theme.ThemeStyle
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var isShizukuReady by mutableStateOf(false)
    private var shizukuBlockReason by mutableStateOf(ShizukuBlockReason.SERVICE_NOT_RUNNING)
    private var showAbout by mutableStateOf(false)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var themeStyle by mutableStateOf(ThemeStyle.PAPER_GREEN)
    private var backgroundPath by mutableStateOf<String?>(null)
    private var appLang by mutableStateOf<String?>(null)
    private var autoPermissionRequestAttempted = false
    private var syncTriggered = false

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            runOnUiThread {
                updateShizukuStatus()
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.shizuku_permission_denied), Toast.LENGTH_LONG).show()
                } else {
                    triggerSyncWhenShizukuReady()
                }
            }
        }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            updateShizukuStatus()
            requestShizukuPermissionAutomatically()
            triggerSyncWhenShizukuReady()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { updateShizukuStatus() }
    }

    /** 首启语言向导：应用当前语言（未选时为英文默认）渲染 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OperationState.reset()

        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")

        themeMode = SettingsStore.readThemeMode(this)
        themeStyle = SettingsStore.readThemeStyle(this)
        backgroundPath = SettingsStore.readBackgroundPath(this)
        appLang = LanguageManager.readLanguage(this)
        LanguageManager.initApp(this)
        LogStore.restore(applicationContext)

        runCatching {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        }

        updateShizukuStatus()
        requestShizukuPermissionAutomatically()
        triggerSyncWhenShizukuReady()

        setContent {
            KrfrTheme(themeMode = themeMode, themeStyle = themeStyle) {
                if (showAbout) {
                    AboutScreen(onBack = { showAbout = false })
                } else if (!isShizukuReady) {
                    ShizukuNotReadyScreen(
                        reason = shizukuBlockReason,
                        onRequestPermission = { requestShizukuPermission() }
                    )
                } else if (appLang == null) {
                    LanguageSetupScreen(
                        initialLang = LanguageManager.systemDefaultLang(this),
                        onSelected = { lang ->
                            appLang = lang
                            LanguageManager.writeLanguage(this, lang)
                            recreate()
                        }
                    )
                } else {
                    MainScaffold(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            SettingsStore.writeThemeMode(this, mode)
                        },
                        themeStyle = themeStyle,
                        onThemeStyleChange = { style ->
                            themeStyle = style
                            SettingsStore.writeThemeStyle(this, style)
                        },
                        backgroundPath = backgroundPath,
                        onBackgroundPathChange = { path ->
                            backgroundPath = path
                            SettingsStore.writeBackgroundPath(this, path)
                        },
                        appLang = appLang,
                        onLangChange = { lang ->
                            appLang = lang
                            LanguageManager.writeLanguage(this, lang)
                            recreate()
                        },
                        onShowAbout = { showAbout = true }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
        triggerSyncWhenShizukuReady()
    }

    private fun requestShizukuPermission() {
        if (!ShizukuHelper.isBinderAvailable()) {
            Toast.makeText(this, getString(R.string.start_shizuku_first), Toast.LENGTH_LONG).show()
            updateShizukuStatus()
            return
        }
        ShizukuHelper.requestPermissionIfNeeded()
        updateShizukuStatus()
    }

    private fun requestShizukuPermissionAutomatically() {
        if (autoPermissionRequestAttempted || ShizukuHelper.hasPermission()) return
        if (ShizukuHelper.isBinderAvailable()) {
            autoPermissionRequestAttempted = true
            ShizukuHelper.requestPermissionIfNeeded()
        }
    }

    private fun updateShizukuStatus() {
        isShizukuReady = ShizukuHelper.hasPermission()
        if (!isShizukuReady) {
            shizukuBlockReason = resolveShizukuBlockReason(this)
        }
    }

    /** Shizuku 连上后：若本开机尚未清理，则清成无覆盖默认状态。 */
    private fun triggerSyncWhenShizukuReady() {
        if (!ShizukuHelper.hasPermission() || syncTriggered) {
            return
        }
        syncTriggered = true
        Thread({
            runCatching {
                CarrierConfigManager.ensureClearedAfterReboot(applicationContext)
            }.onFailure {
                syncTriggered = false
            }
        }, "KrfrBootClear").start()
    }

    override fun onDestroy() {
        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        }
        super.onDestroy()
    }
}
