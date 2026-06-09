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
import androidx.compose.foundation.lazy.items
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
            ControlEntry(GetoIcons.Tune, "Brightness", "Override screen brightness (0–255)", "screen_brightness", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Auto-Brightness", "1 = auto, 0 = manual", "screen_brightness_mode", "SYSTEM"),
            ControlEntry(GetoIcons.Speed, "Screen Timeout", "Timeout in ms (e.g. 30000 = 30s)", "screen_off_timeout", "SYSTEM"),
            ControlEntry(GetoIcons.PhoneAndroid, "Font Scale", "Font size multiplier (e.g. 1.0, 1.15, 1.3)", "font_scale", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Rotation", "0 = portrait, 1 = landscape, 2 = reverse", "user_rotation", "SYSTEM"),
            ControlEntry(GetoIcons.Block, "Accelerometer Rotation", "1 = auto-rotate on, 0 = off", "accelerometer_rotation", "SYSTEM"),
        ),
    ),
    ControlCategory(
        name = "Audio",
        icon = GetoIcons.VolumeUp,
        entries = listOf(
            ControlEntry(GetoIcons.VolumeUp, "Media Volume", "Set media stream volume", "volume_music", "SYSTEM"),
            ControlEntry(GetoIcons.Notifications, "Notification Volume", "Set notification stream volume", "volume_notification", "SYSTEM"),
            ControlEntry(GetoIcons.Schedule, "Alarm Volume", "Set alarm stream volume", "volume_alarm", "SYSTEM"),
            ControlEntry(GetoIcons.VolumeUp, "Ring Volume", "Set ring stream volume", "volume_ring", "SYSTEM"),
            ControlEntry(GetoIcons.Block, "Vibrate on Ring", "1 = vibrate, 0 = silent", "vibrate_when_ringing", "SYSTEM"),
            ControlEntry(GetoIcons.Notifications, "DND Mode", "0 = off, 1 = priority, 2 = total silence", "zen_mode", "GLOBAL"),
        ),
    ),
    ControlCategory(
        name = "Network",
        icon = GetoIcons.Wifi,
        entries = listOf(
            ControlEntry(GetoIcons.Wifi, "Wi-Fi State", "1 = enabled, 0 = disabled", "wifi_on", "GLOBAL"),
            ControlEntry(GetoIcons.Bluetooth, "Bluetooth State", "1 = enabled, 0 = disabled (API limited)", "bluetooth_on", "GLOBAL"),
            ControlEntry(GetoIcons.Block, "Airplane Mode", "1 = on, 0 = off (Shizuku required)", "airplane_mode_on", "GLOBAL"),
        ),
    ),
    ControlCategory(
        name = "Device State",
        icon = GetoIcons.BatteryCharging,
        entries = listOf(
            ControlEntry(GetoIcons.BatteryAlert, "Battery Saver", "1 = on, 0 = off (Shizuku required)", "low_power", "GLOBAL"),
            ControlEntry(GetoIcons.Speed, "Pointer Speed", "Mouse/touch pointer speed (-7 to 7)", "pointer_speed", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Haptic Feedback", "1 = enabled, 0 = disabled", "haptic_feedback_enabled", "SYSTEM"),
            ControlEntry(GetoIcons.Tune, "Sound Effects", "1 = enabled, 0 = disabled", "sound_effects_enabled", "SYSTEM"),
            ControlEntry(GetoIcons.Lock, "Screen Lock Timeout", "Timeout in ms before lock", "lock_screen_lock_after_timeout", "SECURE"),
        ),
    ),
    ControlCategory(
        name = "Privacy Controls",
        icon = GetoIcons.Shield,
        entries = listOf(
            ControlEntry(GetoIcons.Fingerprint, "Location Mode", "0=off, 1=sensors, 2=battery, 3=high accuracy", "location_mode", "SECURE"),
            ControlEntry(GetoIcons.Block, "Install Non-Market Apps", "1 = allow, 0 = block", "install_non_market_apps", "SECURE"),
            ControlEntry(GetoIcons.Security, "Developer Options", "1 = enabled, 0 = disabled", "development_settings_enabled", "GLOBAL"),
            ControlEntry(GetoIcons.Terminal, "USB Debugging", "1 = enabled, 0 = disabled", "adb_enabled", "GLOBAL"),
        ),
    ),
)

@Composable
internal fun AppControlsTab(
    modifier: Modifier = Modifier,
    onAddRule: () -> Unit,
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
                        text = "These controls require WRITE_SECURE_SETTINGS via AShell U. Tap \"+ Add to Rules\" on any control to create a per-app rule for it.",
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
    onAddRule: () -> Unit,
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
    onAddRule: () -> Unit,
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

        TextButton(onClick = onAddRule) {
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
