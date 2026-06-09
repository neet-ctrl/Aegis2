package com.android.geto.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.android.geto.R
import com.android.geto.service.AegisMonitorService

class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        AegisMonitorService.start(context)
        showReenableNotification(context)
    }

    private fun showReenableNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Aegis Update Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when Aegis needs re-configuration after an update"
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }

        val accessIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            accessIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aegis updated — Re-enable App Lock")
            .setContentText("Android disables accessibility services on app updates. Tap to re-enable.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Aegis was updated. Android automatically disables the Accessibility Service (App Lock) after every app update. " +
                        "Please tap here to open Accessibility Settings and re-enable 'Aegis App Lock'.",
                ),
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "aegis_update_alerts"
        private const val NOTIF_ID = 7010
    }
}
