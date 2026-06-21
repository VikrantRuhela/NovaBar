package com.novabar.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_settings")

data class NovaSettings(
    val isEnabled: Boolean = true,
    val positionY: Int = 12, // Legacy position Y
    val cornerRadius: Int = 24, // dp
    val opacity: Float = 0.85f,
    val blurRadius: Int = 25, // px
    val animationSpeedMultiplier: Float = 1.0f,
    val mediaControlsEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val stopwatchEnabled: Boolean = true,
    val navigationEnabled: Boolean = true,
    val chargingEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val colorAdaptationEnabled: Boolean = true,
    val allowedNotificationPackages: Set<String> = emptySet(),
    
    // LAYOUT SETTINGS:
    val barWidthScale: Float = 1.0f,
    val barHeightPadding: Int = 0, // dp
    val barBorderThickness: Int = 1, // dp
    val barGravity: String = "Center", // "Center", "Left", "Right"
    val offsetX: Int = 0, // dp
    val offsetY: Int = 12, // dp
    val showWhenIdle: Boolean = true,

    // NOW BAR STATE SYSTEM SETTINGS:
    val defaultPresentationMode: String = "Compact", // "Minimized", "Compact"
    val visualizerStyle: String = "Waveform", // "Waveform", "Pulse", "Minimal"
    val visualizerSensitivity: Float = 1.0f,
    val albumArtCornerRadius: Int = 8, // dp
    val progressVisibility: Boolean = true,
    val autoCollapseTimeout: Int = 5, // seconds
    val textSize: String = "Default",
    val overlayPosition: String = "Below Status Bar",
    val alwaysOnBar: Boolean = false,
    val alwaysOnConfig: String = "Time • Battery",
    val timeFormat: String = "System Default",
    val showSeconds: Boolean = false
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val IS_ENABLED = booleanPreferencesKey("is_enabled")
        private val POSITION_Y = intPreferencesKey("position_y")
        private val CORNER_RADIUS = intPreferencesKey("corner_radius")
        private val OPACITY = floatPreferencesKey("opacity")
        private val BLUR_RADIUS = intPreferencesKey("blur_radius")
        private val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        private val MEDIA_ENABLED = booleanPreferencesKey("media_enabled")
        private val TIMER_ENABLED = booleanPreferencesKey("timer_enabled")
        private val STOPWATCH_ENABLED = booleanPreferencesKey("stopwatch_enabled")
        private val NAVIGATION_ENABLED = booleanPreferencesKey("navigation_enabled")
        private val CHARGING_ENABLED = booleanPreferencesKey("charging_enabled")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val COLOR_ADAPTATION_ENABLED = booleanPreferencesKey("color_adaptation_enabled")
        private val ALLOWED_PACKAGES = stringSetPreferencesKey("allowed_packages")
        
        // LAYOUT KEYS:
        private val BAR_WIDTH_SCALE = floatPreferencesKey("bar_width_scale")
        private val BAR_HEIGHT_PADDING = intPreferencesKey("bar_height_padding")
        private val BAR_BORDER_THICKNESS = intPreferencesKey("bar_border_thickness")
        private val BAR_GRAVITY = stringPreferencesKey("bar_gravity")
        private val OFFSET_X = intPreferencesKey("offset_x")
        private val OFFSET_Y = intPreferencesKey("offset_y")
        private val SHOW_WHEN_IDLE = booleanPreferencesKey("show_when_idle")

        // STATE SYSTEM KEYS:
        private val DEFAULT_PRESENTATION_MODE = stringPreferencesKey("default_presentation_mode")
        private val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        private val VISUALIZER_SENSITIVITY = floatPreferencesKey("visualizer_sensitivity")
        private val ALBUM_ART_CORNER_RADIUS = intPreferencesKey("album_art_corner_radius")
        private val PROGRESS_VISIBILITY = booleanPreferencesKey("progress_visibility")
        private val AUTO_COLLAPSE_TIMEOUT = intPreferencesKey("auto_collapse_timeout")
        private val TEXT_SIZE = stringPreferencesKey("text_size")
        private val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        private val ALWAYS_ON_BAR = booleanPreferencesKey("always_on_bar")
        private val ALWAYS_ON_CONFIG = stringPreferencesKey("always_on_config")
        private val TIME_FORMAT = stringPreferencesKey("time_format")
        private val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
    }

    val settingsFlow: Flow<NovaSettings> = context.dataStore.data.map { preferences ->
        NovaSettings(
            isEnabled = preferences[IS_ENABLED] ?: true,
            positionY = preferences[POSITION_Y] ?: 12,
            cornerRadius = preferences[CORNER_RADIUS] ?: 24,
            opacity = preferences[OPACITY] ?: 0.85f,
            blurRadius = preferences[BLUR_RADIUS] ?: 25,
            animationSpeedMultiplier = preferences[ANIMATION_SPEED] ?: 1.0f,
            mediaControlsEnabled = preferences[MEDIA_ENABLED] ?: true,
            timerEnabled = preferences[TIMER_ENABLED] ?: true,
            stopwatchEnabled = preferences[STOPWATCH_ENABLED] ?: true,
            navigationEnabled = preferences[NAVIGATION_ENABLED] ?: true,
            chargingEnabled = preferences[CHARGING_ENABLED] ?: true,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            colorAdaptationEnabled = preferences[COLOR_ADAPTATION_ENABLED] ?: true,
            allowedNotificationPackages = preferences[ALLOWED_PACKAGES] ?: emptySet(),
            
            barWidthScale = preferences[BAR_WIDTH_SCALE] ?: 1.0f,
            barHeightPadding = preferences[BAR_HEIGHT_PADDING] ?: 0,
            barBorderThickness = preferences[BAR_BORDER_THICKNESS] ?: 1,
            barGravity = preferences[BAR_GRAVITY] ?: "Center",
            offsetX = preferences[OFFSET_X] ?: 0,
            offsetY = preferences[OFFSET_Y] ?: (preferences[POSITION_Y] ?: 12),
            showWhenIdle = preferences[SHOW_WHEN_IDLE] ?: true,

            defaultPresentationMode = preferences[DEFAULT_PRESENTATION_MODE] ?: "Compact",
            visualizerStyle = preferences[VISUALIZER_STYLE] ?: "Waveform",
            visualizerSensitivity = preferences[VISUALIZER_SENSITIVITY] ?: 1.0f,
            albumArtCornerRadius = preferences[ALBUM_ART_CORNER_RADIUS] ?: 8,
            progressVisibility = preferences[PROGRESS_VISIBILITY] ?: true,
            autoCollapseTimeout = preferences[AUTO_COLLAPSE_TIMEOUT] ?: 5,
            textSize = preferences[TEXT_SIZE] ?: "Default",
            overlayPosition = preferences[OVERLAY_POSITION] ?: "Below Status Bar",
            alwaysOnBar = preferences[ALWAYS_ON_BAR] ?: false,
            alwaysOnConfig = preferences[ALWAYS_ON_CONFIG] ?: "Time • Battery",
            timeFormat = preferences[TIME_FORMAT] ?: "System Default",
            showSeconds = preferences[SHOW_SECONDS] ?: false
        )
    }

    suspend fun updateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_ENABLED] = enabled }
    }

    suspend fun updatePositionY(y: Int) {
        context.dataStore.edit { 
            it[POSITION_Y] = y 
            it[OFFSET_Y] = y 
        }
    }

    suspend fun updateCornerRadius(radius: Int) {
        context.dataStore.edit { it[CORNER_RADIUS] = radius }
    }

    suspend fun updateOpacity(opacity: Float) {
        context.dataStore.edit { it[OPACITY] = opacity }
    }

    suspend fun updateBlurRadius(radius: Int) {
        context.dataStore.edit { it[BLUR_RADIUS] = radius }
    }

    suspend fun updateAnimationSpeed(speed: Float) {
        context.dataStore.edit { it[ANIMATION_SPEED] = speed }
    }

    suspend fun updateMediaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MEDIA_ENABLED] = enabled }
    }

    suspend fun updateTimerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TIMER_ENABLED] = enabled }
    }

    suspend fun updateStopwatchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[STOPWATCH_ENABLED] = enabled }
    }

    suspend fun updateNavigationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NAVIGATION_ENABLED] = enabled }
    }

    suspend fun updateChargingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CHARGING_ENABLED] = enabled }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateColorAdaptationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[COLOR_ADAPTATION_ENABLED] = enabled }
    }

    suspend fun updateBarWidthScale(scale: Float) {
        context.dataStore.edit { it[BAR_WIDTH_SCALE] = scale }
    }

    suspend fun updateBarHeightPadding(padding: Int) {
        context.dataStore.edit { it[BAR_HEIGHT_PADDING] = padding }
    }

    suspend fun updateBarBorderThickness(thickness: Int) {
        context.dataStore.edit { it[BAR_BORDER_THICKNESS] = thickness }
    }

    suspend fun updateBarGravity(gravity: String) {
        context.dataStore.edit { it[BAR_GRAVITY] = gravity }
    }

    suspend fun updateOffsetX(x: Int) {
        context.dataStore.edit { it[OFFSET_X] = x }
    }

    suspend fun updateOffsetY(y: Int) {
        context.dataStore.edit { 
            it[OFFSET_Y] = y 
            it[POSITION_Y] = y 
        }
    }

    suspend fun updateShowWhenIdle(show: Boolean) {
        context.dataStore.edit { it[SHOW_WHEN_IDLE] = show }
    }

    suspend fun updateDefaultPresentationMode(mode: String) {
        context.dataStore.edit { it[DEFAULT_PRESENTATION_MODE] = mode }
    }

    suspend fun updateVisualizerStyle(style: String) {
        context.dataStore.edit { it[VISUALIZER_STYLE] = style }
    }

    suspend fun updateVisualizerSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[VISUALIZER_SENSITIVITY] = sensitivity }
    }

    suspend fun updateAlbumArtCornerRadius(radius: Int) {
        context.dataStore.edit { it[ALBUM_ART_CORNER_RADIUS] = radius }
    }

    suspend fun updateProgressVisibility(visible: Boolean) {
        context.dataStore.edit { it[PROGRESS_VISIBILITY] = visible }
    }

    suspend fun updateAutoCollapseTimeout(timeout: Int) {
        context.dataStore.edit { it[AUTO_COLLAPSE_TIMEOUT] = timeout }
    }

    suspend fun updateTextSize(size: String) {
        context.dataStore.edit { it[TEXT_SIZE] = size }
    }

    suspend fun updateOverlayPosition(position: String) {
        context.dataStore.edit { it[OVERLAY_POSITION] = position }
    }

    suspend fun updateAlwaysOnBar(enabled: Boolean) {
        context.dataStore.edit { it[ALWAYS_ON_BAR] = enabled }
    }

    suspend fun updateAlwaysOnConfig(config: String) {
        context.dataStore.edit { it[ALWAYS_ON_CONFIG] = config }
    }

    suspend fun updateTimeFormat(format: String) {
        context.dataStore.edit { it[TIME_FORMAT] = format }
    }

    suspend fun updateShowSeconds(show: Boolean) {
        context.dataStore.edit { it[SHOW_SECONDS] = show }
    }

    suspend fun addAllowedPackage(pkg: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[ALLOWED_PACKAGES] ?: emptySet()
            preferences[ALLOWED_PACKAGES] = current + pkg
        }
    }

    suspend fun removeAllowedPackage(pkg: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[ALLOWED_PACKAGES] ?: emptySet()
            preferences[ALLOWED_PACKAGES] = current - pkg
        }
    }

    suspend fun importSettings(s: NovaSettings) {
        context.dataStore.edit { preferences ->
            preferences[IS_ENABLED] = s.isEnabled
            preferences[POSITION_Y] = s.positionY
            preferences[CORNER_RADIUS] = s.cornerRadius
            preferences[OPACITY] = s.opacity
            preferences[BLUR_RADIUS] = s.blurRadius
            preferences[ANIMATION_SPEED] = s.animationSpeedMultiplier
            preferences[MEDIA_ENABLED] = s.mediaControlsEnabled
            preferences[TIMER_ENABLED] = s.timerEnabled
            preferences[STOPWATCH_ENABLED] = s.stopwatchEnabled
            preferences[NAVIGATION_ENABLED] = s.navigationEnabled
            preferences[CHARGING_ENABLED] = s.chargingEnabled
            preferences[NOTIFICATIONS_ENABLED] = s.notificationsEnabled
            preferences[COLOR_ADAPTATION_ENABLED] = s.colorAdaptationEnabled
            preferences[ALLOWED_PACKAGES] = s.allowedNotificationPackages
            
            preferences[BAR_WIDTH_SCALE] = s.barWidthScale
            preferences[BAR_HEIGHT_PADDING] = s.barHeightPadding
            preferences[BAR_BORDER_THICKNESS] = s.barBorderThickness
            preferences[BAR_GRAVITY] = s.barGravity
            preferences[OFFSET_X] = s.offsetX
            preferences[OFFSET_Y] = s.offsetY
            preferences[SHOW_WHEN_IDLE] = s.showWhenIdle

            preferences[DEFAULT_PRESENTATION_MODE] = s.defaultPresentationMode
            preferences[VISUALIZER_STYLE] = s.visualizerStyle
            preferences[VISUALIZER_SENSITIVITY] = s.visualizerSensitivity
            preferences[ALBUM_ART_CORNER_RADIUS] = s.albumArtCornerRadius
            preferences[PROGRESS_VISIBILITY] = s.progressVisibility
            preferences[AUTO_COLLAPSE_TIMEOUT] = s.autoCollapseTimeout
            
            preferences[TEXT_SIZE] = s.textSize
            preferences[OVERLAY_POSITION] = s.overlayPosition
            preferences[ALWAYS_ON_BAR] = s.alwaysOnBar
            preferences[ALWAYS_ON_CONFIG] = s.alwaysOnConfig
            preferences[TIME_FORMAT] = s.timeFormat
            preferences[SHOW_SECONDS] = s.showSeconds
        }
    }
}
