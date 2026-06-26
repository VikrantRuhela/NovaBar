package com.novabar.app.utils

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.RemoteViews
import com.novabar.app.domain.ManeuverType
import java.util.Locale

interface NavigationProvider {
    fun parse(sbn: StatusBarNotification, context: Context): ManeuverType
}

class LoggingNavigationProvider(
    private val name: String,
    private val delegate: NavigationProvider
) : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subtext = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val category = notification.category ?: "null"
        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.map { it.title.toString() }
        
        // Find raw maneuver values dynamically in extras
        var rawManeuver: String? = null
        try {
            for (key in extras.keySet()) {
                val lowercaseKey = key.lowercase()
                if (lowercaseKey.contains("maneuver") || 
                    lowercaseKey.contains("turn") || 
                    lowercaseKey.contains("direction") || 
                    lowercaseKey.contains("icon")) {
                    val value = extras.get(key)
                    if (value != null) {
                        rawManeuver = "$key=$value"
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Safe ignore
        }
        
        // Call delegate to parse
        val result = delegate.parse(sbn, context)
        
        // Log details to developer logger and Logcat
        val logMsg = StringBuilder().apply {
            append("=== Navigation Update [$name] ===\n")
            append("Package: ${sbn.packageName}\n")
            append("Category: $category\n")
            append("Title: '$title'\n")
            append("Text: '$text'\n")
            append("Subtext: '$subtext'\n")
            append("Actions: $actionTitles\n")
            append("Small Icon: ${notification.smallIcon}\n")
            append("Large Icon: ${notification.getLargeIcon() != null}\n")
            append("RemoteViews: contentView=${notification.contentView != null}, bigContentView=${notification.bigContentView != null}, headsUpContentView=${notification.headsUpContentView != null}\n")
            append("Raw Maneuver: ${rawManeuver ?: "Not found in extras"}\n")
            append("Final Maneuver Selected: $result\n")
            append("=================================")
        }.toString()
        
        Log.i("NovaBar-Navigation", logMsg)
        DeveloperLogger.log(context, "NavigationCompat", logMsg)
        
        return result
    }
}

object DefaultNavigationProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object GoogleMapsProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val maneuverFromResource = try {
            val packageContext = context.createPackageContext(sbn.packageName, 0)
            val resources = packageContext.resources
            val drawableResId =
                extractFirstRemoteDrawableResId(notification.contentView, resources)
                    ?: extractFirstRemoteDrawableResId(notification.bigContentView, resources)
                    ?: extractFirstRemoteDrawableResId(notification.headsUpContentView, resources)

            if (drawableResId != null && drawableResId > 0) {
                val resName = resources.getResourceEntryName(drawableResId)
                Log.d("NovaBar-Navigation", "Extracted Google Maps drawable resource ID: $drawableResId, Name: $resName")
                mapResourceNameToManeuverType(resName)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("NovaBar-Navigation", "Failed to extract RemoteViews drawable resource from Google Maps", e)
            null
        }

        if (maneuverFromResource != null) {
            Log.i("NovaBar-Navigation", "Successfully parsed maneuver from Google Maps resource name: $maneuverFromResource")
            return maneuverFromResource
        }

        Log.i("NovaBar-Navigation", "Falling back to text parsing for Google Maps notification")
        return NavigationTextParser.parse(title, text)
    }

    fun extractFirstRemoteDrawableResId(
        rv: RemoteViews?,
        resources: android.content.res.Resources
    ): Int? {
        val actions = getRemoteViewActions(rv)
        if (actions.isEmpty()) {
            return null
        }

        for (action in actions) {
            val fields = collectAllDeclaredFields(action.javaClass)
            val actionClassName = action.javaClass.name.lowercase(Locale.ROOT)
            var methodName = ""
            val candidates = mutableListOf<Pair<String, Int>>()

            for (field in fields) {
                val value = runCatching {
                    field.isAccessible = true
                    field.get(action)
                }.getOrNull() ?: continue
                val normalizedName = field.name.removePrefix("m").lowercase(Locale.ROOT)
                if (normalizedName == "methodname" && value is String) {
                    methodName = value.lowercase(Locale.ROOT)
                }
                candidates.addAll(extractDrawableResIdCandidates(value, normalizedName))
            }

            val looksLikeImageAction =
                methodName.contains("icon") ||
                        methodName.contains("image") ||
                        methodName.contains("drawable") ||
                        actionClassName.contains("icon") ||
                        actionClassName.contains("image") ||
                        actionClassName.contains("drawable")
            if (!looksLikeImageAction) {
                continue
            }

            for ((fieldName, resId) in candidates) {
                val isResourceField =
                    fieldName.contains("res") ||
                            fieldName.contains("icon") ||
                            fieldName.contains("drawable") ||
                            fieldName.contains("value")
                if (!isResourceField) {
                    continue
                }
                if (isDrawableResource(resources, resId)) {
                    return resId
                }
            }
        }
        return null
    }

    private fun extractDrawableResIdCandidates(value: Any, fieldName: String): List<Pair<String, Int>> {
        val candidates = mutableListOf<Pair<String, Int>>()
        when (value) {
            is Int -> {
                if (value > 0) {
                    candidates += fieldName to value
                }
            }
            is IntArray -> {
                value.filter { it > 0 }.forEachIndexed { index, item ->
                    candidates += "$fieldName:$index" to item
                }
            }
            is Array<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }
            is List<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    value is android.graphics.drawable.Icon &&
                    value.type == android.graphics.drawable.Icon.TYPE_RESOURCE
                ) {
                    val resId = value.resId
                    if (resId > 0) {
                        candidates += "$fieldName:icon" to resId
                    }
                }
            }
        }
        return candidates
    }

    private fun getRemoteViewActions(rv: RemoteViews?): List<Any> {
        rv ?: return emptyList()
        return try {
            val actionsField = rv.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            (actionsField.get(rv) as? List<*>)?.filterNotNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun collectAllDeclaredFields(clazz: Class<*>): List<java.lang.reflect.Field> {
        val fields = mutableListOf<java.lang.reflect.Field>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            fields.addAll(current.declaredFields)
            current = current.superclass
        }
        return fields
    }

    private fun isDrawableResource(resources: android.content.res.Resources, resId: Int): Boolean {
        return try {
            val typeName = resources.getResourceTypeName(resId)
            typeName == "drawable" || typeName == "mipmap"
        } catch (_: Exception) {
            false
        }
    }

    private fun mapResourceNameToManeuverType(resName: String): ManeuverType? {
        val name = resName.lowercase(Locale.ROOT)
        return when {
            name.contains("sharp_left") || name.contains("sharp_l") -> ManeuverType.SHARP_LEFT
            name.contains("sharp_right") || name.contains("sharp_r") -> ManeuverType.SHARP_RIGHT
            
            name.contains("slight_left") || name.contains("slight_l") || name.contains("bear_left") -> ManeuverType.SLIGHT_LEFT
            name.contains("slight_right") || name.contains("slight_r") || name.contains("bear_right") -> ManeuverType.SLIGHT_RIGHT
            
            name.contains("keep_left") || name.contains("stay_left") -> ManeuverType.KEEP_LEFT
            name.contains("keep_right") || name.contains("stay_right") -> ManeuverType.KEEP_RIGHT
            
            name.contains("fork_left") || name.contains("fork_l") -> ManeuverType.FORK_LEFT
            name.contains("fork_right") || name.contains("fork_r") -> ManeuverType.FORK_RIGHT
            
            name.contains("ramp_left") || name.contains("ramp_l") -> ManeuverType.RAMP_LEFT
            name.contains("ramp_right") || name.contains("ramp_r") -> ManeuverType.RAMP_RIGHT
            
            name.contains("u_turn") || name.contains("uturn") -> ManeuverType.UTURN
            
            name.contains("roundabout_exit") || name.contains("roundabout_out") -> ManeuverType.ROUNDABOUT_EXIT
            name.contains("roundabout") -> ManeuverType.ROUNDABOUT_ENTER
            
            name.contains("turn_left") || name.contains("left_turn") || name.endsWith("_left") || name == "left" -> ManeuverType.LEFT
            name.contains("turn_right") || name.contains("right_turn") || name.endsWith("_right") || name == "right" -> ManeuverType.RIGHT
            
            name.contains("straight") || name.contains("continue") || name.contains("go_straight") || name.contains("keep_straight") -> ManeuverType.STRAIGHT
            name.contains("merge") -> ManeuverType.MERGE
            name.contains("exit") -> ManeuverType.EXIT
            name.contains("destination") || name.contains("arrive") || name.contains("arrival") -> ManeuverType.DESTINATION
            
            else -> null
        }
    }
}

object WazeProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object HereWeGoProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object TomTomProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object OrganicMapsProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object MagicEarthProvider : NavigationProvider {
    override fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        return NavigationTextParser.parse(title, text)
    }
}

object NavigationTextParser {
    fun parse(title: String, text: String): ManeuverType {
        val combined = "$title $text".lowercase()
        
        fun matches(pattern: String): Boolean {
            return Regex(pattern).containsMatchIn(combined)
        }
        
        return when {
            // Destination / Arrival
            matches("\\b(arrive|arrived|reached|destination|arrival)\\b") -> ManeuverType.DESTINATION
            
            // U-turn
            matches("\\b(u-turn|uturn|u turn)\\b") -> ManeuverType.UTURN
            
            // Roundabout Exit
            matches("\\broundabout\\b") && matches("\\b(exit|take the)\\b") -> ManeuverType.ROUNDABOUT_EXIT
            
            // Roundabout Enter
            matches("\\b(roundabout|enter roundabout)\\b") -> ManeuverType.ROUNDABOUT_ENTER
            
            // Sharp Left
            matches("\\b(sharp left|hard left)\\b") -> ManeuverType.SHARP_LEFT
            
            // Sharp Right
            matches("\\b(sharp right|hard right)\\b") -> ManeuverType.SHARP_RIGHT
            
            // Slight Left
            matches("\\b(slight left|bear left|half left)\\b") -> ManeuverType.SLIGHT_LEFT
            
            // Slight Right
            matches("\\b(slight right|bear right|half right)\\b") -> ManeuverType.SLIGHT_RIGHT
            
            // Keep Left
            matches("\\b(keep left|stay left|keep to the left|stay on the left)\\b") -> ManeuverType.KEEP_LEFT
            
            // Keep Right
            matches("\\b(keep right|stay right|keep to the right|stay on the right)\\b") -> ManeuverType.KEEP_RIGHT

            // Fork Left / Fork Right
            matches("\\bfork left\\b") -> ManeuverType.FORK_LEFT
            matches("\\bfork right\\b") -> ManeuverType.FORK_RIGHT
            
            // Ramp Left / Ramp Right
            matches("\\bramp left\\b") -> ManeuverType.RAMP_LEFT
            matches("\\bramp right\\b") -> ManeuverType.RAMP_RIGHT
            
            // Merge
            matches("\\bmerge\\b") -> ManeuverType.MERGE
            
            // Exit
            matches("\\bexit\\b") -> ManeuverType.EXIT
            
            // Explicit turn left / turn right (checked before general straight)
            matches("\\bturn left\\b") -> ManeuverType.LEFT
            matches("\\bturn right\\b") -> ManeuverType.RIGHT
            
            // Continue Straight (checked before standalone left/right)
            matches("\\b(straight|continue on|continue straight|go straight|keep straight)\\b") -> ManeuverType.STRAIGHT
            
            // Standalone Left / Right (fallback)
            matches("\\bleft\\b") -> ManeuverType.LEFT
            matches("\\bright\\b") -> ManeuverType.RIGHT
            
            else -> ManeuverType.UNKNOWN
        }
    }
}

