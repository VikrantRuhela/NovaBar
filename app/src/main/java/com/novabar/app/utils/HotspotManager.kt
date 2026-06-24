package com.novabar.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.novabar.app.domain.HotspotState
import com.novabar.app.domain.OverlayStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object HotspotManager {
    private const val TAG = "HotspotManager"
    
    private val _isHotspotActive = MutableStateFlow(false)
    val isHotspotActive: StateFlow<Boolean> = _isHotspotActive
    
    private val _isSupported = MutableStateFlow(true)
    val isSupported: StateFlow<Boolean> = _isSupported
    
    private var isRegistered = false
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action = intent.action
            if (action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
                // wifi_state: 13 represents WIFI_AP_STATE_ENABLED, 11 represents WIFI_AP_STATE_DISABLED
                val state = intent.getIntExtra("wifi_state", 11)
                val active = state == 13
                _isHotspotActive.value = active
                
                // Update OverlayStateManager with active state; disableHotspot is not supported via public APIs.
                OverlayStateManager.hotspotState.value = if (active) {
                    HotspotState(isActive = true, isDisableSupported = false)
                } else {
                    null
                }
                Log.d(TAG, "Hotspot state updated: active=$active (wifi_state=$state)")
            }
        }
    }
    
    fun init(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext
        
        val filter = IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")
        try {
            appContext.registerReceiver(receiver, filter)
            isRegistered = true
            Log.d(TAG, "Successfully registered WIFI_AP_STATE_CHANGED broadcast receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register WIFI_AP_STATE_CHANGED broadcast receiver: ${e.message}", e)
        }
    }
    
    fun disableHotspot(context: Context): Boolean {
        // Disabling the hotspot programmatically is not supported via public third-party APIs.
        return false
    }
}
