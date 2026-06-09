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
package com.android.geto.feature.appsettings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

private data class ControlEntry(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val settingKey: String,
    val settingType: String,
)

private data class ControlCategory(
    val name: String,
    val icon: ImageVector,
    val entries: List<ControlEntry>,
)

private val controlCategories = listOf(
    ControlCategory(
        name = "Display",
        icon = GetoIcons.PhoneAndroid,
        entries = listOf(
            ControlEntry(GetoIcons.BrightnessHigh, "Brightness", "Override screen brightness (0–255)", "screen_brightness", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Auto-Brightness", "1 = auto (adaptive), 0 = manual", "screen_brightness_mode", "SYSTEM"),
            ControlEntry(GetoIcons.Speed, "Screen Timeout", "Timeout in ms (e.g. 30000 = 30s, 0 = never)", "screen_off_timeout", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Font Scale", "Font size multiplier (1.0 = normal, 0.85 = small, 1.15 = large)", "font_scale", "SYSTEM"),
            ControlEntry(GetoIcons.ScreenRotation, "Rotation", "0 = portrait, 1 = landscape, 2 = reverse portrait, 3 = reverse landscape", "user_rotation", "SYSTEM"),
            ControlEntry(GetoIcons.Block, "Auto-Rotate", "1 = auto-rotate on, 0 = locked to portrait", "accelerometer_rotation", "SYSTEM"),
            ControlEntry(GetoIcons.BatteryFull, "Battery % in Status Bar", "1 = show battery percentage, 0 = hide", "show_battery_percent", "SYSTEM"),
            ControlEntry(GetoIcons.Speed, "Window Animation Scale", "Animation speed: 0.0 = off, 0.5 = faster, 1.0 = normal, 2.0 = slower", "window_animation_scale", "GLOBAL"),
            ControlEntry(GetoIcons.Speed, "Transition Animation Scale", "Screen transition speed: 0.0 = off, 0.5 = faster, 1.0 = normal", "transition_animation_scale", "GLOBAL"),
            ControlEntry(GetoIcons.Speed, "Animator Duration Scale", "Animator speed: 0.0 = off, 0.5 = faster, 1.0 = normal", "animator_duration_scale", "GLOBAL"),
            ControlEntry(GetoIcons.Fullscreen, "Immersive Mode Policy", "vr=*;apps=-*;touch_pad=*;navigation=navkeys;* (or 'null' to clear)", "policy_control", "GLOBAL"),
        ),
    ),
    ControlCategory(
        name = "Audio",
        icon = GetoIcons.VolumeUp,
        entries = listOf(
            ControlEntry(GetoIcons.Block, "Vibrate on Ring", "1 = vibrate when ringing, 0 = no vibration", "vibrate_when_ringing", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Dial Pad Tones", "1 = play tones when dialing, 0 = silent dial pad", "dtmf_tone", "SYSTEM"),
            ControlEntry(GetoIcons.Notifications, "DND / Do Not Disturb", "0 = off, 1 = priority only, 2 = total silence, 3 = alarms only", "zen_mode", "GLOBAL"),
        ),
    ),
    ControlCategory(
        name = "Network",
        icon = GetoIcons.Wifi,
        entries = listOf(
            ControlEntry(GetoIcons.Block, "Airplane Mode", "1 = on (all radios off), 0 = off", "airplane_mode_on", "GLOBAL"),
            ControlEntry(GetoIcons.Dns, "Private DNS Mode", "off / opportunistic / hostname (e.g. dns.google)", "private_dns_mode", "GLOBAL"),
            ControlEntry(GetoIcons.Wifi, "Wi-Fi Scanning (Always On)", "1 = scan for Wi-Fi even when Wi-Fi is off, 0 = disable", "wifi_scan_always_enabled", "GLOBAL"),
        ),
    ),
    ControlCategory(
        name = "Device State",
        icon = GetoIcons.BatteryCharging,
        entries = listOf(
            ControlEntry(GetoIcons.BatteryAlert, "Battery Saver", "1 = Battery Saver on, 0 = off", "low_power", "GLOBAL"),
            ControlEntry(GetoIcons.BatteryFull, "Adaptive Battery", "1 = learn usage and restrict background apps, 0 = off", "adaptive_battery_management_enabled", "GLOBAL"),
            ControlEntry(GetoIcons.Memory, "Stay Awake While Charging", "Keep screen on: 1 = AC, 2 = USB, 4 = Wireless (bitmask, combine with |)", "stay_on_while_plugged_in", "GLOBAL"),
            ControlEntry(GetoIcons.Speed, "Pointer Speed", "Mouse/touchpad speed: -7 (slowest) to 7 (fastest), 0 = default", "pointer_speed", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Haptic Feedback", "1 = vibrate on touch interactions, 0 = off", "haptic_feedback_enabled", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Touch Sounds", "1 = play sounds on UI taps, 0 = off", "sound_effects_enabled", "SYSTEM"),
            ControlEntry(GetoIcons.Lock, "Screen Lock Delay", "ms before lock after screen off (e.g. 5000 = 5s, 0 = immediately)", "lock_screen_lock_after_timeout", "SECURE"),
        ),
    ),
    ControlCategory(
        name = "Privacy & Developer",
        icon = GetoIcons.Shield,
        entries = listOf(
            ControlEntry(GetoIcons.Block, "Install Unknown Apps", "1 = allow installing from unknown sources, 0 = block", "install_non_market_apps", "SECURE"),
            ControlEntry(GetoIcons.DeveloperMode, "Developer Options", "1 = show Developer Options in Settings, 0 = hide", "development_settings_enabled", "GLOBAL"),
            ControlEntry(GetoIcons.Terminal, "USB Debugging (ADB)", "1 = ADB over USB enabled, 0 = disabled", "adb_enabled", "GLOBAL"),
            ControlEntry(GetoIcons.Wifi, "Wireless ADB (Android 11+)", "1 = ADB over Wi-Fi enabled, 0 = disabled", "adb_wifi_enabled", "GLOBAL"),
        ),
    ),
)

@Composable
internal fun AppControlsTab(
    modifier: Modifier = Modifier,
    onAddRule: (key: String, settingTypeName: String, label: String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = GetoIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "All entries here work via WRITE_SETTINGS or WRITE_SECURE_SETTINGS (grant once via ADB). Tap \"+ Add\" to create a per-app rule for any control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        controlCategories.forEach { category ->
            item {
                ControlCategorySection(
                    category = category,
                    onAddRule = onAddRule,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ControlCategorySection(
    category: ControlCategory,
    onAddRule: (key: String, settingTypeName: String, label: String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            category.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )
                }
                ControlEntryRow(entry = entry, onAddRule = onAddRule)
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ControlEntryRow(
    entry: ControlEntry,
    onAddRule: (key: String, settingTypeName: String, label: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = entry.settingType,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = entry.settingKey,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        TextButton(onClick = { onAddRule(entry.settingKey, entry.settingType, entry.title) }) {
            Icon(
                imageVector = GetoIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
