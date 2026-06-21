package com.novabar.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

import com.novabar.app.data.BatteryReceiver
import com.novabar.app.data.SettingsRepository
import com.novabar.app.data.NovaSettings
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.ui.components.NovaBarUi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class OverlayService : Service() {

    private val tag = "OverlayService"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository
    private val batteryReceiver = BatteryReceiver()
    private var settingsJob: Job? = null

    private var composeView: ComposeView? = null
    private var lifecycleOwner: MyLifecycleOwner? = null
    private var isOverlayAdded = false
    private var currentParams: WindowManager.LayoutParams? = null
    private var currentBlurRadius = -1

    companion object {
        private const val CHANNEL_ID = "nova_bar_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(applicationContext)
        batteryReceiver.register(this)
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                if (settings.isEnabled) {
                    showOverlay(settings)
                } else {
                    removeOverlay()
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        settingsJob?.cancel()
        removeOverlay()
        batteryReceiver.unregister(this)
        scope.cancel()
    }

    private fun startForegroundService() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("Nova Bar Active")
            .setContentText("Samsung Now Bar overlay is running")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start foreground service", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova Bar Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showOverlay(settings: NovaSettings) {
        val density = resources.displayMetrics.density

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = settings.blurRadius.coerceAtLeast(1)
            }
        }

        currentParams = layoutParams

        if (composeView == null) {
            val owner = MyLifecycleOwner()
            owner.start()
            owner.resume()
            lifecycleOwner = owner

            val view = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NovaBarUi()
                }
                setOnTouchListener { view, event ->
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
                    val oldWidth = oldRight - oldLeft
                    val oldHeight = oldBottom - oldTop
                    if (width != oldWidth || height != oldHeight) {
                        currentParams?.let { params ->
                            // Keep width and height as WRAP_CONTENT to let WindowManager scale bounds dynamically
                            params.width = WindowManager.LayoutParams.WRAP_CONTENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            
                            val wasNotTouchable = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
                            val shouldBeNotTouchable = width <= 5 || height <= 5
                            
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
                                        Log.e("OverlayService", "Failed to update layout size in layout change listener", e)
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
                currentBlurRadius = settings.blurRadius
            } catch (e: Exception) {
                Log.e(tag, "Failed to add WindowManager overlay view", e)
            }
        } else {
            try {
                windowManager.updateViewLayout(composeView, layoutParams)
                currentBlurRadius = settings.blurRadius
            } catch (e: Exception) {
                Log.e(tag, "Failed to update WindowManager overlay view", e)
            }
        }
    }

    private fun removeOverlay() {
        if (isOverlayAdded && composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                Log.e(tag, "Failed to remove WindowManager overlay view", e)
            }
            isOverlayAdded = false
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        composeView = null
    }

    // Custom LifecycleOwner for running Jetpack Compose tree inside Service context
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
