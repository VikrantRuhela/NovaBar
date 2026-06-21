package com.novabar.app.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.graphics.ColorUtils
import com.novabar.app.data.SettingsRepository
import com.novabar.app.data.NovaSettings
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.DiagnosticsManager
import com.novabar.app.utils.LuminanceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class NovaAccessibilityService : AccessibilityService() {

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
        DiagnosticsManager.overlayEngine.value = "ACCESSIBILITY"
        DiagnosticsManager.accessibilityServiceEnabled.value = true
        DiagnosticsManager.accessibilityOverlayActive.value = false

        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
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

    override fun onInterrupt() {
        Log.d(tag, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        DiagnosticsManager.accessibilityServiceEnabled.value = false
        DiagnosticsManager.accessibilityOverlayActive.value = false
        settingsJob?.cancel()
        removeOverlay()
        scope.cancel()
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
