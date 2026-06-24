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
    private val _isStatusBarVisible = MutableStateFlow(true)
    val isStatusBarVisible: StateFlow<Boolean> = _isStatusBarVisible

    private val onApplyWindowInsetsListener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
        val visible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
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
        _isStatusBarVisible.value = !hidden
    }

    fun start() {
        // Primary: WindowInsetsCompat
        ViewCompat.setOnApplyWindowInsetsListener(view, onApplyWindowInsetsListener)

        // Fallback: OnSystemUiVisibilityChangeListener
        view.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChangeListener)

        // Initial check
        val currentVisibility = view.systemUiVisibility
        val fullScreen = (currentVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        val hideNav = (currentVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
        val immersiveSticky = (currentVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
        val hidden = fullScreen || hideNav || immersiveSticky
        if (hidden) {
            _isStatusBarVisible.value = false
        } else {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            if (rootInsets != null) {
                val visible = rootInsets.isVisible(WindowInsetsCompat.Type.statusBars())
                _isStatusBarVisible.value = visible
            }
        }
    }

    fun stop() {
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        view.setOnSystemUiVisibilityChangeListener(null)
    }
}

