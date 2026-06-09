package com.android.geto.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.android.geto.R

@RequiresApi(Build.VERSION_CODES.N)
class AegisAutoRotateTileService : TileService() {

    private fun isAutoRotateEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    private fun canWrite(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(this)
        else true

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            icon = Icon.createWithResource(this@AegisAutoRotateTileService, R.drawable.ic_qs_auto_rotate)
            if (!canWrite()) {
                state = Tile.STATE_UNAVAILABLE
                label = "Auto-rotate"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "Grant Modify Settings"
            } else {
                val enabled = isAutoRotateEnabled()
                state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = "Auto-rotate"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = if (enabled) "On" else "Off"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!canWrite()) return
        val current = isAutoRotateEnabled()
        val newVal = if (current) 0 else 1
        val success = try {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, newVal)
            Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (current) 1 else 0) == newVal
        } catch (_: Exception) { false }
        val actual = if (success) newVal == 1 else current
        qsTile?.apply {
            state = if (actual) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (success) {
                    if (actual) "On" else "Off"
                } else "Write failed"
            }
            updateTile()
        }
    }
}
