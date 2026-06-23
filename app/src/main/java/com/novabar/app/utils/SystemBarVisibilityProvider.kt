package com.novabar.app.utils

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SystemBarVisibilityProvider(private val context: Context, private val view: View) {
    private val TAG = "SystemBarVisibility"
    private val _isStatusBarVisible = MutableStateFlow(true)
    val isStatusBarVisible: StateFlow<Boolean> = _isStatusBarVisible

    private val onApplyWindowInsetsListener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
        val visible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
        val navVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
        val sysVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
        Log.d(TAG, "DIAG_LOG: WindowInsets callback invoked. insets=$insets, statusBarsVisible=$visible, navigationBarsVisible=$navVisible, systemBarsVisible=$sysVisible")
        
        Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider emitting _isStatusBarVisible = $visible (from WindowInsets callback)")
        _isStatusBarVisible.value = visible
        
        // Also call CutoutManager
        val platformInsets = insets.toWindowInsets()
        CutoutManager.detectCutout(context, platformInsets)
        
        insets
    }

    private val onSystemUiVisibilityChangeListener = View.OnSystemUiVisibilityChangeListener { visibility ->
        val fullScreen = (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        val hideNav = (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
        val immersiveSticky = (visibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
        val hidden = fullScreen || hideNav || immersiveSticky
        Log.d(TAG, "DIAG_LOG: Legacy SystemUiVisibility callback invoked. flags=$visibility (fullScreen=$fullScreen, hideNav=$hideNav, immersiveSticky=$immersiveSticky) -> isVisible=${!hidden}")
        
        Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider emitting _isStatusBarVisible = ${!hidden} (from Legacy callback)")
        _isStatusBarVisible.value = !hidden
    }

    fun start() {
        Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider start() called on view: $view")
        
        // Primary: WindowInsetsCompat
        ViewCompat.setOnApplyWindowInsetsListener(view, onApplyWindowInsetsListener)
        Log.d(TAG, "DIAG_LOG: Registered WindowInsets listener")

        // Fallback: OnSystemUiVisibilityChangeListener
        view.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChangeListener)
        Log.d(TAG, "DIAG_LOG: Registered Legacy SystemUiVisibility change listener")

        // Initial check
        val currentVisibility = view.systemUiVisibility
        val fullScreen = (currentVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        val hideNav = (currentVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
        val immersiveSticky = (currentVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
        val hidden = fullScreen || hideNav || immersiveSticky
        Log.d(TAG, "DIAG_LOG: Initial systemUiVisibility=$currentVisibility (fullScreen=$fullScreen, hideNav=$hideNav, immersiveSticky=$immersiveSticky)")
        
        if (hidden) {
            Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider emitting _isStatusBarVisible = false (initial legacy check)")
            _isStatusBarVisible.value = false
        } else {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            Log.d(TAG, "DIAG_LOG: Initial rootInsets=$rootInsets")
            if (rootInsets != null) {
                val visible = rootInsets.isVisible(WindowInsetsCompat.Type.statusBars())
                Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider emitting _isStatusBarVisible = $visible (initial rootInsets check)")
                _isStatusBarVisible.value = visible
            }
        }
    }

    fun stop() {
        Log.d(TAG, "DIAG_LOG: SystemBarVisibilityProvider stop() called")
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        view.setOnSystemUiVisibilityChangeListener(null)
    }
}

