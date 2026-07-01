package com.novabar.app.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.novabar.app.data.NovaSettings
import com.novabar.app.data.SettingsRepository
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.OverlayState
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.utils.LuminanceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverlayViewModel(private val repository: SettingsRepository) : ViewModel() {
    val activeState: StateFlow<OverlayState> = OverlayStateManager.activeState
    val settingsFlow: StateFlow<NovaSettings> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        NovaSettings()
    )
    val isDarkBackground: StateFlow<Boolean> = LuminanceManager.isDarkBackground

    fun dismissNotification() {
        OverlayStateManager.dismissNotification()
    }

    fun disableHotspot(context: Context) {
        OverlayStateManager.disableHotspot(context)
    }
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settingsFlow: StateFlow<NovaSettings> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        NovaSettings()
    )

    fun setEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateEnabled(enabled)
    }

    fun setPositionY(y: Int) = viewModelScope.launch {
        repository.updatePositionY(y)
    }

    fun setCornerRadius(radius: Int) = viewModelScope.launch {
        repository.updateCornerRadius(radius)
    }

    fun setOpacity(opacity: Float) = viewModelScope.launch {
        repository.updateOpacity(opacity)
    }
    fun setAnimationSpeed(speed: Float) = viewModelScope.launch {
        repository.updateAnimationSpeed(speed)
    }
    fun setMediaEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateMediaEnabled(enabled)
    }

    fun setTimerEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateTimerEnabled(enabled)
    }

    fun setStopwatchEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateStopwatchEnabled(enabled)
    }

    fun setNavigationEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateNavigationEnabled(enabled)
    }

    fun setChargingEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateChargingEnabled(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateNotificationsEnabled(enabled)
    }

    fun setVoiceRecorderEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateVoiceRecorderEnabled(enabled)
    }

    fun setAlwaysOnNovaGuy(enabled: Boolean) = viewModelScope.launch {
        repository.updateAlwaysOnNovaGuy(enabled)
    }

    fun setNovaGuyMinInterval(min: Int) = viewModelScope.launch {
        repository.updateNovaGuyMinInterval(min)
    }

    fun setNovaGuyMaxInterval(max: Int) = viewModelScope.launch {
        repository.updateNovaGuyMaxInterval(max)
    }

    fun setLockscreenGuardianEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateLockscreenGuardianEnabled(enabled)
    }

    fun setEnableNovaGuyMessages(enabled: Boolean) = viewModelScope.launch {
        repository.updateEnableNovaGuyMessages(enabled)
    }

    fun setContextAwareMessagesEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateContextAwareMessagesEnabled(enabled)
    }

    fun setPillAppearanceStyle(style: String) = viewModelScope.launch {
        repository.updatePillAppearanceStyle(style)
    }

    fun setPillCustomColor(color: Int) = viewModelScope.launch {
        repository.updatePillCustomColor(color)
    }

    fun setColorAdaptationEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateColorAdaptationEnabled(enabled)
    }

    fun setBarWidthScale(scale: Float) = viewModelScope.launch {
        repository.updateBarWidthScale(scale)
    }

    fun setBarHeightPadding(padding: Int) = viewModelScope.launch {
        repository.updateBarHeightPadding(padding)
    }

    fun setBarBorderThickness(thickness: Int) = viewModelScope.launch {
        repository.updateBarBorderThickness(thickness)
    }

    fun setBarGravity(gravity: String) = viewModelScope.launch {
        repository.updateBarGravity(gravity)
    }

    fun setOffsetX(x: Int) = viewModelScope.launch {
        repository.updateOffsetX(x)
    }

    fun setOffsetY(y: Int) = viewModelScope.launch {
        repository.updateOffsetY(y)
    }

    fun setShowWhenIdle(show: Boolean) = viewModelScope.launch {
        repository.updateShowWhenIdle(show)
    }

    fun setDefaultPresentationMode(mode: String) = viewModelScope.launch {
        repository.updateDefaultPresentationMode(mode)
    }

    fun setVisualizerStyle(style: String) = viewModelScope.launch {
        repository.updateVisualizerStyle(style)
    }

    fun setVisualizerSensitivity(sensitivity: Float) = viewModelScope.launch {
        repository.updateVisualizerSensitivity(sensitivity)
    }

    fun setAlbumArtCornerRadius(radius: Int) = viewModelScope.launch {
        repository.updateAlbumArtCornerRadius(radius)
    }

    fun setProgressVisibility(visible: Boolean) = viewModelScope.launch {
        repository.updateProgressVisibility(visible)
    }

    fun setAutoCollapseTimeout(timeout: Int) = viewModelScope.launch {
        repository.updateAutoCollapseTimeout(timeout)
    }

    fun setTextSize(size: String) = viewModelScope.launch {
        repository.updateTextSize(size)
    }

    fun setOverlayPosition(position: String) = viewModelScope.launch {
        repository.updateOverlayPosition(position)
    }

    fun setAlwaysOnBar(enabled: Boolean) = viewModelScope.launch {
        repository.updateAlwaysOnBar(enabled)
    }

    fun setAlwaysOnConfig(config: String) = viewModelScope.launch {
        repository.updateAlwaysOnConfig(config)
    }

    fun setTimeFormat(format: String) = viewModelScope.launch {
        repository.updateTimeFormat(format)
    }

    fun setShowSeconds(show: Boolean) = viewModelScope.launch {
        repository.updateShowSeconds(show)
    }

    fun setShowOnLockscreen(show: Boolean) = viewModelScope.launch {
        repository.updateShowOnLockscreen(show)
    }

    fun addAllowedPackage(pkg: String) = viewModelScope.launch {
        repository.addAllowedPackage(pkg)
    }

    fun removeAllowedPackage(pkg: String) = viewModelScope.launch {
        repository.removeAllowedPackage(pkg)
    }

    fun importSettings(settings: NovaSettings) = viewModelScope.launch {
        repository.importSettings(settings)
    }

    fun setCameraCutoutMode(enabled: Boolean) = viewModelScope.launch {
        repository.updateCameraCutoutMode(enabled)
    }

    fun setCameraCutoutGapScale(scale: Float) = viewModelScope.launch {
        repository.updateCameraCutoutGapScale(scale)
    }

    fun setLeftSegmentWidthDp(width: Int) = viewModelScope.launch {
        repository.updateLeftSegmentWidthDp(width)
    }

    fun setRightSegmentWidthDp(width: Int) = viewModelScope.launch {
        repository.updateRightSegmentWidthDp(width)
    }

    fun setOverlayEngine(engine: OverlayEngine) = viewModelScope.launch {
        repository.updateOverlayEngine(engine)
    }

    fun setFollowStatusBarVisibility(follow: Boolean) = viewModelScope.launch {
        repository.updateFollowStatusBarVisibility(follow)
    }

    fun setTorchEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateTorchEnabled(enabled)
    }

    fun setHotspotEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateHotspotEnabled(enabled)
    }

    fun setDebugModeEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateDebugModeEnabled(enabled)
    }

    fun setPillTextSize(size: Float) = viewModelScope.launch {
        repository.updatePillTextSize(size)
    }

    fun setAppIconMode(mode: String) = viewModelScope.launch {
        repository.updateAppIconMode(mode)
    }
}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SettingsRepository(context.applicationContext)
        return when {
            modelClass.isAssignableFrom(OverlayViewModel::class.java) -> OverlayViewModel(repo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
