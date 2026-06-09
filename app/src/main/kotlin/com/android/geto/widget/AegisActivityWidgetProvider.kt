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

class AegisActivityWidgetProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_activity)

        val entries = AegisActivityLog.getEntries(context).take(3)

        fun entryLine(index: Int): String {
            val e = entries.getOrNull(index) ?: return ""
            return "${e.title}: ${e.subtitle.take(40)}${if (e.subtitle.length > 40) "…" else ""}"
        }

        views.setTextViewText(R.id.widget_activity_entry1, entryLine(0).ifEmpty { "No recent activity" })
        views.setTextViewText(R.id.widget_activity_entry2, entryLine(1))
        views.setTextViewText(R.id.widget_activity_entry3, entryLine(2))

        val updatedText = if (entries.isNotEmpty()) {
            "Updated ${AegisActivityLog.formatRelativeTime(entries.first().timestampMs)}"
        } else {
            "No activity yet"
        }
        views.setTextViewText(R.id.widget_activity_timestamp, updatedText)

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 2, launchIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_activity_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
