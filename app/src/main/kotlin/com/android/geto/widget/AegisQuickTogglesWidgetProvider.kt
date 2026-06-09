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
import android.app.UiModeManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.RemoteViews
import com.android.geto.R
import com.android.geto.activity.main.MainActivity

class AegisQuickTogglesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            runCatching { updateWidget(context, appWidgetManager, widgetId) }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_toggles)

        val statuses = readTop10Statuses(context)

        val nameIds = intArrayOf(
            R.id.wqt_name_1, R.id.wqt_name_2, R.id.wqt_name_3, R.id.wqt_name_4, R.id.wqt_name_5,
            R.id.wqt_name_6, R.id.wqt_name_7, R.id.wqt_name_8, R.id.wqt_name_9, R.id.wqt_name_10,
        )
        val statusIds = intArrayOf(
            R.id.wqt_status_1, R.id.wqt_status_2, R.id.wqt_status_3, R.id.wqt_status_4, R.id.wqt_status_5,
            R.id.wqt_status_6, R.id.wqt_status_7, R.id.wqt_status_8, R.id.wqt_status_9, R.id.wqt_status_10,
        )

        statuses.forEachIndexed { index, (name, isOn) ->
            if (index < nameIds.size) {
                views.setTextViewText(nameIds[index], name)
                views.setTextViewText(statusIds[index], if (isOn) "ON" else "OFF")
                views.setTextColor(
                    statusIds[index],
                    if (isOn) android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#F44336"),
                )
            }
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "com.android.geto.ACTION_OPEN_QUICK_TOGGLES"
        }
        val pendingIntent = PendingIntent.getActivity(context, 10, intent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_quick_toggles_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun readTop10Statuses(context: Context): List<Pair<String, Boolean>> {
        val cr = context.contentResolver

        fun safeInt(key: String, default: Int = 0) = runCatching {
            Settings.Global.getInt(cr, key, default)
        }.getOrDefault(default)

        fun safeSecureInt(key: String, default: Int = 0) = runCatching {
            Settings.Secure.getInt(cr, key, default)
        }.getOrDefault(default)

        fun safeSystemInt(key: String, default: Int = 0) = runCatching {
            Settings.System.getInt(cr, key, default)
        }.getOrDefault(default)

        val devOptions = safeInt(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED) == 1
        val usbDebug = safeInt(Settings.Global.ADB_ENABLED) == 1
        val wifiDebug = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            safeInt("adb_wifi_enabled") == 1
        } else false

        val uiManager = runCatching {
            context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        }.getOrNull()
        val darkMode = uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES

        val autoRotate = safeSystemInt(Settings.System.ACCELEROMETER_ROTATION) == 1

        val wifiManager = runCatching {
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        }.getOrNull()
        val wifi = wifiManager?.isWifiEnabled == true

        val btManager = runCatching {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        }.getOrNull()
        val bluetooth = btManager?.adapter?.isEnabled == true

        val nightLight = safeSecureInt("night_display_activated") == 1

        val powerManager = runCatching {
            context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        }.getOrNull()
        val batterySaver = powerManager?.isPowerSaveMode == true

        val dndEnabled = runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.currentInterruptionFilter?.let { it != android.app.NotificationManager.INTERRUPTION_FILTER_ALL } == true
        }.getOrDefault(false)

        return listOf(
            "Dev Options" to devOptions,
            "USB Debug" to usbDebug,
            "WiFi Debug" to wifiDebug,
            "Dark Mode" to darkMode,
            "Auto-rotate" to autoRotate,
            "WiFi" to wifi,
            "Bluetooth" to bluetooth,
            "Night Light" to nightLight,
            "Battery Saver" to batterySaver,
            "Do Not Disturb" to dndEnabled,
        )
    }
}
