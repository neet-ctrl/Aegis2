package com.android.geto.watchdog

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

enum class AccessibilityStatus {
    OK,
    DISABLED,
    ENABLED_NOT_RUNNING,
    MALFUNCTIONING,
}

data class AccessibilityHealth(
    val enabledInSettings: Boolean,
    val heartbeatAlive: Boolean,
    val batteryOptimizationExempt: Boolean,
    val lastHeartbeatMs: Long,
) {
    val status: AccessibilityStatus
        get() = when {
            !enabledInSettings -> AccessibilityStatus.DISABLED
            enabledInSettings && heartbeatAlive -> AccessibilityStatus.OK
            enabledInSettings && !heartbeatAlive && lastHeartbeatMs == 0L -> AccessibilityStatus.ENABLED_NOT_RUNNING
            else -> AccessibilityStatus.MALFUNCTIONING
        }

    val statusLabel: String
        get() = when (status) {
            AccessibilityStatus.OK -> "Running"
            AccessibilityStatus.DISABLED -> "Disabled"
            AccessibilityStatus.ENABLED_NOT_RUNNING -> "Not Running"
            AccessibilityStatus.MALFUNCTIONING -> "Malfunctioning"
        }

    val issueDescription: String
        get() = when (status) {
            AccessibilityStatus.OK ->
                "Accessibility service is healthy and monitoring foreground apps."
            AccessibilityStatus.DISABLED ->
                "Enable 'Aegis App Lock' in Accessibility Settings to use App Lock."
            AccessibilityStatus.ENABLED_NOT_RUNNING ->
                if (!batteryOptimizationExempt)
                    "Service not started — battery optimization may be blocking it. Disable battery optimization for Aegis."
                else
                    "Service is enabled but has not started yet. Try disabling and re-enabling in Accessibility Settings."
            AccessibilityStatus.MALFUNCTIONING ->
                if (!batteryOptimizationExempt)
                    "Service was killed by battery optimization. Disable battery optimization for Aegis to fix this."
                else
                    "Service crashed or was killed by the system. Re-enable in Accessibility Settings to fix."
        }
}

object AccessibilityHealthMonitor {
    const val PREFS_NAME = "aegis_accessibility_watchdog"
    const val KEY_HEARTBEAT = "heartbeat_ms"

    private const val HEARTBEAT_STALE_MS = 5 * 60 * 1000L

    fun getHealth(context: Context): AccessibilityHealth {
        val enabledInSettings = isServiceEnabledInSettings(context)
        val lastHeartbeat = getLastHeartbeatMs(context)
        val heartbeatAlive = lastHeartbeat > 0 &&
            (System.currentTimeMillis() - lastHeartbeat) < HEARTBEAT_STALE_MS
        val batteryExempt = isBatteryOptimizationExempt(context)

        return AccessibilityHealth(
            enabledInSettings = enabledInSettings,
            heartbeatAlive = heartbeatAlive,
            batteryOptimizationExempt = batteryExempt,
            lastHeartbeatMs = lastHeartbeat,
        )
    }

    fun isServiceEnabledInSettings(context: Context): Boolean {
        return try {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            services?.contains("com.android.geto", ignoreCase = true) == true
        } catch (_: Exception) {
            false
        }
    }

    fun getLastHeartbeatMs(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_HEARTBEAT, 0L)

    fun writeHeartbeat(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_HEARTBEAT, System.currentTimeMillis()).apply()
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
}
