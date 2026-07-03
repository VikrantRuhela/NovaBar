package com.novabar.app

import android.app.Application
import com.novabar.app.data.SettingsRepository
import com.novabar.app.utils.AppIconManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.novabar.app.utils.TorchManager.init(this)
        com.novabar.app.utils.HotspotManager.init(this)
        com.novabar.app.utils.DeveloperLogger.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(this@NovaApplication)
                val settings = repository.settingsFlow.first()
                AppIconManager.switchIcon(this@NovaApplication, settings.appIconMode)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
