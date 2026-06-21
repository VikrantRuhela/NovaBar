package com.novabar.app.services

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.novabar.app.data.NovaSettings
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.domain.DiagnosticsManager
import com.novabar.app.ui.components.NovaBarUi

class OverlayHost(private val context: Context) {
    private val TAG = "OverlayHost"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: MyLifecycleOwner? = null
    private var isOverlayAdded = false
    private var currentParams: WindowManager.LayoutParams? = null

    fun show(windowType: Int, settings: NovaSettings) {
        val density = context.resources.displayMetrics.density

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = when (settings.barGravity) {
                "Left" -> Gravity.TOP or Gravity.START
                "Right" -> Gravity.TOP or Gravity.END
                else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            x = (settings.offsetX * density).toInt()
            y = (settings.offsetY * density).toInt()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }

        currentParams = layoutParams

        // Update diagnostics
        DiagnosticsManager.windowType.value = when (windowType) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY -> "TYPE_APPLICATION_OVERLAY"
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY -> "TYPE_ACCESSIBILITY_OVERLAY"
            else -> "Unknown ($windowType)"
        }
        DiagnosticsManager.touchableState.value = "Touchable"
        DiagnosticsManager.overlayVisible.value = true

        if (composeView == null) {
            val owner = MyLifecycleOwner()
            owner.start()
            owner.resume()
            lifecycleOwner = owner

            val view = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NovaBarUi()
                }
                setOnTouchListener { view, event ->
                    DiagnosticsManager.incrementTouchEvents()
                    val action = event.action
                    if (action == android.view.MotionEvent.ACTION_OUTSIDE) {
                        Log.d("NovaBar", "OUTSIDE_TOUCH_DETECTED (ACTION_OUTSIDE)")
                        OverlayStateManager.collapse()
                        true
                    } else {
                        if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_UP) {
                            val rx = event.rawX
                            val ry = event.rawY
                            val location = IntArray(2)
                            view.getLocationOnScreen(location)
                            val vx = location[0]
                            val vy = location[1]
                            val vw = view.width
                            val vh = view.height
                            
                            if (rx < vx || rx > vx + vw || ry < vy || ry > vy + vh) {
                                Log.d("NovaBar", "OUTSIDE_TOUCH_DETECTED (Manual coordinate check): touch=($rx, $ry), view=($vx, $vy, $vw, $vh)")
                                OverlayStateManager.collapse()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                }
                addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    val width = right - left
                    val height = bottom - top
                    
                    DiagnosticsManager.windowWidth.value = width
                    DiagnosticsManager.windowHeight.value = height
                    DiagnosticsManager.currentOverlayBounds.value = "l=$left, t=$top, r=$right, b=$bottom (w=$width, h=$height)"
                    composeView?.let {
                        DiagnosticsManager.overlayAttached.value = it.isAttachedToWindow
                    }

                    val oldWidth = oldRight - oldLeft
                    val oldHeight = oldBottom - oldTop
                    if (width != oldWidth || height != oldHeight) {
                        currentParams?.let { params ->
                            params.width = WindowManager.LayoutParams.WRAP_CONTENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            
                            val wasNotTouchable = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
                            val shouldBeNotTouchable = width <= 5 || height <= 5
                            
                            DiagnosticsManager.touchableState.value = if (shouldBeNotTouchable) "Not Touchable" else "Touchable"

                            if (wasNotTouchable != shouldBeNotTouchable) {
                                if (shouldBeNotTouchable) {
                                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                } else {
                                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                }
                                
                                if (isOverlayAdded) {
                                    try {
                                        windowManager.updateViewLayout(this, params)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to update layout size in layout change listener", e)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            view.setViewTreeLifecycleOwner(owner)
            view.setViewTreeViewModelStoreOwner(owner)
            view.setViewTreeSavedStateRegistryOwner(owner)

            composeView = view
        }

        if (!isOverlayAdded) {
            try {
                windowManager.addView(composeView, layoutParams)
                isOverlayAdded = true
                DiagnosticsManager.overlayAttached.value = composeView?.isAttachedToWindow == true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add WindowManager overlay view", e)
            }
        } else {
            try {
                windowManager.updateViewLayout(composeView, layoutParams)
                DiagnosticsManager.overlayAttached.value = composeView?.isAttachedToWindow == true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update WindowManager overlay view", e)
            }
        }
    }

    fun remove() {
        if (isOverlayAdded && composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove WindowManager overlay view", e)
            }
            isOverlayAdded = false
        }
        DiagnosticsManager.overlayVisible.value = false
        DiagnosticsManager.overlayAttached.value = false
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        composeView = null
    }

    private class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val controller = SavedStateRegistryController.create(this)

        init {
            controller.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun start() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun resume() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    }
}
