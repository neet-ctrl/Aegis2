package com.android.geto.tile

import android.app.UiModeManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.android.geto.R

@RequiresApi(Build.VERSION_CODES.N)
class AegisDarkModeTileService : TileService() {

    private fun isDarkModeEnabled(): Boolean {
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            icon = Icon.createWithResource(this@AegisDarkModeTileService, R.drawable.ic_qs_dark_mode)
            val enabled = isDarkModeEnabled()
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Dark Mode"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = if (enabled) "On" else "Off"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val current = isDarkModeEnabled()
        val targetMode = if (current) UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
        val success = try {
            val uiManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            uiManager?.nightMode = targetMode
            isDarkModeEnabled() == !current
        } catch (_: Exception) { false }
        val actual = if (success) !current else current
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
