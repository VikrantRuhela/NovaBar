package com.novabar.app.services

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.app.KeyguardManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
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
import kotlinx.coroutines.*

class OverlayHost(private val context: Context) {
    private val TAG = "OverlayHost"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: MyLifecycleOwner? = null
    private var isOverlayAdded = false
    private var currentParams: WindowManager.LayoutParams? = null
    private var isReceiverRegistered = false
    private var currentSettings: NovaSettings? = null
    private var scope: CoroutineScope? = null
    private var windowJob: Job? = null

    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateVisibilityForLockscreen()
        }
    }

    private fun updateVisibilityForLockscreen() {
        val settings = currentSettings ?: return
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked
        val shouldHide = !settings.showOnLockscreen && isLocked
        
        composeView?.visibility = if (shouldHide) android.view.View.GONE else android.view.View.VISIBLE
        DiagnosticsManager.overlayVisible.value = !shouldHide
    }

    fun show(windowType: Int, settings: NovaSettings) {
        com.novabar.app.utils.CutoutManager.detectCutout(context)
        val density = context.resources.displayMetrics.density
        currentSettings = settings

        // Compute maximum bounds width & height dynamically
        val collapsedHeight = if (settings.defaultPresentationMode == "Minimized") 38 else 44
        val collapsedHeightPx = ((collapsedHeight + settings.barHeightPadding).coerceAtLeast(if (settings.defaultPresentationMode == "Minimized") 24 else 30)) * density

        val heights = listOf(
            (38 + settings.barHeightPadding).coerceAtLeast(24),
            (44 + settings.barHeightPadding).coerceAtLeast(30),
            205 // Expanded height
        )
        val maxSupportedHeightDp = heights.maxOrNull() ?: 205
        val maxSupportedHeightPx = (maxSupportedHeightDp * density).toInt()

        val widths = listOf(
            (115f * settings.barWidthScale).toInt(),
            (185f * settings.barWidthScale).toInt(),
            290 // Expanded width
        )
        val maxSupportedWidthDp = widths.maxOrNull() ?: 290
        val maxSupportedWidthPx = (maxSupportedWidthDp * density).toInt()

        val screenWidthPx = context.resources.displayMetrics.widthPixels
        val screenHeightPx = context.resources.displayMetrics.heightPixels

        val targetY = (settings.offsetY * density).toInt()
        val clampedY = targetY.coerceIn(0, (screenHeightPx - maxSupportedHeightPx).coerceAtLeast(0))

        val baseX = (settings.offsetX * density).toInt()
        val clampedX = when (settings.barGravity) {
            "Left", "Right" -> {
                baseX.coerceIn(0, (screenWidthPx - maxSupportedWidthPx).coerceAtLeast(0))
            }
            else -> {
                val maxOffset = (screenWidthPx - maxSupportedWidthPx) / 2
                baseX.coerceIn(-maxOffset, maxOffset)
            }
        }

        val initialMode = OverlayStateManager.windowMode.value
        val initialWidthDp = when (initialMode) {
            "Minimized" -> (115f * settings.barWidthScale).toInt()
            "Compact" -> (185f * settings.barWidthScale).toInt()
            else -> 290
        }
        val initialHeightDp = when (initialMode) {
            "Minimized" -> (38 + settings.barHeightPadding).coerceAtLeast(24)
            "Compact" -> (44 + settings.barHeightPadding).coerceAtLeast(30)
            else -> 205
        }
        val initialWidthPx = (initialWidthDp * density).toInt()
        val initialHeightPx = (initialHeightDp * density).toInt()

        val layoutParams = WindowManager.LayoutParams(
            initialWidthPx,
            initialHeightPx,
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
            x = clampedX
            y = clampedY

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
        
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        windowJob?.cancel()
        windowJob = scope?.launch {
            OverlayStateManager.windowMode.collect { mode ->
                updateWindowSize(mode, settings)
            }
        }
        DiagnosticsManager.windowX.value = layoutParams.x
        DiagnosticsManager.windowY.value = layoutParams.y

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
                addOnComputeInternalInsetsListenerReflection(this) { region ->
                    val isSplit = settings.cameraCutoutMode && com.novabar.app.utils.CutoutManager.hasCenteredPunchHole.value && 
                            (OverlayStateManager.windowMode.value == "Compact" || OverlayStateManager.windowMode.value == "Minimized")
                    if (isSplit) {
                        region.setEmpty()
                        region.op(OverlayStateManager.leftPillBounds.value, Region.Op.UNION)
                        region.op(OverlayStateManager.rightPillBounds.value, Region.Op.UNION)
                    } else {
                        region.set(OverlayStateManager.pillBounds.value)
                    }
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

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(lockscreenReceiver, filter)
            isReceiverRegistered = true
        }
        updateVisibilityForLockscreen()
    }

    fun remove() {
        windowJob?.cancel()
        windowJob = null
        scope?.cancel()
        scope = null
        if (isOverlayAdded && composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove WindowManager overlay view", e)
            }
            isOverlayAdded = false
        }
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(lockscreenReceiver)
            } catch (e: Exception) {
                // Ignore
            }
            isReceiverRegistered = false
        }
        currentSettings = null
        DiagnosticsManager.overlayVisible.value = false
        DiagnosticsManager.overlayAttached.value = false
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        composeView = null
    }

    private fun updateWindowSize(mode: String, settings: NovaSettings) {
        val params = currentParams ?: return
        val compose = composeView ?: return
        val density = context.resources.displayMetrics.density

        val targetWidth: Int
        val targetHeight: Int

        when (mode) {
            "Minimized" -> {
                val w = (115f * settings.barWidthScale).toInt()
                val h = (38 + settings.barHeightPadding).coerceAtLeast(24)
                targetWidth = (w * density).toInt()
                targetHeight = (h * density).toInt()
            }
            "Compact" -> {
                val w = (185f * settings.barWidthScale).toInt()
                val h = (44 + settings.barHeightPadding).coerceAtLeast(30)
                targetWidth = (w * density).toInt()
                targetHeight = (h * density).toInt()
            }
            else -> { // "Expanded"
                targetWidth = (290 * density).toInt()
                targetHeight = (205 * density).toInt()
            }
        }

        if (params.width != targetWidth || params.height != targetHeight) {
            params.width = targetWidth
            params.height = targetHeight
            try {
                windowManager.updateViewLayout(compose, params)
                Log.d("OverlayHost", "Updated WindowManager size for mode $mode: ${targetWidth}x${targetHeight}")
            } catch (e: Exception) {
                Log.e("OverlayHost", "Failed to update WindowManager layout size", e)
            }
        }
    }

    private fun addOnComputeInternalInsetsListenerReflection(view: android.view.View, onCompute: (android.graphics.Region) -> Unit) {
        try {
            val viewTreeObserver = view.viewTreeObserver
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val infoClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")
            val setTouchableInsetsMethod = infoClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = infoClass.getField("touchableRegion")
            val TOUCHABLE_INSETS_REGION = 3
            
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass),
                object : java.lang.reflect.InvocationHandler {
                    override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
                        if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                            val info = args[0]
                            setTouchableInsetsMethod.invoke(info, TOUCHABLE_INSETS_REGION)
                            val touchableRegion = touchableRegionField.get(info) as android.graphics.Region
                            onCompute(touchableRegion)
                        }
                        return null
                    }
                }
            )
            
            val addMethod = ViewTreeObserver::class.java.getMethod("addOnComputeInternalInsetsListener", listenerClass)
            addMethod.invoke(viewTreeObserver, proxy)
        } catch (e: Exception) {
            Log.e(TAG, "Reflection failed for OnComputeInternalInsetsListener", e)
        }
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
