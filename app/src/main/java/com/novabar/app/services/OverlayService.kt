package com.novabar.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

import com.novabar.app.data.BatteryReceiver
import com.novabar.app.data.SettingsRepository
import com.novabar.app.data.NovaSettings
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.domain.DiagnosticsManager
import com.novabar.app.ui.components.NovaBarUi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class OverlayService : Service() {

    private val tag = "OverlayService"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private val batteryReceiver = BatteryReceiver()
    private var settingsJob: Job? = null
    private lateinit var overlayHost: OverlayHost

    companion object {
        private const val CHANNEL_ID = "nova_bar_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        overlayHost = OverlayHost(this)
        batteryReceiver.register(this)
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DiagnosticsManager.overlayEngine.value = "APPLICATION"
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                OverlayStateManager.settingsFlow.value = settings
                if (settings.isEnabled && settings.overlayEngine == OverlayEngine.APPLICATION) {
                    showOverlay(settings)
                } else {
                    removeOverlay()
                    if (!settings.isEnabled) {
                        stopSelf()
                    }
                }
            }
        }

        // Listen for reset/recreate request from Diagnostics
        scope.launch {
            DiagnosticsManager.resetTrigger.collectLatest { trigger ->
                if (trigger > 0) {
                    val settings = settingsRepository.settingsFlow.first()
                    if (settings.isEnabled && settings.overlayEngine == OverlayEngine.APPLICATION) {
                        removeOverlay()
                        delay(200)
                        showOverlay(settings)
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        settingsJob?.cancel()
        removeOverlay()
        batteryReceiver.unregister(this)
        scope.cancel()
    }

    private fun startForegroundService() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("Nova Bar")
            .setContentText("Nova Bar is running")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start foreground service", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova Bar Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showOverlay(settings: NovaSettings) {
        overlayHost.show(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, settings)
    }

    private fun removeOverlay() {
        overlayHost.remove()
    }
}
