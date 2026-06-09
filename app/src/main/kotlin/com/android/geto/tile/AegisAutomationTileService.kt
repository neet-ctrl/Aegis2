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

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.android.geto.R

@RequiresApi(Build.VERSION_CODES.N)
class AegisAutomationTileService : TileService() {

    private lateinit var prefs: SharedPreferences
    private var automationsEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("aegis_tile_prefs", MODE_PRIVATE)
        automationsEnabled = prefs.getBoolean("automations_enabled", true)
    }

    override fun onStartListening() {
        super.onStartListening()
        automationsEnabled = prefs.getBoolean("automations_enabled", true)
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        automationsEnabled = !automationsEnabled
        prefs.edit().putBoolean("automations_enabled", automationsEnabled).apply()
        refreshTile()
    }

    private fun refreshTile() {
        qsTile?.apply {
            icon = Icon.createWithResource(this@AegisAutomationTileService, R.drawable.ic_qs_automation)
            state = if (automationsEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Automations"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (automationsEnabled) "Active" else "Paused"
            }
            contentDescription = if (automationsEnabled) "Aegis Automations Active" else "Aegis Automations Paused"
            updateTile()
        }
    }
}
