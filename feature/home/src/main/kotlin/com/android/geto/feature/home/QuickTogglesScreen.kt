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
package com.android.geto.feature.home

import android.app.NotificationManager
import android.app.UiModeManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.android.geto.designsystem.icon.GetoIcons

// ── Data model ───────────────────────────────────────────────────────────────

internal data class QuickToggle(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val category: String,
    val priority: Int,
    val readState: (Context) -> Boolean,
    val writeState: (Context, Boolean) -> Boolean,
    val requiresWriteSecure: Boolean = false,
    val requiresWriteSettings: Boolean = false,
    val requiresNotificationPolicy: Boolean = false,
    val minApi: Int = 1,
)

// ── Toggle definitions ────────────────────────────────────────────────────────

private fun buildAllToggles(): List<QuickToggle> = listOf(

    // ─── Developer Tools ────────────────────────────────────────────────────

    QuickToggle(
        id = "dev_options",
        label = "Developer Options",
        icon = GetoIcons.DeveloperMode,
        category = "Developer Tools",
        priority = 1,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, if (on) 1 else 0) } catch (_: Exception) { false }
            true
        },
    ),
    QuickToggle(
        id = "usb_debugging",
        label = "USB Debugging",
        icon = GetoIcons.Usb,
        category = "Developer Tools",
        priority = 2,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, if (on) 1 else 0) } catch (_: Exception) { false }
            true
        },
    ),
    QuickToggle(
        id = "wifi_debugging",
        label = "WiFi Debugging",
        icon = GetoIcons.BugReport,
        category = "Developer Tools",
        priority = 3,
        requiresWriteSecure = true,
        minApi = Build.VERSION_CODES.R,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Settings.Global.getInt(ctx.contentResolver, "adb_wifi_enabled", 0) == 1
            } else false
        },
        writeState = { ctx, on ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { Settings.Global.putInt(ctx.contentResolver, "adb_wifi_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            }
            true
        },
    ),
    QuickToggle(
        id = "show_touches",
        label = "Show Touches",
        icon = GetoIcons.Visibility,
        category = "Developer Tools",
        priority = 4,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, "show_touches", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, "show_touches", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "pointer_location",
        label = "Pointer Location",
        icon = GetoIcons.NetworkCheck,
        category = "Developer Tools",
        priority = 5,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, "pointer_location", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, "pointer_location", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "show_layout_bounds",
        label = "Layout Bounds",
        icon = GetoIcons.Layers,
        category = "Developer Tools",
        priority = 6,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "show_layout_bounds", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "show_layout_bounds", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "dont_keep_activities",
        label = "Don't Keep Activities",
        icon = GetoIcons.DeleteSweep,
        category = "Developer Tools",
        priority = 7,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "window_anim_off",
        label = "Window Anim Off",
        icon = GetoIcons.Speed,
        category = "Developer Tools",
        priority = 8,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f) == 0f
        },
        writeState = { ctx, on ->
            try { Settings.Global.putFloat(ctx.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, if (on) 0f else 1f) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "transition_anim_off",
        label = "Transition Anim Off",
        icon = GetoIcons.Speed,
        category = "Developer Tools",
        priority = 9,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
        },
        writeState = { ctx, on ->
            try { Settings.Global.putFloat(ctx.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, if (on) 0f else 1f) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "animator_off",
        label = "Animator Off",
        icon = GetoIcons.Speed,
        category = "Developer Tools",
        priority = 10,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        },
        writeState = { ctx, on ->
            try { Settings.Global.putFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (on) 0f else 1f) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "stay_awake",
        label = "Stay Awake",
        icon = GetoIcons.BatteryCharging,
        category = "Developer Tools",
        priority = 11,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, if (on) 3 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "bugreport_power_menu",
        label = "Bug Report in Menu",
        icon = GetoIcons.BugReport,
        category = "Developer Tools",
        priority = 12,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "bugreport_in_power_menu", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "bugreport_in_power_menu", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "gpu_overdraw",
        label = "GPU Overdraw",
        icon = GetoIcons.Memory,
        category = "Developer Tools",
        priority = 13,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getString(ctx.contentResolver, "show_gpu_overdraw")?.equals("show") == true
        },
        writeState = { ctx, on ->
            try { Settings.Global.putString(ctx.contentResolver, "show_gpu_overdraw", if (on) "show" else "false") } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "strict_mode",
        label = "Strict Mode",
        icon = GetoIcons.Warning,
        category = "Developer Tools",
        priority = 14,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 &&
                Settings.Global.getInt(ctx.contentResolver, "development_enable_freeform_windows_support", 0) == 0 &&
                Settings.Global.getInt(ctx.contentResolver, "enable_console_logging_in_adb", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "enable_console_logging_in_adb", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "verbose_wifi_log",
        label = "Verbose WiFi Log",
        icon = GetoIcons.Wifi,
        category = "Developer Tools",
        priority = 15,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.WIFI_VERBOSE_LOGGING_ENABLED, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.WIFI_VERBOSE_LOGGING_ENABLED, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),

    // ─── Network ─────────────────────────────────────────────────────────────

    QuickToggle(
        id = "wifi",
        label = "WiFi",
        icon = GetoIcons.Wifi,
        category = "Network",
        priority = 16,
        readState = { ctx ->
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.isWifiEnabled == true
        },
        writeState = { ctx, on ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wm?.setWifiEnabled(on)
                true
            } else {
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                false
            }
        },
    ),
    QuickToggle(
        id = "airplane_mode",
        label = "Airplane Mode",
        icon = GetoIcons.Block,
        category = "Network",
        priority = 17,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        },
        writeState = { ctx, on ->
            try {
                Settings.Global.putInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (on) 1 else 0)
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                intent.putExtra("state", on)
                ctx.sendBroadcast(intent)
            } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "bluetooth",
        label = "Bluetooth",
        icon = GetoIcons.Bluetooth,
        category = "Network",
        priority = 18,
        readState = { ctx ->
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.isEnabled == true
        },
        writeState = { ctx, on ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION", "MissingPermission")
                val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                if (on) bm?.adapter?.enable() else bm?.adapter?.disable()
                true
            } else {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                false
            }
        },
    ),
    QuickToggle(
        id = "nfc",
        label = "NFC",
        icon = GetoIcons.Nfc,
        category = "Network",
        priority = 19,
        readState = { ctx ->
            NfcAdapter.getDefaultAdapter(ctx)?.isEnabled == true
        },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "mobile_data",
        label = "Mobile Data",
        icon = GetoIcons.NetworkCheck,
        category = "Network",
        priority = 20,
        readState = { _ -> false },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "hotspot",
        label = "Hotspot",
        icon = GetoIcons.Wifi,
        category = "Network",
        priority = 21,
        readState = { _ -> false },
        writeState = { ctx, _ ->
            val intent = Intent(Intent.ACTION_MAIN)
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { ctx.startActivity(intent) } catch (_: Exception) {
                val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(fallback)
            }
            false
        },
    ),
    QuickToggle(
        id = "data_roaming",
        label = "Data Roaming",
        icon = GetoIcons.Dns,
        category = "Network",
        priority = 22,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.DATA_ROAMING, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.DATA_ROAMING, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "network_anim",
        label = "Network Speed Overlay",
        icon = GetoIcons.Speed,
        category = "Network",
        priority = 23,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "network_traffic_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "network_traffic_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),

    // ─── Display ─────────────────────────────────────────────────────────────

    QuickToggle(
        id = "auto_rotate",
        label = "Auto-rotate",
        icon = GetoIcons.ScreenRotation,
        category = "Display",
        priority = 24,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "auto_brightness",
        label = "Auto-brightness",
        icon = GetoIcons.BrightnessHigh,
        category = "Display",
        priority = 25,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        },
        writeState = { ctx, on ->
            try {
                Settings.System.putInt(
                    ctx.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "dark_mode",
        label = "Dark Mode",
        icon = GetoIcons.Palette,
        category = "Display",
        priority = 26,
        readState = { ctx ->
            val uiManager = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            uiManager?.nightMode == UiModeManager.MODE_NIGHT_YES
        },
        writeState = { ctx, on ->
            try {
                val uiManager = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
                uiManager?.nightMode = if (on) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "night_light",
        label = "Night Light",
        icon = GetoIcons.BrightnessHigh,
        category = "Display",
        priority = 27,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "night_display_activated", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "night_display_activated", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "high_contrast",
        label = "High Contrast",
        icon = GetoIcons.Visibility,
        category = "Display",
        priority = 28,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "high_text_contrast_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "high_text_contrast_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "color_inversion",
        label = "Color Inversion",
        icon = GetoIcons.VisibilityOff,
        category = "Display",
        priority = 29,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "accessibility_display_inversion_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "accessibility_display_inversion_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "screen_saver",
        label = "Screen Saver",
        icon = GetoIcons.PhoneAndroid,
        category = "Display",
        priority = 30,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "screensaver_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "screensaver_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "force_rtl",
        label = "Force RTL Layout",
        icon = GetoIcons.ScreenRotation,
        category = "Display",
        priority = 31,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 &&
                Settings.Global.getString(ctx.contentResolver, "debug.force.rtl") == "1"
        },
        writeState = { ctx, on ->
            try { Settings.Global.putString(ctx.contentResolver, "debug.force.rtl", if (on) "1" else "0") } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "large_text",
        label = "Large Text",
        icon = GetoIcons.AccessibilityNew,
        category = "Display",
        priority = 32,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getFloat(ctx.contentResolver, Settings.System.FONT_SCALE, 1f) >= 1.3f
        },
        writeState = { ctx, on ->
            try { Settings.System.putFloat(ctx.contentResolver, Settings.System.FONT_SCALE, if (on) 1.3f else 1.0f) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "magnification",
        label = "Magnification",
        icon = GetoIcons.Fullscreen,
        category = "Display",
        priority = 33,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "accessibility_display_magnification_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "accessibility_display_magnification_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),

    // ─── Sound & Notifications ────────────────────────────────────────────────

    QuickToggle(
        id = "dnd",
        label = "Do Not Disturb",
        icon = GetoIcons.Notifications,
        category = "Sound",
        priority = 34,
        requiresNotificationPolicy = true,
        readState = { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                nm?.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
        },
        writeState = { ctx, on ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm?.isNotificationPolicyAccessGranted == true) {
                nm.setInterruptionFilter(
                    if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else NotificationManager.INTERRUPTION_FILTER_ALL,
                )
                true
            } else {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                false
            }
        },
    ),
    QuickToggle(
        id = "vibration",
        label = "Vibration",
        icon = GetoIcons.PhoneAndroid,
        category = "Sound",
        priority = 35,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.VIBRATE_ON, 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.VIBRATE_ON, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "silent_mode",
        label = "Silent Mode",
        icon = GetoIcons.HeadphonesOff,
        category = "Sound",
        priority = 36,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 0
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, if (on) 0 else 1) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "touch_sounds",
        label = "Touch Sounds",
        icon = GetoIcons.VolumeUp,
        category = "Sound",
        priority = 37,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "haptic_feedback",
        label = "Haptic Feedback",
        icon = GetoIcons.PhoneAndroid,
        category = "Sound",
        priority = 38,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "lockscreen_sounds",
        label = "Lock Screen Sound",
        icon = GetoIcons.Lock,
        category = "Sound",
        priority = 39,
        requiresWriteSettings = true,
        readState = { ctx ->
            Settings.System.getInt(ctx.contentResolver, Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.LOCKSCREEN_SOUNDS_ENABLED, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),

    // ─── Battery & Performance ────────────────────────────────────────────────

    QuickToggle(
        id = "battery_saver",
        label = "Battery Saver",
        icon = GetoIcons.BatteryAlert,
        category = "Battery",
        priority = 40,
        requiresWriteSecure = true,
        readState = { ctx ->
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "low_power", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "adaptive_battery",
        label = "Adaptive Battery",
        icon = GetoIcons.BatteryFull,
        category = "Battery",
        priority = 41,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "adaptive_battery_management_enabled", 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "adaptive_battery_management_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "performance_mode",
        label = "Performance Mode",
        icon = GetoIcons.Speed,
        category = "Battery",
        priority = 42,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "restricted_device_performance", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "restricted_device_performance", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "background_apps_limit",
        label = "Limit Background",
        icon = GetoIcons.Apps,
        category = "Battery",
        priority = 43,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.APP_STANDBY_ENABLED, 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, Settings.Global.APP_STANDBY_ENABLED, if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "auto_sync",
        label = "Auto Sync",
        icon = GetoIcons.Refresh,
        category = "Battery",
        priority = 44,
        readState = { _ ->
            android.content.ContentResolver.getMasterSyncAutomatically()
        },
        writeState = { _, on ->
            android.content.ContentResolver.setMasterSyncAutomatically(on)
            true
        },
    ),

    // ─── Privacy & Location ───────────────────────────────────────────────────

    QuickToggle(
        id = "location",
        label = "Location",
        icon = GetoIcons.NetworkCheck,
        category = "Privacy",
        priority = 45,
        requiresWriteSecure = true,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lm = ctx.getSystemService(android.location.LocationManager::class.java)
                lm?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.LOCATION_MODE, 0) != Settings.Secure.LOCATION_MODE_OFF
            }
        },
        writeState = { ctx, on ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                try {
                    @Suppress("DEPRECATION")
                    Settings.Secure.putInt(
                        ctx.contentResolver,
                        Settings.Secure.LOCATION_MODE,
                        if (on) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF,
                    )
                } catch (_: Exception) { }
                true
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                false
            }
        },
    ),
    QuickToggle(
        id = "camera_toggle",
        label = "Camera Access",
        icon = GetoIcons.Visibility,
        category = "Privacy",
        priority = 46,
        minApi = Build.VERSION_CODES.S,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val pm = ctx.getSystemService(android.hardware.camera2.CameraManager::class.java)
                runCatching { pm != null }.getOrDefault(true)
            } else true
        },
        writeState = { ctx, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent("android.settings.PRIVACY_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(intent) } catch (_: Exception) {
                    val fallback = Intent(Settings.ACTION_PRIVACY_SETTINGS)
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(fallback)
                }
            }
            false
        },
    ),
    QuickToggle(
        id = "mic_toggle",
        label = "Microphone Access",
        icon = GetoIcons.VolumeUp,
        category = "Privacy",
        priority = 47,
        minApi = Build.VERSION_CODES.S,
        readState = { _ -> true },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "usage_access",
        label = "Usage Access",
        icon = GetoIcons.Analytics,
        category = "Privacy",
        priority = 48,
        readState = { ctx ->
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "notification_history",
        label = "Notification History",
        icon = GetoIcons.Activity,
        category = "Privacy",
        priority = 49,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "notification_history_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "notification_history_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "clipboard_access",
        label = "Clipboard Alert",
        icon = GetoIcons.Copy,
        category = "Privacy",
        priority = 50,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "clipboard_show_access_notifications", 1) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "clipboard_show_access_notifications", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),

    // ─── Accessibility ────────────────────────────────────────────────────────

    QuickToggle(
        id = "talkback",
        label = "TalkBack",
        icon = GetoIcons.AccessibilityNew,
        category = "Accessibility",
        priority = 51,
        readState = { ctx ->
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabled?.contains("com.google.android.marvin.talkback") == true ||
                enabled?.contains("com.android.talkback") == true
        },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "accessibility_shortcut",
        label = "Accessibility Shortcut",
        icon = GetoIcons.AccessibilityNew,
        category = "Accessibility",
        priority = 52,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "accessibility_shortcut_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "accessibility_shortcut_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "color_correction",
        label = "Color Correction",
        icon = GetoIcons.Palette,
        category = "Accessibility",
        priority = 53,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getInt(ctx.contentResolver, "accessibility_display_daltonizer_enabled", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Secure.putInt(ctx.contentResolver, "accessibility_display_daltonizer_enabled", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "select_to_speak",
        label = "Select to Speak",
        icon = GetoIcons.VolumeUp,
        category = "Accessibility",
        priority = 54,
        readState = { ctx ->
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabled?.contains("com.google.android.accessibility.selecttospeak") == true
        },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),

    // ─── System ───────────────────────────────────────────────────────────────

    QuickToggle(
        id = "oem_unlock",
        label = "OEM Unlock",
        icon = GetoIcons.LockOpen,
        category = "System",
        priority = 55,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "oem_unlock_allowed_by_user", 0) == 1
        },
        writeState = { ctx, _ ->
            val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            false
        },
    ),
    QuickToggle(
        id = "install_unknown",
        label = "Unknown Sources",
        icon = GetoIcons.Store,
        category = "System",
        priority = 56,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.packageManager.canRequestPackageInstalls()
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
            }
        },
        writeState = { ctx, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
            false
        },
    ),
    QuickToggle(
        id = "mock_location",
        label = "Mock Location",
        icon = GetoIcons.NetworkCheck,
        category = "System",
        priority = 57,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION)?.isNotEmpty() == true ||
                Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1
        },
        writeState = { ctx, on ->
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    @Suppress("DEPRECATION")
                    Settings.Secure.putInt(ctx.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION, if (on) 1 else 0)
                }
            } catch (_: Exception) { }
            true
        },
    ),
    QuickToggle(
        id = "overlay_permission",
        label = "Draw Over Apps",
        icon = GetoIcons.Layers,
        category = "System",
        priority = 58,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(ctx)
            else true
        },
        writeState = { ctx, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
            false
        },
    ),
    QuickToggle(
        id = "write_settings",
        label = "Modify System Settings",
        icon = GetoIcons.SettingsSuggest,
        category = "System",
        priority = 59,
        readState = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(ctx)
            else true
        },
        writeState = { ctx, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
            false
        },
    ),
    QuickToggle(
        id = "dev_freeform",
        label = "Freeform Windows",
        icon = GetoIcons.Fullscreen,
        category = "System",
        priority = 60,
        requiresWriteSecure = true,
        readState = { ctx ->
            Settings.Global.getInt(ctx.contentResolver, "enable_freeform_support", 0) == 1
        },
        writeState = { ctx, on ->
            try { Settings.Global.putInt(ctx.contentResolver, "enable_freeform_support", if (on) 1 else 0) } catch (_: Exception) { }
            true
        },
    ),
)

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
internal fun QuickTogglesRoute(
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val allToggles = remember { buildAllToggles().filter { it.minApi <= Build.VERSION.SDK_INT } }
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        allToggles.forEach { toggle ->
            runCatching { toggleStates[toggle.id] = toggle.readState(context) }
        }
    }

    QuickTogglesScreen(
        modifier = modifier,
        allToggles = allToggles,
        toggleStates = toggleStates,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onToggle = { toggle, newState ->
            val hasWriteSecure = ContextCompat.checkSelfPermission(
                context, "android.permission.WRITE_SECURE_SETTINGS",
            ) == PackageManager.PERMISSION_GRANTED
            val hasWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.canWrite(context)
            } else true

            val canProceed = when {
                toggle.requiresWriteSecure && !hasWriteSecure -> {
                    Toast.makeText(context, "Requires ADB: grant WRITE_SECURE_SETTINGS first", Toast.LENGTH_LONG).show()
                    false
                }
                toggle.requiresWriteSettings && !hasWriteSettings -> {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    false
                }
                else -> true
            }
            if (canProceed) {
                runCatching {
                    val success = toggle.writeState(context, newState)
                    if (success) toggleStates[toggle.id] = newState
                }
            }
        },
        onNavigationIconClick = onNavigationIconClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickTogglesScreen(
    modifier: Modifier = Modifier,
    allToggles: List<QuickToggle>,
    toggleStates: Map<String, Boolean>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggle: (QuickToggle, Boolean) -> Unit,
    onNavigationIconClick: () -> Unit,
) {
    val filtered = remember(searchQuery, allToggles) {
        if (searchQuery.isBlank()) allToggles
        else allToggles.filter { it.label.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val grouped = remember(filtered) {
        filtered.sortedBy { it.priority }.groupBy { it.category }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quick Toggles",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigationIconClick) {
                        Icon(imageVector = GetoIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(4) }) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            if (grouped.isEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No toggles found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            grouped.forEach { (category, toggles) ->
                item(span = { GridItemSpan(4) }) {
                    CategoryHeader(category = category)
                }
                items(toggles) { toggle ->
                    val isOn = toggleStates[toggle.id] ?: false
                    ToggleTile(
                        toggle = toggle,
                        isOn = isOn,
                        onToggle = { onToggle(toggle, !isOn) },
                    )
                }
                item(span = { GridItemSpan(4) }) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item(span = { GridItemSpan(4) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        placeholder = {
            Text(
                text = "Search toggles…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = GetoIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = GetoIcons.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
            }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun CategoryHeader(category: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .weight(0.04f)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(1.dp),
                ),
        )
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(1.dp),
                ),
        )
    }
}

@Composable
private fun ToggleTile(
    toggle: QuickToggle,
    isOn: Boolean,
    onToggle: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "tile_bg",
    )
    val iconColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tile_icon",
    )
    val textColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tile_text",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = toggle.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = toggle.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            Text(
                text = if (isOn) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                ),
                color = if (isOn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
