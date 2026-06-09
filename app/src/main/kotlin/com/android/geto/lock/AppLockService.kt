package com.android.geto.lock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.android.geto.broadcastreceiver.RevertSettingsBroadcastReceiver
import com.android.geto.domain.repository.AppSettingsRepository
import com.android.geto.domain.usecase.ApplyAppSettingsUseCase
import com.android.geto.engine.AegisAutomationEngine
import com.android.geto.feature.appsettings.security.AppLockManager
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.ACTION_REVERT_SETTINGS
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.NOTIFICATION_EXTRA_COMPONENT_NAME
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.NOTIFICATION_EXTRA_NOTIFICATION_ID
import com.android.geto.watchdog.AccessibilityHealthMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections

class AppLockService : AccessibilityService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppLockServiceEntryPoint {
        fun applyAppSettingsUseCase(): ApplyAppSettingsUseCase
        fun appSettingsRepository(): AppSettingsRepository
    }

    @Volatile
    private var lastLaunchPackage: String = ""

    @Volatile
    private var lastHeartbeatWriteMs: Long = 0L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            try {
                AccessibilityHealthMonitor.writeHeartbeat(applicationContext)
            } catch (_: Exception) {}
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            AccessibilityHealthMonitor.writeHeartbeat(applicationContext)
            lastHeartbeatWriteMs = System.currentTimeMillis()
        } catch (_: Exception) {}
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event ?: return
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return

            // Write periodic heartbeat so watchdog knows service is alive
            val now = System.currentTimeMillis()
            if (now - lastHeartbeatWriteMs >= HEARTBEAT_INTERVAL_MS) {
                lastHeartbeatWriteMs = now
                try {
                    AccessibilityHealthMonitor.writeHeartbeat(applicationContext)
                } catch (_: Exception) {}
            }

            // Ignore our own UI and system surfaces
            if (packageName == applicationContext.packageName) return
            if (packageName == "com.android.systemui") return
            if (packageName == "android") return

            if (packageName != lastLaunchPackage) {
                val prevPackage = lastLaunchPackage
                lastLaunchPackage = packageName

                // Auto-revert settings for the app that just left the foreground
                try {
                    val prefs = applicationContext.getSharedPreferences(
                        PENDING_REVERT_PREFS, Context.MODE_PRIVATE
                    )
                    val pendingRevert = prefs.getString(prevPackage, null)
                    if (pendingRevert != null) {
                        val parts = pendingRevert.split("|")
                        if (parts.size == 2) {
                            val componentName = parts[0]
                            val notificationId = parts[1].toIntOrNull()
                            if (notificationId != null) {
                                prefs.edit().remove(prevPackage).apply()
                                val revertIntent = Intent(ACTION_REVERT_SETTINGS).apply {
                                    setClass(applicationContext, RevertSettingsBroadcastReceiver::class.java)
                                    putExtra(NOTIFICATION_EXTRA_COMPONENT_NAME, componentName)
                                    putExtra(NOTIFICATION_EXTRA_NOTIFICATION_ID, notificationId)
                                }
                                sendBroadcast(revertIntent)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Never let auto-revert errors crash the accessibility service
                }

                // Auto-apply settings for the app that just entered the foreground
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        AppLockServiceEntryPoint::class.java,
                    )
                    serviceScope.launch {
                        try {
                            val allSettings = entryPoint.appSettingsRepository()
                                .appSettingsFlow.first()

                            val packageSettings = allSettings.filter { setting ->
                                setting.enabled &&
                                    setting.componentName.startsWith("$packageName/")
                            }

                            if (packageSettings.isEmpty()) return@launch

                            val componentNames = packageSettings
                                .map { it.componentName }
                                .distinct()

                            for (componentName in componentNames) {
                                try {
                                    entryPoint.applyAppSettingsUseCase().invoke(componentName)
                                    applicationContext.getSharedPreferences(
                                        PENDING_REVERT_PREFS, Context.MODE_PRIVATE
                                    ).edit()
                                        .putString(packageName, "$componentName|0")
                                        .apply()
                                } catch (_: Exception) {
                                    // Never let one component failure abort the rest
                                }
                            }
                        } catch (_: Exception) {
                            // Never let auto-apply errors crash the service
                        }
                    }
                } catch (_: Exception) {
                    // Never let auto-apply bootstrap errors crash the service
                }

                // Fire "App Launch" automation trigger on each new foreground package
                try {
                    AegisAutomationEngine.fireTrigger(this, "App Launch", packageName)
                } catch (_: Exception) {
                    // Never let automation errors crash the accessibility service
                }
            }

            // Skip apps already unlocked in this session
            if (packageName in unlockedThisSession) return

            // Show lock screen if this package has an active lock or block
            val locked = try {
                AppLockManager.isAppLockActive(this, packageName)
            } catch (_: Exception) {
                false
            }

            if (locked) {
                try {
                    val intent = AppLockActivity.createIntent(this, packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                } catch (_: Exception) {
                    // startActivity can throw on edge cases; swallow to keep the service alive
                }
            }
        } catch (_: Exception) {
            // Top-level guard: no uncaught exception must ever escape onAccessibilityEvent.
            // An unhandled exception here crashes the service process and Android
            // immediately marks the accessibility service as "malfunctioning".
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        serviceScope.cancel()
    }

    companion object {
        const val PENDING_REVERT_PREFS = "aegis_pending_revert"
        private const val HEARTBEAT_INTERVAL_MS = 60_000L

        private val unlockedThisSession: MutableSet<String> =
            Collections.synchronizedSet(mutableSetOf())

        fun addUnlocked(packageName: String) {
            unlockedThisSession.add(packageName)
        }

        fun reLock(packageName: String) {
            unlockedThisSession.remove(packageName)
        }

        fun clearAll() {
            unlockedThisSession.clear()
        }
    }
}
