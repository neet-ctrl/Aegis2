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
package com.android.geto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.android.geto.service.AegisMonitorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs: SharedPreferences = context.getSharedPreferences(
            "aegis_automation_prefs",
            Context.MODE_PRIVATE,
        )

        val automationsEnabled = prefs.getBoolean("automations_enabled", false)

        if (automationsEnabled) {
            val tilePrefs = context.getSharedPreferences("aegis_tile_prefs", Context.MODE_PRIVATE)
            tilePrefs.edit().putBoolean("automations_enabled", true).apply()
        }

        AegisMonitorService.start(context)
    }
}
