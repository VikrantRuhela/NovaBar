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
    private var appContext: Context? = null

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
        appContext = ctx
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
                    
                    // VIBRANT DIAGNOSTIC LOGGING FOR SAMSUNG GALAXY A21s
                    val debugTag = "NovaBarTorchDebug"
                    Log.i(debugTag, "=== TORCH BRIGHTNESS DIAGNOSTICS START ===")
                    Log.i(debugTag, "Device Manufacturer: ${android.os.Build.MANUFACTURER}")
                    Log.i(debugTag, "Device Model: ${android.os.Build.MODEL}")
                    Log.i(debugTag, "Device Brand: ${android.os.Build.BRAND}")
                    Log.i(debugTag, "API Level: ${android.os.Build.VERSION.SDK_INT}")
                    Log.i(debugTag, "Camera ID: $id")

                    // 1. Dump all CameraCharacteristics keys
                    try {
                        Log.i(debugTag, "--- Dumping All CameraCharacteristics Keys ---")
                        for (key in chars.keys) {
                            Log.i(debugTag, "  Key: ${key.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(debugTag, "  Error listing keys: ${e.message}", e)
                    }

                    // 2. Dump all fields in CameraCharacteristics
                    try {
                        Log.i(debugTag, "--- Dumping All CameraCharacteristics Fields ---")
                        for (field in CameraCharacteristics::class.java.fields) {
                            Log.i(debugTag, "  Field: ${field.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(debugTag, "  Error listing fields: ${e.message}", e)
                    }

                    // 3. Dump all methods in CameraManager
                    try {
                        Log.i(debugTag, "--- Dumping All CameraManager Methods ---")
                        for (method in CameraManager::class.java.declaredMethods) {
                            val params = method.parameterTypes.joinToString { it.simpleName }
                            Log.i(debugTag, "  Method: ${method.name}($params) [ReturnType: ${method.returnType.simpleName}]")
                        }
                    } catch (e: Exception) {
                        Log.e(debugTag, "  Error listing CameraManager methods: ${e.message}", e)
                    }

                    // 4. Test each reflection lookup
                    val maxFieldResult = try {
                        val f = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_MAXIMUM_LEVEL")
                        "FOUND (${f.get(null)})"
                    } catch (e: Exception) {
                        "NOT FOUND (${e.message})"
                    }
                    Log.i(debugTag, "Reflection FLASH_INFO_STRENGTH_MAXIMUM_LEVEL field: $maxFieldResult")

                    val defaultFieldResult = try {
                        val f = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_DEFAULT_LEVEL")
                        "FOUND (${f.get(null)})"
                    } catch (e: Exception) {
                        "NOT FOUND (${e.message})"
                    }
                    Log.i(debugTag, "Reflection FLASH_INFO_STRENGTH_DEFAULT_LEVEL field: $defaultFieldResult")

                    val turnOnTorchWithStrengthMethodResult = try {
                        val m = CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
                        "FOUND (${m.name})"
                    } catch (e: Exception) {
                        "NOT FOUND (${e.message})"
                    }
                    Log.i(debugTag, "Reflection turnOnTorchWithStrengthLevel method: $turnOnTorchWithStrengthMethodResult")

                    val getTorchStrengthLevelMethodResult = try {
                        val m = CameraManager::class.java.getMethod("getTorchStrengthLevel", String::class.java)
                        "FOUND (${m.name})"
                    } catch (e: Exception) {
                        "NOT FOUND (${e.message})"
                    }
                    Log.i(debugTag, "Reflection getTorchStrengthLevel method: $getTorchStrengthLevelMethodResult")

                    // 5. Test dynamic key construction & lookup
                    val dynamicMaxKeyResult = try {
                        val key = CameraCharacteristics.Key("android.flash.info.strengthMaximumLevel", Int::class.java)
                        val valStr = chars.get(key)?.toString() ?: "NULL"
                        "CONSTRUCTED, value=$valStr"
                    } catch (e: Exception) {
                        "FAILED to construct/get: ${e.message}"
                    }
                    Log.i(debugTag, "Dynamic Key lookup (android.flash.info.strengthMaximumLevel): $dynamicMaxKeyResult")

                    val dynamicDefaultKeyResult = try {
                        val key = CameraCharacteristics.Key("android.flash.info.strengthDefaultLevel", Int::class.java)
                        val valStr = chars.get(key)?.toString() ?: "NULL"
                        "CONSTRUCTED, value=$valStr"
                    } catch (e: Exception) {
                        "FAILED to construct/get: ${e.message}"
                    }
                    Log.i(debugTag, "Dynamic Key lookup (android.flash.info.strengthDefaultLevel): $dynamicDefaultKeyResult")

                    Log.i(debugTag, "=== TORCH BRIGHTNESS DIAGNOSTICS END ===")

                    // Probe strength level support safely via reflection to support older API versions
                    try {
                        var maxKey: CameraCharacteristics.Key<Int>? = null
                        var defaultKey: CameraCharacteristics.Key<Int>? = null
                        
                        try {
                            val maxField = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_MAXIMUM_LEVEL")
                            maxKey = maxField.get(null) as? CameraCharacteristics.Key<Int>
                            Log.d(TAG, "Reflective field FLASH_INFO_STRENGTH_MAXIMUM_LEVEL found successfully")
                        } catch (e: Exception) {
                            Log.d(TAG, "Reflective field FLASH_INFO_STRENGTH_MAXIMUM_LEVEL not found in class: ${e.message}")
                        }
                        
                        try {
                            val defaultField = CameraCharacteristics::class.java.getField("FLASH_INFO_STRENGTH_DEFAULT_LEVEL")
                            defaultKey = defaultField.get(null) as? CameraCharacteristics.Key<Int>
                            Log.d(TAG, "Reflective field FLASH_INFO_STRENGTH_DEFAULT_LEVEL found successfully")
                        } catch (e: Exception) {
                            Log.d(TAG, "Reflective field FLASH_INFO_STRENGTH_DEFAULT_LEVEL not found in class: ${e.message}")
                        }

                        // Fallback 1: Scan characteristics keys list for matches by name if reflection fields were not found
                        if (maxKey == null || defaultKey == null) {
                            Log.d(TAG, "Scanning characteristics keys list for strength keys...")
                            for (key in chars.keys) {
                                val name = key.name
                                if (name.contains("strength", ignoreCase = true)) {
                                    if (name.contains("maximum", ignoreCase = true) || name.contains("max", ignoreCase = true)) {
                                        maxKey = key as? CameraCharacteristics.Key<Int>
                                        Log.d(TAG, "Found max strength key in list by name: $name")
                                    } else if (name.contains("default", ignoreCase = true)) {
                                        defaultKey = key as? CameraCharacteristics.Key<Int>
                                        Log.d(TAG, "Found default strength key in list by name: $name")
                                    }
                                }
                            }
                        }

                        // Fallback 2: Construct Key objects dynamically if not found reflectively or in the list
                        if (maxKey == null) {
                            try {
                                maxKey = CameraCharacteristics.Key("android.flash.info.strengthMaximumLevel", Int::class.java)
                                Log.d(TAG, "Constructed maxKey dynamically")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to construct maxKey dynamically", e)
                            }
                        }
                        if (defaultKey == null) {
                            try {
                                defaultKey = CameraCharacteristics.Key("android.flash.info.strengthDefaultLevel", Int::class.java)
                                Log.d(TAG, "Constructed defaultKey dynamically")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to construct defaultKey dynamically", e)
                            }
                        }

                        if (maxKey != null) {
                            try {
                                maxStrengthLevel = chars.get(maxKey) ?: 1
                            } catch (e: Exception) {
                                Log.d(TAG, "Failed to get maxKey: ${e.message}")
                                maxStrengthLevel = 1
                            }
                        } else {
                            maxStrengthLevel = 1
                        }

                        if (defaultKey != null) {
                            try {
                                defaultStrengthLevel = chars.get(defaultKey) ?: 1
                            } catch (e: Exception) {
                                Log.d(TAG, "Failed to get defaultKey: ${e.message}")
                                defaultStrengthLevel = 1
                            }
                        } else {
                            defaultStrengthLevel = 1
                        }

                        // Fallback 3: For Samsung devices running Android 12 or lower (API < 33)
                        // where HAL does not expose the keys but the reflective strength control API method exists
                        if (maxStrengthLevel <= 1) {
                            val hasSamsungStrengthMethod = try {
                                CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
                                true
                            } catch (e: Exception) {
                                false
                            }
                            val isSamsung = android.os.Build.MANUFACTURER.contains("samsung", ignoreCase = true)
                            if (hasSamsungStrengthMethod && isSamsung) {
                                Log.d(TAG, "Samsung backported method detected with API < 33. Hardcoding max strength level to 5.")
                                maxStrengthLevel = 5
                                defaultStrengthLevel = 3
                            }
                        }

                        // Fallback 4: Samsung proprietary API semSetTorchMode check for Android 12 (API 31)
                        if (maxStrengthLevel <= 1) {
                            val hasSamsungSemMethod = try {
                                CameraManager::class.java.getMethod("semSetTorchMode", String::class.java, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                true
                            } catch (e: Exception) {
                                false
                            }
                            if (hasSamsungSemMethod) {
                                Log.d(TAG, "Samsung semSetTorchMode method detected. Enabling strength support with 5 levels.")
                                maxStrengthLevel = 5
                                defaultStrengthLevel = 3
                            }
                        }
                        
                        isStrengthSupported = maxStrengthLevel > 1
                        Log.i(debugTag, "=== FINAL CAPABILITY RESULTS ===")
                        Log.i(debugTag, "Result maxStrengthLevel: $maxStrengthLevel")
                        Log.i(debugTag, "Result defaultStrengthLevel: $defaultStrengthLevel")
                        Log.i(debugTag, "Result isStrengthSupported: $isStrengthSupported")
                        Log.i(debugTag, "Result getCurrentStrengthLevel(): ${getCurrentStrengthLevel()}")
                        Log.i(debugTag, "=================================")
                    } catch (e: Exception) {
                        Log.e(TAG, "Dynamic strength key check failed: ${e.message}", e)
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
        Log.d(TAG, "setTorchEnabled called: enabled=$enabled, isStrengthSupported=$isStrengthSupported, maxStrength=$maxStrengthLevel")
        try {
            if (enabled) {
                if (isStrengthSupported) {
                    // Try Samsung proprietary semSetTorchMode first
                    try {
                        val semMethod = CameraManager::class.java.getMethod("semSetTorchMode", String::class.java, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        val level = defaultStrengthLevel.coerceIn(1, maxStrengthLevel)
                        semMethod.invoke(cm, id, true, level)
                        Log.d(TAG, "Turned on torch with default strength level: $level via Samsung semSetTorchMode reflection")
                        return
                    } catch (e: Exception) {
                        Log.d(TAG, "Samsung semSetTorchMode reflection not available or failed: ${e.message}")
                    }

                    // Try standard turnOnTorchWithStrengthLevel
                    try {
                        val method = CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
                        val level = defaultStrengthLevel.coerceIn(1, maxStrengthLevel)
                        method.invoke(cm, id, level)
                        Log.d(TAG, "Turned on torch with default strength level: $level via reflection")
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to call turnOnTorchWithStrengthLevel via reflection, falling back", e)
                    }
                }
                cm.setTorchMode(id, true)
                Log.d(TAG, "Turned on torch via standard setTorchMode")
            } else {
                cm.setTorchMode(id, false)
                Log.d(TAG, "Turned off torch via standard setTorchMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch mode to $enabled", e)
        }
    }

    fun setTorchBrightnessPercentage(percentage: Int) {
        val cm = cameraManager ?: return
        val id = cameraId ?: return
        Log.d(TAG, "setTorchBrightnessPercentage: percentage=$percentage%, isStrengthSupported=$isStrengthSupported")
        if (!isStrengthSupported) {
            Log.d(TAG, "setTorchBrightnessPercentage ignored because isStrengthSupported is false")
            return
        }
        
        try {
            val level = ((percentage.toFloat() / 100f) * maxStrengthLevel.toFloat()).toInt().coerceIn(1, maxStrengthLevel)
            
            var success = false
            // Try Samsung proprietary semSetTorchMode first
            try {
                val semMethod = CameraManager::class.java.getMethod("semSetTorchMode", String::class.java, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                semMethod.invoke(cm, id, true, level)
                Log.d(TAG, "Samsung semSetTorchMode reflection success: percentage=$percentage% -> level $level/$maxStrengthLevel")
                success = true
            } catch (e: Exception) {
                Log.d(TAG, "Samsung semSetTorchMode reflection failed: ${e.message}")
            }

            if (!success) {
                // Try standard turnOnTorchWithStrengthLevel
                val method = CameraManager::class.java.getMethod("turnOnTorchWithStrengthLevel", String::class.java, Int::class.javaPrimitiveType)
                method.invoke(cm, id, level)
                Log.d(TAG, "Reflective turnOnTorchWithStrengthLevel success: percentage=$percentage% -> level $level/$maxStrengthLevel")
            }
            
            // Immediately update state so UI responds instantly even if callback takes a moment
            val current = OverlayStateManager.torchState.value ?: TorchState()
            if (current.isActive) {
                OverlayStateManager.torchState.value = current.copy(
                    brightnessPercentage = percentage,
                    isStrengthSupported = isStrengthSupported
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch brightness to $percentage% via reflection", e)
        }
    }

    private fun getCurrentStrengthLevel(): Int {
        val cm = cameraManager ?: return 1
        val id = cameraId ?: return 1
        
        // 1. Try standard getTorchStrengthLevel method first if available
        try {
            val method = CameraManager::class.java.getMethod("getTorchStrengthLevel", String::class.java)
            val result = method.invoke(cm, id) as Int
            Log.d(TAG, "getCurrentStrengthLevel via reflection returned level: $result")
            return result
        } catch (e: Exception) {
            Log.d(TAG, "getCurrentStrengthLevel standard reflection failed: ${e.message}")
        }
        
        // 2. Try Samsung system setting fallback (One UI 4 / Android 12)
        val ctx = appContext
        if (ctx != null) {
            try {
                val dbVal = android.provider.Settings.System.getInt(ctx.contentResolver, "Flashlight_brightness_level")
                Log.d(TAG, "getCurrentStrengthLevel via Settings.System: $dbVal")
                if (dbVal in 1001..1009) {
                    val level = ((dbVal - 1001) / 2 + 1).coerceIn(1, 5)
                    Log.d(TAG, "getCurrentStrengthLevel mapped setting $dbVal to level $level")
                    return level
                } else if (dbVal in 1..5) {
                    return dbVal
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to read Flashlight_brightness_level from Settings.System: ${e.message}")
            }
        }
        
        return 1
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
