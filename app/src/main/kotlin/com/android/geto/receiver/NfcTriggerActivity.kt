package com.android.geto.receiver

import android.app.Activity
import android.os.Bundle
import com.android.geto.engine.AegisAutomationEngine

class NfcTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            AegisAutomationEngine.fireTrigger(this, "NFC Tag Detected")
        } catch (_: Exception) {}
        finish()
    }
}
