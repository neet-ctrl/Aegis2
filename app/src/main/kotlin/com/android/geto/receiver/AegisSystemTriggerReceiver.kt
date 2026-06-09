package com.android.geto.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.android.geto.engine.AegisAutomationEngine

class AegisSystemTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED ->
                AegisAutomationEngine.fireTrigger(context, "Charger Connected")

            Intent.ACTION_POWER_DISCONNECTED ->
                AegisAutomationEngine.fireTrigger(context, "Charger Disconnected")

            Intent.ACTION_PACKAGE_ADDED -> {
                val pkg = intent.data?.schemeSpecificPart ?: return
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    AegisAutomationEngine.fireTrigger(context, "App Install", pkg)
                }
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                val pkg = intent.data?.schemeSpecificPart ?: ""
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    AegisAutomationEngine.fireTrigger(context, "App Uninstall", pkg)
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val deviceName = getBluetoothDeviceName(intent) ?: "Device"
                AegisAutomationEngine.fireTrigger(context, "Bluetooth Connected", deviceName)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val deviceName = getBluetoothDeviceName(intent) ?: "Device"
                AegisAutomationEngine.fireTrigger(context, "Bluetooth Disconnected", deviceName)
            }
        }
    }

    private fun getBluetoothDeviceName(intent: Intent): String? = runCatching {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        device?.name
    }.getOrNull()
}
