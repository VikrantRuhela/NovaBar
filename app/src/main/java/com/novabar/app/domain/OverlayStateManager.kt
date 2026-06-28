package com.novabar.app.domain

import android.graphics.Rect
import android.util.Log
import android.content.Context
import android.telecom.TelecomManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import com.novabar.app.services.NovaAccessibilityService
import com.novabar.app.data.NovaSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object OverlayStateManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val settingsFlow = MutableStateFlow(NovaSettings())

    init {
        scope.launch {
            settingsFlow.collectLatest { settings ->
                DiagnosticsManager.showDebugMarkers.value = settings.debugModeEnabled
            }
        }
    }

    val pillBounds = MutableStateFlow(Rect())
    val leftPillBounds = MutableStateFlow(Rect())
    val rightPillBounds = MutableStateFlow(Rect())

    fun updatePillBounds(rect: Rect) {
        pillBounds.value = rect
    }

    val mediaState = MutableStateFlow<MediaState?>(null)
    val timerState = MutableStateFlow<TimerState?>(null)
    val stopwatchState = MutableStateFlow<StopwatchState?>(null)
    val navigationState = MutableStateFlow<NavigationState?>(null)
    val phoneCallState = MutableStateFlow<PhoneCallState?>(null)
    val torchState = MutableStateFlow<TorchState?>(null)
    val hotspotState = MutableStateFlow<HotspotState?>(null)
    
    private val _chargingState = MutableStateFlow<ChargingState?>(null)
    private var chargingJob: Job? = null
    private val isChargingActive = MutableStateFlow(false)
    
    private val _notificationState = MutableStateFlow<NotificationState?>(null)
    private var notificationJob: Job? = null
    private val isNotificationActive = MutableStateFlow(false)

    // Global expanded state flow
    val isExpanded = MutableStateFlow(false)
    val expandedActivityKey = MutableStateFlow<String?>(null)
    val windowMode = MutableStateFlow("Compact")
    val systemBarVisible = MutableStateFlow(true)

    fun expand() {
        Log.d("NovaBar", "EXPAND_REQUEST")
        windowMode.value = "Expanded"
        isExpanded.value = true
    }

    fun collapse() {
        Log.d("NovaBar", "COLLAPSE_REQUEST")
        isExpanded.value = false
        expandedActivityKey.value = null
    }

    fun toggleExpanded() {
        if (isExpanded.value) {
            Log.d("NovaBar", "COLLAPSE_REQUEST")
            isExpanded.value = false
            expandedActivityKey.value = null
        } else {
            Log.d("NovaBar", "EXPAND_REQUEST")
            windowMode.value = "Expanded"
            isExpanded.value = true
        }
    }

    fun updateCharging(state: ChargingState) {
        _chargingState.value = state
        if (state.isCharging) {
            isChargingActive.value = true
            chargingJob?.cancel()
            chargingJob = scope.launch {
                delay(5000L) // Show for 5 seconds
                if (!isExpanded.value) {
                    isChargingActive.value = false
                }
            }
        } else {
            isChargingActive.value = false
        }
    }

    fun updateNotification(state: NotificationState?) {
        _notificationState.value = state
        if (state != null) {
            isNotificationActive.value = true
            notificationJob?.cancel()
            notificationJob = scope.launch {
                delay(7000L) // Show for 7 seconds
                if (!isExpanded.value) {
                    isNotificationActive.value = false
                }
            }
        } else {
            isNotificationActive.value = false
        }
    }

    fun dismissNotification() {
        isNotificationActive.value = false
        _notificationState.value = null
    }



    // List of active activities, ordered by priority: Call > Navigation > Torch > Media > Charging > Timer > Notifications
    val activeActivities: StateFlow<List<OverlayState>> = combine(
        phoneCallState,
        _chargingState,
        isChargingActive,
        navigationState,
        timerState,
        stopwatchState,
        mediaState,
        _notificationState,
        isNotificationActive,
        torchState,
        hotspotState,
        settingsFlow
    ) { array ->
        val list = mutableListOf<OverlayState>()
        
        val call = array[0] as PhoneCallState?
        val charging = array[1] as ChargingState?
        val chargingActive = array[2] as Boolean
        val nav = array[3] as NavigationState?
        val timer = array[4] as TimerState?
        val stopwatch = array[5] as StopwatchState?
        val media = array[6] as MediaState?
        val notification = array[7] as NotificationState?
        val notificationActive = array[8] as Boolean
        val torch = array[9] as TorchState?
        val hotspot = array[10] as HotspotState?
        val settings = array[11] as NovaSettings

        // Call (Priority 1)
        if (call != null && call.isActive) {
            list.add(OverlayState.PhoneCall(call))
        }
        // Navigation (Priority 2)
        if (nav != null && settings.navigationEnabled) {
            list.add(OverlayState.Navigation(nav))
        }
        // Torch (Priority 3)
        if (torch != null && torch.isActive && settings.torchEnabled) {
            list.add(OverlayState.Torch(torch))
        }
        // Media (Priority 4)
        if (media != null && media.title.isNotEmpty() && settings.mediaControlsEnabled) {
            list.add(OverlayState.Media(media))
        }
        // Hotspot (Priority 5)
        if (hotspot != null && hotspot.isActive && settings.hotspotEnabled) {
            list.add(OverlayState.Hotspot(hotspot))
        }
        // Charging (Priority 6)
        if (chargingActive && charging != null && settings.chargingEnabled) {
            list.add(OverlayState.Charging(charging))
        }
        // Timer/Stopwatch (Priority 7)
        if (timer != null && (timer.isRunning || timer.remainingMs > 0) && settings.timerEnabled) {
            list.add(OverlayState.Timer(timer))
        }
        if (stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMs > 0) && settings.stopwatchEnabled) {
            list.add(OverlayState.Stopwatch(stopwatch))
        }
        // Notification (Priority 8)
        if (notificationActive && notification != null && settings.notificationsEnabled) {
            list.add(OverlayState.Notification(notification))
        }

        // Log active registry & priority order
        val registryNames = list.map { it::class.java.simpleName }
        Log.i("NovaBar-ActivityPriority", "Active Activity Registry: $registryNames")
        Log.i("NovaBar-ActivityPriority", "  Registered Timer: isRunning=${timer?.isRunning}, remainingMs=${timer?.remainingMs}, durationMs=${timer?.durationMs}")
        Log.i("NovaBar-ActivityPriority", "  Registered Media: isPlaying=${media?.isPlaying}, title='${media?.title}', appName='${media?.appName}'")
        
        // Log rejection reasons if they are registered but rejected from the list
        if (timer != null && !(timer.isRunning || timer.remainingMs > 0)) {
            Log.d("NovaBar-ActivityPriority", "  Timer rejected because it is not running and has 0 remaining time.")
        }
        if (media != null && media.title.isEmpty()) {
            Log.d("NovaBar-ActivityPriority", "  Media rejected because title is empty.")
        }

        list
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val selectedActivityIndex = MutableStateFlow(0)

    // Swipe left: focus on next activity
    fun swipeLeft() {
        val list = activeActivities.value
        if (list.size > 1) {
            val nextIndex = (selectedActivityIndex.value + 1) % list.size
            selectedActivityIndex.value = nextIndex
        }
    }

    // Swipe right: focus on previous activity
    fun swipeRight() {
        val list = activeActivities.value
        if (list.size > 1) {
            val prevIndex = if (selectedActivityIndex.value == 0) list.lastIndex else selectedActivityIndex.value - 1
            selectedActivityIndex.value = prevIndex
        }
    }

    // Currently focused active state
    val activeState: StateFlow<OverlayState> = combine(
        activeActivities,
        selectedActivityIndex
    ) { list, index ->
        val state = if (list.isEmpty()) {
            OverlayState.Idle
        } else {
            val clampedIndex = index.coerceIn(0, list.lastIndex)
            list[clampedIndex]
        }
        
        val registryNames = list.map { it::class.java.simpleName }
        val selectedName = state::class.java.simpleName
        Log.i("NovaBar-ActivityPriority", "Display Priority Decision: Selected='$selectedName' (index=$index) from registry $registryNames")
        if (list.isNotEmpty()) {
            val builder = StringBuilder("  Selection details:\n")
            list.forEachIndexed { i, s ->
                val activeIndicator = if (i == index) "-->" else "   "
                builder.append("    $activeIndicator [$i] ${s::class.java.simpleName}\n")
            }
            Log.i("NovaBar-ActivityPriority", builder.toString().trimEnd())
        }
        
        state
    }.stateIn(scope, SharingStarted.Eagerly, OverlayState.Idle)

    init {
        // Automatically focus on new activities that arrive, and auto-focus media when playback starts
        scope.launch {
            var previousList = emptyList<OverlayState>()
            var wasMediaPlaying = false
            activeActivities.collect { currentList ->
                val currentMedia = currentList.firstOrNull { it is OverlayState.Media } as? OverlayState.Media
                val isMediaPlaying = currentMedia?.data?.isPlaying == true

                if (isExpanded.value) {
                    wasMediaPlaying = isMediaPlaying
                    previousList = currentList
                    val currentIndex = selectedActivityIndex.value
                    if (currentIndex >= currentList.size) {
                        selectedActivityIndex.value = (currentList.size - 1).coerceAtLeast(0)
                    }
                    return@collect
                }
                
                val newlyAdded = currentList.firstOrNull { newItem ->
                    previousList.none { it::class.java == newItem::class.java }
                }
                
                if (newlyAdded != null) {
                    val index = currentList.indexOf(newlyAdded)
                    selectedActivityIndex.value = index
                    Log.i("NovaBar-ActivityPriority", "Focusing on newly registered activity: ${newlyAdded::class.java.simpleName} at index $index.")
                } else if (isMediaPlaying && !wasMediaPlaying) {
                    // Check if current focused activity has lower priority than Media
                    val currentIndex = selectedActivityIndex.value
                    val currentState = if (currentIndex in currentList.indices) currentList[currentIndex] else OverlayState.Idle
                    val isLowerPriorityThanMedia = when (currentState) {
                        is OverlayState.Idle,
                        is OverlayState.Hotspot,
                        is OverlayState.Charging,
                        is OverlayState.Timer,
                        is OverlayState.Stopwatch,
                        is OverlayState.Notification -> true
                        else -> false
                    }
                    if (isLowerPriorityThanMedia) {
                        val index = currentList.indexOfFirst { it is OverlayState.Media }
                        if (index >= 0) {
                            selectedActivityIndex.value = index
                            Log.i("NovaBar-ActivityPriority", "Auto-focusing on Media at index $index because playback started and current activity (${currentState::class.java.simpleName}) is lower priority.")
                        }
                    } else {
                        Log.i("NovaBar-ActivityPriority", "Playback started but keeping current focus on higher/equal priority activity: ${currentState::class.java.simpleName}.")
                    }
                } else {
                    val currentIndex = selectedActivityIndex.value
                    if (currentIndex >= currentList.size) {
                        selectedActivityIndex.value = (currentList.size - 1).coerceAtLeast(0)
                    }
                }
                previousList = currentList
                wasMediaPlaying = isMediaPlaying
            }
        }
        scope.launch {
            activeActivities.collect { list ->
                DiagnosticsManager.currentPriorityActivity.value = if (list.isEmpty()) "None" else list.joinToString { it::class.java.simpleName }
            }
        }
        scope.launch {
            activeState.collect { state ->
                DiagnosticsManager.currentPresentationState.value = state::class.java.simpleName
            }
        }
        
        // 5-second automatic activity rotation loop for the compact activity pill only
        scope.launch {
            while (isActive) {
                delay(5000L)
                if (!isExpanded.value) {
                    val list = activeActivities.value
                    if (list.size > 1) {
                        val nextIndex = (selectedActivityIndex.value + 1) % list.size
                        selectedActivityIndex.value = nextIndex
                        Log.d("OverlayStateManager", "Auto-rotating compact activity pill to index $nextIndex (${list[nextIndex]::class.java.simpleName})")
                    }
                }
            }
        }
    }

    fun disableHotspot(context: Context) {
        Log.d("NovaBar", "HOTSPOT_DISABLE_BUTTON_CLICKED")
        val success = com.novabar.app.utils.HotspotManager.disableHotspot(context)
        if (success) {
            collapse()
        }
    }

    fun endCall(context: Context) {
        Log.d("NovaBar", "CALL_END_BUTTON_CLICKED")
        
        // Primary path: TelecomManager.endCall()
        val telecomSuccess = endCallTelecom(context)
        if (telecomSuccess) {
            Log.d("NovaBar", "CALL_END_TELECOM_SUCCESS")
            phoneCallState.value = null
            collapse()
            return
        }
        
        // Fallback path: Accessibility Service
        Log.d("NovaBar", "Telecom endCall failed/denied, trying Accessibility fallback...")
        val accessibilitySuccess = NovaAccessibilityService.triggerEndCall()
        if (accessibilitySuccess) {
            Log.d("NovaBar", "CALL_END_ACCESSIBILITY_SUCCESS")
            phoneCallState.value = null
            collapse()
        } else {
            Log.e("NovaBar", "CALL_END_ACCESSIBILITY_FAILED")
        }
    }

    private fun endCallTelecom(context: Context): Boolean {
        Log.d("NovaBar", "CALL_END_REQUEST_SENT")
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("NovaBar", "CALL_END_TELECOM_FAILED: ANSWER_PHONE_CALLS permission not granted")
            return false
        }
        
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager == null) {
            Log.e("NovaBar", "CALL_END_TELECOM_FAILED: TelecomManager system service not found")
            return false
        }
        
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val success = telecomManager.endCall()
                if (success) {
                    true
                } else {
                    Log.e("NovaBar", "CALL_END_TELECOM_FAILED: TelecomManager.endCall() returned false")
                    false
                }
            } else {
                Log.e("NovaBar", "CALL_END_TELECOM_FAILED: API level < 28 (Pie)")
                false
            }
        } catch (e: SecurityException) {
            Log.e("NovaBar", "CALL_END_TELECOM_FAILED: SecurityException - ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("NovaBar", "CALL_END_TELECOM_FAILED: Exception - ${e.message}")
            false
        }
    }

    private var stopwatchTickJob: Job? = null
    private var timerTickJob: Job? = null

    fun setStopwatchState(state: StopwatchState?) {
        stopwatchState.value = state
        if (state != null && state.isRunning && state.startElapsedRealtime > 0L) {
            startStopwatchTicking()
        } else {
            stopwatchTickJob?.cancel()
            stopwatchTickJob = null
        }
    }

    fun setTimerState(state: TimerState?) {
        timerState.value = state
        if (state != null && state.isRunning && state.targetEndElapsedRealtime > 0L) {
            startTimerTicking()
        } else {
            timerTickJob?.cancel()
            timerTickJob = null
        }
    }

    private fun startStopwatchTicking() {
        if (stopwatchTickJob != null) return
        stopwatchTickJob = scope.launch {
            var lastElapsedCoarse = -1L
            while (isActive) {
                val current = stopwatchState.value
                if (current == null || !current.isRunning || current.startElapsedRealtime <= 0L) {
                    break
                }
                val now = android.os.SystemClock.elapsedRealtime()
                val liveElapsed = (now - current.startElapsedRealtime).coerceAtLeast(0L)
                val elapsedCoarse = liveElapsed / 10
                if (elapsedCoarse != lastElapsedCoarse) {
                    lastElapsedCoarse = elapsedCoarse
                    stopwatchState.value = current.copy(elapsedMs = liveElapsed)
                }
                delay(33L)
            }
            stopwatchTickJob = null
        }
    }

    private fun startTimerTicking() {
        if (timerTickJob != null) return
        timerTickJob = scope.launch {
            var lastRemainingCoarse = -1L
            while (isActive) {
                val current = timerState.value
                if (current == null || !current.isRunning || current.targetEndElapsedRealtime <= 0L) {
                    break
                }
                val now = android.os.SystemClock.elapsedRealtime()
                val liveRemaining = (current.targetEndElapsedRealtime - now).coerceAtLeast(0L)
                val remainingCoarse = if (current.showSeconds) {
                    liveRemaining / 10
                } else {
                    liveRemaining / 1000
                }
                if (remainingCoarse != lastRemainingCoarse) {
                    lastRemainingCoarse = remainingCoarse
                    timerState.value = current.copy(remainingMs = liveRemaining)
                }
                if (liveRemaining <= 0L) {
                    break
                }
                val delayMs = if (current.showSeconds) 100L else 200L
                delay(delayMs)
            }
            timerTickJob = null
        }
    }
}
