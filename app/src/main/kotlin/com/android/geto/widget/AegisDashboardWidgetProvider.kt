/*
 *
 *   Copyright 2023 Einstein Blanco
 *
 *   Licensed under the GNU General Public License v3.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/gpl-3.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
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

class AegisDashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
    ) {
        val prefs = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
        val automationsEnabled = prefs.getBoolean("automations_enabled", true)

        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        views.setTextViewText(
            R.id.widget_dashboard_automations_state,
            if (automationsEnabled) "Active" else "Paused",
        )
        views.setTextViewText(R.id.widget_dashboard_rules_count, "0 Rules")
        views.setTextViewText(R.id.widget_dashboard_locked_apps, "0 Locked")
        views.setTextViewText(R.id.widget_dashboard_triggers_today, "0 Triggers today")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_dashboard_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
