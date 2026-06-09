package com.android.geto.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.android.geto.feature.home.AegisAutomationStore
import com.android.geto.feature.home.AegisConditionStore
import com.android.geto.feature.home.SavedAutomation
import com.android.geto.receiver.AegisTimeScheduleReceiver
import java.util.Calendar

object AegisTimeScheduler {

    const val ACTION_FIRE = "com.android.geto.ACTION_FIRE_TIME_SCHEDULE"
    const val EXTRA_AUTOMATION_ID = "automation_id"
    const val EXTRA_TRIGGER_LABEL = "trigger_label"

    fun scheduleIfNeeded(context: Context, automation: SavedAutomation) {
        if (!automation.isEnabled) return
        val triggerLabel = automation.triggerLabel
        if (triggerLabel != "Time Schedule" && triggerLabel != "Day Schedule") return
        val conditions = AegisConditionStore.getConditions(context, automation.id)
        scheduleFromConditions(context, automation.id, triggerLabel, conditions)
    }

    fun scheduleFromConditions(
        context: Context,
        automationId: Long,
        triggerLabel: String,
        conditions: List<com.android.geto.feature.home.StoredCondition>,
    ) {
        val nextMs: Long = when (triggerLabel) {
            "Time Schedule" -> {
                val cond = conditions.firstOrNull { it.field.trim().lowercase() == "time" } ?: return
                nextTimeMs(cond.value.trim())
            }
            "Day Schedule" -> {
                val cond = conditions.firstOrNull { it.field.trim().lowercase() == "day" } ?: return
                nextDayMs(cond.value.trim())
            }
            else -> return
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, automationId, triggerLabel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMs, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, nextMs, pi)
        }
    }

    fun cancel(context: Context, automationId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AegisTimeScheduleReceiver::class.java).apply {
            action = ACTION_FIRE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            automationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    fun rescheduleAll(context: Context) {
        for (automation in AegisAutomationStore.getAutomations(context)) {
            if (automation.isEnabled) scheduleIfNeeded(context, automation)
        }
    }

    private fun buildPendingIntent(context: Context, automationId: Long, triggerLabel: String): PendingIntent {
        val intent = Intent(context, AegisTimeScheduleReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_AUTOMATION_ID, automationId)
            putExtra(EXTRA_TRIGGER_LABEL, triggerLabel)
        }
        return PendingIntent.getBroadcast(
            context,
            automationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTimeMs(timeStr: String): Long {
        val parts = timeStr.split(":").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 2) return System.currentTimeMillis() + 60_000
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].coerceIn(0, 23))
            set(Calendar.MINUTE, parts[1].coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun nextDayMs(daysStr: String): Long {
        val dayMap = mapOf(
            "sun" to Calendar.SUNDAY,
            "mon" to Calendar.MONDAY,
            "tue" to Calendar.TUESDAY,
            "wed" to Calendar.WEDNESDAY,
            "thu" to Calendar.THURSDAY,
            "fri" to Calendar.FRIDAY,
            "sat" to Calendar.SATURDAY,
        )
        val days = daysStr.split(",").mapNotNull { dayMap[it.trim().lowercase()] }
        if (days.isEmpty()) return System.currentTimeMillis() + 24 * 3_600_000L
        val now = Calendar.getInstance()
        val todayDow = now.get(Calendar.DAY_OF_WEEK)
        for (offset in 1..7) {
            val checkDow = (todayDow - 1 + offset) % 7 + 1
            if (checkDow in days) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        }
        return System.currentTimeMillis() + 24 * 3_600_000L
    }
}
