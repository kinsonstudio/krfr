#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""修复编译问题：协程/回调内 stringResource → context.getString；补 import。"""
import os

UI = "/Users/cai/Doubao/chats/2026-10-15/new-chat-1/krfr/app/src/main/java/com/github/krfr"

def patch(rel, pairs):
    p = os.path.join(UI, rel)
    src = open(p, encoding="utf-8").read()
    miss = []
    for old, new in pairs:
        if old in src:
            src = src.replace(old, new)
        else:
            miss.append(old[:70])
    open(p, "w", encoding="utf-8").write(src)
    if miss:
        print(rel, "MISS:", miss)

# ---------- SettingsScreen：协程/回调内改用 context.getString ----------
patch("ui/screens/SettingsScreen.kt", [
    ('statusMessage = stringResource(R.string.backup_exported)', 'statusMessage = context.getString(R.string.backup_exported)'),
    ('statusMessage = stringResource(R.string.export_failed, it.message ?: stringResource(R.string.unknown_error))',
     'statusMessage = context.getString(R.string.export_failed, it.message ?: context.getString(R.string.unknown_error))'),
    ('statusMessage = stringResource(R.string.restore_done, count)', 'statusMessage = context.getString(R.string.restore_done, count)'),
    ('"恢复已被强制终止"', 'context.getString(R.string.restore_forced_stopped)'),
    ('stringResource(R.string.restore_failed, it.message ?: stringResource(R.string.invalid_file)).take(200)',
     'context.getString(R.string.restore_failed, it.message ?: context.getString(R.string.invalid_file)).take(200)'),
    ('statusMessage = if (cancelled) {\n                                    stringResource(R.string.forced_stopped)\n                                } else {\n                                    stringResource(R.string.no_running_task)\n                                }',
     'statusMessage = if (cancelled) {\n                                    context.getString(R.string.forced_stopped)\n                                } else {\n                                    context.getString(R.string.no_running_task)\n                                }'),
    ('updateMessage = stringResource(R.string.checking_update)', 'updateMessage = context.getString(R.string.checking_update)'),
    ('updateMessage = stringResource(R.string.already_latest)', 'updateMessage = context.getString(R.string.already_latest)'),
    ('updateMessage = stringResource(R.string.cannot_show_update)', 'updateMessage = context.getString(R.string.cannot_show_update)'),
    ('updateMessage = stringResource(R.string.update_check_failed, error.message ?: stringResource(R.string.network_error)).take(120)',
     'updateMessage = context.getString(R.string.update_check_failed, error.message ?: context.getString(R.string.network_error)).take(120)'),
])

# ---------- QuickPresetScreen ----------
patch("ui/screens/QuickPresetScreen.kt", [
    ('stringResource(R.string.preset_applied, simCard.slot, preset.name)',
     'context.getString(R.string.preset_applied, simCard.slot, preset.name)'),
    ('stringResource(R.string.preset_applied_warn, simCard.slot, preset.name, result.warnings.joinToString("；")).take(320)',
     'context.getString(R.string.preset_applied_warn, simCard.slot, preset.name, result.warnings.joinToString("；")).take(320)'),
    ('"操作已被强制终止"', 'context.getString(R.string.operation_cancelled)'),
    ('stringResource(R.string.apply_failed, e.message?.lineSequence()?.firstOrNull()?.take(120) ?: stringResource(R.string.unknown_error))',
     'context.getString(R.string.apply_failed, e.message?.lineSequence()?.firstOrNull()?.take(120) ?: context.getString(R.string.unknown_error))'),
])

# ---------- ConfigScreen ----------
patch("ui/screens/ConfigScreen.kt", [
    ('statusMessage = stringResource(R.string.reset_all_done)', 'statusMessage = context.getString(R.string.reset_all_done)'),
    ('stringResource(R.string.reset_all_failed, formatErrorMessage(context, e))',
     'context.getString(R.string.reset_all_failed, formatErrorMessage(context, e))'),
    ('stringResource(R.string.operation_cancelled)', 'context.getString(R.string.operation_cancelled)'),
    ('stringResource(R.string.save_failed, formatErrorMessage(context, e))',
     'context.getString(R.string.save_failed, formatErrorMessage(context, e))'),
    ('stringResource(R.string.reset_failed, formatErrorMessage(context, e))',
     'context.getString(R.string.reset_failed, formatErrorMessage(context, e))'),
    ('buildOperationMessage(stringResource(R.string.config_reset), result.warnings)',
     'buildOperationMessage(context.getString(R.string.config_reset), result.warnings)'),
    ('buildOperationMessage(stringResource(R.string.config_saved), result.warnings)',
     'buildOperationMessage(context.getString(R.string.config_saved), result.warnings)'),
])

# ---------- 补 import ----------
def ensure_import(rel, imp, after):
    p = os.path.join(UI, rel)
    src = open(p, encoding="utf-8").read()
    if imp in src:
        return
    if after in src:
        src = src.replace(after, after + "\n" + imp, 1)
    else:
        lines = src.split("\n")
        idx = next((i for i, l in enumerate(lines) if l.startswith("package ")), 0)
        lines.insert(idx + 1, imp)
        src = "\n".join(lines)
    open(p, "w", encoding="utf-8").write(src)

IMP = "import androidx.compose.ui.res.stringResource"
IMPR = "import com.github.krfr.R"
for rel in ["ui/screens/AboutScreen.kt", "ui/screens/ConfigScreen.kt", "ui/screens/QuickPresetScreen.kt",
            "ui/screens/SettingsScreen.kt", "ui/screens/ShizukuNotReadyScreen.kt", "ui/screens/SimStatusScreen.kt",
            "ui/components/KrfrWidgets.kt"]:
    ensure_import(rel, IMP, "import androidx.compose.ui.platform.LocalContext")
    ensure_import(rel, IMPR, "import com.github.krfr.ui.theme.KrfrAdaptive")

ensure_import("ui/MainScaffold.kt", IMPR, "import com.github.krfr.ui.screens.ConfigScreen")

# ConfigScreen 需要 Context（formatConfigSummary/formatErrorMessage 签名用了 Context 类型）
p = os.path.join(UI, "ui/screens/ConfigScreen.kt")
src = open(p, encoding="utf-8").read()
if "import android.content.Context" not in src:
    src = src.replace("import androidx.compose.animation.core.LinearEasing",
                      "import android.content.Context\nimport androidx.compose.animation.core.LinearEasing", 1)
open(p, "w", encoding="utf-8").write(src)

# SimStatusReader 需要 Context
p = os.path.join(UI, "manager/SimStatusReader.kt")
src = open(p, encoding="utf-8").read()
if "import android.content.Context" not in src:
    src = src.replace("import android.os.IBinder", "import android.content.Context\nimport android.os.IBinder", 1)
open(p, "w", encoding="utf-8").write(src)

print("patch done")
