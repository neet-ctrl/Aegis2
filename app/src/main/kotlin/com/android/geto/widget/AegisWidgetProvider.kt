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

class AegisWidgetProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_aegis)

        val automationsActive = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
            .getBoolean("automations_enabled", true)
        val enabledRules = AegisAutomationStore.getEnabledCount(context)
        val rulesLabel = if (enabledRules == 0) "No rules" else "$enabledRules Rule${if (enabledRules == 1) "" else "s"}"

        val lastEntry = AegisActivityLog.getEntries(context).firstOrNull()
        val lastTriggerText = if (lastEntry != null) {
            "${lastEntry.title} · ${AegisActivityLog.formatRelativeTime(lastEntry.timestampMs)}"
        } else {
            "No triggers yet"
        }

        views.setTextViewText(R.id.widget_status_automations, if (automationsActive) "Active" else "Paused")
        views.setTextViewText(R.id.widget_status_rules, rulesLabel)
        views.setTextViewText(R.id.widget_last_trigger, lastTriggerText)

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
