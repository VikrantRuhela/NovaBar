package com.novabar.app.domain

import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object OverlayStateManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val pillBounds = MutableStateFlow(Rect())

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

    fun expand() {
        Log.d("NovaBar", "EXPAND_REQUEST")
        isExpanded.value = true
    }

    fun collapse() {
        Log.d("NovaBar", "COLLAPSE_REQUEST")
        isExpanded.value = false
    }

    fun toggleExpanded() {
        if (isExpanded.value) {
            Log.d("NovaBar", "COLLAPSE_REQUEST")
        } else {
            Log.d("NovaBar", "EXPAND_REQUEST")
        }
        isExpanded.value = !isExpanded.value
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
    }
}
