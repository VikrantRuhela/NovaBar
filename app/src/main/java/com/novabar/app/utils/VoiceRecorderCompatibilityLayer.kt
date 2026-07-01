package com.novabar.app.utils

import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.novabar.app.data.NovaSettings
import com.novabar.app.domain.VoiceRecorderState
import com.novabar.app.domain.OverlayStateManager
import java.util.regex.Pattern

sealed class ParsedVoiceRecorderState {
    data class Active(val state: VoiceRecorderState) : ParsedVoiceRecorderState()
    object None : ParsedVoiceRecorderState()
}

object VoiceRecorderCompatibilityLayer {
    private const val TAG = "VoiceRecorderCompat"

    // Supported Whitelisted Voice Recorder Packages
    private val whitelistPackages = setOf(
        "com.sec.android.app.voicenote", // Samsung
        "com.google.android.apps.recorder", // Google Recorder
        "com.coloros.soundrecorder", // Oppo / Realme
        "com.oneplus.soundrecorder", // OnePlus
        "com.motorola.audiorecorder", // Motorola
        "com.android.soundrecorder", // HyperOS / Xiaomi
        "com.nothing.soundrecorder", // Nothing OS
        "com.android.bbksoundrecorder" // Vivo
    )

    fun parse(
        sbn: StatusBarNotification,
        settings: NovaSettings,
        context: Context,
        isActiveSession: Boolean = false
    ): ParsedVoiceRecorderState {
        val packageName = sbn.packageName.lowercase()
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val subtext = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        
        // Exclude system screen recorders and call recording to prevent false positives
        if (packageName.contains("screenrecord") || 
            packageName.contains("systemui") || 
            title.lowercase().contains("screen recording") || 
            text.lowercase().contains("screen recording") || 
            title.lowercase().contains("call recording") || 
            text.lowercase().contains("recording call")) {
            android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: parse rejected because it matches screen recorder/call recording exclusion patterns (pkg=$packageName, title='$title', text='$text').")
            return ParsedVoiceRecorderState.None
        }

        var confidence = 0
        val reasons = mutableListOf<String>()

        // 1. Package Name Validation
        if (whitelistPackages.contains(packageName)) {
            confidence += 50
            reasons.add("Whitelisted package: $packageName (+50)")
        } else if (packageName.contains("recorder") || 
                   packageName.contains("voicenote") || 
                   packageName.contains("soundrec") || 
                   packageName.contains("audiorec") || 
                   packageName.contains("dictaphone")) {
            confidence += 30
            reasons.add("Pattern-matched recorder package: $packageName (+30)")
        } else {
            // Not a recorder app
            android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: parse rejected because package name '$packageName' is not a whitelisted/pattern-matched recorder package.")
            return ParsedVoiceRecorderState.None
        }

        // 2. Ongoing Status Validation
        val isOngoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) {
            confidence += 20
            reasons.add("Ongoing event (+20)")
        }

        // 3. Category Validation
        val category = notification.category ?: ""
        if (category == android.app.Notification.CATEGORY_SERVICE || 
            category == android.app.Notification.CATEGORY_TRANSPORT || 
            category == android.app.Notification.CATEGORY_STATUS) {
            confidence += 15
            reasons.add("Category matches service/status/transport: $category (+15)")
        }

        // 4. Content / Extras Keyword Validation
        val titleLower = title.lowercase()
        val textLower = text.lowercase()
        val subtextLower = subtext.lowercase()
        
        val hasKeywords = titleLower.contains("recording") || 
                          titleLower.contains("voice memo") || 
                          titleLower.contains("recorder") || 
                          titleLower.contains("dictaphone") ||
                          textLower.contains("recording") || 
                          textLower.contains("voice memo") || 
                          textLower.contains("recorder") ||
                          subtextLower.contains("recording")
                          
        if (hasKeywords) {
            confidence += 15
            reasons.add("Title/Text contains recording keywords (+15)")
        }

