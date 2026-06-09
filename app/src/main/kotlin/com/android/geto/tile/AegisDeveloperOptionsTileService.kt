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
class AegisDeveloperOptionsTileService : TileService() {

    private fun isDevOptionsEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

    private fun hasPermission(): Boolean =
        packageManager.checkPermission(
            "android.permission.WRITE_SECURE_SETTINGS",
            packageName,
        ) == PackageManager.PERMISSION_GRANTED

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            icon = Icon.createWithResource(this@AegisDeveloperOptionsTileService, R.drawable.ic_qs_developer)
            if (!hasPermission()) {
                state = Tile.STATE_UNAVAILABLE
                label = "Dev Options"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "Grant via ADB"
            } else {
                val enabled = isDevOptionsEnabled()
                state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = "Dev Options"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = if (enabled) "Enabled" else "Disabled"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!hasPermission()) return
        val current = isDevOptionsEnabled()
        val newVal = if (current) 0 else 1
        val success = try {
            Settings.Global.putInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, newVal)
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, if (current) 1 else 0) == newVal
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
