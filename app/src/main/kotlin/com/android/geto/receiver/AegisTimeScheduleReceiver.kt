package com.android.geto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.geto.engine.AegisAutomationEngine
import com.android.geto.feature.home.AegisAutomationStore
import com.android.geto.scheduler.AegisTimeScheduler

class AegisTimeScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AegisTimeScheduler.ACTION_FIRE) return

        val automationId = intent.getLongExtra(AegisTimeScheduler.EXTRA_AUTOMATION_ID, -1L)
        val triggerLabel = intent.getStringExtra(AegisTimeScheduler.EXTRA_TRIGGER_LABEL) ?: return
        if (automationId < 0L) return

        AegisAutomationEngine.fireTrigger(context, triggerLabel, "Scheduled")

        val automation = AegisAutomationStore.getAutomations(context)
            .firstOrNull { it.id == automationId } ?: return
        if (automation.isEnabled) {
            AegisTimeScheduler.scheduleIfNeeded(context, automation)
        }
    }
}
