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
package com.android.geto.feature.appsettings.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.component.DialogContainer
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.domain.model.AppSetting
import com.android.geto.domain.model.SecureSetting
import com.android.geto.domain.model.SettingType
import com.android.geto.feature.appsettings.R
import com.android.geto.feature.appsettings.getSettingTypeTitle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

private data class SettingMeta(
    val settingType: SettingType,
    val description: String,
    val valueFormat: String,
    val suggestedLabel: String,
)

private val knownSettingsMeta: Map<String, SettingMeta> = mapOf(
    // ── SYSTEM keys (require WRITE_SETTINGS) ──────────────────────────────
    "screen_brightness" to SettingMeta(
        SettingType.SYSTEM,
        "Screen brightness level (used in manual mode only)",
        "0 (darkest) – 255 (max bright)",
        "Set Brightness",
    ),
    "screen_brightness_mode" to SettingMeta(
        SettingType.SYSTEM,
        "Automatic adaptive brightness toggle",
        "0 = manual, 1 = auto (adaptive)",
        "Auto Brightness",
    ),
    "screen_off_timeout" to SettingMeta(
        SettingType.SYSTEM,
        "How long the screen stays on before turning off from inactivity",
        "Milliseconds — 15000=15s, 30000=30s, 60000=1min, 0=never",
        "Screen Timeout",
    ),
    "font_scale" to SettingMeta(
        SettingType.SYSTEM,
        "Font size multiplier applied across all app UIs system-wide",
        "1.0 = normal, 0.85 = small, 1.15 = large, 1.3 = largest",
        "Font Scale",
    ),
    "accelerometer_rotation" to SettingMeta(
        SettingType.SYSTEM,
        "Whether the screen auto-rotates based on physical device orientation",
        "1 = auto-rotate on, 0 = locked to portrait",
        "Auto Rotate",
    ),
    "haptic_feedback_enabled" to SettingMeta(
        SettingType.SYSTEM,
        "Vibration feedback for touch interactions, keypresses, and UI actions",
        "1 = on, 0 = off",
        "Haptic Feedback",
    ),
    "sound_effects_enabled" to SettingMeta(
        SettingType.SYSTEM,
        "Audible sound effects for tapping UI elements like keyboard keys and buttons",
        "1 = on, 0 = off",
        "Touch Sounds",
    ),
    "pointer_speed" to SettingMeta(
        SettingType.SYSTEM,
        "Cursor/pointer movement speed for external mouse or touchpad input devices",
        "-7 (slowest) to 7 (fastest), 0 = system default",
        "Pointer Speed",
    ),
    // ── GLOBAL keys (require WRITE_SECURE_SETTINGS via ADB) ──────────────
    "zen_mode" to SettingMeta(
        SettingType.GLOBAL,
        "Do Not Disturb mode — controls which notifications and sounds break through",
        "0 = off, 1 = priority only, 2 = total silence, 3 = alarms only",
        "DND Mode",
    ),
    "low_power" to SettingMeta(
        SettingType.GLOBAL,
        "Battery Saver mode — restricts background activity and reduces CPU/GPU performance",
        "1 = Battery Saver on, 0 = off",
        "Battery Saver",
    ),
    "development_settings_enabled" to SettingMeta(
        SettingType.GLOBAL,
        "Whether the Developer Options menu is visible in the Settings app",
        "1 = visible in Settings, 0 = hidden from Settings",
        "Hide Dev Options",
    ),
    "adb_enabled" to SettingMeta(
        SettingType.GLOBAL,
        "USB debugging (ADB over cable) — allows shell access and sideloading from a computer",
        "1 = enabled, 0 = disabled",
        "Hide USB Debug",
    ),
    "adb_wifi_enabled" to SettingMeta(
        SettingType.GLOBAL,
        "Wireless ADB debugging over Wi-Fi without a USB cable (Android 11+)",
        "1 = enabled, 0 = disabled",
        "Hide WiFi ADB",
    ),
    "wifi_scan_always_enabled" to SettingMeta(
        SettingType.GLOBAL,
        "Allows apps and system services to scan for Wi-Fi networks for location even when Wi-Fi is off",
        "1 = on, 0 = off",
        "Wi-Fi Scanning",
    ),
    "adaptive_battery_management_enabled" to SettingMeta(
        SettingType.GLOBAL,
        "System learns which apps you rarely use and restricts their background battery usage",
        "1 = on, 0 = off",
        "Adaptive Battery",
    ),
    "private_dns_mode" to SettingMeta(
        SettingType.GLOBAL,
        "Encrypted DNS configuration — Private DNS / DNS-over-TLS setting",
        "off / opportunistic / hostname (e.g. dns.google or 1dot1dot1dot1.cloudflare-dns.com)",
        "Private DNS",
    ),
    // ── SECURE keys (require WRITE_SECURE_SETTINGS via ADB) ──────────────
    "accessibility_enabled" to SettingMeta(
        SettingType.SECURE,
        "Master global switch for all accessibility services on this device",
        "1 = all services active, 0 = all disabled (affects screen readers, Aegis App Lock, etc.)",
        "Accessibility",
    ),
)

