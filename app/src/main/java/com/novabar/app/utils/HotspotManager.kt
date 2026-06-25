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
            val appContext = context?.applicationContext ?: return
            val action = intent.action
            
            DeveloperLogger.log(appContext, "HOTSPOT", "Callback received: action=$action")
            
            if (action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
                // wifi_state: 13 represents WIFI_AP_STATE_ENABLED, 11 represents WIFI_AP_STATE_DISABLED
                val state = intent.getIntExtra("wifi_state", 11)
                val prevState = intent.getIntExtra("previous_wifi_state", -1)
                DeveloperLogger.log(appContext, "HOTSPOT", "Raw hotspot state: wifi_state=$state, previous_wifi_state=$prevState")
                
                val active = state == 13
                val oldActive = _isHotspotActive.value
                
                if (oldActive != active) {
                    DeveloperLogger.log(appContext, "HOTSPOT", "State transition: active changed from $oldActive to $active")
                }
                
                _isHotspotActive.value = active
                
                // Update OverlayStateManager with active state; disableHotspot is not supported via public APIs.
                val finalState = if (active) {
                    HotspotState(isActive = true, isDisableSupported = false)
                } else {
                    null
                }
                OverlayStateManager.hotspotState.value = finalState
                
                DeveloperLogger.log(appContext, "HOTSPOT", "Final activity registration decision: registered=${finalState != null} (state: $finalState)")
                Log.i(TAG, "Hotspot state updated: active=$active (wifi_state=$state)")
            }
        }
    }
    
    fun init(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext
        
        val filter = IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
            isRegistered = true
            Log.i(TAG, "Successfully registered WIFI_AP_STATE_CHANGED broadcast receiver")
            DeveloperLogger.log(appContext, "HOTSPOT", "Receiver registration succeeded: action=android.net.wifi.WIFI_AP_STATE_CHANGED")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register WIFI_AP_STATE_CHANGED broadcast receiver: ${e.message}", e)
            DeveloperLogger.log(appContext, "HOTSPOT", "Receiver registration failed: exception=${e.message}")
        }
    }
    
    fun disableHotspot(context: Context): Boolean {
        // Disabling the hotspot programmatically is not supported via public third-party APIs.
        val appContext = context.applicationContext
        DeveloperLogger.log(appContext, "HOTSPOT", "disableHotspot() invoked (not supported via public APIs)")
        return false
    }
}
