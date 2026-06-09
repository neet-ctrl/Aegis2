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

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class AegisDarkModeTileService : TileService() {

    private fun isDarkModeEnabled(): Boolean {
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            val enabled = isDarkModeEnabled()
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Dark Mode"
            subtitle = if (enabled) "On" else "Off"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val current = isDarkModeEnabled()
        runCatching {
            val uiManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            uiManager?.nightMode = if (current) UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
        }
        qsTile?.apply {
            val newEnabled = !current
            state = if (newEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (newEnabled) "On" else "Off"
            updateTile()
        }
    }
}
