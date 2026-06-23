package com.novabar.app.utils

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager
import com.novabar.app.domain.DiagnosticsManager
import kotlinx.coroutines.flow.MutableStateFlow

object CutoutManager {
    private const val TAG = "CutoutManager"

    val hasDisplayCutout = MutableStateFlow(false)
    val hasCenteredPunchHole = MutableStateFlow(false)
    val cutoutCenterX = MutableStateFlow(0) // in px
    val cutoutWidth = MutableStateFlow(0) // in px
    val cutoutHeight = MutableStateFlow(0) // in px

    fun detectCutout(context: Context) {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets
            
            // Check for display cutout
            val cutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                insets.displayCutout
            } else {
                null
            }

            if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                val bounds = metrics.bounds
                val screenWidth = bounds.width()
                val density = context.resources.displayMetrics.density

                // Find a cutout that is horizontally centered at the top
                var punchHole: Rect? = null
                for (rect in cutout.boundingRects) {
                    val rectCenterX = (rect.left + rect.right) / 2
                    // Must be centered within tolerance of 20dp
                    val isCenteredHorizontally = Math.abs(rectCenterX - screenWidth / 2) < (20 * density).toInt()
                    
                    // Punch hole expected width in dp
                    val widthDp = rect.width() / density
                    // Punch hole typically has a width between 10dp and 65dp
                    val isExpectedSize = widthDp in 10f..65f

                    if (isCenteredHorizontally && isExpectedSize) {
                        punchHole = rect
                        break
                    }
                }

                if (punchHole != null) {
                    hasDisplayCutout.value = true
                    hasCenteredPunchHole.value = true
                    cutoutCenterX.value = (punchHole.left + punchHole.right) / 2
                    cutoutWidth.value = punchHole.width()
                    cutoutHeight.value = punchHole.height()
                    Log.d(TAG, "Centered punch hole detected: width=${punchHole.width()}px, height=${punchHole.height()}px, center=${cutoutCenterX.value}px")
                } else {
                    hasDisplayCutout.value = true
                    hasCenteredPunchHole.value = false
                    cutoutCenterX.value = 0
                    cutoutWidth.value = 0
                    cutoutHeight.value = 0
                    Log.d(TAG, "Display cutout exists but is not a centered punch-hole camera")
                }
            } else {
                hasDisplayCutout.value = false
                hasCenteredPunchHole.value = false
                cutoutCenterX.value = 0
                cutoutWidth.value = 0
                cutoutHeight.value = 0
                Log.d(TAG, "No display cutout detected")
            }

            // Sync with DiagnosticsManager
            DiagnosticsManager.hasDisplayCutout.value = hasDisplayCutout.value
            DiagnosticsManager.cutoutWidth.value = cutoutWidth.value
            DiagnosticsManager.cutoutCenterX.value = cutoutCenterX.value
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting cutout", e)
            hasDisplayCutout.value = false
            hasCenteredPunchHole.value = false
            cutoutCenterX.value = 0
            cutoutWidth.value = 0
            cutoutHeight.value = 0
        }
    }
}
