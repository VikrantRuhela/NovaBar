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
        "com.novabar.app.SettingsActivityAliasFrost",
        "com.novabar.app.SettingsActivityAliasMaterialYou"
    )

    fun switchIcon(context: Context, mode: String) {
        val targetAlias = when (mode) {
            "Automatic" -> "com.novabar.app.SettingsActivityAliasAutomatic"
            "Midnight" -> "com.novabar.app.SettingsActivityAliasMidnight"
            "Frost" -> "com.novabar.app.SettingsActivityAliasFrost"
            "Material You" -> "com.novabar.app.SettingsActivityAliasMaterialYou"
            else -> "com.novabar.app.SettingsActivityAliasAutomatic"
        }

        val pm = context.packageManager

        for (alias in ALIASES) {
            val componentName = ComponentName(context, alias)
            val newState = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                // To avoid killing the app if we only switch icon state, we use DONT_KILL_APP
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "Alias $alias set to state: $newState")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set alias $alias component enabled state", e)
            }
        }
    }
}
