package com.android.geto.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.android.geto.R
import com.android.geto.activity.main.MainActivity
import com.android.geto.feature.home.AegisActionStore
import com.android.geto.feature.home.AegisActivityLog
import com.android.geto.feature.home.AegisAutomationStore
import com.android.geto.feature.home.AegisConditionStore
import com.android.geto.feature.home.SavedAutomation
import com.android.geto.feature.home.StoredCondition

object AegisAutomationEngine {

    private const val CHANNEL_ID = "aegis_automation_fired"
    private const val CHANNEL_NAME = "Automation Triggers"

    fun fireTrigger(context: Context, triggerLabel: String, detail: String = "") {
        if (!isGloballyEnabled(context)) return

        val automations = AegisAutomationStore.getAutomations(context)
        val matching = automations.filter { it.isEnabled && it.triggerLabel == triggerLabel }
        if (matching.isEmpty()) return

        ensureNotificationChannel(context)

        for (automation in matching) {
            val delayMs = automation.delaySeconds * 1_000L
            if (delayMs > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    runAutomation(context, automation, triggerLabel, detail)
                }, delayMs)
            } else {
                runAutomation(context, automation, triggerLabel, detail)
            }
        }
    }

    fun fireBatteryTrigger(context: Context, pct: Int) {
        fireTrigger(context, "Battery %", "$pct%")
    }

    private fun runAutomation(
        context: Context,
        automation: SavedAutomation,
        triggerLabel: String,
        detail: String,
    ) {
        // ── Evaluate conditions before executing any actions ────────────────────
        val conditions = AegisConditionStore.getConditions(context, automation.id)
        if (!checkConditions(conditions, automation.conditionLogic, triggerLabel, detail)) {
            return  // Conditions not met — skip this automation silently
        }

        val actions = AegisActionStore.getActions(context, automation.id)
        var applied = 0
        for (action in actions) {
            runCatching {
                val cr = context.contentResolver
                when (action.settingType) {
                    "SYSTEM" -> Settings.System.putString(cr, action.settingKey, action.value)
                    "GLOBAL" -> Settings.Global.putString(cr, action.settingKey, action.value)
                    "SECURE" -> Settings.Secure.putString(cr, action.settingKey, action.value)
                }
                applied++
            }
        }

        val logDetail = buildString {
            append("\"${automation.name}\" triggered by $triggerLabel")
            if (detail.isNotEmpty()) append(" ($detail)")
            append(" → ${automation.actionSummary}")
            if (actions.isNotEmpty()) append(" ($applied/${actions.size} applied)")
        }
        AegisActivityLog.addEntry(context, "Automation Fired", logDetail, "trigger")

        showNotification(context, automation, triggerLabel, detail, applied, actions.size)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Condition evaluation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if all (AND) or any (OR) conditions pass for the given trigger event.
     * An empty condition list always passes — the automation fires unconditionally.
     */
    private fun checkConditions(
        conditions: List<StoredCondition>,
        logic: String,
        triggerLabel: String,
        detail: String,
    ): Boolean {
        if (conditions.isEmpty()) return true
        val results = conditions.map { evaluateCondition(it, triggerLabel, detail) }
        return if (logic.uppercase() == "OR") results.any { it } else results.all { it }
    }

    private fun evaluateCondition(
        cond: StoredCondition,
        triggerLabel: String,
        detail: String,
    ): Boolean {
        val field = cond.field.trim().lowercase()
        val op = cond.operator.trim()
        val condValue = cond.value.trim()

        return when (field) {
            // ── App Launch / App Close: detail = package name ────────────────
            "app", "package", "app name" -> {
                val matches = detail.contains(condValue, ignoreCase = true) ||
                    detail.equals(condValue, ignoreCase = true)
                if (op == "is not" || op == "!=") !matches else matches
            }

            // ── Battery %: detail = "45%" ────────────────────────────────────
            "battery", "battery %" -> {
                val current = detail.removeSuffix("%").trim().toIntOrNull() ?: return true
                val threshold = condValue.removeSuffix("%").trim().toIntOrNull() ?: return true
                compareNumeric(current, op, threshold)
            }

            // ── Wi-Fi: detail = BSSID or SSID ───────────────────────────────
            "ssid", "wifi", "wi-fi", "network" -> {
                val matches = detail.contains(condValue, ignoreCase = true)
                if (op == "is not" || op == "!=") !matches else matches
            }

            // ── Bluetooth: detail = device name ─────────────────────────────
            "device", "bluetooth", "name" -> {
                val matches = detail.contains(condValue, ignoreCase = true)
                if (op == "is not" || op == "!=") !matches else matches
            }

            // ── Volume: detail = "Media=12" ──────────────────────────────────
            "volume" -> {
                val current = detail.substringAfter("=").trim().toIntOrNull() ?: return true
                val threshold = condValue.toIntOrNull() ?: return true
                compareNumeric(current, op, threshold)
            }

            // ── Time / Day — used only for scheduling, not runtime evaluation ─
            "time", "day" -> true

            // ── Unknown field — don't block the automation ───────────────────
            else -> true
        }
    }

    private fun compareNumeric(current: Int, op: String, threshold: Int): Boolean = when (op) {
        "<" -> current < threshold
        "<=" -> current <= threshold
        ">" -> current > threshold
        ">=" -> current >= threshold
        "=", "is", "==" -> current == threshold
        "is not", "!=" -> current != threshold
        else -> true
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Notification helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private fun showNotification(
        context: Context,
        automation: SavedAutomation,
        triggerLabel: String,
        detail: String,
        applied: Int,
        totalActions: Int,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            automation.id.toInt(),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = buildString {
            append("IF $triggerLabel")
            if (detail.isNotEmpty()) append(" ($detail)")
            append(" → THEN ${automation.actionSummary}")
            if (totalActions > 0) append(" ($applied/$totalActions applied)")
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(automation.name)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(("auto_${automation.id}").hashCode(), notif)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Fired when an Aegis automation runs" }
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun isGloballyEnabled(context: Context): Boolean =
        context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
            .getBoolean("automations_enabled", true)
}