        // 5. Actions / Intents Validation
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString().lowercase() }
        
        val hasPauseOrResume = actionTitles.any { it.contains("pause") || it.contains("resume") || it.contains("record") || it.contains("continue") || it.contains("start") }
        val hasStopOrSave = actionTitles.any { it.contains("stop") || it.contains("save") || it.contains("discard") || it.contains("done") || it.contains("delete") || it.contains("finish") }
        
        if (hasPauseOrResume) {
            confidence += 15
            reasons.add("Has pause/resume/start actions (+15)")
        }
        if (hasStopOrSave) {
            confidence += 15
            reasons.add("Has stop/save/delete actions (+15)")
        }

        // 6. RemoteViews Extraction & Visual Validation
        val extractedTexts = mutableListOf<String>()
        val remoteViewsIntents = mutableListOf<Pair<String, android.app.PendingIntent>>()
        val viewsList = listOfNotNull(notification.contentView, notification.bigContentView, notification.headsUpContentView)
        for (views in viewsList) {
            try {
                val actionsField = views.javaClass.getDeclaredField("mActions")
                actionsField.isAccessible = true
                val viewActions = actionsField.get(views) as? List<*> ?: continue
                for (action in viewActions) {
                    if (action == null) continue
                    
                    var methodName: String? = null
                    var value: Any? = null
                    var viewId = 0
                    var pendingIntent: android.app.PendingIntent? = null
                    
                    var clazz: Class<*>? = action.javaClass
                    while (clazz != null && clazz != Any::class.java) {
                        for (field in clazz.declaredFields) {
                            try {
                                field.isAccessible = true
                                val fieldVal = field.get(action)
                                if (field.name == "methodName" && fieldVal is String) {
                                    methodName = fieldVal
                                } else if (field.name == "value") {
                                    value = fieldVal
                                } else if (field.name == "viewId" && fieldVal is Int) {
                                    viewId = fieldVal
                                }
                            } catch (e: Exception) {}
                        }
                        clazz = clazz.superclass
                    }
                    pendingIntent = extractPendingIntent(action)
                    
                    if (methodName == "setText" && value is CharSequence) {
                        val str = value.toString().trim()
                        if (str.isNotEmpty()) {
                            extractedTexts.add(str)
                        }
                    }
                    
                    if (pendingIntent != null && viewId != 0) {
                        val resName = try {
                            val packageContext = context.createPackageContext(packageName, 0)
                            packageContext.resources.getResourceEntryName(viewId).lowercase()
                        } catch (e: Exception) {
                            "id_$viewId"
                        }
                        remoteViewsIntents.add(Pair(resName, pendingIntent))
                    }
                }
            } catch (e: Exception) {}
        }

        // Check for ticking duration matches
        // Supports m:ss, mm:ss, hh:mm:ss (supports single digit minutes like 0:05, 1:23)
        val durationRegex = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\b")
        var hasRemoteViewsDuration = false
        var parsedDurationMs = 0L

        fun extractDuration(str: String): Long? {
            val matcher = durationRegex.matcher(str)
            if (matcher.find()) {
                val hrsStr = matcher.group(1)
                val minsStr = matcher.group(2)
                val secsStr = matcher.group(3)
                
                val hrs = hrsStr?.toIntOrNull() ?: 0
                val mins = minsStr?.toIntOrNull() ?: 0
                val secs = secsStr?.toIntOrNull() ?: 0
                
                return (hrs * 3600L + mins * 60L + secs) * 1000L
            }
            return null
        }

        // Try extracting duration from all texts
        var durationSource = ""
        val potentialSources = listOf(title, text, subtext) + extractedTexts
        for (source in potentialSources) {
            val parsed = extractDuration(source)
            if (parsed != null) {
                parsedDurationMs = parsed
                hasRemoteViewsDuration = true
                durationSource = source
                break
            }
        }

        if (hasRemoteViewsDuration) {
            confidence += 30
            reasons.add("Ticking duration extracted from '$durationSource': $parsedDurationMs ms (+30)")
        }

        val hasRemoteViewsRecorderLabels = extractedTexts.any { txt ->
            val lower = txt.lowercase()
            lower.contains("recording") || lower.contains("recorder") || lower.contains("dictaphone") || lower.contains("voice memo")
        }
        if (hasRemoteViewsRecorderLabels) {
            confidence += 15
            reasons.add("Recording labels found in RemoteViews (+15)")
        }

        android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: Confidence Score Checklist | pkg=$packageName | score=$confidence | reasons=$reasons")

        // Reject if threshold not met
        if (confidence < 65) {
            val hasDurationOrActions = parsedDurationMs > 0L || actions.isNotEmpty() || hasRemoteViewsDuration
            if (isActiveSession && hasDurationOrActions) {
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: Accepting active session update despite confidence $confidence (isActiveSession=true, hasDurationOrActions=true)")
            } else {
                android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: Classification REJECTED: Confidence $confidence is below threshold 65 (isActiveSession=$isActiveSession, hasDurationOrActions=$hasDurationOrActions)")
                return ParsedVoiceRecorderState.None
            }
        }

        var pauseIntent: android.app.PendingIntent? = null
        var resumeIntent: android.app.PendingIntent? = null
        var stopIntent: android.app.PendingIntent? = null

        val pauseKeywords = listOf("pause", "pausa", "reprendre", "一時停止", "일시정지", "暂停", "रोकें", "hold")
        val resumeKeywords = listOf("resume", "play", "continue", "start", "reanudar", "reproducir", "reprendre", "lire", "fortsetzen", "wiedergeben", "retomar", "riprendi", "riproduci", "再開", "再生", "재개", "继续", "播放", "продолжить", "воспроизвести", "चालू करें")
        val stopKeywords = listOf("stop", "save", "discard", "done", "delete", "finish", "detener", "parar", "arrêter", "stoppen", "halt", "ferma", "interrompi", "중지", "停止", "बंद करें", "стоп")

        // A. Extract from standard Notification.Actions using multi-language keywords
        for (action in actions) {
            val actionTitle = action.title.toString().lowercase()
            val intent = action.actionIntent ?: continue
            
            val matchesPause = pauseKeywords.any { actionTitle.contains(it) }
            val matchesPlay = resumeKeywords.any { actionTitle.contains(it) }
            
            if (matchesPause && matchesPlay) {
                if (pauseIntent == null) pauseIntent = intent
                if (resumeIntent == null) resumeIntent = intent
            } else if (matchesPause) {
                if (pauseIntent == null) pauseIntent = intent
            } else if (matchesPlay) {
                if (resumeIntent == null) resumeIntent = intent
            }
            
            if (stopKeywords.any { actionTitle.contains(it) }) {
                if (stopIntent == null) stopIntent = intent
            }
        }

        // B. Extract from RemoteViews PendingIntents (resource ID names are in English code symbols!)
        for (pair in remoteViewsIntents) {
            val resName = pair.first.lowercase()
            val intent = pair.second
            
            val matchesPause = resName.contains("pause") || resName.contains("hold")
            val matchesPlay = resName.contains("resume") || resName.contains("play") || resName.contains("start") || resName.contains("record") || resName.contains("continue")
            
            if (matchesPause && matchesPlay) {
                if (pauseIntent == null) pauseIntent = intent
                if (resumeIntent == null) resumeIntent = intent
            } else if (matchesPause) {
                if (pauseIntent == null) pauseIntent = intent
            } else if (matchesPlay) {
                if (resumeIntent == null) resumeIntent = intent
            }
            
            if (resName.contains("stop") || resName.contains("save") || resName.contains("discard") || resName.contains("delete") || resName.contains("done") || resName.contains("finish") || resName.contains("close")) {
                if (stopIntent == null) stopIntent = intent
            }
        }

        // C. Fallback for whitelisted packages: Action 0 is play/pause toggle, Action 1 is stop/save.
        if (whitelistPackages.contains(packageName) && actions.isNotEmpty()) {
            if (pauseIntent == null) pauseIntent = actions[0].actionIntent
            if (resumeIntent == null) resumeIntent = actions[0].actionIntent
            if (stopIntent == null && actions.size > 1) {
                stopIntent = actions[1].actionIntent
            }
        }

        // D. Parse foreign package layout name to determine state (100% language independent!)
        val layoutName = try {
            val packageContext = context.createPackageContext(packageName, 0)
            packageContext.resources.getResourceEntryName(notification.contentView.layoutId).lowercase()
        } catch (e: Exception) {
            ""
        }

        val hasPauseKeyword = actionTitles.any { act -> pauseKeywords.any { act.contains(it) } } ||
                             remoteViewsIntents.any { (resName, _) -> resName.contains("pause") || resName.contains("hold") }
                             
        val hasResumeKeyword = actionTitles.any { act -> resumeKeywords.any { act.contains(it) } } ||
                              remoteViewsIntents.any { (resName, _) -> resName.contains("resume") || resName.contains("play") || resName.contains("start") || resName.contains("record") || resName.contains("continue") }

        val localizedPauseKeywords = listOf(
            "pause", "pausa", "pausiert", "suspendu", "一時停止", "일시정지", "暂停", "रोकें", "hold",
            "pausado", "pausada", "pauser", "перерыв", "пауза", "وقف", "กึ่ง", "dừng", "susz", "stoppet"
        )

        // E. Determine isRecording using layoutName, intents, keywords, and ongoing flag
        val isRecording = if (layoutName.contains("paused") || layoutName.contains("pause")) {
            false
        } else if (layoutName.contains("recording") || layoutName.contains("record")) {
            true
        } else if (hasPauseKeyword && !hasResumeKeyword) {
            true
        } else if (hasResumeKeyword && !hasPauseKeyword) {
            false
        } else {
            // Fallback to text keywords
            val isPausedByKeywords = titleLower.contains("paused") || 
                                     titleLower.contains("pausado") ||
                                     titleLower.contains("suspendido") ||
                                     textLower.contains("paused") || 
                                     textLower.contains("pausado") ||
                                     localizedPauseKeywords.any { kw ->
                                         titleLower.contains(kw) || 
                                         textLower.contains(kw) || 
                                         extractedTexts.any { it.lowercase().contains(kw) }
                                     }
            if (isPausedByKeywords) {
                false
            } else {
                isOngoing
            }
        }
        val isPaused = !isRecording

        val appIcon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }

        val appName = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            ""
        }

        val current = OverlayStateManager.voiceRecorderState.value
        val startElapsed = if (isRecording) {
            if (current != null && current.isRecording && Math.abs(current.durationMs - parsedDurationMs) < 2000L) {
                current.startElapsedRealtime
            } else {
                android.os.SystemClock.elapsedRealtime() - parsedDurationMs
            }
        } else {
            0L
        }

        val hasPause = (pauseIntent != null)
        val hasResume = (resumeIntent != null)
        val hasStop = (stopIntent != null)

        // Log diagnostics to DeveloperLogger for verification
        try {
            val actionsStr = actions.mapIndexed { idx, act ->
                "Action #$idx: title='${act.title}', hasIntent=${act.actionIntent != null}"
            }.joinToString("\n")
            
            val remoteIntentsStr = remoteViewsIntents.map { (resName, _) ->
                "View ID '$resName': hasIntent=true"
            }.joinToString("\n")
            
            val extrasStr = DeveloperLogger.bundleToReadableString(extras)
            
            val logMessage = "Package: $packageName\n" +
                             "Title: $title\n" +
                             "Text: $text\n" +
                             "Ongoing: $isOngoing\n" +
                             "Category: $category\n" +
                             "Layout Name: $layoutName\n" +
                             "Action Count: ${actions.size}\n" +
                             "Public Actions:\n$actionsStr\n" +
                             "RemoteViews Click Intents:\n$remoteIntentsStr\n" +
                             "RemoteViews Extracted Texts: $extractedTexts\n" +
                             "Resolved Actions: pause=${pauseIntent != null}, resume=${resumeIntent != null}, stop=${stopIntent != null}\n" +
                             "Final State: isRecording=$isRecording, duration=$parsedDurationMs ms\n" +
                             "Extras:\n$extrasStr"
                             
            DeveloperLogger.log(context, "VOICE_RECORDER_DETECTION", logMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to DeveloperLogger", e)
        }

        val state = VoiceRecorderState(
            isRecording = isRecording,
            isPaused = isPaused,
            durationMs = parsedDurationMs,
            startElapsedRealtime = startElapsed,
            hasPause = hasPause,
            hasResume = hasResume,
            hasStop = hasStop,
            appIcon = appIcon,
            appName = appName,
            packageName = packageName,
            pauseIntent = pauseIntent,
            resumeIntent = resumeIntent,
            stopIntent = stopIntent
        )

        val dumpStr = "contentView: " + dumpRemoteViewsActions(context, packageName, notification.contentView) + "\n" +
                      "bigContentView: " + dumpRemoteViewsActions(context, packageName, notification.bigContentView) + "\n" +
                      "headsUpContentView: " + dumpRemoteViewsActions(context, packageName, notification.headsUpContentView)

        android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 3: parse accepted | pkg=$packageName | isRecording=$isRecording | isPaused=$isPaused | durationMs=$parsedDurationMs | startElapsed=$startElapsed | layoutName=$layoutName | hasPause=$hasPause | hasResume=$hasResume | hasStop=$hasStop\n$dumpStr")
        Log.i(TAG, "[$packageName] Classification ACCEPTED. State: isRecording=$isRecording, durationMs=$parsedDurationMs")
        return ParsedVoiceRecorderState.Active(state)
    }

    private fun dumpRemoteViewsActions(context: Context, packageName: String, views: android.widget.RemoteViews?): String {
        if (views == null) return "null"
        val builder = java.lang.StringBuilder()
        try {
            val actionsField = views.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            val viewActions = actionsField.get(views) as? List<*> ?: return "empty actions"
            
            val packageContext = try {
                context.createPackageContext(packageName, 0)
            } catch (e: Exception) {
                null
            }
            
            builder.append("RemoteViews Actions (count=${viewActions.size}):\n")
            for (action in viewActions) {
                if (action == null) continue
                val className = action.javaClass.simpleName
                builder.append("  [$className]: ")
                
                var methodName: String? = null
                var value: Any? = null
                var viewId = 0
                var pendingIntent: android.app.PendingIntent? = null
                
                var clazz: Class<*>? = action.javaClass
                while (clazz != null && clazz != Any::class.java) {
                    for (field in clazz.declaredFields) {
                        try {
                            field.isAccessible = true
                            val fieldVal = field.get(action)
                            android.util.Log.d("VoiceRecorder", "  [dumpRemoteViewsActions] Class: ${clazz.name} | Field: ${field.name} | Type: ${field.type.name} | Value: $fieldVal")
                            if (field.name == "methodName" && fieldVal is String) {
                                methodName = fieldVal
                            } else if (field.name == "value") {
                                value = fieldVal
                            } else if (field.name == "viewId" && fieldVal is Int) {
                                viewId = fieldVal
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("VoiceRecorder", "  [dumpRemoteViewsActions] Error reading field ${field.name} in ${clazz.name}: ${e.message}", e)
                        }
                    }
                    clazz = clazz.superclass
                }
                pendingIntent = extractPendingIntent(action)
                
                val resName = if (viewId != 0 && packageContext != null) {
                    try {
                        packageContext.resources.getResourceEntryName(viewId)
                    } catch (e: Exception) {
                        "id_$viewId"
                    }
                } else {
                    "0"
                }
                
                builder.append("viewId=$viewId ($resName)")
                if (methodName != null) builder.append(", methodName=$methodName")
                if (value != null) builder.append(", value=$value")
                if (pendingIntent != null) builder.append(", pendingIntent=$pendingIntent")
                builder.append("\n")
            }
        } catch (e: java.lang.Exception) {
            builder.append("failed to dump: ${e.message}\n")
        }
        return builder.toString()
    }

    private fun extractPendingIntent(obj: Any?, depth: Int = 0): android.app.PendingIntent? {
        if (obj == null || depth > 3) return null
        if (obj is android.app.PendingIntent) return obj
        
        val className = obj.javaClass.name
        if (className.startsWith("android.os.") || 
            className.startsWith("android.content.res.") || 
            className.startsWith("java.lang.")) {
            return null
        }
        
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null && clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                try {
                    field.isAccessible = true
                    val value = field.get(obj) ?: continue
                    android.util.Log.d("VoiceRecorder", "  [extractPendingIntent] Depth $depth | Field ${field.name} in ${clazz.name} has value class ${value.javaClass.name}")
                    if (value is android.app.PendingIntent) {
                        android.util.Log.d("VoiceRecorder", "  [extractPendingIntent] Found PendingIntent: $value")
                        return value
                    }
                    if (value.javaClass.name.contains("RemoteResponse") || 
                        value.javaClass.name.contains("ClickResponse") || 
                        value.javaClass.name.contains("SetOnClick") ||
                        value.javaClass.name.startsWith("android.view.RemoteViews") ||
                        value.javaClass.name.startsWith("android.widget.RemoteViews")) {
                        val found = extractPendingIntent(value, depth + 1)
                        if (found != null) return found
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VoiceRecorder", "  [extractPendingIntent] Error reading field ${field.name} in ${clazz.name}: ${e.message}", e)
                }
            }
            clazz = clazz.superclass
        }
        return null
    }
}
