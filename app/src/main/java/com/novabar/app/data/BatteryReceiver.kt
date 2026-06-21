package com.novabar.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.novabar.app.domain.ChargingState
import com.novabar.app.domain.OverlayStateManager

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) {
                (level * 100f / scale).toInt()
            } else {
                0
            }

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val speed = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> {
                    // Estimate speed based on current if available
                    try {
                        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                        val currentMicroAmps = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0L
                        if (currentMicroAmps > 3000000) "Super Fast" else "Fast"
                    } catch (e: Exception) {
                        "Fast"
                    }
                }
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB (Slow)"
                else -> "Normal"
            }

            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val temperature = tempTenths / 10f // Convert tenths of a degree to Celsius

            val chargingState = ChargingState(
                isCharging = isCharging,
                batteryPercentage = percentage,
                speed = speed,
                temperature = temperature
            )
            OverlayStateManager.updateCharging(chargingState)
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            // Already unregistered or not registered
        }
    }
}
