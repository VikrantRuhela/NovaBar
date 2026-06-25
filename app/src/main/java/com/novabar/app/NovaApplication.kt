package com.novabar.app

import android.app.Application

class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.novabar.app.utils.TorchManager.init(this)
        com.novabar.app.utils.HotspotManager.init(this)
        com.novabar.app.utils.DeveloperLogger.init(this)
    }
}
