package com.novabar.app.utils

import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import com.novabar.app.data.NovaSettings
import com.novabar.app.domain.TimerState
import com.novabar.app.domain.StopwatchState
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.services.NovaNotificationListener

sealed class ParsedClockState {
    data class Timer(val state: TimerState) : ParsedClockState()
    data class Stopwatch(val state: StopwatchState) : ParsedClockState()
    object None : ParsedClockState()
}

interface ClockProvider {
    fun parse(
        sbn: StatusBarNotification,
        settings: NovaSettings,
        context: Context
    ): ParsedClockState
}

class LoggingClockProvider(private val oemTag: String, private val delegate: ClockProvider) : ClockProvider {
    override fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val extras = sbn.notification.extras ?: android.os.Bundle()
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val whenTime = sbn.notification.`when`
        val showChronometer = extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER, false)
        val actions = sbn.notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        
        Log.i("NovaBar-ClockCompat", "[$oemTag] Incoming notification: package=${sbn.packageName}, title='$title', text='$text', when=$whenTime, showChronometer=$showChronometer, actions=$actionTitles")
        val result = delegate.parse(sbn, settings, context)
        Log.i("NovaBar-ClockCompat", "[$oemTag] Parsing result: $result")
        
        // Log to DeveloperLogger for Diagnostics screen
        try {
            val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                extras.getBoolean("android.chronometerCountDown", false)
            } else {
                false
            }
            val chronometerFields = "when=$whenTime, showChronometer=$showChronometer, chronometerCountDown=$isCountDown"
            val extrasStr = DeveloperLogger.bundleToReadableString(extras)
            
            when (result) {
                is ParsedClockState.Stopwatch -> {
                    val accepted = settings.stopwatchEnabled
                    val reason = "[$oemTag] Matched stopwatch pattern. Result: $result"
                    DeveloperLogger.log(
                        context,
                        "STOPWATCH_DETECTION",
                        "Package: ${sbn.packageName}\n" +
                        "Title: $title\n" +
                        "Extras: $extrasStr\n" +
                        "Chronometer Fields: $chronometerFields\n" +
                        "Detection Result: ${if (accepted) "ACCEPTED" else "REJECTED"}\n" +
                        "Reason: $reason"
                    )
                }
                is ParsedClockState.Timer -> {
                    val accepted = settings.timerEnabled
                    val reason = "[$oemTag] Matched timer pattern. Result: $result"
                    DeveloperLogger.log(
                        context,
                        "TIMER_DETECTION",
                        "Package: ${sbn.packageName}\n" +
                        "Title: $title\n" +
                        "Extras: $extrasStr\n" +
                        "Chronometer Fields: $chronometerFields\n" +
                        "Detection Result: ${if (accepted) "ACCEPTED" else "REJECTED"}\n" +
                        "Reason: $reason"
                    )
                }
                is ParsedClockState.None -> {
                    DeveloperLogger.log(
                        context,
                        "CLOCK_DETECTION",
                        "Package: ${sbn.packageName}\n" +
                        "Title: $title\n" +
                        "Extras: $extrasStr\n" +
                        "Chronometer Fields: $chronometerFields\n" +
                        "Detection Result: REJECTED\n" +
                        "Reason: [$oemTag] Clock notification did not match stopwatch or timer pattern."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NovaBar-ClockCompat", "Error writing to DeveloperLogger: ${e.message}", e)
        }
        
        return result
    }
}

object GoogleClockProvider : ClockProvider {
    private const val TAG = "GoogleClockProvider"
    
    private data class ExtractedRemoteViewsData(
        val textValues: List<String>,
        val baseTimeMs: Long?,
        val isCountDown: Boolean?
    )
    