private fun String.toFriendlyLabel(): String =
    split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private data class KeyDropdownItem(
    val key: String,
    val currentValue: String?,
    val meta: SettingMeta?,
)

private fun buildDropdownItems(
    query: String,
    secureSettings: List<SecureSetting>,
): List<KeyDropdownItem> {
    val q = query.lowercase().trim()

    val knownMatches: List<KeyDropdownItem> = if (q.isBlank()) {
        knownSettingsMeta.entries.map { (k, meta) ->
            KeyDropdownItem(k, secureSettings.firstOrNull { it.name == k }?.value, meta)
        }
    } else {
        knownSettingsMeta.entries
            .filter { (k, meta) ->
                k.contains(q) ||
                    meta.description.lowercase().contains(q) ||
                    meta.suggestedLabel.lowercase().contains(q)
            }
            .map { (k, meta) ->
                KeyDropdownItem(k, secureSettings.firstOrNull { it.name == k }?.value, meta)
            }
    }

    val knownKeys = knownMatches.map { it.key }.toSet()
    val systemOnlyItems: List<KeyDropdownItem> = secureSettings
        .filter { ss -> ss.name != null && ss.name !in knownKeys }
        .map { ss -> KeyDropdownItem(ss.name ?: "", ss.value, null) }

    return knownMatches + systemOnlyItems
}

@OptIn(FlowPreview::class)
@Composable
internal fun AppSettingDialog(
    modifier: Modifier = Modifier,
    componentName: String,
    secureSettings: List<SecureSetting>,
    initialLabel: String = "",
    initialKey: String = "",
    initialSettingTypeIndex: Int = 0,
    onAddAppSetting: (AppSetting) -> Unit,
    onDismissRequest: () -> Unit,
    onGetSecureSettingsByName: (
        settingType: SettingType,
        text: String,
    ) -> Unit,
) {
    var selectedRadioOptionIndex by remember { mutableIntStateOf(initialSettingTypeIndex) }
    var label by remember { mutableStateOf(initialLabel) }
    var key by remember { mutableStateOf(initialKey) }
    var valueOnLaunch by remember { mutableStateOf("") }
    var valueOnRevert by remember { mutableStateOf("") }
    var showLabelError by remember { mutableStateOf(false) }
    var showKeyError by remember { mutableStateOf(false) }
    var showKeyNotFoundError by remember { mutableStateOf(false) }
    var showValueOnLaunchError by remember { mutableStateOf(false) }
    var showValueOnRevertError by remember { mutableStateOf(false) }
    var secureSettingsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        snapshotFlow { key }.debounce(500).distinctUntilChanged().onEach {
            onGetSecureSettingsByName(
                SettingType.entries[selectedRadioOptionIndex],
                key,
            )
        }.collect()
    }

    LaunchedEffect(key1 = Unit) {
        snapshotFlow { selectedRadioOptionIndex }.debounce(500)
            .distinctUntilChanged().onEach {
                onGetSecureSettingsByName(
                    SettingType.entries[selectedRadioOptionIndex],
                    key,
                )
            }.collect()
    }

    DialogContainer(
        modifier = modifier.verticalScroll(rememberScrollState()),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Text(
                modifier = Modifier.padding(10.dp),
                text = stringResource(R.string.add_app_setting),
                style = MaterialTheme.typography.titleLarge,
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "How to fill this form",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Choose Type → SYSTEM / SECURE / GLOBAL\n2. Type a Key and pick from the dropdown — auto-fills Value on Revert and suggests a Label\n3. Tap ℹ on any dropdown result to see its description and value format\n4. Set Value on Launch = what changes when you tap ▶\n5. Set Value on Revert = what it restores when you tap ↺",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            AppSettingDialogRadioButtonGroup(
                selected = selectedRadioOptionIndex,
                onSelect = { selectedRadioOptionIndex = it },
            )

            AppSettingDialogTextFields(
                key = key,
                label = label,
                secureSettings = secureSettings,
                secureSettingsExpanded = secureSettingsExpanded,
                showKeyError = showKeyError,
                showKeyNotFoundError = showKeyNotFoundError,
                showLabelError = showLabelError,
                showValueOnLaunchError = showValueOnLaunchError,
                showValueOnRevertError = showValueOnRevertError,
                valueOnLaunch = valueOnLaunch,
                valueOnRevert = valueOnRevert,
                onUpdateKey = { key = it },
                onUpdateLabel = { label = it },
                onUpdateSecureSettingsExpanded = { secureSettingsExpanded = it },
                onUpdateValueOnLaunch = { valueOnLaunch = it },
                onUpdateValueOnRevert = { valueOnRevert = it },
                onUpdateSettingTypeIndex = { selectedRadioOptionIndex = it },
            )

            AppSettingDialogButtons(
                onCancelClick = onDismissRequest,
                onAddClick = {
                    showLabelError = label.isBlank()
                    showKeyError = key.isBlank()
                    showKeyNotFoundError =
                        key.isNotBlank() && !secureSettings.mapNotNull { it.name }.contains(key)
                    showValueOnLaunchError = valueOnLaunch.isBlank()
                    showValueOnRevertError = valueOnRevert.isBlank()

                    if (!showLabelError &&
                        !showKeyNotFoundError &&
                        !showKeyError &&
                        !showValueOnLaunchError &&
                        !showValueOnRevertError
                    ) {
                        onAddAppSetting(
                            AppSetting(
                                enabled = true,
                                settingType = SettingType.entries[selectedRadioOptionIndex],
                                componentName = componentName,
                                label = label,
                                key = key,
                                valueOnLaunch = valueOnLaunch,
                                valueOnRevert = valueOnRevert,
                            ),
                        )
                        onDismissRequest()
                    }
                },
            )
        }
    }
}

