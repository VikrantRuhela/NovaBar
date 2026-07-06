package com.novabar.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    private const val TAG = "AppIconManager"

    private val ALIASES = listOf(
        "com.novabar.app.SettingsActivityAliasAutomatic",
        "com.novabar.app.SettingsActivityAliasMidnight",
        "com.novabar.app.SettingsActivityAliasFrost"
    )

    fun switchIcon(context: Context, mode: String) {
        val targetAlias = when (mode) {
            "Automatic" -> "com.novabar.app.SettingsActivityAliasAutomatic"
            "Midnight" -> "com.novabar.app.SettingsActivityAliasMidnight"
            "Frost" -> "com.novabar.app.SettingsActivityAliasFrost"
            else -> "com.novabar.app.SettingsActivityAliasAutomatic"
        }

        val pm = context.packageManager

        // 1. Enable the new target alias first to ensure there is always at least one enabled launcher entry point.
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, targetAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Enabled target alias: $targetAlias first")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable target alias $targetAlias", e)
        }

        // 2. Disable all other aliases afterward.
        for (alias in ALIASES) {
            if (alias != targetAlias) {
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(context, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Disabled alias: $alias")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disable alias $alias", e)
                }
            }
        }
    }
}
