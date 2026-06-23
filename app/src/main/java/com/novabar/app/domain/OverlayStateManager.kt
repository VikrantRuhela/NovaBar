package com.novabar.app.domain

import android.graphics.Rect
import android.util.Log
import android.content.Context
import android.telecom.TelecomManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import com.novabar.app.services.NovaAccessibilityService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object OverlayStateManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
    
    private val _chargingState = MutableStateFlow<ChargingState?>(null)
    private var chargingJob: Job? = null
    private val isChargingActive = MutableStateFlow(false)
    
    private val _notificationState = MutableStateFlow<NotificationState?>(null)
    private var notificationJob: Job? = null
    private val isNotificationActive = MutableStateFlow(false)

    // Global expanded state flow
    val isExpanded = MutableStateFlow(false)
    val windowMode = MutableStateFlow("Compact")

    fun expand() {
        Log.d("NovaBar", "EXPAND_REQUEST")
        windowMode.value = "Expanded"
        isExpanded.value = true
    }

    fun collapse() {
        Log.d("NovaBar", "COLLAPSE_REQUEST")
        isExpanded.value = false
    }

    fun toggleExpanded() {
        if (isExpanded.value) {
            Log.d("NovaBar", "COLLAPSE_REQUEST")
            isExpanded.value = false
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
                isChargingActive.value = false
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
                isNotificationActive.value = false
            }
        } else {
            isNotificationActive.value = false
        }
    }

    fun dismissNotification() {
        isNotificationActive.value = false
        _notificationState.value = null
    }

    // List of active activities, ordered by priority: Call > Charging > Nav > Timer > Stopwatch > Music > Notification
    val activeActivities: StateFlow<List<OverlayState>> = combine(
        phoneCallState,
        _chargingState,
        isChargingActive,
        navigationState,
        timerState,
        stopwatchState,
        mediaState,
        _notificationState,
        isNotificationActive
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

        if (call != null && call.isActive) {
            list.add(OverlayState.PhoneCall(call))
        }
        if (chargingActive && charging != null) {
            list.add(OverlayState.Charging(charging))
        }
        if (nav != null) {
            list.add(OverlayState.Navigation(nav))
        }
        if (timer != null && (timer.isRunning || timer.remainingMs > 0)) {
            list.add(OverlayState.Timer(timer))
        }
        if (stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMs > 0)) {
            list.add(OverlayState.Stopwatch(stopwatch))
        }
        if (media != null && media.title.isNotEmpty()) {
            list.add(OverlayState.Media(media))
        }
        if (notificationActive && notification != null) {
            list.add(OverlayState.Notification(notification))
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
        if (list.isEmpty()) {
            OverlayState.Idle
        } else {
            val clampedIndex = index.coerceIn(0, list.lastIndex)
            list[clampedIndex]
        }
    }.stateIn(scope, SharingStarted.Eagerly, OverlayState.Idle)

    init {
        // Automatically focus on new activities that arrive
        scope.launch {
            var previousList = emptyList<OverlayState>()
            activeActivities.collect { currentList ->
                val newlyAdded = currentList.firstOrNull { newItem ->
                    previousList.none { it::class.java == newItem::class.java }
                }
                
                if (newlyAdded != null) {
                    val index = currentList.indexOf(newlyAdded)
                    selectedActivityIndex.value = index
                } else {
                    val currentIndex = selectedActivityIndex.value
                    if (currentIndex >= currentList.size) {
                        selectedActivityIndex.value = (currentList.size - 1).coerceAtLeast(0)
                    }
                }
                previousList = currentList
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
