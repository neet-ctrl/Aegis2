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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

@Composable
internal fun AutomationsRoute(modifier: Modifier = Modifier) {
    AutomationsScreen(modifier = modifier)
}

@Composable
internal fun AutomationsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showBuilder by remember { mutableStateOf(false) }
    var builderTriggerLabel by remember { mutableStateOf<String?>(null) }
    var builderInitialName by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    val automations = remember(refreshKey) {
        AegisAutomationStore.getVisibleAutomations(context)
    }

    if (showBuilder) {
        AutomationBuilderSheet(
            onDismiss = {
                showBuilder = false
                builderTriggerLabel = null
                builderInitialName = ""
                refreshKey++
            },
            initialTriggerLabel = builderTriggerLabel,
            initialName = builderInitialName,
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBuilder = true },
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
                        onTriggerClick = { label ->
                            builderTriggerLabel = label
                            builderInitialName = ""
                            showBuilder = true
                        },
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
                    if (automations.isEmpty()) {
                        Text(
                            text = "No automations created yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (automations.isEmpty()) {
                item { AutomationEmptyHint(onTapCreate = { showBuilder = true }) }
            } else {
                items(automations) { automation ->
                    AutomationCard(
                        automation = automation,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onDelete = {
                            AegisAutomationStore.deleteAutomation(context, automation.id)
                            refreshKey++
                        },
                        onToggle = {
                            AegisAutomationStore.toggleEnabled(context, automation.id)
                            refreshKey++
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            item {
                HiddenVaultSection()
            }

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
                        ExampleAutomationCard(
                            example = example,
                            onUseTemplate = { ex ->
                                builderTriggerLabel = ex.trigger.substringBefore(":").trim()
                                builderInitialName = ""
                                showBuilder = true
                            },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HiddenVaultSection() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var vaultUnlocked by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (vaultUnlocked) GetoIcons.LockOpen else GetoIcons.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Hidden Vault",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (vaultUnlocked) "Vault unlocked — hidden automations visible" else "Automations marked as hidden are stored here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) GetoIcons.ExpandLess else GetoIcons.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = GetoIcons.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Hidden automations are protected by your device screen lock. They run normally but don't appear in the main list. Mark automations as hidden when creating them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    if (!vaultUnlocked) {
                        Button(
                            onClick = { vaultUnlocked = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            ),
                        ) {
                            Icon(
                                imageVector = GetoIcons.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Vault")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = GetoIcons.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                                Text(
                                    text = "No hidden automations",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Create an automation and toggle \"Hidden\" to store it here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
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
    onTriggerClick: (String) -> Unit,
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
                TriggerChip(
                    icon = trigger.icon,
                    label = trigger.label,
                    onClick = { onTriggerClick(trigger.label) },
                )
            }
        }
    }
}

@Composable
private fun TriggerChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
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
private fun AutomationEmptyHint(onTapCreate: () -> Unit) {
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
private fun AutomationCard(
    automation: SavedAutomation,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (automation.isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = GetoIcons.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = automation.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "IF ${automation.triggerLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Switch(
                        checked = automation.isEnabled,
                        onCheckedChange = { onToggle() },
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = GetoIcons.DeleteSweep,
                            contentDescription = "Delete automation",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = GetoIcons.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "THEN ${automation.actionSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (automation.conditionCount > 0 || automation.delaySeconds > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (automation.conditionCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "${automation.conditionCount} condition${if (automation.conditionCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    if (automation.delaySeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = "${automation.delaySeconds}s delay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleAutomationCard(example: ExampleAutomation, onUseTemplate: (ExampleAutomation) -> Unit) {
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

            androidx.compose.material3.TextButton(onClick = { onUseTemplate(example) }) {
                Text(
                    text = "Use",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
