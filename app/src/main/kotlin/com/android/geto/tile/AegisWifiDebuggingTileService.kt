/*
 *
 *   Copyright 2023 Einstein Blanco
 *
 *   Licensed under the GNU General Public License v3.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/gpl-3.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.android.geto.tile

import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.N)
class AegisWifiDebuggingTileService : TileService() {

    private fun isWifiDebugEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                state = Tile.STATE_UNAVAILABLE
                label = "WiFi Debug"
                subtitle = "Needs Android 11+"
            } else if (!hasPermission()) {
                state = Tile.STATE_UNAVAILABLE
                label = "WiFi Debug"
                subtitle = "ADB setup needed"
            } else {
                val enabled = isWifiDebugEnabled()
                state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = "WiFi Debug"
                subtitle = if (enabled) "Enabled" else "Disabled"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !hasPermission()) return
        val current = isWifiDebugEnabled()
        val newVal = if (current) 0 else 1
        runCatching {
            Settings.Global.putInt(contentResolver, "adb_wifi_enabled", newVal)
        }
        qsTile?.apply {
            state = if (newVal == 1) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (newVal == 1) "Enabled" else "Disabled"
            updateTile()
        }
    }
}
