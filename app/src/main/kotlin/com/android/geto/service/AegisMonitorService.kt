package com.android.geto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.android.geto.R
import com.android.geto.activity.main.MainActivity
import com.android.geto.engine.AegisAutomationEngine

class AegisMonitorService : Service() {

    private var lastBatteryPct = -1
    private var wifiConnectedFired = false

    private val dynamicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON ->
                    AegisAutomationEngine.fireTrigger(context, "Screen On")

                Intent.ACTION_SCREEN_OFF ->
                    AegisAutomationEngine.fireTrigger(context, "Screen Off")

                Intent.ACTION_USER_PRESENT ->
                    AegisAutomationEngine.fireTrigger(context, "Device Unlock")

                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
                    if (pct >= 0 && pct != lastBatteryPct) {
                        val prev = lastBatteryPct
                        lastBatteryPct = pct
                        if (prev >= 0) {
                            AegisAutomationEngine.fireBatteryTrigger(context, pct)
                        }
                    }
                }

                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        1 -> AegisAutomationEngine.fireTrigger(context, "Headphones Connected")
                        0 -> AegisAutomationEngine.fireTrigger(context, "Headphones Disconnected")
                    }
                }

                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    when (netInfo?.detailedState) {
                        NetworkInfo.DetailedState.CONNECTED -> {
                            if (!wifiConnectedFired) {
                                wifiConnectedFired = true
                                val ssid = intent.getStringExtra(WifiManager.EXTRA_BSSID) ?: ""
                                AegisAutomationEngine.fireTrigger(context, "Wi-Fi Connected", ssid)
                            }
                        }
                        NetworkInfo.DetailedState.DISCONNECTED -> {
                            wifiConnectedFired = false
                            AegisAutomationEngine.fireTrigger(context, "Wi-Fi Disconnected")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        registerDynamicReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(dynamicReceiver) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerDynamicReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dynamicReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dynamicReceiver, filter)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aegis Automation Monitor")
            .setContentText("Listening for system events to trigger automations")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(MONITOR_CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    MONITOR_CHANNEL_ID,
                    "Automation Monitor",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Persistent notification for the Aegis background monitor"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 7001
        private const val MONITOR_CHANNEL_ID = "aegis_monitor_service"

        fun start(context: Context) {
            val intent = Intent(context, AegisMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AegisMonitorService::class.java))
        }
    }
}
