package com.android.geto.widget

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.UiModeManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.RemoteViews
import com.android.geto.R

class AegisQuickTogglesWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.android.geto.ACTION_WIDGET_TOGGLE"
        const val EXTRA_TOGGLE_INDEX = "toggle_index"

        private val TILE_IDS = intArrayOf(
            R.id.wqt_tile_1, R.id.wqt_tile_2, R.id.wqt_tile_3,
            R.id.wqt_tile_4, R.id.wqt_tile_5, R.id.wqt_tile_6,
            R.id.wqt_tile_7, R.id.wqt_tile_8, R.id.wqt_tile_9,
            R.id.wqt_tile_10,
        )
        private val ICON_IDS = intArrayOf(
            R.id.wqt_icon_1, R.id.wqt_icon_2, R.id.wqt_icon_3,
            R.id.wqt_icon_4, R.id.wqt_icon_5, R.id.wqt_icon_6,
            R.id.wqt_icon_7, R.id.wqt_icon_8, R.id.wqt_icon_9,
            R.id.wqt_icon_10,
        )
        private val NAME_IDS = intArrayOf(
            R.id.wqt_name_1, R.id.wqt_name_2, R.id.wqt_name_3,
            R.id.wqt_name_4, R.id.wqt_name_5, R.id.wqt_name_6,
            R.id.wqt_name_7, R.id.wqt_name_8, R.id.wqt_name_9,
            R.id.wqt_name_10,
        )
        private val STATUS_IDS = intArrayOf(
            R.id.wqt_status_1, R.id.wqt_status_2, R.id.wqt_status_3,
            R.id.wqt_status_4, R.id.wqt_status_5, R.id.wqt_status_6,
            R.id.wqt_status_7, R.id.wqt_status_8, R.id.wqt_status_9,
            R.id.wqt_status_10,
        )
        private val ICON_RES = intArrayOf(
            R.drawable.ic_qs_developer,
            R.drawable.ic_qs_usb,
            R.drawable.ic_qs_wifi_debug,
            R.drawable.ic_qs_dark_mode,
            R.drawable.ic_qs_auto_rotate,
            R.drawable.ic_qs_wifi,
            R.drawable.ic_qs_bluetooth,
            R.drawable.ic_qs_night_light,
            R.drawable.ic_qs_battery_saver,
            R.drawable.ic_qs_dnd,
        )
        private val LABELS = arrayOf(
            "Dev Options", "USB Debug", "WiFi Debug", "Dark Mode", "Auto-rotate",
            "WiFi", "Bluetooth", "Night Light", "Battery Saver", "Do Not Disturb",
        )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            runCatching { updateWidget(context, appWidgetManager, widgetId) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val index = intent.getIntExtra(EXTRA_TOGGLE_INDEX, -1)
            if (index in 0..9) {
                Thread {
                    runCatching { performToggle(context, index) }
                    val manager = AppWidgetManager.getInstance(context)
                    val component = ComponentName(context, AegisQuickTogglesWidgetProvider::class.java)
                    val ids = manager.getAppWidgetIds(component)
                    onUpdate(context, manager, ids)
                }.start()
            }
            return
        }
        super.onReceive(context, intent)
    }

    private fun performToggle(context: Context, index: Int) {
        val cr = context.contentResolver
        val pm = context.packageManager
        val hasSecure = pm.checkPermission(
            "android.permission.WRITE_SECURE_SETTINGS", context.packageName,
        ) == PackageManager.PERMISSION_GRANTED
        val hasWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true

        when (index) {
            0 -> { // Dev Options
                if (!hasSecure) return
                val cur = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
                runCatching { Settings.Global.putInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, if (cur) 0 else 1) }
            }
            1 -> { // USB Debug
                if (!hasSecure) return
                val cur = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
                runCatching { Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, if (cur) 0 else 1) }
            }
            2 -> { // WiFi Debug
                if (!hasSecure || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
                val cur = Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1
                runCatching { Settings.Global.putInt(cr, "adb_wifi_enabled", if (cur) 0 else 1) }
            }
            3 -> { // Dark Mode
                runCatching {
                    val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
                    val cur = uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES
                    uiManager?.nightMode = if (cur) UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
                }
            }
            4 -> { // Auto-rotate
                if (!hasWriteSettings) return
                val cur = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                runCatching { Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, if (cur) 0 else 1) }
            }
            5 -> { // WiFi
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        val cur = wm?.isWifiEnabled == true
                        wm?.setWifiEnabled(!cur)
                    }
                }
                // Android 10+: can't toggle WiFi directly — read-only status
            }
            6 -> { // Bluetooth
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        @Suppress("DEPRECATION", "MissingPermission")
                        val btm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val cur = btm?.adapter?.isEnabled == true
                        if (cur) btm?.adapter?.disable() else btm?.adapter?.enable()
                    }
                }
                // Android 13+: can't toggle BT directly — read-only status
            }
            7 -> { // Night Light
                if (!hasSecure) return
                val cur = Settings.Secure.getInt(cr, "night_display_activated", 0) == 1
                runCatching { Settings.Secure.putInt(cr, "night_display_activated", if (cur) 0 else 1) }
            }
            8 -> { // Battery Saver
                if (!hasSecure) return
                val pm2 = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val cur = pm2?.isPowerSaveMode == true
                runCatching { Settings.Global.putInt(cr, "low_power", if (cur) 0 else 1) }
            }
            9 -> { // Do Not Disturb
                runCatching {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm?.isNotificationPolicyAccessGranted == true) {
                        val cur = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                        nm.setInterruptionFilter(
                            if (cur) NotificationManager.INTERRUPTION_FILTER_ALL
                            else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                        )
                    }
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_toggles)
        val statuses = readStatuses(context)
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT

        statuses.forEachIndexed { index, isOn ->
            val tileId = TILE_IDS[index]
            val iconId = ICON_IDS[index]
            val nameId = NAME_IDS[index]
            val statusId = STATUS_IDS[index]

            views.setImageViewResource(iconId, ICON_RES[index])
            views.setTextViewText(nameId, LABELS[index])
            views.setTextViewText(statusId, if (isOn) "ON" else "OFF")

            views.setInt(
                tileId, "setBackgroundResource",
                if (isOn) R.drawable.widget_tile_active else R.drawable.widget_tile_inactive,
            )

            val toggleIntent = Intent(context, AegisQuickTogglesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
                putExtra(EXTRA_TOGGLE_INDEX, index)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                widgetId * 100 + index,
                toggleIntent,
                pendingFlags,
            )
            views.setOnClickPendingIntent(tileId, pi)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun readStatuses(context: Context): List<Boolean> {
        val cr = context.contentResolver
        return listOf(
            runCatching { Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 }.getOrDefault(false),
            runCatching { Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1 }.getOrDefault(false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                runCatching { Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1 }.getOrDefault(false)
            else false,
            runCatching {
                val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
                uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES
            }.getOrDefault(false),
            runCatching { Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 }.getOrDefault(false),
            runCatching {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wm?.isWifiEnabled == true
            }.getOrDefault(false),
            runCatching {
                val btm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                btm?.adapter?.isEnabled == true
            }.getOrDefault(false),
            runCatching { Settings.Secure.getInt(cr, "night_display_activated", 0) == 1 }.getOrDefault(false),
            runCatching {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isPowerSaveMode == true
            }.getOrDefault(false),
            runCatching {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.currentInterruptionFilter?.let { it != NotificationManager.INTERRUPTION_FILTER_ALL } == true
            }.getOrDefault(false),
        )
    }
}
