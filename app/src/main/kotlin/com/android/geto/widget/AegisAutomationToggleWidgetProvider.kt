package com.android.geto.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.android.geto.R

class AegisAutomationToggleWidgetProvider : AppWidgetProvider() {

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_AUTOMATIONS) {
            try {
                val prefs = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
                val current = prefs.getBoolean("automations_enabled", true)
                prefs.edit().putBoolean("automations_enabled", !current).apply()

                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, AegisAutomationToggleWidgetProvider::class.java),
                )
                for (id in ids) updateWidget(context, manager, id)
            } catch (_: Exception) {
                // Swallow so the broadcast never crashes the process
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
    ) {
        val prefs = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("automations_enabled", true)

        val views = RemoteViews(context.packageName, R.layout.widget_automation_toggle)

        views.setTextViewText(
            R.id.widget_toggle_state,
            if (enabled) "Automations ON" else "Automations OFF",
        )
        views.setTextViewText(
            R.id.widget_toggle_hint,
            if (enabled) "Tap to pause" else "Tap to resume",
        )
        views.setInt(
            R.id.widget_toggle_indicator,
            "setBackgroundColor",
            if (enabled) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt(),
        )

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val toggleIntent = Intent(context, AegisAutomationToggleWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_AUTOMATIONS
        }
        val togglePending = PendingIntent.getBroadcast(context, 0, toggleIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_toggle_root, togglePending)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_TOGGLE_AUTOMATIONS = "com.android.geto.ACTION_TOGGLE_AUTOMATIONS"
    }
}
