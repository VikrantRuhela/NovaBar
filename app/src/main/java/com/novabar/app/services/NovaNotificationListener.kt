package com.novabar.app.services

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.novabar.app.data.SettingsRepository
import com.novabar.app.domain.*
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        private var activeController: MediaController? = null
        private var activeTimerSbn: StatusBarNotification? = null
        private var activeStopwatchSbn: StatusBarNotification? = null

        fun getActiveMediaController(): MediaController? = activeController
        fun getActiveTimerSbn(): StatusBarNotification? = activeTimerSbn
        fun getActiveStopwatchSbn(): StatusBarNotification? = activeStopwatchSbn

        fun play() {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_PLAY")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_PLAY")
            try {
                activeController?.transportControls?.play()
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_PLAY")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send play control", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_PLAY, error=${e.message}")
            }
        }

        fun pause() {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_PAUSE")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_PAUSE")
            try {
                activeController?.transportControls?.pause()
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_PAUSE")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send pause control", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_PAUSE, error=${e.message}")
            }
        }

        fun skipToNext() {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_NEXT")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_NEXT")
            try {
                activeController?.transportControls?.skipToNext()
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_NEXT")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send skipToNext control", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_NEXT, error=${e.message}")
            }
        }

        fun skipToPrevious() {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_PREVIOUS")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_PREVIOUS")
            try {
                activeController?.transportControls?.skipToPrevious()
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_PREVIOUS")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send skipToPrevious control", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_PREVIOUS, error=${e.message}")
            }
        }

        fun seekTo(posMs: Long) {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_SEEK")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_SEEK")
            try {
                activeController?.transportControls?.seekTo(posMs)
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_SEEK")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send seekTo control", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_SEEK, error=${e.message}")
            }
        }

        fun pauseTimer() {
            val success = triggerNotificationAction(activeTimerSbn, listOf("pause", "stop"), "TIMER_PAUSE")
            if (success) {
                val current = OverlayStateManager.timerState.value
                if (current != null) {
                    val liveRemaining = if (current.isRunning) {
                        (current.targetEndElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    } else {
                        current.remainingMs
                    }
                    OverlayStateManager.setTimerState(current.copy(
                        isRunning = false,
                        remainingMs = liveRemaining,
                        targetEndElapsedRealtime = 0L
                    ))
                }
            }
        }

        fun resumeTimer() {
            val success = triggerNotificationAction(activeTimerSbn, listOf("resume", "continue", "start", "play"), "TIMER_RESUME")
            if (success) {
                val current = OverlayStateManager.timerState.value
                if (current != null) {
                    val liveRemaining = if (current.isRunning) {
                        (current.targetEndElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    } else {
                        current.remainingMs
                    }
                    OverlayStateManager.setTimerState(current.copy(
                        isRunning = true,
                        targetEndElapsedRealtime = android.os.SystemClock.elapsedRealtime() + liveRemaining
                    ))
                }
            }
        }

        fun resetTimer() {
            val success = triggerNotificationAction(activeTimerSbn, listOf("reset", "restart", "delete", "dismiss", "cancel"), "TIMER_RESET")
            if (success) {
                val current = OverlayStateManager.timerState.value
                if (current != null) {
                    OverlayStateManager.setTimerState(current.copy(
                        isRunning = false,
                        remainingMs = current.durationMs,
                        targetEndElapsedRealtime = 0L
                    ))
                }
            }
        }

        fun pauseStopwatch() {
            val success = triggerNotificationAction(activeStopwatchSbn, listOf("pause", "stop"), "STOPWATCH_PAUSE")
            if (success) {
                val current = OverlayStateManager.stopwatchState.value
                if (current != null) {
                    val liveElapsed = if (current.isRunning) {
                        (android.os.SystemClock.elapsedRealtime() - current.startElapsedRealtime).coerceAtLeast(0L)
                    } else {
                        current.elapsedMs
                    }
                    OverlayStateManager.setStopwatchState(current.copy(
                        isRunning = false,
                        elapsedMs = liveElapsed,
                        startElapsedRealtime = 0L
                    ))
                }
            }
        }

        fun resumeStopwatch() {
            val success = triggerNotificationAction(activeStopwatchSbn, listOf("resume", "continue", "start", "play"), "STOPWATCH_RESUME")
            if (success) {
                val current = OverlayStateManager.stopwatchState.value
                if (current != null) {
                    val liveElapsed = if (current.isRunning) {
                        (android.os.SystemClock.elapsedRealtime() - current.startElapsedRealtime).coerceAtLeast(0L)
                    } else {
                        current.elapsedMs
                    }
                    OverlayStateManager.setStopwatchState(current.copy(
                        isRunning = true,
                        startElapsedRealtime = android.os.SystemClock.elapsedRealtime() - liveElapsed
                    ))
                }
            }
        }

        fun lapStopwatch() {
            triggerNotificationAction(activeStopwatchSbn, listOf("lap", "split"), "STOPWATCH_LAP")
        }

        private fun triggerNotificationAction(sbn: StatusBarNotification?, keywords: List<String>, buttonName: String): Boolean {
            Log.d("NovaBar", "BUTTON_CLICKED: button=$buttonName")
            if (sbn == null) {
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=$buttonName, reason=No active notification")
                return false
            }
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=$buttonName")
            val actions = sbn.notification.actions
            if (actions == null) {
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=$buttonName, reason=Notification has no actions")
                return false
            }
            for (action in actions) {
                val title = action.title.toString().lowercase()
                if (keywords.any { title.contains(it) }) {
                    try {
                        action.actionIntent.send()
                        Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=$buttonName")
                        return true
                    } catch (e: Exception) {
                        Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=$buttonName, error=${e.message}", e)
                        return false
                    }
                }
            }
            Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=$buttonName, reason=No action matched keywords $keywords")
            return false
        }

        fun toggleShuffle() {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_SHUFFLE")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_SHUFFLE")
            try {
                val controller = activeController ?: return
                val extras = try { controller.extras } catch (e: Exception) { null }
                val currentMode = extras?.getInt("android.support.v4.media.session.extra.SHUFFLE_MODE") ?: 0
                val newMode = if (currentMode == 0) 1 else 0
                
                val bundle = Bundle().apply {
                    putInt("android.support.v4.media.session.extra.SHUFFLE_MODE", newMode)
                }
                controller.sendCommand("android.support.v4.media.session.command.SET_SHUFFLE_MODE", bundle, null)
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_SHUFFLE")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to toggle shuffle", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_SHUFFLE, error=${e.message}")
            }
        }

        fun showOutputSwitcher(context: Context) {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_OUTPUT_SWITCHER")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_OUTPUT_SWITCHER")
            try {
                val controller = activeController ?: return
                val packageName = controller.packageName
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent("android.settings.MEDIA_CONTROLLER_OUTPUT_SWITCHER").apply {
                        putExtra("android.settings.extra.media_controller_package_name", packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_OUTPUT_SWITCHER")
                } else {
                    val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_OUTPUT_SWITCHER")
                }
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to show output switcher", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_OUTPUT_SWITCHER, error=${e.message}")
            }
        }

        fun adjustVolume(context: Context) {
            Log.d("NovaBar", "BUTTON_CLICKED: button=MEDIA_VOLUME")
            Log.d("NovaBar", "BUTTON_ACTION_STARTED: button=MEDIA_VOLUME")
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.adjustStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_SAME,
                    android.media.AudioManager.FLAG_SHOW_UI
                )
                Log.d("NovaBar", "BUTTON_ACTION_SUCCESS: button=MEDIA_VOLUME")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to adjust volume / show UI", e)
                Log.e("NovaBar", "BUTTON_ACTION_FAILED: button=MEDIA_VOLUME, error=${e.message}")
            }
        }

        fun getHumanReadableAppName(packageName: String, pm: PackageManager): String {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                if (label.isNotEmpty() && label != packageName) {
                    return label
                }
            } catch (e: Exception) {
                // ignore
            }
            
            val parts = packageName.split(".")
            if (parts.size >= 2) {
                val candidate = parts[parts.size - 1]
                val candidate2 = parts[parts.size - 2]
                val name = if (candidate.equals("android", ignoreCase = true) || candidate.equals("music", ignoreCase = true) || candidate.equals("app", ignoreCase = true)) {
                    candidate2
                } else {
                    candidate
                }
                return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            return packageName
        }

        fun parseTimeToMs(text: String): Long? {
            if (text.isEmpty()) return null
            val clean = text.trim()
            
            // 1. Try to extract colon-separated time: hh:mm:ss.SS, mm:ss.SS, hh:mm:ss, mm:ss anywhere in the string
            val colonRegex = "(?:\\b(\\d{1,2}):)?(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?".toRegex()
            val colonMatch = colonRegex.find(clean)
            if (colonMatch != null) {
                try {
                    val p1 = colonMatch.groupValues[1]
                    val p2 = colonMatch.groupValues[2]
                    val p3 = colonMatch.groupValues[3]
                    val p4 = colonMatch.groupValues[4]
                    
                    val hrs: Long
                    val mins: Long
                    val secs: Long
                    var ms = 0L
                    
                    if (p1.isNotEmpty()) {
                        hrs = p1.toLong()
                        mins = p2.toLong()
                        secs = p3.toLong()
                    } else {
                        hrs = 0L
                        mins = p2.toLong()
                        secs = p3.toLong()
                    }
                    
                    if (p4.isNotEmpty()) {
                        ms = when (p4.length) {
                            1 -> p4.toLong() * 100L
                            2 -> p4.toLong() * 10L
                            3 -> p4.toLong()
                            else -> 0L
                        }
                    }
                    
                    return (hrs * 3600 + mins * 60 + secs) * 1000L + ms
                } catch (e: Exception) {
                    // Ignore and try next method
                }
            }
            
            // 2. Try decimal or integer values followed by h, m, s, min, mins, minutes, second, seconds, etc.
            try {
                var totalMs = 0L
                val regex = "(\\d+(?:\\.\\d+)?)\\s*([hms])".toRegex()
                val matches = regex.findAll(clean.lowercase())
                var found = false
                for (match in matches) {
                    val value = match.groupValues[1].toDouble()
                    val unit = match.groupValues[2]
                    when (unit) {
                        "h" -> { totalMs += kotlin.math.floor(value * 3600000.0).toLong(); found = true }
                        "m" -> { totalMs += kotlin.math.floor(value * 60000.0).toLong(); found = true }
                        "s" -> { totalMs += kotlin.math.floor(value * 1000.0).toLong(); found = true }
                    }
                }
                if (found) return totalMs
            } catch (e: Exception) {
                // Ignore
            }

            // 3. Try pure decimal or integer seconds: e.g. "13.42" or "13"
            val decimalSecRegex = "^\\s*(\\d+)(?:\\.(\\d{1,3}))?\\s*$".toRegex()
            val decimalSecMatch = decimalSecRegex.find(clean)
            if (decimalSecMatch != null) {
                try {
                    val p1 = decimalSecMatch.groupValues[1]
                    val p2 = decimalSecMatch.groupValues[2]
                    val secs = p1.toLong()
                    var ms = 0L
                    if (p2.isNotEmpty()) {
                        ms = when (p2.length) {
                            1 -> p2.toLong() * 100L
                            2 -> p2.toLong() * 10L
                            3 -> p2.toLong()
                            else -> 0L
                        }
                    }
                    return secs * 1000L + ms
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            return null
        }
    }

    private val tag = "NovaNotificationListener"
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var settingsRepository: SettingsRepository
    private var activeControllers = mutableMapOf<MediaSession.Token, MediaControllerCallback>()

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        scope.launch {
            updateMediaSessions(controllers)
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        settingsRepository = SettingsRepository(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "Notification listener connected")
        DiagnosticsManager.notificationListenerConnected.value = true
        try {
            val componentName = ComponentName(this, NovaNotificationListener::class.java)
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            } catch (e: Exception) {
                Log.e(tag, "Failed to add sessions changed listener", e)
            }
            
            // Check active sessions immediately
            try {
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                updateMediaSessions(controllers)
            } catch (e: Exception) {
                Log.e(tag, "Failed to get active sessions", e)
            }
            
            // Re-query and parse existing notifications to restore state
            val activeNotifs = try {
                activeNotifications
            } catch (e: Exception) {
                null
            }
            DiagnosticsManager.notificationCount.value = activeNotifs?.size ?: 0
            activeNotifs?.forEach { sbn ->
                try {
                    onNotificationPosted(sbn)
                } catch (e: Exception) {
                    Log.e(tag, "Error posting initial notification", e)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting up active session listener", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        DiagnosticsManager.notificationListenerConnected.value = false
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            // Already removed or failed
        }
        for (callback in activeControllers.values) {
            try {
                callback.unregister()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeControllers.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // --- MEDIA SESSIONS HANDLERS ---

    private fun updateMediaSessions(controllers: List<MediaController>?) {
        val currentTokens = controllers?.map { it.sessionToken } ?: emptyList()
        
        // Remove inactive controllers
        val toRemove = activeControllers.keys.filter { it !in currentTokens }
        for (token in toRemove) {
            activeControllers[token]?.unregister()
            activeControllers.remove(token)
        }

        // Add new controllers
        controllers?.forEach { controller ->
            val token = controller.sessionToken
            if (token !in activeControllers) {
                val callback = MediaControllerCallback(controller)
                activeControllers[token] = callback
                callback.register()
            }
        }
        
        evaluateActiveMediaState()
    }

    private fun evaluateActiveMediaState() {
        val playing = activeControllers.values.firstOrNull { it.isPlaying() }
            ?: activeControllers.values.firstOrNull() // fallback to last active if none is currently playing
        
        if (playing != null) {
            activeController = playing.controller
            OverlayStateManager.mediaState.value = playing.state

            // Diagnostics
            DiagnosticsManager.mediaSessionActive.value = true
            DiagnosticsManager.mediaAppName.value = playing.state.appName
            DiagnosticsManager.mediaPackageName.value = playing.controller.packageName
            DiagnosticsManager.mediaPlaybackState.value = if (playing.isPlaying()) "Playing" else "Paused/Stopped"
            DiagnosticsManager.mediaTrackTitle.value = playing.state.title
            DiagnosticsManager.mediaArtist.value = playing.state.artist
            DiagnosticsManager.mediaProgress.value = "${((playing.state.progress) * 100).toInt()}%"
            
            // Controls Available
            val caps = playing.controller.playbackState?.actions ?: 0L
            val controls = mutableListOf<String>()
            if (caps and PlaybackState.ACTION_PLAY != 0L) controls.add("Play")
            if (caps and PlaybackState.ACTION_PAUSE != 0L) controls.add("Pause")
            if (caps and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) controls.add("Next")
            if (caps and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) controls.add("Prev")
            DiagnosticsManager.mediaControlsAvailable.value = if (controls.isEmpty()) "None" else controls.joinToString(", ")
            
            DiagnosticsManager.lastMediaUpdateTimestamp.value = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(playing.state.lastUpdateTime))
        } else {
            activeController = null
            OverlayStateManager.mediaState.value = null

            // Diagnostics
            DiagnosticsManager.mediaSessionActive.value = false
            DiagnosticsManager.mediaAppName.value = "None"
            DiagnosticsManager.mediaPackageName.value = "None"
            DiagnosticsManager.mediaPlaybackState.value = "None"
            DiagnosticsManager.mediaTrackTitle.value = "None"
            DiagnosticsManager.mediaArtist.value = "None"
            DiagnosticsManager.mediaProgress.value = "0%"
            DiagnosticsManager.mediaControlsAvailable.value = "None"
            DiagnosticsManager.lastMediaUpdateTimestamp.value = "None"
        }
    }

    private inner class MediaControllerCallback(val controller: MediaController) : MediaController.Callback() {
        var state = MediaState()
            private set

        private val callback = this
        private var updateJob: Job? = null

        fun register() {
            try {
                controller.registerCallback(callback, Handler(Looper.getMainLooper()))
                updateState()
                Log.d("NovaBar", "MEDIA_SESSION_FOUND: package=${controller.packageName}")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to register callback", e)
            }
        }

        fun unregister() {
            try {
                updateJob?.cancel()
                controller.unregisterCallback(callback)
                Log.d("NovaBar", "MEDIA_SESSION_LOST: package=${controller.packageName}")
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to unregister callback", e)
            }
        }

        fun isPlaying(): Boolean = state.isPlaying

        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            updateState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState()
        }

        private fun updateState() {
            updateJob?.cancel()
            updateJob = scope.launch(Dispatchers.IO) {
                val playbackState = try { controller.playbackState } catch (e: Exception) { null }
                val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
                val metadata = try { controller.metadata } catch (e: Exception) { null }

                val title = try { metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) } catch (e: Exception) { null } ?: ""
                val artist = try { metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) } catch (e: Exception) { null } ?: ""
                val duration = try { metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) } catch (e: Exception) { null } ?: 0L
                val position = try { playbackState?.position } catch (e: Exception) { null } ?: 0L

                val albumArt = try {
                    metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                } catch (e: Exception) {
                    null
                }

                var appIcon: android.graphics.drawable.Drawable? = null
                var appName = ""
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(controller.packageName, 0)
                    appIcon = pm.getApplicationIcon(appInfo)
                    appName = pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appName = getHumanReadableAppName(controller.packageName, packageManager)
                }

                val progress = if (duration > 0) position.toFloat() / duration else 0f
                val extras = try { controller.extras } catch (e: Exception) { null }
                
                val shuffleMode = extras?.getInt("android.support.v4.media.session.extra.SHUFFLE_MODE") ?: 0
                val isShuffle = shuffleMode == 1 || shuffleMode == 2

                val lastUpdate = playbackState?.lastPositionUpdateTime ?: android.os.SystemClock.elapsedRealtime()
                
                val newState = MediaState(
                    isPlaying = isPlaying,
                    title = title,
                    artist = artist,
                    progress = progress.coerceIn(0f, 1f),
                    duration = duration,
                    position = position,
                    albumArt = albumArt,
                    appIcon = appIcon,
                    appName = appName,
                    isShuffleEnabled = isShuffle,
                    lastUpdateTime = lastUpdate
                )

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        state = newState
                        evaluateActiveMediaState()
                        Log.d("NovaBar", "MEDIA_SESSION_UPDATED: package=${controller.packageName}, title=${state.title}, artist=${state.artist}, isPlaying=${state.isPlaying}")
                    }
                }
            }
        }
    }

    // --- NOTIFICATION POSTED/REMOVED HANDLERS ---

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch(Dispatchers.IO) {
            try {
                if (sbn.packageName != "com.google.android.apps.maps" && isScreenRecordingNotification(sbn)) {
                    Log.d("NovaBar", "SCREEN_RECORDING_NOTIFICATION_IGNORED: package=${sbn.packageName}")
                    return@launch
                }
                val packageName = sbn.packageName
                val notification = sbn.notification
                val extras = notification.extras

                // Check if it is a media notification, and if so, update active sessions immediately as a backup
                val isMedia = notification.category == Notification.CATEGORY_TRANSPORT ||
                        notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                        packageName.contains("spotify") || packageName.contains("music") || packageName.contains("poweramp")
                
                if (isMedia) {
                    val componentName = ComponentName(this@NovaNotificationListener, NovaNotificationListener::class.java)
                    try {
                        val controllers = mediaSessionManager.getActiveSessions(componentName)
                        updateMediaSessions(controllers)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to get active sessions in notification posted callback", e)
                    }
                }

                val settings = settingsRepository.settingsFlow.first()
                if (!settings.isEnabled) return@launch

                // 0. Check if Phone Call Notification
                val isCall = sbn.notification.category == Notification.CATEGORY_CALL ||
                        packageName.contains("dialer") || packageName.contains("incallui") || packageName.contains("telecom")
                
                if (isCall) {
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    
                    val actions = notification.actions
                    var isIncomingCall = false
                    if (actions != null) {
                        for (action in actions) {
                            val titleStr = action.title.toString().lowercase()
                            if (titleStr.contains("answer") || titleStr.contains("decline") || titleStr.contains("accept")) {
                                isIncomingCall = true
                            }
                        }
                    }
                    
                    val appIcon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        null
                    }

                    OverlayStateManager.phoneCallState.value = PhoneCallState(
                        isActive = true,
                        callerName = title,
                        phoneNumber = text,
                        isIncoming = isIncomingCall,
                        appIcon = appIcon,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                    return@launch
                }

                // 1. Check if Navigation Notification
                val isMapsPkg = packageName == "com.google.android.apps.maps"
                val isNav = sbn.notification.category == Notification.CATEGORY_NAVIGATION ||
                        packageName.contains("maps") || packageName.contains("waze")

                if (isNav || isMapsPkg) {
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    val category = sbn.notification.category ?: "null"
                    val extrasStr = com.novabar.app.utils.DeveloperLogger.bundleToReadableString(extras)
                    Log.i("NovaBar-Navigation", "Navigation Notification Received:\n" +
                            "Package: $packageName\n" +
                            "Category: $category\n" +
                            "Title: '$title'\n" +
                            "Text: '$text'\n" +
                            "Extras: $extrasStr\n" +
                            "isNav classification: $isNav\n" +
                            "navigationEnabled: ${settings.navigationEnabled}")
                }

                if (isNav && settings.navigationEnabled) {
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    val eta = extras.getCharSequence("android.car.EXTENSIONS")?.toString() ?: "" // Fallback
                    
                    val largeIcon = notification.getLargeIcon() ?: notification.smallIcon
                    val drawable = try {
                        largeIcon?.loadDrawable(this@NovaNotificationListener)
                    } catch (e: Exception) {
                        null
                    }

                    val maneuverType = com.novabar.app.utils.NavigationCompatibilityLayer.parse(sbn, this@NovaNotificationListener)

                    val appLabel = try {
                        val pm = packageManager
                        val info = pm.getApplicationInfo(packageName, 0)
                        pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {
                        "Navigation"
                    }

                    Log.i("NovaBar-Navigation", "Classification SUCCESS: Registering navigation activity with maneuver: $maneuverType from app: $appLabel")

                    OverlayStateManager.navigationState.value = NavigationState(
                        maneuverInstruction = title,
                        distanceRemaining = text,
                        eta = eta,
                        maneuverIcon = drawable,
                        maneuverType = maneuverType,
                        appName = appLabel
                    )
                    return@launch
                } else if (isNav || isMapsPkg) {
                    val reason = if (!settings.navigationEnabled) "Navigation disabled in settings" else "Not classified as navigation category/package"
                    Log.i("NovaBar-Navigation", "Classification REJECTED: $reason")
                }

                // 2. Check if Clock app (Timer or Stopwatch)
                val isClock = packageName.contains("clock", ignoreCase = true) || 
                        packageName.contains("deskclock", ignoreCase = true) || 
                        packageName.contains("alarm", ignoreCase = true)
                val showChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
                val actions = notification.actions ?: emptyArray()
                val actionTitles = actions.map { it.title.toString().lowercase() }
                val hasClockActions = actionTitles.any { 
                    it.contains("lap") || it.contains("split") || it.contains("+1") || 
                    it.contains("pause") || it.contains("resume") || it.contains("stop") ||
                    it.contains("start") || it.contains("reset")
                }
                
                val isClockLike = isClock || showChronometer || hasClockActions
                if (isClockLike) {
                    when (val clockState = com.novabar.app.utils.ClockCompatibilityLayer.parse(sbn, settings, this@NovaNotificationListener)) {
                        is com.novabar.app.utils.ParsedClockState.Stopwatch -> {
                            activeStopwatchSbn = sbn
                            OverlayStateManager.setStopwatchState(clockState.state)
                            return@launch
                        }
                        is com.novabar.app.utils.ParsedClockState.Timer -> {
                            activeTimerSbn = sbn
                            OverlayStateManager.setTimerState(clockState.state)
                            return@launch
                        }
                        com.novabar.app.utils.ParsedClockState.None -> {
                            // Do nothing, let it flow to other notification handlers
                        }
                    }
                }

                // 3. Fallback: High Priority Notification
                if (settings.notificationsEnabled) {
                    // If package filter is enabled, check if app is allowed
                    if (settings.allowedNotificationPackages.isNotEmpty() && packageName !in settings.allowedNotificationPackages) {
                        return@launch
                    }

                    val ranking = Ranking()
                    if (currentRanking.getRanking(sbn.key, ranking)) {
                        val importance = ranking.importance
                        val isHighPriority = importance >= android.app.NotificationManager.IMPORTANCE_HIGH || 
                                (notification.flags and Notification.FLAG_HIGH_PRIORITY) != 0
                        
                        if (isHighPriority) {
                            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                            
                            // Ignore media notifications since they are handled separately
                            val isMedia = notification.category == Notification.CATEGORY_TRANSPORT ||
                                    notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                            
                            if (isMedia) return@launch

                            val appIcon = try {
                                packageManager.getApplicationIcon(packageName)
                            } catch (e: Exception) {
                                null
                            }

                            val appName = getHumanReadableAppName(packageName, packageManager)

                            val actions = notification.actions?.mapIndexed { index, action ->
                                NotificationAction(action.title.toString(), index)
                            } ?: emptyList()

                            val state = NotificationState(
                                id = sbn.key,
                                packageName = packageName,
                                appIcon = appIcon,
                                appName = appName,
                                title = title,
                                summary = text,
                                actions = actions
                            )
                            OverlayStateManager.updateNotification(state)
                        }
                    }
                }
                val activeNotifs = try { activeNotifications } catch (ex: Exception) { null }
                DiagnosticsManager.notificationCount.value = activeNotifs?.size ?: 0
                val appLabel = try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (ex: Exception) {
                    packageName
                }
                DiagnosticsManager.lastNotificationApp.value = appLabel
                DiagnosticsManager.lastNotificationPackage.value = packageName
            } catch (e: Exception) {
                Log.e(tag, "Error handling posted notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        scope.launch(Dispatchers.IO) {
            try {
                if (sbn.packageName != "com.google.android.apps.maps" && isScreenRecordingNotification(sbn)) {
                    Log.d("NovaBar", "SCREEN_RECORDING_NOTIFICATION_REMOVED_IGNORED: package=${sbn.packageName}")
                    return@launch
                }
                val packageName = sbn.packageName
                val currentNotification = OverlayStateManager.activeState.value
                
                // Clear call state if incall notification is removed
                val isCall = sbn.notification.category == Notification.CATEGORY_CALL ||
                        packageName.contains("dialer") || packageName.contains("incallui") || packageName.contains("telecom")
                if (isCall) {
                    OverlayStateManager.phoneCallState.value = null
                }
                
                // Clear notification mode if active one is removed
                if (currentNotification is OverlayState.Notification && currentNotification.data.id == sbn.key) {
                    OverlayStateManager.updateNotification(null)
                }

                // Clear navigation state if Maps notification removed
                val isNav = sbn.notification.category == Notification.CATEGORY_NAVIGATION ||
                        packageName.contains("maps") || packageName.contains("waze")
                if (isNav) {
                    Log.i("NovaBar-Navigation", "Activity removal: removing active navigation state because notification was removed (package: $packageName)")
                    OverlayStateManager.navigationState.value = null
                }

                // Clear timer/stopwatch if clock notification removed
                if (activeTimerSbn?.key == sbn.key) {
                    Log.i("NovaBar-Timer", "Activity removal: removing active timer state because notification was removed.")
                    activeTimerSbn = null
                    OverlayStateManager.setTimerState(null)
                }
                if (activeStopwatchSbn?.key == sbn.key) {
                    Log.i("NovaBar-Stopwatch", "Activity removal: removing active stopwatch state because notification was removed.")
                    activeStopwatchSbn = null
                    OverlayStateManager.setStopwatchState(null)
                }
                val activeNotifs = try { activeNotifications } catch (ex: Exception) { null }
                DiagnosticsManager.notificationCount.value = activeNotifs?.size ?: 0
            } catch (e: Exception) {
                Log.e(tag, "Error handling removed notification", e)
            }
        }
    }

    private fun isScreenRecordingNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName.lowercase()
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
        
        val matchesPkg = packageName == "com.samsung.android.app.smartcapture" ||
                         packageName == "com.miui.screenrecorder" ||
                         packageName == "com.xiaomi.screenrecorder" ||
                         packageName == "com.android.systemui"
                         
        val matchesKeywords = title.contains("screen recording") ||
                             title.contains("recording screen") ||
                             title.contains("recording in progress") ||
                             title.contains("stop recording") ||
                             text.contains("screen recording") ||
                             text.contains("recording screen") ||
                             text.contains("recording in progress") ||
                             text.contains("stop recording")
                             
        return matchesPkg && matchesKeywords
    }
}
