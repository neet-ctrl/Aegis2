package com.android.geto.tile

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class TilePreferencesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName: ComponentName? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
            }

        val settingsAction = when (componentName?.shortClassName) {
            ".tile.AegisDeveloperOptionsTileService",
            ".tile.AegisUsbDebuggingTileService",
            ".tile.AegisWifiDebuggingTileService",
            -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS

            ".tile.AegisDarkModeTileService" -> Settings.ACTION_DISPLAY_SETTINGS
            ".tile.AegisAutoRotateTileService" -> Settings.ACTION_DISPLAY_SETTINGS

            else -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        }

        try {
            startActivity(Intent(settingsAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }

        finish()
    }
}