@Composable
private fun AppSettingDialogRadioButtonGroup(
    modifier: Modifier = Modifier,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        SettingType.entries.forEachIndexed { index, settingType ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = index == selected,
                        role = Role.RadioButton,
                        enabled = true,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = index == selected,
                    onClick = null,
                )
                Text(
                    text = settingType.getSettingTypeTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun AppSettingDialogTextFields(
    key: String,
    label: String,
    secureSettings: List<SecureSetting>,
    secureSettingsExpanded: Boolean,
    showKeyError: Boolean,
    showKeyNotFoundError: Boolean,
    showLabelError: Boolean,
    showValueOnLaunchError: Boolean,
    showValueOnRevertError: Boolean,
    valueOnLaunch: String,
    valueOnRevert: String,
    onUpdateKey: (String) -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdateSecureSettingsExpanded: (Boolean) -> Unit,
    onUpdateValueOnLaunch: (String) -> Unit,
    onUpdateValueOnRevert: (String) -> Unit,
    onUpdateSettingTypeIndex: (Int) -> Unit,
) {
    val labelIsBlank = stringResource(id = R.string.setting_label_is_blank)
    val valueOnLaunchIsBlank = stringResource(id = R.string.setting_value_on_launch_is_blank)
    val valueOnRevertIsBlank = stringResource(id = R.string.setting_value_on_revert_is_blank)
    val meta = knownSettingsMeta[key]

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        value = label,
        onValueChange = onUpdateLabel,
        label = { Text(text = stringResource(R.string.setting_label)) },
        placeholder = {
            Text(
                text = "e.g. Low brightness",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
            )
        },
        isError = showLabelError,
        supportingText = {
            if (showLabelError) {
                Text(text = labelIsBlank)
            } else {
                Text(
                    text = "Friendly name for this rule — auto-filled when you pick a key from the dropdown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    AppSettingDialogTextFieldWithDropdownMenu(
        key = key,
        label = label,
        secureSettings = secureSettings,
        secureSettingsExpanded = secureSettingsExpanded,
        showKeyError = showKeyError,
        showKeyNotFoundError = showKeyNotFoundError,
        onUpdateKey = onUpdateKey,
        onUpdateLabel = onUpdateLabel,
        onUpdateSecureSettingsExpanded = onUpdateSecureSettingsExpanded,
        onUpdateValueOnRevert = onUpdateValueOnRevert,
        onUpdateSettingTypeIndex = onUpdateSettingTypeIndex,
    )

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        value = valueOnLaunch,
        onValueChange = onUpdateValueOnLaunch,
        label = { Text(text = stringResource(R.string.setting_value_on_launch)) },
        placeholder = {
            Text(
                text = if (meta != null) "e.g. ${meta.valueFormat.substringBefore(" ").substringBefore("–").trim()}" else "e.g. 50",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
            )
        },
        isError = showValueOnLaunchError,
        supportingText = {
            if (showValueOnLaunchError) {
                Text(text = valueOnLaunchIsBlank)
            } else if (meta != null) {
                Text(
                    text = "Format: ${meta.valueFormat}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Value applied when you tap ▶ (launch)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        value = valueOnRevert,
        onValueChange = onUpdateValueOnRevert,
        label = { Text(text = stringResource(R.string.setting_value_on_revert)) },
        placeholder = {
            Text(
                text = "e.g. 128",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
            )
        },
        isError = showValueOnRevertError,
        supportingText = {
            if (showValueOnRevertError) {
                Text(text = valueOnRevertIsBlank)
            } else {
                Text(
                    text = "Value restored when you tap ↺ — auto-filled from current system value when you pick a key",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingDialogTextFieldWithDropdownMenu(
    modifier: Modifier = Modifier,
    key: String,
    label: String,
    secureSettings: List<SecureSetting>,
    secureSettingsExpanded: Boolean,
    showKeyError: Boolean,
    showKeyNotFoundError: Boolean,
    onUpdateKey: (String) -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdateSecureSettingsExpanded: (Boolean) -> Unit,
    onUpdateValueOnRevert: (String) -> Unit,
    onUpdateSettingTypeIndex: (Int) -> Unit,
) {
    var infoDialogKey by remember { mutableStateOf<String?>(null) }
    val infoMeta = infoDialogKey?.let { knownSettingsMeta[it] }

    if (infoDialogKey != null && infoMeta != null) {
        AlertDialog(
            onDismissRequest = { infoDialogKey = null },
            icon = {
                Icon(
                    imageVector = GetoIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = infoDialogKey ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = infoMeta.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HorizontalDivider()
                    Text(
                        text = "Type to select",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = infoMeta.settingType.getSettingTypeTitle(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    HorizontalDivider()
                    Text(
                        text = "Value format",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = infoMeta.valueFormat,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                    HorizontalDivider()
                    Text(
                        text = "Suggested label",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "\"${infoMeta.suggestedLabel}\"",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogKey = null }) {
                    Text("Got it")
                }
            },
        )
    }

    ExposedDropdownMenuBox(
        modifier = modifier.fillMaxWidth(),
        expanded = secureSettingsExpanded,
        onExpandedChange = onUpdateSecureSettingsExpanded,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(
                    type = MenuAnchorType.SecondaryEditable,
                    enabled = true,
                )
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            value = key,
            onValueChange = onUpdateKey,
            label = { Text(text = stringResource(R.string.setting_key)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secureSettingsExpanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            isError = showKeyError || showKeyNotFoundError,
            supportingText = {
                if (showKeyError) {
                    Text(text = stringResource(id = R.string.setting_key_is_blank))
                } else if (showKeyNotFoundError) {
                    Text(text = stringResource(id = R.string.setting_key_not_found))
                } else {
                    Text(
                        text = "Type to search — tap ℹ on any result to see its description and value format",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        val dropdownItems = remember(key, secureSettings) {
            buildDropdownItems(query = key, secureSettings = secureSettings)
        }

        if (dropdownItems.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = secureSettingsExpanded,
                onDismissRequest = { onUpdateSecureSettingsExpanded(false) },
            ) {
                dropdownItems.forEach { item ->
                    val itemKey = item.key
                    val meta = item.meta
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemKey,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (meta != null) {
                                    Text(
                                        text = meta.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = item.currentValue?.let { "Current: $it" }
                                        ?: meta?.valueFormat?.let { "Format: $it" }
                                        ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        },
                        onClick = {
                            onUpdateKey(itemKey)
                            onUpdateValueOnRevert(item.currentValue ?: "")
                            if (label.isBlank()) {
                                val suggested = meta?.suggestedLabel ?: itemKey.toFriendlyLabel()
                                onUpdateLabel(suggested)
                            }
                            meta?.settingType?.let { type ->
                                onUpdateSettingTypeIndex(SettingType.entries.indexOf(type))
                            }
                            onUpdateSecureSettingsExpanded(false)
                        },
                        trailingIcon = if (meta != null) {
                            {
                                IconButton(
                                    onClick = { infoDialogKey = itemKey },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = GetoIcons.Info,
                                        contentDescription = "Info about $itemKey",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        } else null,
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSettingDialogButtons(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onCancelClick,
            modifier = Modifier.padding(5.dp),
        ) {
            Text(text = stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onAddClick,
            modifier = Modifier.padding(5.dp),
        ) {
            Text(text = stringResource(R.string.add))
        }
    }
}
