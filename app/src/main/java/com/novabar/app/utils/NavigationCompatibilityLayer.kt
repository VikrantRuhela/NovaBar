package com.novabar.app.utils

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.novabar.app.domain.ManeuverType

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
        return NavigationTextParser.parse(title, text)
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
        return when {
            // Destination / Arrival
            combined.contains("arrive") || combined.contains("arrived") || 
            combined.contains("reached") || combined.contains("destination") || 
            combined.contains("arrival") -> ManeuverType.DESTINATION
            
            // U-turn
            combined.contains("u-turn") || combined.contains("uturn") || 
            combined.contains("u turn") -> ManeuverType.UTURN
            
            // Roundabout Exit
            combined.contains("roundabout") && (combined.contains("exit") || combined.contains("take the")) -> ManeuverType.ROUNDABOUT_EXIT
            
            // Roundabout Enter
            combined.contains("roundabout") || combined.contains("enter roundabout") -> ManeuverType.ROUNDABOUT_ENTER
            
            // Sharp Left
            combined.contains("sharp left") || combined.contains("hard left") -> ManeuverType.SHARP_LEFT
            
            // Sharp Right
            combined.contains("sharp right") || combined.contains("hard right") -> ManeuverType.SHARP_RIGHT
            
            // Slight Left
            combined.contains("slight left") || combined.contains("bear left") || combined.contains("half left") -> ManeuverType.SLIGHT_LEFT
            
            // Slight Right
            combined.contains("slight right") || combined.contains("bear right") || combined.contains("half right") -> ManeuverType.SLIGHT_RIGHT
            
            // Keep Left
            combined.contains("keep left") || combined.contains("stay left") || combined.contains("keep to the left") || combined.contains("stay on the left") -> ManeuverType.KEEP_LEFT
            
            // Keep Right
            combined.contains("keep right") || combined.contains("stay right") || combined.contains("keep to the right") || combined.contains("stay on the right") -> ManeuverType.KEEP_RIGHT
            
            // Merge
            combined.contains("merge") -> ManeuverType.MERGE
            
            // Exit
            combined.contains("exit") -> ManeuverType.EXIT
            
            // Turn Left
            combined.contains("turn left") || combined.contains(" left") || combined.startsWith("left") -> ManeuverType.LEFT
            
            // Turn Right
            combined.contains("turn right") || combined.contains(" right") || combined.startsWith("right") -> ManeuverType.RIGHT
            
            // Continue Straight
            combined.contains("straight") || combined.contains("continue on") || combined.contains("continue straight") || combined.contains("go straight") || combined.contains("keep straight") -> ManeuverType.STRAIGHT
            
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
}
