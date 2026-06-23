package com.novabar.app.domain

import kotlinx.coroutines.flow.MutableStateFlow

object DiagnosticsManager {
    @Volatile
    var expandClickTime = 0L

    // GENERAL SECTION
    val overlayEngine = MutableStateFlow("Unknown")
    val windowType = MutableStateFlow("Unknown")
    val touchableState = MutableStateFlow("Unknown")
    val currentActivity = MutableStateFlow("Unknown")
    val currentPresentationState = MutableStateFlow("Unknown")
    val blurEnabled = MutableStateFlow(false)
    val blurBackend = MutableStateFlow("None")
    val windowWidth = MutableStateFlow(0)
    val windowHeight = MutableStateFlow(0)
    val touchEventsReceived = MutableStateFlow(0L)

    // MEDIA SECTION
    val mediaSessionActive = MutableStateFlow(false)
    val mediaAppName = MutableStateFlow("None")
    val mediaPackageName = MutableStateFlow("None")
    val mediaPlaybackState = MutableStateFlow("None")
    val mediaTrackTitle = MutableStateFlow("None")
    val mediaArtist = MutableStateFlow("None")
    val mediaProgress = MutableStateFlow("0%")
    val mediaControlsAvailable = MutableStateFlow("None")
    val lastMediaUpdateTimestamp = MutableStateFlow("None")

    // NOTIFICATION SECTION
    val notificationListenerConnected = MutableStateFlow(false)
    val lastNotificationApp = MutableStateFlow("None")
    val lastNotificationPackage = MutableStateFlow("None")
    val notificationCount = MutableStateFlow(0)
    val notificationPermissionStatus = MutableStateFlow("Unknown")

    // ACCESSIBILITY SECTION
    val accessibilityServiceEnabled = MutableStateFlow(false)
    val accessibilityOverlayActive = MutableStateFlow(false)
    val lastAccessibilityEvent = MutableStateFlow("None")
    val accessibilityForegroundPackage = MutableStateFlow("Unknown")

    // CUTOUT SECTION
    val hasDisplayCutout = MutableStateFlow(false)
    val cutoutWidth = MutableStateFlow(0)
    val cutoutCenterX = MutableStateFlow(0)
    val cameraCutoutModeEnabled = MutableStateFlow(false)

    // OVERLAY SECTION
    val overlayVisible = MutableStateFlow(false)
    val overlayAttached = MutableStateFlow(false)
    val currentOverlayBounds = MutableStateFlow("0, 0, 0, 0")
    val currentPriorityActivity = MutableStateFlow("None")
    val resetTrigger = MutableStateFlow(0)
    val windowX = MutableStateFlow(0)
    val windowY = MutableStateFlow(0)
    val showDebugMarkers = MutableStateFlow(false)

    fun incrementTouchEvents() {
        touchEventsReceived.value += 1
    }
}
