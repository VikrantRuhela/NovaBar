package com.novabar.app.domain

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class MediaState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val progress: Float = 0f, // 0f to 1f
    val duration: Long = 0L,  // in ms
    val position: Long = 0L,  // in ms
    val albumArt: Bitmap? = null,
    val appIcon: Drawable? = null,
    val appName: String = "",
    val isShuffleEnabled: Boolean = false,
    val lastUpdateTime: Long = 0L
)

data class TimerState(
    val isRunning: Boolean = false,
    val durationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val label: String = "",
    val hasPause: Boolean = false,
    val hasResume: Boolean = false,
    val hasReset: Boolean = false,
    val targetEndElapsedRealtime: Long = 0L,
    val showSeconds: Boolean = false
)

data class StopwatchState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L,
    val hasPause: Boolean = false,
    val hasResume: Boolean = false,
    val hasLap: Boolean = false,
    val startElapsedRealtime: Long = 0L,
    val showSeconds: Boolean = false
)


data class NavigationState(
    val maneuverInstruction: String = "",
    val distanceRemaining: String = "",
    val eta: String = "",
    val maneuverIcon: Drawable? = null
)

data class ChargingState(
    val isCharging: Boolean = false,
    val batteryPercentage: Int = 0,
    val speed: String = "",       // "Fast", "Super Fast", "Normal"
    val temperature: Float = 0f  // in Celsius
)

data class NotificationState(
    val id: String = "",
    val packageName: String = "",
    val appIcon: Drawable? = null,
    val appName: String = "",
    val title: String = "",
    val summary: String = "",
    val actions: List<NotificationAction> = emptyList()
)

data class NotificationAction(
    val title: String,
    val actionId: Int
)

data class PhoneCallState(
    val isActive: Boolean = false,
    val callerName: String = "",
    val phoneNumber: String = "",
    val durationMs: Long = 0L,
    val isIncoming: Boolean = false,
    val appIcon: Drawable? = null,
    val lastUpdateTime: Long = 0L
)

sealed class OverlayState {
    object Idle : OverlayState()
    data class Charging(val data: ChargingState) : OverlayState()
    data class Notification(val data: NotificationState) : OverlayState()
    data class Timer(val data: TimerState) : OverlayState()
    data class Stopwatch(val data: StopwatchState) : OverlayState()
    data class Navigation(val data: NavigationState) : OverlayState()
    data class Media(val data: MediaState) : OverlayState()
    data class PhoneCall(val data: PhoneCallState) : OverlayState()
}
