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
    
    val isNovaGuyActive = MutableStateFlow(false)
    private var novaGuyHideJob: Job? = null
    
    val isScreenLocked = MutableStateFlow(false)
    val isScreenOn = MutableStateFlow(true)
    val selectedNovaGuyMessage = MutableStateFlow<NovaGuyMessage?>(null)
    val novaGuyEyeOffset = MutableStateFlow(0f)
    private var eyeAnimationJob: Job? = null

    fun onLockscreenWake() {
        val settings = settingsFlow.value
        if (settings.isEnabled && settings.lockscreenGuardianEnabled) {
            isNovaGuyActive.value = true
            novaGuyHideJob?.cancel()
        }
    }

    fun onLockscreenDismiss() {
        isNovaGuyActive.value = false
        novaGuyHideJob?.cancel()
    }

    private fun startEyeAnimation() {
        eyeAnimationJob?.cancel()
        eyeAnimationJob = scope.launch {
            novaGuyEyeOffset.value = 0f
            // Initial delay before starting the glance sequence
            delay(1000L)
            while (true) {
                // Look Right -> Hold -> Return Center
                animateEyeTo(5.5f, duration = 600L)
                delay(1500L) // Hold briefly
                animateEyeTo(0f, duration = 800L)
                
                // Pause at center
                delay(1200L)
                
                // Look Left -> Hold -> Return Center
                animateEyeTo(-5.5f, duration = 600L)
                delay(1500L) // Hold briefly
                animateEyeTo(0f, duration = 800L)
                
                // Calm pause before repeating (if still active)
                delay(5000L)
            }
        }
    }

    private suspend fun animateEyeTo(target: Float, duration: Long) {
        val start = novaGuyEyeOffset.value
        val startTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - startTime
            if (elapsed >= duration) {
                novaGuyEyeOffset.value = target
                break
            }
            val fraction = elapsed.toFloat() / duration
            val easedFraction = interpolateFastOutSlowIn(fraction)
            novaGuyEyeOffset.value = start + (target - start) * easedFraction
            delay(16) // ~60fps
        }
    }

    private fun interpolateFastOutSlowIn(fraction: Float): Float {
        return fraction * fraction * (3f - 2f * fraction)
    }

    init {
        scope.launch {
            settingsFlow.collectLatest { settings ->
                DiagnosticsManager.showDebugMarkers.value = settings.debugModeEnabled
            }
        }

        // Monitor isNovaGuyActive to run eye animation
        scope.launch {
            isNovaGuyActive.collectLatest { active ->
                if (active) {
                    startEyeAnimation()
                } else {
                    eyeAnimationJob?.cancel()
                    novaGuyEyeOffset.value = 0f
                }
            }
        }

        // Trigger NovaGuy periodically when idle
        scope.launch {
            while (true) {
                val settings = settingsFlow.value
                val minMs = settings.novaGuyMinInterval * 60_000L
                val maxMs = settings.novaGuyMaxInterval * 60_000L
                val randomDelay = kotlin.random.Random.nextLong(minMs, maxMs + 1L)
                delay(randomDelay)
                
                val isExpandedVal = isExpanded.value
                
                val call = phoneCallState.value
                val charging = isChargingActive.value
                val nav = navigationState.value
                val timer = timerState.value
                val stopwatch = stopwatchState.value
                val media = mediaState.value
                val notification = isNotificationActive.value
                val torch = torchState.value
                val hotspot = hotspotState.value
                val voice = voiceRecorderState.value
                
                val currentSettings = settingsFlow.value

                val isCallActive = call != null && call.isActive
                val isNavActive = nav != null && currentSettings.navigationEnabled
                val isVoiceActive = voice != null && (voice.isRecording || voice.isPaused) && currentSettings.voiceRecorderEnabled
                val isTorchActive = torch != null && torch.isActive && currentSettings.torchEnabled
                val isHotspotActive = hotspot != null && hotspot.isActive && currentSettings.hotspotEnabled
                val isTimerActive = timer != null && (timer.isRunning || timer.remainingMs > 0) && currentSettings.timerEnabled
                val isStopwatchActive = stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMs > 0) && currentSettings.stopwatchEnabled

                val isMediaActive = media != null && media.title.isNotEmpty() && currentSettings.mediaControlsEnabled
                val isChargingActiveState = charging && currentSettings.chargingEnabled

                var activeCount = 0
                if (isCallActive) activeCount++
                if (isNavActive) activeCount++
                if (isVoiceActive) activeCount++
                if (isTorchActive) activeCount++
                if (isHotspotActive) activeCount++
                if (isTimerActive) activeCount++
                if (isStopwatchActive) activeCount++
                if (isMediaActive) activeCount++
                if (isChargingActiveState) activeCount++

                val isBlocked = isCallActive || isNavActive || isVoiceActive || isTorchActive || isHotspotActive || isTimerActive || isStopwatchActive || (activeCount > 1)

                val isLocked = isScreenLocked.value

                if (!isBlocked && !isExpandedVal && currentSettings.isEnabled && currentSettings.alwaysOnNovaGuy && !isLocked) {
                    isNovaGuyActive.value = true
                    
                    novaGuyHideJob?.cancel()
                    novaGuyHideJob = scope.launch {
                        delay(9500L) // 3.5 seconds of animation + 6 seconds of static visibility
                        if (!isExpanded.value) {
                            isNovaGuyActive.value = false
                        }
                    }
                }
            }
        }

        // Monitor settings changes to instantly hide NovaGuy if disabled
        scope.launch {
            settingsFlow.collectLatest { settings ->
                if (!settings.isEnabled) {
                    isNovaGuyActive.value = false
                    novaGuyHideJob?.cancel()
                } else {
                    if (!settings.alwaysOnNovaGuy && !isScreenLocked.value) {
                        isNovaGuyActive.value = false
                        novaGuyHideJob?.cancel()
                    }
                    if (!settings.lockscreenGuardianEnabled && isScreenLocked.value) {
                        isNovaGuyActive.value = false
                        novaGuyHideJob?.cancel()
                    }
                }
            }
        }

        // Monitor active activities, if any becomes active, turn off NovaGuy
        scope.launch {
            combine(
                phoneCallState,
                isChargingActive,
                navigationState,
                timerState,
                stopwatchState,
                mediaState,
                isNotificationActive,
                torchState,
                hotspotState,
                voiceRecorderState,
                settingsFlow
            ) { states ->
                val call = states[0] as PhoneCallState?
                val charging = states[1] as Boolean
                val nav = states[2] as NavigationState?
                val timer = states[3] as TimerState?
                val stopwatch = states[4] as StopwatchState?
                val media = states[5] as MediaState?
                val notification = states[6] as Boolean
                val torch = states[7] as TorchState?
                val hotspot = states[8] as HotspotState?
                val voice = states[9] as VoiceRecorderState?
                val settings = states[10] as NovaSettings

                val hasActive = (call != null && call.isActive) ||
                        (charging && settings.chargingEnabled) ||
                        (nav != null && settings.navigationEnabled) ||
                        (timer != null && (timer.isRunning || timer.remainingMs > 0) && settings.timerEnabled) ||
                        (stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMs > 0) && settings.stopwatchEnabled) ||
                        (media != null && media.title.isNotEmpty() && settings.mediaControlsEnabled) ||
                        (notification && settings.notificationsEnabled) ||
                        (torch != null && torch.isActive && settings.torchEnabled) ||
                        (hotspot != null && hotspot.isActive && settings.hotspotEnabled) ||
                        (voice != null && (voice.isRecording || voice.isPaused) && settings.voiceRecorderEnabled)
                
                hasActive
            }.collectLatest { hasActive ->
                if (hasActive) {
                    isNovaGuyActive.value = false
                    novaGuyHideJob?.cancel()
                }
            }
        }

        // Monitor isExpanded to cancel hide timer or dismiss NovaGuy when collapsed
        scope.launch {
            isExpanded.collectLatest { expanded ->
                if (expanded) {
                    if (activeState.value is OverlayState.NovaGuy) {
                        novaGuyHideJob?.cancel()
                    }
                } else {
                    if (activeState.value is OverlayState.NovaGuy) {
                        // Instead of dismissing immediately, schedule a 6-second delay to hide NovaGuy.
                        // This allows the collapse animation to complete smoothly without layout thrashing.
                        novaGuyHideJob?.cancel()
                        novaGuyHideJob = scope.launch {
                            delay(6000L)
                            if (!isExpanded.value) {
                                isNovaGuyActive.value = false
                            }
                        }
                    }
                }
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
    val voiceRecorderState = MutableStateFlow<VoiceRecorderState?>(null)
    
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

    fun getDeviceContext(): DeviceContext {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        val chg = _chargingState.value
        val isCharging = chg?.isCharging ?: false
        val batteryPercentage = chg?.batteryPercentage ?: 100
        
        val media = mediaState.value
        val isMusicPlaying = media?.isPlaying ?: false

        return DeviceContext(
            hourOfDay = hour,
            isCharging = isCharging,
            batteryPercentage = batteryPercentage,
            isMusicPlaying = isMusicPlaying
        )
    }

    fun expand() {
        Log.d("NovaBar", "EXPAND_REQUEST")
        windowMode.value = "Expanded"
        isExpanded.value = true
        if (activeState.value is OverlayState.NovaGuy) {
            val devCtx = getDeviceContext()
            selectedNovaGuyMessage.value = NovaGuyMessageProvider.selectRandomMessage(
                deviceContext = devCtx,
                contextAwareEnabled = settingsFlow.value.contextAwareMessagesEnabled
            )
        }
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
            if (activeState.value is OverlayState.NovaGuy) {
                val devCtx = getDeviceContext()
                selectedNovaGuyMessage.value = NovaGuyMessageProvider.selectRandomMessage(
                    deviceContext = devCtx,
                    contextAwareEnabled = settingsFlow.value.contextAwareMessagesEnabled
                )
            }
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



    // List of active activities, ordered by priority: Call > Navigation > Voice Recorder > Torch > Media > Hotspot > Charging > Timer > Notifications > NovaGuy
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
        voiceRecorderState,
        isNovaGuyActive,
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
        val voiceRecorder = array[11] as VoiceRecorderState?
        val isNovaGuy = array[12] as Boolean
        val settings = array[13] as NovaSettings

        val isCallActive = call != null && call.isActive
        val isNavActive = nav != null && settings.navigationEnabled
        val isVoiceActive = voiceRecorder != null && (voiceRecorder.isRecording || voiceRecorder.isPaused) && settings.voiceRecorderEnabled
        val isTorchActive = torch != null && torch.isActive && settings.torchEnabled
        val isHotspotActive = hotspot != null && hotspot.isActive && settings.hotspotEnabled
        val isTimerActive = timer != null && (timer.isRunning || timer.remainingMs > 0) && settings.timerEnabled
        val isStopwatchActive = stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMs > 0) && settings.stopwatchEnabled
        val isMediaActive = media != null && media.title.isNotEmpty() && settings.mediaControlsEnabled
        val isChargingActiveState = chargingActive && charging != null && settings.chargingEnabled

        // Call (Priority 1)
        if (isCallActive && call != null) {
            list.add(OverlayState.PhoneCall(call))
        }
        // Navigation (Priority 2)
        if (isNavActive && nav != null) {
            list.add(OverlayState.Navigation(nav))
        }
        // Voice Recorder (Priority 3)
        if (isVoiceActive && voiceRecorder != null) {
            list.add(OverlayState.VoiceRecorder(voiceRecorder))
        }
        // Torch (Priority 4)
        if (isTorchActive && torch != null) {
            list.add(OverlayState.Torch(torch))
        }
        // Media (Priority 5)
        if (isMediaActive && media != null) {
            list.add(OverlayState.Media(media))
        }
        // Hotspot (Priority 6)
        if (isHotspotActive && hotspot != null) {
            list.add(OverlayState.Hotspot(hotspot))
        }
        // Charging (Priority 7)
        if (isChargingActiveState && charging != null) {
            list.add(OverlayState.Charging(charging))
        }
        // Timer/Stopwatch (Priority 8)
        if (isTimerActive && timer != null) {
            list.add(OverlayState.Timer(timer))
        }
        if (isStopwatchActive && stopwatch != null) {
            list.add(OverlayState.Stopwatch(stopwatch))
        }
        // Notification (Priority 9)
        if (notificationActive && notification != null && settings.notificationsEnabled) {
            list.add(OverlayState.Notification(notification))
        }
        // NovaGuy (Priority 10 / Idle)
        if (isNovaGuy && settings.isEnabled) {
            list.add(OverlayState.NovaGuy)
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

    enum class TransitionType {
        AUTOMATIC,
        SWIPE_LEFT,
        SWIPE_RIGHT
    }
    val lastTransitionType = MutableStateFlow(TransitionType.AUTOMATIC)

    fun resetTransitionType() {
        lastTransitionType.value = TransitionType.AUTOMATIC
    }

    // Swipe left: focus on next activity
    fun swipeLeft() {
        val list = activeActivities.value
        android.util.Log.d("NovaBar-SwipeDebug", "2. swipeLeft() invoked. activeActivities size = ${list.size}")
        if (list.size > 1) {
            val oldIndex = selectedActivityIndex.value
            lastTransitionType.value = TransitionType.SWIPE_LEFT
            val nextIndex = (selectedActivityIndex.value + 1) % list.size
            selectedActivityIndex.value = nextIndex
            android.util.Log.d(
                "NovaBar-SwipeDebug",
                "3. swipeLeft() changed selectedActivityIndex: $oldIndex -> $nextIndex. lastTransitionType immediately after swipe: ${lastTransitionType.value}"
            )
        }
    }

    // Swipe right: focus on previous activity
    fun swipeRight() {
        val list = activeActivities.value
        android.util.Log.d("NovaBar-SwipeDebug", "2. swipeRight() invoked. activeActivities size = ${list.size}")
        if (list.size > 1) {
            val oldIndex = selectedActivityIndex.value
            lastTransitionType.value = TransitionType.SWIPE_RIGHT
            val prevIndex = if (selectedActivityIndex.value == 0) list.lastIndex else selectedActivityIndex.value - 1
            selectedActivityIndex.value = prevIndex
            android.util.Log.d(
                "NovaBar-SwipeDebug",
                "3. swipeRight() changed selectedActivityIndex: $oldIndex -> $prevIndex. lastTransitionType immediately after swipe: ${lastTransitionType.value}"
            )
        }
    }

    // Currently focused active state
    val activeState: StateFlow<OverlayState> = combine(
        activeActivities,
        selectedActivityIndex
    ) { list, index ->
        val isLocked = isScreenLocked.value
        val hasNovaGuy = list.any { it is OverlayState.NovaGuy }
        
        // Find if any interactive live activity is active in the list
        val hasInteractiveLiveActivity = list.any { state ->
            state is OverlayState.PhoneCall ||
            state is OverlayState.Navigation ||
            state is OverlayState.VoiceRecorder ||
            state is OverlayState.Torch ||
            state is OverlayState.Hotspot ||
            state is OverlayState.Timer ||
            state is OverlayState.Stopwatch ||
            state is OverlayState.Notification
        }
        
        val nonNovaGuyCount = list.count { it !is OverlayState.NovaGuy }
        
        // Override conditions:
        // 1. Locked: Lock Screen Guardian takes absolute precedence and overrides all activities
        // 2. Unlocked: NovaGuy temporarily overrides passive activities (Media, Charging, Idle Clock)
        //    only if no interactive Live Activity or Multi-Activity Dashboard (non-NovaGuy count > 1) is active
        val shouldOverrideWithNovaGuy = hasNovaGuy && (
            isLocked || (!hasInteractiveLiveActivity && nonNovaGuyCount <= 1)
        )
        
        val state = if (shouldOverrideWithNovaGuy) {
            list.first { it is OverlayState.NovaGuy }
        } else {
            if (list.isEmpty()) {
                OverlayState.Idle
            } else {
                val clampedIndex = index.coerceIn(0, list.lastIndex)
                list[clampedIndex]
            }
        }
        
        val registryNames = list.map { it::class.java.simpleName }
        val selectedName = state::class.java.simpleName
        android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 5: Priority Update Decision | Selected='$selectedName' (index=$index) | registry=$registryNames")
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

    private var voiceRecorderTickJob: Job? = null

    fun setVoiceRecorderState(state: VoiceRecorderState?) {
        val oldState = voiceRecorderState.value
        voiceRecorderState.value = state
        
        if (state == null) {
            android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 4: State Cleared | state=null")
            android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 5: Activity removed from overlay registry")
        } else {
            val isRecordingChanged = oldState?.isRecording != state.isRecording
            val durationChanged = oldState?.durationMs != state.durationMs
            val pauseIntentChanged = oldState?.pauseIntent != state.pauseIntent
            val resumeIntentChanged = oldState?.resumeIntent != state.resumeIntent
            val stopIntentChanged = oldState?.stopIntent != state.stopIntent
            
            if (oldState == null) {
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 4: State Created | isRecording=${state.isRecording} | durationMs=${state.durationMs} | pauseIntent=${state.pauseIntent != null} | resumeIntent=${state.resumeIntent != null} | stopIntent=${state.stopIntent != null}")
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 5: Activity registered in overlay registry")
            } else {
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 4: State Updated | isRecording=${state.isRecording} (changed=$isRecordingChanged) | durationMs=${state.durationMs} (changed=$durationChanged) | pauseIntent=${state.pauseIntent != null} (changed=$pauseIntentChanged) | resumeIntent=${state.resumeIntent != null} (changed=$resumeIntentChanged) | stopIntent=${state.stopIntent != null} (changed=$stopIntentChanged)")
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 5: Activity updated in overlay registry")
            }
        }
        
        if (state != null && state.isRecording && state.startElapsedRealtime > 0L) {
            startVoiceRecorderTicking()
        } else {
            voiceRecorderTickJob?.cancel()
            voiceRecorderTickJob = null
        }
    }

    private fun startVoiceRecorderTicking() {
        if (voiceRecorderTickJob != null) return
        voiceRecorderTickJob = scope.launch {
            var lastElapsedCoarse = -1L
            while (isActive) {
                val current = voiceRecorderState.value
                if (current == null || !current.isRecording || current.startElapsedRealtime <= 0L) {
                    break
                }
                val now = android.os.SystemClock.elapsedRealtime()
                val liveDuration = (now - current.startElapsedRealtime).coerceAtLeast(0L)
                val elapsedCoarse = liveDuration / 1000
                if (elapsedCoarse != lastElapsedCoarse) {
                    lastElapsedCoarse = elapsedCoarse
                    voiceRecorderState.value = current.copy(durationMs = liveDuration)
                }
                delay(100L)
            }
            voiceRecorderTickJob = null
        }
    }
}
