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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        private var activeController: MediaController? = null

        fun getActiveMediaController(): MediaController? = activeController

        fun play() {
            try {
                activeController?.transportControls?.play()
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send play control", e)
            }
        }

        fun pause() {
            try {
                activeController?.transportControls?.pause()
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send pause control", e)
            }
        }

        fun skipToNext() {
            try {
                activeController?.transportControls?.skipToNext()
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send skipToNext control", e)
            }
        }

        fun skipToPrevious() {
            try {
                activeController?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send skipToPrevious control", e)
            }
        }

        fun seekTo(posMs: Long) {
            try {
                activeController?.transportControls?.seekTo(posMs)
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to send seekTo control", e)
            }
        }

        fun toggleShuffle() {
            try {
                val controller = activeController ?: return
                val extras = try { controller.extras } catch (e: Exception) { null }
                val currentMode = extras?.getInt("android.support.v4.media.session.extra.SHUFFLE_MODE") ?: 0
                val newMode = if (currentMode == 0) 1 else 0
                
                val bundle = Bundle().apply {
                    putInt("android.support.v4.media.session.extra.SHUFFLE_MODE", newMode)
                }
                controller.sendCommand("android.support.v4.media.session.command.SET_SHUFFLE_MODE", bundle, null)
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to toggle shuffle", e)
            }
        }

        fun showOutputSwitcher(context: Context) {
            try {
                val controller = activeController ?: return
                val packageName = controller.packageName
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent("android.settings.MEDIA_CONTROLLER_OUTPUT_SWITCHER").apply {
                        putExtra("android.settings.extra.media_controller_package_name", packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } else {
                    val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to show output switcher", e)
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
            
            val parts = clean.split(":")
            if (parts.size in 2..3) {
                try {
                    var secs = 0L
                    var mins = 0L
                    var hrs = 0L
                    if (parts.size == 2) {
                        mins = parts[0].toLong()
                        val secPart = parts[1].split(".")[0]
                        secs = secPart.toLong()
                    } else {
                        hrs = parts[0].toLong()
                        mins = parts[1].toLong()
                        val secPart = parts[2].split(".")[0]
                        secs = secPart.toLong()
                    }
                    return (hrs * 3600 + mins * 60 + secs) * 1000L
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            try {
                var totalMs = 0L
                val regex = "(\\d+)\\s*([hms])".toRegex()
                val matches = regex.findAll(clean.lowercase())
                var found = false
                for (match in matches) {
                    val value = match.groupValues[1].toLong()
                    val unit = match.groupValues[2]
                    when (unit) {
                        "h" -> { totalMs += value * 3600000L; found = true }
                        "m" -> { totalMs += value * 60000L; found = true }
                        "s" -> { totalMs += value * 1000L; found = true }
                    }
                }
                if (found) return totalMs
            } catch (e: Exception) {
                // Ignore
            }
            
            return null
        }
    }

    private val tag = "NovaNotificationListener"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
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
        } else {
            activeController = null
            OverlayStateManager.mediaState.value = null
        }
    }

    private inner class MediaControllerCallback(val controller: MediaController) : MediaController.Callback() {
        var state = MediaState()
            private set

        private val callback = this

        fun register() {
            try {
                controller.registerCallback(callback)
                updateState()
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to register callback", e)
            }
        }

        fun unregister() {
            try {
                controller.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e("NovaNotificationListener", "Failed to unregister callback", e)
            }
        }

        fun isPlaying(): Boolean = state.isPlaying

        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            updateState()
            evaluateActiveMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState()
            evaluateActiveMediaState()
        }

        private fun updateState() {
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

            state = MediaState(
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
                lastUpdateTime = System.currentTimeMillis()
            )
        }
    }

    // --- NOTIFICATION POSTED/REMOVED HANDLERS ---

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                if (!settings.isEnabled) return@launch

                val packageName = sbn.packageName
                val notification = sbn.notification
                val extras = notification.extras

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
                val isNav = sbn.notification.category == Notification.CATEGORY_NAVIGATION ||
                        packageName.contains("maps") || packageName.contains("waze")
                
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

                    OverlayStateManager.navigationState.value = NavigationState(
                        maneuverInstruction = title,
                        distanceRemaining = text,
                        eta = eta,
                        maneuverIcon = drawable
                    )
                    return@launch
                }

                // 2. Check if Clock app (Timer or Stopwatch)
                val isClock = packageName.contains("clock") || packageName.contains("deskclock") || packageName.contains("alarm")
                if (isClock) {
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                    val actions = notification.actions ?: emptyArray()
                    val actionTitles = actions.map { it.title.toString().lowercase() }
                    
                    val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        extras.getBoolean("android.chronometerCountDown", false)
                    } else {
                        false
                    }

                    val isStopwatch = title.lowercase().contains("stopwatch") || 
                            text.lowercase().contains("stopwatch") || 
                            actionTitles.any { it.contains("lap") } ||
                            (actionTitles.contains("pause") && actionTitles.contains("reset") && !actionTitles.contains("+1"))
                    
                    val isTimer = title.lowercase().contains("timer") || 
                            text.lowercase().contains("timer") || 
                            isCountDown ||
                            actionTitles.any { it.contains("+1") || it.contains("add") || it.contains("cancel") }
                    
                    val whenTime = notification.`when`
                    val showChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
                    
                    if (isStopwatch && settings.stopwatchEnabled) {
                        val isRunning = actionTitles.contains("pause") || actionTitles.contains("lap")
                        var elapsedMs = 0L
                        if (showChronometer && whenTime > 0) {
                            elapsedMs = System.currentTimeMillis() - whenTime
                        } else {
                            elapsedMs = parseTimeToMs(text) ?: parseTimeToMs(title) ?: 0L
                        }
                        OverlayStateManager.stopwatchState.value = StopwatchState(
                            isRunning = isRunning,
                            elapsedMs = elapsedMs
                        )
                        return@launch
                    } else if (isTimer && settings.timerEnabled) {
                        val isRunning = actionTitles.contains("pause") || actionTitles.contains("+1") || actionTitles.contains("add")
                        var remainingMs = 0L
                        if (showChronometer && whenTime > 0) {
                            remainingMs = whenTime - System.currentTimeMillis()
                        } else {
                            remainingMs = parseTimeToMs(text) ?: parseTimeToMs(title) ?: 0L
                        }
                        OverlayStateManager.timerState.value = TimerState(
                            isRunning = isRunning,
                            durationMs = if (remainingMs > 0) remainingMs else 0L,
                            remainingMs = if (remainingMs > 0) remainingMs else 0L,
                            label = if (title.lowercase().contains("timer")) text else title
                        )
                        return@launch
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
            } catch (e: Exception) {
                Log.e(tag, "Error handling posted notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        scope.launch {
            try {
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
                    OverlayStateManager.navigationState.value = null
                }

                // Clear timer/stopwatch if clock notification removed
                val isClock = packageName.contains("clock") || packageName.contains("deskclock") || packageName.contains("alarm")
                if (isClock) {
                    OverlayStateManager.timerState.value = null
                    OverlayStateManager.stopwatchState.value = null
                }
            } catch (e: Exception) {
                Log.e(tag, "Error handling removed notification", e)
            }
        }
    }
}
