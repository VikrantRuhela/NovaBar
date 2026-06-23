package com.novabar.app.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.domain.TorchState
import kotlinx.coroutines.flow.first

object TorchManager {
    private const val TAG = "TorchManager"
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isTorchCallbackRegistered = false
    private val handler = Handler(Looper.getMainLooper())

    private var maxStrengthLevel = 1
    private var defaultStrengthLevel = 1
    private var isStrengthSupported = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(camId: String, enabled: Boolean) {
            if (camId == cameraId) {
                Log.d(TAG, "onTorchModeChanged: $enabled")
                if (enabled) {
                    val pct = if (isStrengthSupported) {
                        val currentStrength = getCurrentStrengthLevel()
                        ((currentStrength.toFloat() / maxStrengthLevel.toFloat()) * 100).toInt().coerceIn(1, 100)
                    } else {
                        100
                    }

                    OverlayStateManager.torchState.value = TorchState(
                        isActive = true,
                        brightnessPercentage = pct,
                        isStrengthSupported = isStrengthSupported
                    )
                } else {
                    OverlayStateManager.torchState.value = null
                    OverlayStateManager.collapse()
                }
            }
        }

        // Available on Android 13+ (API 33+) or vendor-supported implementations
        override fun onTorchStrengthLevelChanged(camId: String, newStrengthLevel: Int) {
            if (camId == cameraId) {
                Log.d(TAG, "onTorchStrengthLevelChanged: $newStrengthLevel")
                val current = OverlayStateManager.torchState.value ?: TorchState()
                if (current.isActive) {
                    val pct = ((newStrengthLevel.toFloat() / maxStrengthLevel.toFloat()) * 100).toInt().coerceIn(1, 100)
                    OverlayStateManager.torchState.value = current.copy(
                        brightnessPercentage = pct
                    )
                }
            }
        }
    }

    fun init(context: Context) {
        if (cameraManager != null) return
        val ctx = context.applicationContext
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cm == null) {
            Log.e(TAG, "CameraManager not available")
            return
        }
        cameraManager = cm

        try {
            // Find first back-facing camera with flash support
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    
                    // Probe strength level support safely via reflection to support older API versions
                    try {
                        val maxField = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_MAXIMUM_LEVEL")
                        val defaultField = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_DEFAULT_LEVEL")
                        
                        val maxKey = maxField.get(null) as? CameraCharacteristics.Key<Int>
                        val defaultKey = defaultField.get(null) as? CameraCharacteristics.Key<Int>
                        
                        if (maxKey != null) {
                            maxStrengthLevel = chars.get(maxKey) ?: 1
                        }
                        if (defaultKey != null) {
                            defaultStrengthLevel = chars.get(defaultKey) ?: 1
                        }
                        
                        isStrengthSupported = maxStrengthLevel > 1
                        Log.d(TAG, "Reflective check: maxStrengthLevel=$maxStrengthLevel, default=$defaultStrengthLevel, supported=$isStrengthSupported")
                    } catch (e: Exception) {
                        Log.d(TAG, "Reflection for torch strength levels not supported or failed: ${e.message}")
                        maxStrengthLevel = 1
                        defaultStrengthLevel = 1
                        isStrengthSupported = false
                    }
                    break
                }
            }

            if (cameraId != null) {
                cm.registerTorchCallback(torchCallback, handler)
                isTorchCallbackRegistered = true
                Log.d(TAG, "TorchCallback registered on cameraId: $cameraId")
            } else {
                Log.e(TAG, "No back-facing camera with flash found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TorchManager", e)
        }
    }

    fun setTorchEnabled(enabled: Boolean) {
        val cm = cameraManager ?: return
        val id = cameraId ?: return
        try {
            if (enabled) {
                if (isStrengthSupported) {
                    try {
                        val method = CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
                        val level = defaultStrengthLevel.coerceIn(1, maxStrengthLevel)
                        method.invoke(cm, id, level)
                        Log.d(TAG, "Turned on torch with default strength level: $level")
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to call turnOnTorchWithStrengthLevel via reflection, falling back", e)
                    }
                }
                cm.setTorchMode(id, true)
            } else {
                cm.setTorchMode(id, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch mode to $enabled", e)
        }
    }

    fun setTorchBrightnessPercentage(percentage: Int) {
        val cm = cameraManager ?: return
        val id = cameraId ?: return
        if (!isStrengthSupported) return
        
        try {
            val level = ((percentage.toFloat() / 100f) * maxStrengthLevel.toFloat()).toInt().coerceIn(1, maxStrengthLevel)
            
            // Reflectively invoke turnOnTorchWithStrengthLevel
            val method = CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
            method.invoke(cm, id, level)
            Log.d(TAG, "Reflective turnOnTorchWithStrengthLevel: $percentage% -> level $level/$maxStrengthLevel")
            
            // Immediately update state so UI responds instantly even if callback takes a moment
            val current = OverlayStateManager.torchState.value ?: TorchState()
            if (current.isActive) {
                OverlayStateManager.torchState.value = current.copy(
                    brightnessPercentage = percentage
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch brightness to $percentage% via reflection", e)
        }
    }

    private fun getCurrentStrengthLevel(): Int {
        val cm = cameraManager ?: return 1
        val id = cameraId ?: return 1
        return try {
            val method = CameraManager::class.java.getMethod("getTorchStrengthLevel", String::class.java)
            method.invoke(cm, id) as Int
        } catch (e: Exception) {
            1
        }
    }

    fun destroy() {
        if (isTorchCallbackRegistered) {
            cameraManager?.unregisterTorchCallback(torchCallback)
            isTorchCallbackRegistered = false
        }
        cameraManager = null
        cameraId = null
    }
}
