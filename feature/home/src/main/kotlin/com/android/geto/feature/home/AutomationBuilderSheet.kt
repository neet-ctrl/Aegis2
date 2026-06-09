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

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

private data class TriggerOption(
    val icon: ImageVector,
    val label: String,
    val category: String,
)

private val allTriggers = listOf(
    TriggerOption(GetoIcons.Apps, "App Launch", "App Events"),
    TriggerOption(GetoIcons.Close, "App Close", "App Events"),
    TriggerOption(GetoIcons.Android, "App Install", "App Events"),
    TriggerOption(GetoIcons.DeleteSweep, "App Uninstall", "App Events"),
    TriggerOption(GetoIcons.Notifications, "Screen On", "Screen & Device"),
    TriggerOption(GetoIcons.Block, "Screen Off", "Screen & Device"),
    TriggerOption(GetoIcons.LockOpen, "Device Unlock", "Screen & Device"),
    TriggerOption(GetoIcons.Lock, "Device Lock", "Screen & Device"),
    TriggerOption(GetoIcons.FlashOn, "Charger Connected", "Power & Battery"),
    TriggerOption(GetoIcons.FlashOn, "Charger Disconnected", "Power & Battery"),
    TriggerOption(GetoIcons.BatteryAlert, "Battery %", "Power & Battery"),
    TriggerOption(GetoIcons.Wifi, "Wi-Fi Connected", "Connectivity"),
    TriggerOption(GetoIcons.WifiOff, "Wi-Fi Disconnected", "Connectivity"),
    TriggerOption(GetoIcons.Bluetooth, "Bluetooth Connected", "Connectivity"),
    TriggerOption(GetoIcons.Bluetooth, "Bluetooth Disconnected", "Connectivity"),
    TriggerOption(GetoIcons.Nfc, "NFC Tag Detected", "Connectivity"),
    TriggerOption(GetoIcons.Headphones, "Headphones Connected", "Audio"),
    TriggerOption(GetoIcons.HeadphonesOff, "Headphones Disconnected", "Audio"),
    TriggerOption(GetoIcons.VolumeUp, "Volume Changed", "Audio"),
    TriggerOption(GetoIcons.Schedule, "Time Schedule", "Time & Schedule"),
    TriggerOption(GetoIcons.Schedule, "Day Schedule", "Time & Schedule"),
)

private data class ActionOption(
    val icon: ImageVector,
    val label: String,
    val settingKey: String,
    val settingType: String,
    val valuePlaceholder: String,
)

private val allActions = listOf(
    ActionOption(GetoIcons.BrightnessHigh, "Set Brightness", "screen_brightness", "SYSTEM", "0–255"),
    ActionOption(GetoIcons.VolumeUp, "Set Media Volume", "volume_music", "SYSTEM", "0–15"),
    ActionOption(GetoIcons.Notifications, "Set DND Mode", "zen_mode", "GLOBAL", "0=off, 1=priority, 2=silence"),
    ActionOption(GetoIcons.BatteryAlert, "Battery Saver", "low_power", "GLOBAL", "1=on, 0=off"),
    ActionOption(GetoIcons.Speed, "Screen Timeout", "screen_off_timeout", "SYSTEM", "ms, e.g. 30000"),
    ActionOption(GetoIcons.Tune, "Font Scale", "font_scale", "SYSTEM", "1.0, 1.15, 1.3"),
    ActionOption(GetoIcons.Block, "Airplane Mode", "airplane_mode_on", "GLOBAL", "1=on, 0=off"),
    ActionOption(GetoIcons.ScreenRotation, "Auto Rotate", "accelerometer_rotation", "SYSTEM", "1=auto, 0=off"),
    ActionOption(GetoIcons.Tune, "Haptic Feedback", "haptic_feedback_enabled", "SYSTEM", "1=on, 0=off"),
    ActionOption(GetoIcons.Speed, "Pointer Speed", "pointer_speed", "SYSTEM", "-7 to 7"),
    ActionOption(GetoIcons.Memory, "Stay Awake (Charging)", "stay_on_while_plugged_in", "GLOBAL", "0=off, 1=AC, 2=USB, 3=AC+USB"),
    ActionOption(GetoIcons.Dns, "Private DNS Mode", "private_dns_mode", "GLOBAL", "off / opportunistic / hostname"),
    ActionOption(GetoIcons.BatteryFull, "Adaptive Battery", "adaptive_battery_management_enabled", "GLOBAL", "1=on, 0=off"),
)

