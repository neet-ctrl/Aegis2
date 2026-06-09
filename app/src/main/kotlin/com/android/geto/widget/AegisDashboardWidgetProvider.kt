package com.android.geto.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.android.geto.R
import com.android.geto.activity.main.MainActivity
import com.android.geto.feature.home.AegisActivityLog
import com.android.geto.feature.home.AegisAutomationStore

class AegisDashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            try {
                updateWidget(context, appWidgetManager, widgetId)
            } catch (_: Exception) {
                // Never let an exception propagate — the system would show "can't load"
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        val automationsEnabled = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
            .getBoolean("automations_enabled", true)
        val enabledRules = AegisAutomationStore.getEnabledCount(context)
        val lockedCount = getLockedCount(context)
        val triggersToday = AegisActivityLog.getTodayCount(context, "trigger")

        views.setTextViewText(
            R.id.widget_dashboard_automations_state,
            if (automationsEnabled) "Active" else "Paused",
        )
        views.setTextViewText(
            R.id.widget_dashboard_rules_count,
            if (enabledRules == 0) "No rules" else "$enabledRules Rule${if (enabledRules == 1) "" else "s"}",
        )
        views.setTextViewText(
            R.id.widget_dashboard_locked_apps,
            if (lockedCount == 0) "No locks" else "$lockedCount Locked",
        )
        views.setTextViewText(
            R.id.widget_dashboard_triggers_today,
            if (triggersToday == 0) "No triggers today" else "$triggersToday trigger${if (triggersToday == 1) "" else "s"} today",
        )

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_dashboard_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun getLockedCount(context: Context): Int {
        val prefs = context.getSharedPreferences("aegis_app_lock_v1", Context.MODE_PRIVATE)
        return prefs.all.keys.count { it.endsWith("_enabled") && prefs.getBoolean(it, false) }
    }
}