    override fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val notification = sbn.notification
        val extras = notification.extras ?: android.os.Bundle()
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        val whenTime = notification.`when`
        val showChronometer = extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER, false)
        
        val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getBoolean("android.chronometerCountDown", false)
        } else {
            false
        }

        val hasLap = actionTitles.any { it.contains("lap") || it.contains("split") }
        val hasPause = actionTitles.any { it.contains("pause") || it.contains("stop") }
        val hasResume = actionTitles.any { it.contains("resume") || it.contains("continue") || it.contains("start") || it.contains("play") }
        val hasReset = actionTitles.any { it.contains("reset") || it.contains("restart") || it.contains("delete") || it.contains("dismiss") || it.contains("cancel") || it.contains("clear") }
        
        // Extract texts and timing details from RemoteViews
        val extractedData = extractDataFromRemoteViews(notification)
        val remoteViewsTexts = extractedData.textValues
        val hasStopwatchText = remoteViewsTexts.any { it.lowercase().contains("stopwatch") }
        val hasTimerText = remoteViewsTexts.any { it.lowercase().contains("timer") }

        val isStopwatch = title.lowercase().contains("stopwatch") || 
                text.lowercase().contains("stopwatch") || 
                hasStopwatchText ||
                hasLap ||
                (extractedData.baseTimeMs != null && extractedData.isCountDown == false) ||
                (showChronometer && !isCountDown && whenTime > 0L && whenTime < System.currentTimeMillis())
                
        val isTimer = !isStopwatch && (
            title.lowercase().contains("timer") || 
            text.lowercase().contains("timer") || 
            hasTimerText ||
            extractedData.isCountDown == true ||
            isCountDown ||
            actionTitles.any { it.contains("+1") || it.contains("add") || it.contains("cancel") } ||
            (whenTime > System.currentTimeMillis()) ||
            (hasPause || hasResume)
        )

        // Log general Google Clock notification information
        Log.i("GoogleClockCompat", "=== Google Clock Notification ===")
        Log.i("GoogleClockCompat", "  Package: ${sbn.packageName}")
        Log.i("GoogleClockCompat", "  ID: ${sbn.id}")
        Log.i("GoogleClockCompat", "  Category: ${notification.category}")
        Log.i("GoogleClockCompat", "  Title: '$title'")
        Log.i("GoogleClockCompat", "  Text: '$text'")
        Log.i("GoogleClockCompat", "  When: $whenTime")
        Log.i("GoogleClockCompat", "  PostTime: ${sbn.postTime}")
        Log.i("GoogleClockCompat", "  ShowChronometer: $showChronometer")
        Log.i("GoogleClockCompat", "  IsCountDown: $isCountDown")
        Log.i("GoogleClockCompat", "  Action Titles: $actionTitles")
        Log.i("GoogleClockCompat", "  Classification: isStopwatch=$isStopwatch, isTimer=$isTimer")
        Log.i("GoogleClockCompat", "  RemoteViews Texts: $remoteViewsTexts")
        Log.i("GoogleClockCompat", "  RemoteViews BaseMs: ${extractedData.baseTimeMs}, isCountDown: ${extractedData.isCountDown}")
        
        // Log all notification extras in detail
        try {
            val keys = extras.keySet()
            Log.i("GoogleClockCompat", "  Extras bundle content (${keys.size} keys):")
            for (key in keys) {
                Log.i("GoogleClockCompat", "    $key = ${extras.get(key)}")
            }
        } catch (e: Exception) {
            Log.e("GoogleClockCompat", "  Failed to print extras keys: ${e.message}")
        }

        logRemoteViewsDetails(notification.contentView, "contentView")
        logRemoteViewsDetails(notification.bigContentView, "bigContentView")
        logRemoteViewsDetails(notification.headsUpContentView, "headsUpContentView")

        if (isStopwatch && settings.stopwatchEnabled) {
            val isRunning = hasPause || hasLap
            val currentStopwatch = OverlayStateManager.stopwatchState.value
            
            val elapsedMs = if (isRunning && extractedData.baseTimeMs != null) {
                (android.os.SystemClock.elapsedRealtime() - extractedData.baseTimeMs).coerceAtLeast(0L)
            } else {
                var parsed = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
                if (parsed == null) {
                    for (t in remoteViewsTexts) {
                        val p = NovaNotificationListener.parseTimeToMs(t)
                        if (p != null && p > 0L) {
                            parsed = p
                            break
                        }
                    }
                }
                parsed ?: if (currentStopwatch != null) currentStopwatch.elapsedMs else 0L
            }
            
            val startElapsedRealtime = if (isRunning) {
                extractedData.baseTimeMs ?: (android.os.SystemClock.elapsedRealtime() - elapsedMs)
            } else {
                0L
            }
            
            Log.i("GoogleClockCompat", "  Stopwatch Parsing Details:")
            Log.i("GoogleClockCompat", "    isRunning: $isRunning")
            Log.i("GoogleClockCompat", "    elapsedMs: $elapsedMs")
            Log.i("GoogleClockCompat", "    startElapsedRealtime: $startElapsedRealtime")
            Log.i("GoogleClockCompat", "    currentStopwatchState: $currentStopwatch")
            
            return ParsedClockState.Stopwatch(StopwatchState(
                isRunning = isRunning,
                elapsedMs = elapsedMs.coerceAtLeast(0L),
                hasPause = hasPause,
                hasResume = hasResume,
                hasLap = hasLap,
                startElapsedRealtime = startElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        if (isTimer && settings.timerEnabled) {
            val isRunning = hasPause || actionTitles.any { it.contains("+1") }
            val currentTimer = OverlayStateManager.timerState.value
            
            var remainingMs = if (isRunning && extractedData.baseTimeMs != null) {
                (extractedData.baseTimeMs - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            } else {
                var parsed = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
                if (parsed == null) {
                    for (t in remoteViewsTexts) {
                        val p = NovaNotificationListener.parseTimeToMs(t)
                        if (p != null && p > 0L) {
                            parsed = p
                            break
                        }
                    }
                }
                parsed ?: if (currentTimer != null) currentTimer.remainingMs else 0L
            }

            Log.i("GoogleClockCompat", "  Timer Parsing Details:")
            Log.i("GoogleClockCompat", "    isRunning: $isRunning")
            Log.i("GoogleClockCompat", "    raw remainingMs (from base or text): $remainingMs")

            val durationMs = if (currentTimer != null && currentTimer.durationMs > remainingMs) {
                currentTimer.durationMs
            } else {
                remainingMs
            }

            // Clamp remainingMs to durationMs to prevent overshoot
            if (durationMs > 0L) {
                remainingMs = remainingMs.coerceAtMost(durationMs)
            }

            val targetEndElapsedRealtime = if (isRunning) {
                extractedData.baseTimeMs ?: (android.os.SystemClock.elapsedRealtime() + remainingMs)
            } else {
                0L
            }
            
            Log.i("GoogleClockCompat", "    final durationMs: $durationMs")
            Log.i("GoogleClockCompat", "    final remainingMs: $remainingMs")
            Log.i("GoogleClockCompat", "    targetEndElapsedRealtime: $targetEndElapsedRealtime")
            Log.i("GoogleClockCompat", "    currentTimerState: $currentTimer")
            
            return ParsedClockState.Timer(TimerState(
                isRunning = isRunning,
                durationMs = durationMs,
                remainingMs = remainingMs.coerceAtLeast(0L),
                label = if (title.lowercase().contains("timer")) text else title,
                hasPause = hasPause,
                hasResume = hasResume,
                hasReset = hasReset,
                targetEndElapsedRealtime = targetEndElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        return ParsedClockState.None
    }

    private fun extractDataFromRemoteViews(notification: android.app.Notification): ExtractedRemoteViewsData {
        val texts = mutableListOf<String>()
        var baseTimeMs: Long? = null
        var isCountDown: Boolean? = null
        
        val viewsList = listOfNotNull(notification.contentView, notification.bigContentView, notification.headsUpContentView)
        for (views in viewsList) {
            try {
                val actionsField = views.javaClass.getDeclaredField("mActions")
                actionsField.isAccessible = true
                val actions = actionsField.get(views) as? List<*> ?: continue
                for (action in actions) {
                    if (action == null) continue
                    
                    var clazz: Class<*>? = action.javaClass
                    var methodName: String? = null
                    var value: Any? = null
                    
                    while (clazz != null && clazz != Any::class.java) {
                        for (field in clazz.declaredFields) {
                            try {
                                field.isAccessible = true
                                if (field.name == "methodName") {
                                    methodName = field.get(action) as? String
                                } else if (field.name == "value") {
                                    value = field.get(action)
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        clazz = clazz.superclass
                    }
                    
                    if (methodName == "setText" && value is CharSequence) {
                        val str = value.toString().trim()
                        if (str.isNotEmpty()) {
                            texts.add(str)
                        }
                    } else if (methodName == "setBase" && value is Long) {
                        baseTimeMs = value
                    } else if (methodName == "setCountDown" && value is Boolean) {
                        isCountDown = value
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return ExtractedRemoteViewsData(texts, baseTimeMs, isCountDown)
    }

    private fun logRemoteViewsDetails(views: android.widget.RemoteViews?, name: String) {
        if (views == null) return
        Log.i("GoogleClockCompat", "  --- RemoteViews: $name ---")
        try {
            val actionsField = views.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            val actions = actionsField.get(views) as? List<*> ?: return
            Log.i("GoogleClockCompat", "    Total actions: ${actions.size}")
            for ((index, action) in actions.withIndex()) {
                if (action == null) continue
                val actionClass = action.javaClass.simpleName
                Log.i("GoogleClockCompat", "    Action #$index: $actionClass")
                
                var clazz: Class<*>? = action.javaClass
                while (clazz != null && clazz != Any::class.java) {
                    for (field in clazz.declaredFields) {
                        try {
                            field.isAccessible = true
                            val value = field.get(action)
                            if (value != null) {
                                Log.i("GoogleClockCompat", "      Field '${field.name}' (${field.type.simpleName}) = $value")
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    clazz = clazz.superclass
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleClockCompat", "    Error listing actions for $name: ${e.message}")
        }
    }
}

object XiaomiClockProvider : ClockProvider {
    private const val TAG = "XiaomiClockProvider"
    
    override fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val notification = sbn.notification
        val extras = notification.extras ?: android.os.Bundle()
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        val whenTime = notification.`when`
        val showChronometer = extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER, false)
        
        val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getBoolean("android.chronometerCountDown", false)
        } else {
            false
        }

        val hasLap = actionTitles.any { it.contains("lap") || it.contains("split") }
        val hasPause = actionTitles.any { it.contains("pause") || it.contains("stop") }
        val hasResume = actionTitles.any { it.contains("resume") || it.contains("continue") || it.contains("start") || it.contains("play") }
        val hasReset = actionTitles.any { it.contains("reset") || it.contains("restart") || it.contains("delete") || it.contains("dismiss") || it.contains("cancel") || it.contains("clear") }

        val isStopwatch = title.lowercase().contains("stopwatch") || 
                text.lowercase().contains("stopwatch") || 
                hasLap ||
                (showChronometer && !isCountDown && whenTime > 0L && whenTime < System.currentTimeMillis())
                
        val isTimer = !isStopwatch && (
            title.lowercase().contains("timer") || 
            text.lowercase().contains("timer") || 
            isCountDown ||
            actionTitles.any { it.contains("+1") || it.contains("add") || it.contains("cancel") } ||
            extras.containsKey("miui.focus.param") ||
            (hasPause || hasResume)
        )

        if (isStopwatch && settings.stopwatchEnabled) {
            val isRunning = (title.lowercase().contains("running") ||
                    text.lowercase().contains("running") ||
                    hasPause || hasLap) &&
                    !title.lowercase().contains("paused") &&
                    !text.lowercase().contains("paused")
            
            val currentStopwatch = OverlayStateManager.stopwatchState.value
            val elapsedMs = if (showChronometer && whenTime > 0L && isRunning) {
                System.currentTimeMillis() - whenTime
            } else {
                val parsed = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
                parsed ?: if (currentStopwatch != null) currentStopwatch.elapsedMs else 0L
            }
            val startElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() - elapsedMs
            } else {
                0L
            }
            return ParsedClockState.Stopwatch(StopwatchState(
                isRunning = isRunning,
                elapsedMs = elapsedMs.coerceAtLeast(0L),
                hasPause = hasPause,
                hasResume = hasResume,
                hasLap = hasLap,
                startElapsedRealtime = startElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        if (isTimer && settings.timerEnabled) {
            var miuiTimerWhen: Long? = null
            var miuiTimerSystemCurrent: Long? = null
            var miuiIsRunning: Boolean? = null
            var miuiDurationMs: Long? = null
            
            val miuiFocusParam = extras.getString("miui.focus.param")
            if (!miuiFocusParam.isNullOrEmpty()) {
                try {
                    val json = org.json.JSONObject(miuiFocusParam)
                    val timerWhen = json.optLong("timerWhen", 0L)
                    val timerSystemCurrent = json.optLong("timerSystemCurrent", 0L)
                    val timerTotal = json.optLong("timerTotal", 0L)
                    val timerType = json.optInt("timerType", 0)
                    
                    if (timerWhen > 0 && timerSystemCurrent > 0) {
                        miuiTimerWhen = timerWhen
                        miuiTimerSystemCurrent = timerSystemCurrent
                        miuiIsRunning = timerType == -1
                        miuiDurationMs = timerTotal
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse miui.focus.param: ${e.message}")
                }
            }

            val isRunning = if (miuiIsRunning != null) {
                miuiIsRunning || hasPause || actionTitles.any { it.contains("+1") }
            } else {
                hasPause || actionTitles.any { it.contains("+1") } || (showChronometer && isCountDown)
            }

            val currentTimer = OverlayStateManager.timerState.value
            val textRemainingMs = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)

            var remainingMs = (if (miuiTimerWhen != null && miuiTimerSystemCurrent != null) {
                if (isRunning) {
                    miuiTimerWhen - System.currentTimeMillis()
                } else {
                    miuiTimerWhen - miuiTimerSystemCurrent
                }
            } else if (showChronometer && whenTime > 0L) {
                whenTime - System.currentTimeMillis()
            } else {
                textRemainingMs ?: if (currentTimer != null) currentTimer.remainingMs else 0L
            }).coerceAtLeast(0L)
            
            // Align remaining time to text value if close
            if (textRemainingMs != null) {
                val diff = Math.abs(remainingMs - textRemainingMs)
                if (diff < 1500L) {
                    remainingMs = textRemainingMs
                }
            }

            val durationMs = if (miuiDurationMs != null && miuiDurationMs > 0L) {
                miuiDurationMs
            } else {
                if (currentTimer != null && currentTimer.durationMs > remainingMs) {
                    currentTimer.durationMs
                } else {
                    remainingMs
                }
            }

            // Clamp remainingMs to durationMs to prevent overshoot
            if (durationMs > 0L) {
                remainingMs = remainingMs.coerceAtMost(durationMs)
            }

            val targetEndElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() + remainingMs
            } else {
                0L
            }
            
            return ParsedClockState.Timer(TimerState(
                isRunning = isRunning,
                durationMs = durationMs,
                remainingMs = remainingMs,
                label = if (title.lowercase().contains("timer")) text else title,
                hasPause = hasPause,
                hasResume = hasResume,
                hasReset = hasReset,
                targetEndElapsedRealtime = targetEndElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        return ParsedClockState.None
    }
}

object SamsungClockProvider : ClockProvider {
    private const val TAG = "SamsungClockProvider"
    
    override fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val notification = sbn.notification
        val extras = notification.extras ?: android.os.Bundle()
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        val whenTime = notification.`when`
        val showChronometer = extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER, false)
        
        val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getBoolean("android.chronometerCountDown", false)
        } else {
            false
        }

        val hasLap = actionTitles.any { it.contains("lap") || it.contains("split") }
        val hasPause = actionTitles.any { it.contains("pause") || it.contains("stop") }
        val hasResume = actionTitles.any { it.contains("resume") || it.contains("continue") || it.contains("start") || it.contains("play") }
        val hasReset = actionTitles.any { it.contains("reset") || it.contains("restart") || it.contains("delete") || it.contains("dismiss") || it.contains("cancel") || it.contains("clear") }
        
        val isStopwatch = title.lowercase().contains("stopwatch") || 
                text.lowercase().contains("stopwatch") || 
                hasLap ||
                (actionTitles.contains("pause") && actionTitles.contains("reset") && !actionTitles.contains("+1"))
                
        val isTimer = !isStopwatch && (
            title.lowercase().contains("timer") || 
            text.lowercase().contains("timer") || 
            isCountDown ||
            actionTitles.any { it.contains("+1") || it.contains("add") || it.contains("cancel") } ||
            // Fallback for One UI 8.5 (F16)
            (hasPause || hasResume)
        )

        if (isStopwatch && settings.stopwatchEnabled) {
            val isRunning = hasPause || hasLap
            val currentStopwatch = OverlayStateManager.stopwatchState.value
            val elapsedMs = if (showChronometer && whenTime > 0L && isRunning) {
                System.currentTimeMillis() - whenTime
            } else {
                val parsed = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
                parsed ?: if (currentStopwatch != null) currentStopwatch.elapsedMs else 0L
            }
            val startElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() - elapsedMs
            } else {
                0L
            }
            return ParsedClockState.Stopwatch(StopwatchState(
                isRunning = isRunning,
                elapsedMs = elapsedMs.coerceAtLeast(0L),
                hasPause = hasPause,
                hasResume = hasResume,
                hasLap = hasLap,
                startElapsedRealtime = startElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        if (isTimer && settings.timerEnabled) {
            val isRunning = hasPause || actionTitles.any { it.contains("+1") }
            val currentTimer = OverlayStateManager.timerState.value
            
            // Align remaining time to text representation if available
            val textRemainingMs = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
            
            var remainingMs = if (showChronometer && whenTime > 0L) {
                (whenTime - System.currentTimeMillis()).coerceAtLeast(0L)
            } else {
                textRemainingMs ?: if (currentTimer != null) currentTimer.remainingMs else 0L
            }

            // Align chronometer remaining to the text value if they are close (within 1.5 seconds)
            if (textRemainingMs != null && showChronometer && whenTime > 0L) {
                val diff = Math.abs(remainingMs - textRemainingMs)
                if (diff < 1500L) {
                    remainingMs = textRemainingMs
                }
            }
            
            val durationMs = if (remainingMs > 0) {
                remainingMs
            } else {
                if (currentTimer != null && currentTimer.durationMs > 0L) {
                    currentTimer.durationMs
                } else {
                    0L
                }
            }

            // Clamp remainingMs to durationMs to prevent overshoot
            if (durationMs > 0L) {
                remainingMs = remainingMs.coerceAtMost(durationMs)
            }

            val targetEndElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() + remainingMs
            } else {
                0L
            }
            
            return ParsedClockState.Timer(TimerState(
                isRunning = isRunning,
                durationMs = durationMs,
                remainingMs = remainingMs,
                label = if (title.lowercase().contains("timer")) text else title,
                hasPause = hasPause,
                hasResume = hasResume,
                hasReset = hasReset,
                targetEndElapsedRealtime = targetEndElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        return ParsedClockState.None
    }
}

object DefaultClockProvider : ClockProvider {
    private const val TAG = "DefaultClockProvider"
    
    override fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val notification = sbn.notification
        val extras = notification.extras ?: android.os.Bundle()
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        val whenTime = notification.`when`
        val showChronometer = extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER, false)
        
        val isCountDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getBoolean("android.chronometerCountDown", false)
        } else {
            false
        }

        val hasLap = actionTitles.any { it.contains("lap") || it.contains("split") }
        val hasPause = actionTitles.any { it.contains("pause") || it.contains("stop") }
        val hasResume = actionTitles.any { it.contains("resume") || it.contains("continue") || it.contains("start") || it.contains("play") }
        val hasReset = actionTitles.any { it.contains("reset") || it.contains("restart") || it.contains("delete") || it.contains("dismiss") || it.contains("cancel") || it.contains("clear") }
        
        val isStopwatch = title.lowercase().contains("stopwatch") || 
                text.lowercase().contains("stopwatch") || 
                hasLap ||
                (actionTitles.contains("pause") && actionTitles.contains("reset") && !actionTitles.contains("+1"))
                
        val isTimer = !isStopwatch && (
            title.lowercase().contains("timer") || 
            text.lowercase().contains("timer") || 
            isCountDown ||
            actionTitles.any { it.contains("+1") || it.contains("add") || it.contains("cancel") } ||
            (hasPause || hasResume)
        )

        if (isStopwatch && settings.stopwatchEnabled) {
            val isRunning = hasPause || hasLap
            val currentStopwatch = OverlayStateManager.stopwatchState.value
            val elapsedMs = if (showChronometer && whenTime > 0L && isRunning) {
                System.currentTimeMillis() - whenTime
            } else {
                val parsed = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
                parsed ?: if (currentStopwatch != null) currentStopwatch.elapsedMs else 0L
            }
            val startElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() - elapsedMs
            } else {
                0L
            }
            return ParsedClockState.Stopwatch(StopwatchState(
                isRunning = isRunning,
                elapsedMs = elapsedMs.coerceAtLeast(0L),
                hasPause = hasPause,
                hasResume = hasResume,
                hasLap = hasLap,
                startElapsedRealtime = startElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        if (isTimer && settings.timerEnabled) {
            val isRunning = hasPause || actionTitles.any { it.contains("+1") }
            val currentTimer = OverlayStateManager.timerState.value
            
            // Align remaining time to text representation if available
            val textRemainingMs = NovaNotificationListener.parseTimeToMs(text) ?: NovaNotificationListener.parseTimeToMs(title)
            
            var remainingMs = if (showChronometer && whenTime > 0L) {
                (whenTime - System.currentTimeMillis()).coerceAtLeast(0L)
            } else {
                textRemainingMs ?: if (currentTimer != null) currentTimer.remainingMs else 0L
            }

            // Align chronometer remaining to the text value if they are close (within 1.5 seconds)
            if (textRemainingMs != null && showChronometer && whenTime > 0L) {
                val diff = Math.abs(remainingMs - textRemainingMs)
                if (diff < 1500L) {
                    remainingMs = textRemainingMs
                }
            }
            
            val durationMs = if (remainingMs > 0) {
                remainingMs
            } else {
                if (currentTimer != null && currentTimer.durationMs > 0L) {
                    currentTimer.durationMs
                } else {
                    0L
                }
            }

            // Clamp remainingMs to durationMs to prevent overshoot
            if (durationMs > 0L) {
                remainingMs = remainingMs.coerceAtMost(durationMs)
            }

            val targetEndElapsedRealtime = if (isRunning) {
                android.os.SystemClock.elapsedRealtime() + remainingMs
            } else {
                0L
            }
            
            return ParsedClockState.Timer(TimerState(
                isRunning = isRunning,
                durationMs = durationMs,
                remainingMs = remainingMs,
                label = if (title.lowercase().contains("timer")) text else title,
                hasPause = hasPause,
                hasResume = hasResume,
                hasReset = hasReset,
                targetEndElapsedRealtime = targetEndElapsedRealtime,
                showSeconds = settings.showSeconds
            ))
        }

        return ParsedClockState.None
    }
}

object ClockCompatibilityLayer {
    private const val TAG = "ClockCompatibilityLayer"

    private val providers = mapOf(
        "com.google.android.deskclock" to LoggingClockProvider("GoogleClock", GoogleClockProvider),
        "com.sec.android.app.clockpackage" to LoggingClockProvider("SamsungClock", SamsungClockProvider),
        "com.android.deskclock" to LoggingClockProvider("XiaomiClock", XiaomiClockProvider),
        "com.oneplus.deskclock" to LoggingClockProvider("OnePlusClock", DefaultClockProvider),
        "com.coloros.alarmclock" to LoggingClockProvider("OppoRealmeClock", DefaultClockProvider),
        "com.android.BBKClock" to LoggingClockProvider("VivoClock", DefaultClockProvider),
        "com.nothing.deskclock" to LoggingClockProvider("NothingClock", DefaultClockProvider)
    )

    fun parse(sbn: StatusBarNotification, settings: NovaSettings, context: Context): ParsedClockState {
        val packageName = sbn.packageName
        val provider = providers[packageName] ?: LoggingClockProvider("FallbackClock", DefaultClockProvider)
        return provider.parse(sbn, settings, context)
    }
}