object NavigationCompatibilityLayer {
    private val providers = mapOf(
        "com.google.android.apps.maps" to LoggingNavigationProvider("GoogleMaps", GoogleMapsProvider),
        "com.waze" to LoggingNavigationProvider("Waze", WazeProvider),
        "com.here.app.maps" to LoggingNavigationProvider("HereWeGo", HereWeGoProvider),
        "com.tomtom.gplay.navapp" to LoggingNavigationProvider("TomTom", TomTomProvider),
        "app.organicmaps" to LoggingNavigationProvider("OrganicMaps", OrganicMapsProvider),
        "com.generalmagic.magicearth" to LoggingNavigationProvider("MagicEarth", MagicEarthProvider)
    )

    fun parse(sbn: StatusBarNotification, context: Context): ManeuverType {
        val packageName = sbn.packageName
        val provider = providers[packageName] ?: LoggingNavigationProvider("Fallback", DefaultNavigationProvider)
        return provider.parse(sbn, context)
    }

    fun extractManeuverDrawable(sbn: StatusBarNotification, context: Context): Drawable? {
        val notification = sbn.notification
        return try {
            val packageContext = context.createPackageContext(sbn.packageName, 0)
            val resources = packageContext.resources
            val drawableResId =
                GoogleMapsProvider.extractFirstRemoteDrawableResId(notification.contentView, resources)
                    ?: GoogleMapsProvider.extractFirstRemoteDrawableResId(notification.bigContentView, resources)
                    ?: GoogleMapsProvider.extractFirstRemoteDrawableResId(notification.headsUpContentView, resources)

            if (drawableResId != null && drawableResId > 0) {
                packageContext.getDrawable(drawableResId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
