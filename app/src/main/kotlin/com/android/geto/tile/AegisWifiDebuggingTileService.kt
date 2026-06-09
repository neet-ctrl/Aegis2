package com.android.geto.tile

import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.android.geto.R

@RequiresApi(Build.VERSION_CODES.N)
class AegisWifiDebuggingTileService : TileService() {

    private fun isWifiDebugEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
    }

    private fun hasPermission(): Boolean =
        packageManager.checkPermission(
            "android.permission.WRITE_SECURE_SETTINGS",
            packageName,
        ) == PackageManager.PERMISSION_GRANTED

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            icon = Icon.createWithResource(this@AegisWifiDebuggingTileService, R.drawable.ic_qs_wifi_debug)
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                    state = Tile.STATE_UNAVAILABLE
                    label = "WiFi Debug"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "Needs Android 11+"
                }
                !hasPermission() -> {
                    state = Tile.STATE_UNAVAILABLE
                    label = "WiFi Debug"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "Grant via ADB"
                }
                else -> {
                    val enabled = isWifiDebugEnabled()
                    state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = "WiFi Debug"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = if (enabled) "Enabled" else "Disabled"
                }
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !hasPermission()) return
        val current = isWifiDebugEnabled()
        val newVal = if (current) 0 else 1
        val success = try {
            Settings.Global.putInt(contentResolver, "adb_wifi_enabled", newVal)
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", if (current) 1 else 0) == newVal
        } catch (_: Exception) { false }
        val actual = if (success) newVal == 1 else current
        qsTile?.apply {
            state = if (actual) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (success) {
                    if (actual) "Enabled" else "Disabled"
                } else "Write failed"
            }
            updateTile()
        }
    }
}
