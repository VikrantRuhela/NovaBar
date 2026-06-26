package com.novabar.app.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.graphics.Rect
import androidx.core.graphics.ColorUtils
import com.novabar.app.data.SettingsRepository
import com.novabar.app.data.NovaSettings
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.DiagnosticsManager
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.utils.LuminanceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class NovaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: NovaAccessibilityService? = null

        fun getInstance(): NovaAccessibilityService? = instance

        fun triggerEndCall(): Boolean {
            val service = instance
            if (service == null) {
                Log.e("NovaBar", "CALL_END_ACCESSIBILITY_FAILED: NovaAccessibilityService instance is null")
                return false
            }
            return service.triggerEndCallViaAccessibility()
        }
    }

    private val tag = "NovaAccessibilityService"
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var settingsJob: Job? = null
    private lateinit var overlayHost: OverlayHost

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        overlayHost = OverlayHost(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        DiagnosticsManager.overlayEngine.value = "ACCESSIBILITY"
        DiagnosticsManager.accessibilityServiceEnabled.value = true
        DiagnosticsManager.accessibilityOverlayActive.value = false

        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                OverlayStateManager.settingsFlow.value = settings
                if (settings.isEnabled && settings.overlayEngine == OverlayEngine.ACCESSIBILITY) {
                    showOverlay(settings)
                } else {
                    removeOverlay()
                }
            }
        }

        // Listen for reset/recreate request from Diagnostics
        scope.launch {
            DiagnosticsManager.resetTrigger.collectLatest { trigger ->
                if (trigger > 0) {
                    val settings = settingsRepository.settingsFlow.first()
                    if (settings.isEnabled && settings.overlayEngine == OverlayEngine.ACCESSIBILITY) {
                        removeOverlay()
                        delay(200)
                        showOverlay(settings)
                    }
                }
            }
        }
    }

    private fun showOverlay(settings: NovaSettings) {
        overlayHost.show(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, settings)
        DiagnosticsManager.accessibilityOverlayActive.value = true
    }

    private fun removeOverlay() {
        overlayHost.remove()
        DiagnosticsManager.accessibilityOverlayActive.value = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventTypeStr = try {
            AccessibilityEvent.eventTypeToString(event.eventType)
        } catch (e: Exception) {
            "Unknown (${event.eventType})"
        }
        val pkg = event.packageName?.toString() ?: "Unknown"
        DiagnosticsManager.lastAccessibilityEvent.value = "$eventTypeStr (pkg=$pkg)"
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            checkImmersiveMode()
        }
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            DiagnosticsManager.accessibilityForegroundPackage.value = pkg
            DiagnosticsManager.currentActivity.value = pkg
            scope.launch {
                val settings = settingsRepository.settingsFlow.first()
                if (settings.isEnabled && settings.colorAdaptationEnabled) {
                    analyzeBackgroundLuminance()
                }
            }
        }
    }

    private fun checkImmersiveMode() {
        val windowsList = try {
            windows
        } catch (e: Exception) {
            Log.e(tag, "Failed to query windows: ${e.message}")
            null
        }
        
        if (windowsList == null || windowsList.isEmpty()) {
            return
        }
        
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        var isStatusBarWindowVisible = false
        for (window in windowsList) {
            if (window.type == AccessibilityWindowInfo.TYPE_SYSTEM) {
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                val heightDp = bounds.height() / displayMetrics.density
                if (bounds.top == 0 && bounds.left == 0 && bounds.bottom > 0 && heightDp < 100 && bounds.width() >= screenWidth) {
                    isStatusBarWindowVisible = true
                    break
                }
            }
        }
        
        Log.d("SystemBarVisibility", "checkImmersiveMode: isStatusBarVisible=$isStatusBarWindowVisible")
        OverlayStateManager.systemBarVisible.value = isStatusBarWindowVisible
    }

    override fun onInterrupt() {
        Log.d(tag, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        DiagnosticsManager.accessibilityServiceEnabled.value = false
        DiagnosticsManager.accessibilityOverlayActive.value = false
        settingsJob?.cancel()
        removeOverlay()
        scope.cancel()
    }

    fun triggerEndCallViaAccessibility(): Boolean {
        Log.d("NovaBar", "CALL_END_REQUEST_SENT: Attempting Accessibility fallback")
        val currentWindows = try { windows } catch (e: Exception) { emptyList() }
        for (window in currentWindows) {
            val root = try { window.root } catch (e: Exception) { null }
            if (root != null) {
                if (findAndPerformEndCallClick(root)) {
                    root.recycle()
                    Log.d("NovaBar", "CALL_END_ACCESSIBILITY_SUCCESS: Found and clicked end call button in windows list")
                    return true
                }
                root.recycle()
            }
        }
        val activeRoot = try { rootInActiveWindow } catch (e: Exception) { null }
        if (activeRoot != null) {
            val result = findAndPerformEndCallClick(activeRoot)
            activeRoot.recycle()
            if (result) {
                Log.d("NovaBar", "CALL_END_ACCESSIBILITY_SUCCESS: Found and clicked end call button in active window")
                return true
            }
        }
        Log.e("NovaBar", "CALL_END_ACCESSIBILITY_FAILED: Could not find clickable End Call button in any window")
        return false
    }

    private fun findAndPerformEndCallClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val matchesText = text.contains("end call") || text.contains("hang up") || text.contains("hangup") || text.contains("decline") || text.contains("end")
        val matchesDesc = desc.contains("end call") || desc.contains("hang up") || desc.contains("hangup") || desc.contains("decline") || desc.contains("end")
        val matchesId = viewId.contains("end_call") || viewId.contains("endcall") || viewId.contains("decline") || viewId.contains("hangup") || viewId.contains("disconnect")

        if ((matchesText || matchesDesc || matchesId) && node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                Log.d("NovaBar", "Accessibility clicked: id=$viewId, text=$text, desc=$desc")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndPerformEndCallClick(child)) {
                child?.recycle()
                return true
            }
            child?.recycle()
        }
        return false
    }

    private fun analyzeBackgroundLuminance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    applicationContext.mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val buffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            val hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                            
                            if (hardwareBitmap != null) {
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                        if (softwareBitmap != null) {
                                            val isDark = calculateLuminanceForTopCenter(softwareBitmap)
                                            LuminanceManager.isDarkBackground.value = isDark
                                            softwareBitmap.recycle()
                                        }
                                    } catch (e: Exception) {
                                        Log.e(tag, "Error processing screenshot pixels", e)
                                    } finally {
                                        hardwareBitmap.recycle()
                                        buffer.close()
                                    }
                                }
                            } else {
                                buffer.close()
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.e(tag, "Failed to capture screenshot for luminance analysis: error $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error taking accessibility screenshot", e)
            }
        }
    }

    private fun calculateLuminanceForTopCenter(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height

        // Define a small box at the top center under the status bar
        val boxWidth = (width * 0.20f).toInt().coerceAtLeast(200)
        val boxHeight = (height * 0.05f).toInt().coerceAtLeast(80).coerceAtMost(150)
        
        val startX = (width - boxWidth) / 2
        val startY = 20 // Skip the very top status bar boundary shadow

        var totalLuminance = 0.0
        var sampleCount = 0

        val gridCols = 10
        val gridRows = 5
        val xStep = (boxWidth / gridCols).coerceAtLeast(1)
        val yStep = (boxHeight / gridRows).coerceAtLeast(1)

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val x = startX + c * xStep
                val y = startY + r * yStep
                if (x in 0 until width && y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val luminance = ColorUtils.calculateLuminance(pixel)
                    totalLuminance += luminance
                    sampleCount++
                }
            }
        }

        val avgLuminance = if (sampleCount > 0) totalLuminance / sampleCount else 0.5
        Log.d(tag, "Sampled $sampleCount pixels, average luminance: $avgLuminance")
        
        // If average luminance is below 0.45, the background is dark (requires white foreground text/icons)
        return avgLuminance < 0.45
    }
}