private data class AutomationCondition(
    val field: String,
    val operator: String,
    val value: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AutomationBuilderSheet(
    onDismiss: () -> Unit,
    initialTriggerLabel: String? = null,
    initialName: String = "",
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var step by remember { mutableIntStateOf(if (initialTriggerLabel != null) 1 else 0) }
    var selectedTrigger by remember {
        mutableStateOf<TriggerOption?>(
            initialTriggerLabel?.let { label -> allTriggers.firstOrNull { it.label == label } },
        )
    }
    val conditions = remember { mutableStateListOf<AutomationCondition>() }
    var conditionLogic by remember { mutableStateOf("AND") }
    val selectedActions = remember { mutableStateListOf<Pair<ActionOption, String>>() }
    var delaySeconds by remember { mutableStateOf("0") }
    var automationName by remember { mutableStateOf(initialName) }
    var isHidden by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "New Automation",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Step ${step + 1} of 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(GetoIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            StepIndicator(currentStep = step, totalSteps = 5)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            when (step) {
                0 -> TriggerStep(
                    selected = selectedTrigger,
                    onSelect = { selectedTrigger = it },
                )
                1 -> ConditionsStep(
                    conditions = conditions,
                    logic = conditionLogic,
                    onLogicChange = { conditionLogic = it },
                    onAddCondition = {
                        conditions.add(AutomationCondition("App", "is", ""))
                    },
                    onRemoveCondition = { conditions.removeAt(it) },
                    onUpdateCondition = { idx, cond -> conditions[idx] = cond },
                )
                2 -> ActionsStep(
                    selectedActions = selectedActions,
                    onToggleAction = { action ->
                        val existing = selectedActions.indexOfFirst { it.first.label == action.label }
                        if (existing >= 0) {
                            selectedActions.removeAt(existing)
                        } else {
                            selectedActions.add(Pair(action, ""))
                        }
                    },
                    onUpdateValue = { idx, value ->
                        val (action, _) = selectedActions[idx]
                        selectedActions[idx] = Pair(action, value)
                    },
                )
                3 -> DelayStep(
                    delaySeconds = delaySeconds,
                    onDelayChange = { delaySeconds = it },
                )
                4 -> NameAndSaveStep(
                    name = automationName,
                    onNameChange = { automationName = it },
                    isHidden = isHidden,
                    onHiddenChange = { isHidden = it },
                    trigger = selectedTrigger,
                    conditionCount = conditions.size,
                    actionCount = selectedActions.size,
                    delay = delaySeconds,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            if (automationName.isBlank()) {
                                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                            } else {
                                val actionSummary = selectedActions
                                    .joinToString(", ") { it.first.label }
                                    .ifEmpty { "No actions" }
                                val automation = SavedAutomation(
                                    id = System.currentTimeMillis(),
                                    name = automationName.trim(),
                                    triggerLabel = selectedTrigger?.label ?: "Unknown",
                                    conditionCount = conditions.size,
                                    actionSummary = actionSummary,
                                    delaySeconds = delaySeconds.toIntOrNull() ?: 0,
                                    isHidden = isHidden,
                                    isEnabled = true,
                                    createdAt = System.currentTimeMillis(),
                                )
                                AegisAutomationStore.addAutomation(context, automation)
                                AegisActionStore.setActions(
                                    context,
                                    automation.id,
                                    selectedActions.map { (action, value) ->
                                        StoredAction(
                                            label = action.label,
                                            settingKey = action.settingKey,
                                            settingType = action.settingType,
                                            value = value,
                                        )
                                    },
                                )
                                AegisActivityLog.addEntry(
                                    context,
                                    "Automation Created",
                                    "\"$automationName\" saved — trigger: ${selectedTrigger?.label ?: "none"}",
                                    "system",
                                )
                                Toast.makeText(context, "Automation \"$automationName\" saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(if (step == 0) 1f else 1f),
                    enabled = when (step) {
                        0 -> selectedTrigger != null
                        2 -> selectedActions.isNotEmpty()
                        else -> true
                    },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (step < 4) "Next" else "Save Automation")
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(totalSteps) { idx ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (idx <= currentStep) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerStep(
    selected: TriggerOption?,
    onSelect: (TriggerOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(
            icon = GetoIcons.FlashOn,
            title = "Choose Trigger",
            subtitle = "What event should start this automation?",
        )

        val byCategory = allTriggers.groupBy { it.category }
        byCategory.forEach { (category, triggers) ->
            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                triggers.forEach { trigger ->
                    val isSelected = selected?.label == trigger.label
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(trigger) },
                        label = { Text(trigger.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = trigger.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionsStep(
    conditions: List<AutomationCondition>,
    logic: String,
    onLogicChange: (String) -> Unit,
    onAddCondition: () -> Unit,
    onRemoveCondition: (Int) -> Unit,
    onUpdateCondition: (Int, AutomationCondition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(
            icon = GetoIcons.Tune,
            title = "Add Conditions (optional)",
            subtitle = "Refine when the automation fires with extra IF conditions",
        )

        if (conditions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("AND", "OR").forEach { logic2 ->
                    FilterChip(
                        selected = logic == logic2,
                        onClick = { onLogicChange(logic2) },
                        label = { Text(logic2) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }

            conditions.forEachIndexed { idx, cond ->
                ConditionRow(
                    condition = cond,
                    index = idx,
                    logic = if (idx == 0) "IF" else logic,
                    onRemove = { onRemoveCondition(idx) },
                    onUpdate = { onUpdateCondition(idx, it) },
                )
            }
        }

        TextButton(
            onClick = onAddCondition,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Icon(GetoIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Condition")
        }

        if (conditions.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Skip this step — the automation will fire whenever the trigger occurs. Or add conditions to make it more specific (e.g., only when battery < 20%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ConditionRow(
    condition: AutomationCondition,
    index: Int,
    logic: String,
    onRemove: () -> Unit,
    onUpdate: (AutomationCondition) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = logic,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            OutlinedTextField(
                value = condition.field,
                onValueChange = { onUpdate(condition.copy(field = it)) },
                label = { Text("Field") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )
            OutlinedTextField(
                value = condition.value,
                onValueChange = { onUpdate(condition.copy(value = it)) },
                label = { Text("Value") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = GetoIcons.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsStep(
    selectedActions: List<Pair<ActionOption, String>>,
    onToggleAction: (ActionOption) -> Unit,
    onUpdateValue: (Int, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(
            icon = GetoIcons.PlayArrow,
            title = "Choose Actions (THEN)",
            subtitle = "What should happen when the trigger fires? Select one or more.",
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            allActions.forEach { action ->
                val isSelected = selectedActions.any { it.first.label == action.label }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleAction(action) },
                    label = { Text(action.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }
        }

        selectedActions.forEachIndexed { idx, (action, value) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(action.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(
                            text = "THEN ${action.label}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onUpdateValue(idx, it) },
                        label = { Text("Value (${action.valuePlaceholder})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    )
                    Text(
                        text = "${action.settingType} › ${action.settingKey}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DelayStep(
    delaySeconds: String,
    onDelayChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(
            icon = GetoIcons.Timer,
            title = "Set Delay (optional)",
            subtitle = "Wait this many seconds after the trigger before running actions",
        )

        OutlinedTextField(
            value = delaySeconds,
            onValueChange = { onDelayChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Delay in seconds") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(GetoIcons.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            supportingText = {
                Text("0 = immediate. Example: 5 means actions run 5 seconds after trigger.")
            },
        )
    }
}

@Composable
private fun NameAndSaveStep(
    name: String,
    onNameChange: (String) -> Unit,
    isHidden: Boolean,
    onHiddenChange: (Boolean) -> Unit,
    trigger: TriggerOption?,
    conditionCount: Int,
    actionCount: Int,
    delay: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(
            icon = GetoIcons.CheckCircle,
            title = "Name & Save",
            subtitle = "Give your automation a name and review the summary",
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Automation name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("e.g. \"YouTube Brightness Boost\"") },
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SummaryRow("Trigger", trigger?.label ?: "None selected")
                SummaryRow("Conditions", if (conditionCount == 0) "None (always fire)" else "$conditionCount condition(s)")
                SummaryRow("Actions", "$actionCount action(s) configured")
                SummaryRow("Delay", if (delay == "0" || delay.isBlank()) "Immediate" else "$delay second(s)")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Hidden Automation",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Hide this from the main list (Hidden Vault)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isHidden,
                onCheckedChange = onHiddenChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
