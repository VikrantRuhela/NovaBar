package com.novabar.app.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow

object DeveloperLogger {
    private const val TAG = "DeveloperLogger"
    private const val PREFS_NAME = "nova_bar_developer_logs"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val LOG_FILE_NAME = "developer_logs.txt"
    
    val isLoggingEnabled = MutableStateFlow(false)
    
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isLoggingEnabled.value = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
    }
    
    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        isLoggingEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
        if (enabled) {
            logGeneralInfo(context)
        } else {
            clearLog(context)
        }
    }
    
    fun getLogFile(context: Context): File {
        return File(context.cacheDir, LOG_FILE_NAME)
    }
    
    fun clearLog(context: Context) {
        val file = getLogFile(context)
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun readLog(context: Context): String {
        val file = getLogFile(context)
        if (!file.exists()) return "No logs recorded yet. Turn on logging and toggle Timer/Stopwatch/Hotspot states to record."
        return try {
            file.readText()
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }
    
    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        if (!isLoggingEnabled.value) return
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] [$tag] $message\n"
        
        try {
            val file = getLogFile(context)
            FileWriter(file, true).use { writer ->
                writer.append(logLine)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to log file: ${e.message}", e)
        }
    }
    
    private fun logGeneralInfo(context: Context) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
        val versionName = packageInfo?.versionName ?: "Unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "Unknown"
        } else {
            packageInfo?.versionCode?.toString() ?: "Unknown"
        }
        
        log(context, "GENERAL", "=== DEVICE DIAGNOSTICS START ===")
        log(context, "GENERAL", "Device Model: ${Build.MODEL}")
        log(context, "GENERAL", "Manufacturer: ${Build.MANUFACTURER}")
        log(context, "GENERAL", "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        log(context, "GENERAL", "Build Fingerprint: ${Build.FINGERPRINT}")
        log(context, "GENERAL", "Nova Bar Version: $versionName (Build $versionCode)")
        log(context, "GENERAL", "=================================")
    }

    fun bundleToReadableString(bundle: Bundle?): String {
        if (bundle == null) return "null"
        val sb = StringBuilder("{")
        try {
            val keys = bundle.keySet()
            var first = true
            for (key in keys) {
                if (!first) sb.append(", ")
                first = false
                sb.append(key).append("=").append(bundle.get(key))
            }
        } catch (e: Exception) {
            sb.append("Error reading keys: ").append(e.message)
        }
        sb.append("}")
        return sb.toString()
    }
}
