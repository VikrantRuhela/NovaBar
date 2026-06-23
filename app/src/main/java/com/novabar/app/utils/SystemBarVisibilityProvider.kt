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
        Log.d(TAG, "WindowInsets status bar visibility change: $visible")
        _isStatusBarVisible.value = visible
        
        // Also call CutoutManager
        val platformInsets = insets.toWindowInsets()
        CutoutManager.detectCutout(context, platformInsets)
        
        insets
    }

    private val onSystemUiVisibilityChangeListener = View.OnSystemUiVisibilityChangeListener { visibility ->
        val hidden = (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0 ||
                (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 ||
                (visibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
        Log.d(TAG, "Legacy SystemUiVisibility change flags: $visibility -> isVisible: ${!hidden}")
        _isStatusBarVisible.value = !hidden
    }

    fun start() {
        // Primary: WindowInsetsCompat
        ViewCompat.setOnApplyWindowInsetsListener(view, onApplyWindowInsetsListener)

        // Fallback: OnSystemUiVisibilityChangeListener
        view.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChangeListener)

        // Initial check
        val currentVisibility = view.systemUiVisibility
        val hidden = (currentVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0 ||
                (currentVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 ||
                (currentVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
        if (hidden) {
            _isStatusBarVisible.value = false
        } else {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            if (rootInsets != null) {
                _isStatusBarVisible.value = rootInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    fun stop() {
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        view.setOnSystemUiVisibilityChangeListener(null)
    }
}
