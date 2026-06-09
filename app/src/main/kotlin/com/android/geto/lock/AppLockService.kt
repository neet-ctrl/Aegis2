package com.android.geto.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.android.geto.engine.AegisAutomationEngine
import com.android.geto.feature.appsettings.security.AppLockManager
import java.util.Collections

class AppLockService : AccessibilityService() {

    @Volatile
    private var lastLaunchPackage: String = ""

    // onServiceConnected is intentionally NOT overridden here.
    // All event types, feedback type, flags and timeout are declared in
    // res/xml/app_lock_service.xml. Re-setting serviceInfo programmatically
    // in onServiceConnected() causes an NPE on some OEM ROMs (serviceInfo
    // getter can return null mid-bind) and may trigger the "malfunctioning"
    // state by forcing a redundant re-bind.

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event ?: return
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return

            // Ignore our own UI and system surfaces
            if (packageName == applicationContext.packageName) return
            if (packageName == "com.android.systemui") return
            if (packageName == "android") return

            // Fire "App Launch" automation trigger on each new foreground package
            if (packageName != lastLaunchPackage) {
                lastLaunchPackage = packageName
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
                    // startActivity can throw on edge cases (e.g. process death race);
                    // swallow to keep the service alive
                }
            }
        } catch (_: Exception) {
            // Top-level guard: no uncaught exception must ever escape onAccessibilityEvent.
            // An unhandled exception here crashes the service process and Android
            // immediately marks the accessibility service as "malfunctioning".
        }
    }

    override fun onInterrupt() {}

    companion object {
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
