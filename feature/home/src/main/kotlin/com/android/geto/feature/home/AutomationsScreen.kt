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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

@Composable
internal fun AutomationsRoute(modifier: Modifier = Modifier) {
    AutomationsScreen(modifier = modifier)
}

@Composable
internal fun AutomationsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                icon = {
                    Icon(imageVector = GetoIcons.Add, contentDescription = null)
                },
                text = { Text("New Automation") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Available Triggers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Choose a trigger to start building an IF/THEN automation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            triggerCategories.forEach { category ->
                item {
                    TriggerCategorySection(
                        category = category,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "My Automations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "No automations created yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { AutomationEmptyHint() }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Example Automations",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    exampleAutomations.forEach { example ->
                        ExampleAutomationCard(example = example)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private data class TriggerItem(
    val icon: ImageVector,
    val label: String,
    val iconTint: Color? = null,
)

private data class TriggerCategory(
    val name: String,
    val icon: ImageVector,
    val triggers: List<TriggerItem>,
)

private val triggerCategories = listOf(
    TriggerCategory(
        name = "App Events",
        icon = GetoIcons.Apps,
        triggers = listOf(
            TriggerItem(GetoIcons.Apps, "App Launch"),
            TriggerItem(GetoIcons.Close, "App Close"),
            TriggerItem(GetoIcons.Android, "App Install"),
            TriggerItem(GetoIcons.DeleteSweep, "App Uninstall"),
        ),
    ),
    TriggerCategory(
        name = "Screen & Device",
        icon = GetoIcons.PhoneAndroid,
        triggers = listOf(
            TriggerItem(GetoIcons.Notifications, "Screen On"),
            TriggerItem(GetoIcons.Block, "Screen Off"),
            TriggerItem(GetoIcons.LockOpen, "Device Unlock"),
            TriggerItem(GetoIcons.Lock, "Device Lock"),
        ),
    ),
    TriggerCategory(
        name = "Power & Battery",
        icon = GetoIcons.BatteryCharging,
        triggers = listOf(
            TriggerItem(GetoIcons.FlashOn, "Charger Connected"),
            TriggerItem(GetoIcons.FlashOn, "Charger Disconnected"),
            TriggerItem(GetoIcons.BatteryAlert, "Battery %"),
        ),
    ),
    TriggerCategory(
        name = "Connectivity",
        icon = GetoIcons.Wifi,
        triggers = listOf(
            TriggerItem(GetoIcons.Wifi, "Wi-Fi Connected"),
            TriggerItem(GetoIcons.WifiOff, "Wi-Fi Disconnected"),
            TriggerItem(GetoIcons.Bluetooth, "Bluetooth Connected"),
            TriggerItem(GetoIcons.Bluetooth, "Bluetooth Disconnected"),
            TriggerItem(GetoIcons.Nfc, "NFC Tag Detected"),
        ),
    ),
    TriggerCategory(
        name = "Audio",
        icon = GetoIcons.Headphones,
        triggers = listOf(
            TriggerItem(GetoIcons.Headphones, "Headphones Connected"),
            TriggerItem(GetoIcons.HeadphonesOff, "Headphones Disconnected"),
            TriggerItem(GetoIcons.VolumeUp, "Volume Changed"),
        ),
    ),
    TriggerCategory(
        name = "Time & Schedule",
        icon = GetoIcons.Schedule,
        triggers = listOf(
            TriggerItem(GetoIcons.Schedule, "Time Schedule"),
            TriggerItem(GetoIcons.Schedule, "Day Schedule"),
        ),
    ),
)

private data class ExampleAutomation(
    val icon: ImageVector,
    val trigger: String,
    val action: String,
)

private val exampleAutomations = listOf(
    ExampleAutomation(GetoIcons.Apps, "App Launch: YouTube", "Brightness → 80%"),
    ExampleAutomation(GetoIcons.FlashOn, "Charger Connected", "Enable Performance Mode"),
    ExampleAutomation(GetoIcons.BatteryAlert, "Battery < 20%", "Enable Battery Saver"),
    ExampleAutomation(GetoIcons.Headphones, "Headphones Connected", "Volume → 60%"),
    ExampleAutomation(GetoIcons.Wifi, "Wi-Fi Connected (Home)", "Disable Mobile Data"),
    ExampleAutomation(GetoIcons.LockOpen, "Device Unlocked", "Disable DND"),
)

@Composable
private fun TriggerCategorySection(
    category: TriggerCategory,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(category.triggers) { trigger ->
                TriggerChip(icon = trigger.icon, label = trigger.label)
            }
        }
    }
}

@Composable
private fun TriggerChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AutomationEmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = GetoIcons.Automations,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "Tap a trigger above or press \"New Automation\" to build your first IF/THEN rule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ExampleAutomationCard(example: ExampleAutomation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = example.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IF ${example.trigger}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "THEN ${example.action}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = "Example",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
