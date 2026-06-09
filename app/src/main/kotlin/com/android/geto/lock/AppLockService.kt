package com.android.geto.lock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.android.geto.feature.appsettings.security.AppLockManager
import java.util.Collections

class AppLockService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == application.packageName) return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return

        if (packageName in unlockedThisSession) return

        if (AppLockManager.isAppLockActive(this, packageName)) {
            val intent = AppLockActivity.createIntent(this, packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
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
