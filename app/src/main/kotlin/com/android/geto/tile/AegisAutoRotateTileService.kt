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

import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

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
            val enabled = isAutoRotateEnabled()
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Auto-rotate"
            subtitle = if (enabled) "On" else "Off"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!canWrite()) return
        val current = isAutoRotateEnabled()
        val newVal = if (current) 0 else 1
        runCatching {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, newVal)
        }
        qsTile?.apply {
            state = if (newVal == 1) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (newVal == 1) "On" else "Off"
            updateTile()
        }
    }
}
