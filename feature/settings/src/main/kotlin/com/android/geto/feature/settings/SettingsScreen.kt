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
package com.android.geto.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.designsystem.theme.supportsDynamicTheming
import com.android.geto.domain.model.Theme
import com.android.geto.domain.model.UserData
import com.android.geto.feature.settings.dialog.ThemeDialog

@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()

    SettingsScreen(
        modifier = modifier,
        settingsUiState = settingsUiState,
        onUpdateTheme = viewModel::updateTheme,
        onUpdateDynamicTheme = viewModel::updateDynamicTheme,
    )
}

@VisibleForTesting
@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsUiState: SettingsUiState,
    onUpdateTheme: (Theme) -> Unit,
    onUpdateDynamicTheme: (Boolean) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        when (settingsUiState) {
            SettingsUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is SettingsUiState.Success -> {
                SuccessState(
                    userData = settingsUiState.userData,
                    onUpdateDynamicTheme = onUpdateDynamicTheme,
                    onUpdateTheme = onUpdateTheme,
                )
            }
        }
    }
}

@Composable
private fun SuccessState(
    modifier: Modifier = Modifier,
    userData: UserData,
    onUpdateDynamicTheme: (Boolean) -> Unit,
    onUpdateTheme: (Theme) -> Unit,
) {
    val context = LocalContext.current

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }

    var selectedTheme by remember {
        mutableIntStateOf(Theme.entries.indexOf(userData.theme))
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(
                context,
                "Backup location selected — encrypted backup coming in next update",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(
                context,
                "Restore selected — full restore coming in next update",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsSection(title = "Appearance") {
            if (supportsDynamicTheming()) {
                SettingsToggleRow(
                    icon = GetoIcons.Palette,
                    title = "Material You",
                    subtitle = "Use wallpaper-based dynamic colors (Android 12+)",
                    checked = userData.dynamicTheme,
                    onCheckedChange = onUpdateDynamicTheme,
                )
            }

            SettingsNavigationRow(
                icon = GetoIcons.Settings,
                title = "Theme",
                subtitle = userData.theme.getTitle(),
                onClick = { showThemeDialog = true },
            )
        }

        SettingsSection(title = "Backup & Restore") {
            SettingsNavigationRow(
                icon = GetoIcons.SaveAlt,
                title = "Backup Aegis",
                subtitle = "Save all rules, automations & settings to an encrypted .aegis file",
                onClick = {
                    backupLauncher.launch(
                        "aegis_backup_${System.currentTimeMillis()}.aegis",
                    )
                },
            )

            SettingsNavigationRow(
                icon = GetoIcons.Restore,
                title = "Restore Aegis",
                subtitle = "Restore from a previously saved .aegis backup file",
                onClick = {
                    restoreLauncher.launch(arrayOf("*/*"))
                },
            )
        }

        SettingsSection(title = "Security & Privacy") {
            SettingsInfoRow(
                icon = GetoIcons.Fingerprint,
                title = "App Lock",
                subtitle = "Per-app biometric/PIN/pattern lock — set in each app's control page",
            )
            SettingsInfoRow(
                icon = GetoIcons.Lock,
                title = "Encrypted Storage",
                subtitle = "All rules and settings stored securely on-device only",
            )
            SettingsInfoRow(
                icon = GetoIcons.Block,
                title = "Zero Cloud",
                subtitle = "No cloud sync, no analytics, no telemetry — ever",
            )
        }

        AshellSetupSection()

        SettingsSection(title = "AShell U Permissions") {
            PermissionStatusRow(
                title = "WRITE_SECURE_SETTINGS",
                subtitle = "Core per-app settings control",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "DUMP",
                subtitle = "System state and app info reading",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "WRITE_SETTINGS",
                subtitle = "System-level settings modification",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "READ_LOGS",
                subtitle = "Activity Center and event logging",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "CHANGE_CONFIGURATION",
                subtitle = "DPI scaling and display configuration",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "BATTERY_STATS",
                subtitle = "Battery monitoring and triggers",
                isGranted = false,
            )
            PermissionStatusRow(
                title = "GET_USAGE_STATS",
                subtitle = "App launch detection and usage triggers",
                isGranted = false,
            )
        }

        SettingsSection(title = "About Aegis") {
            SettingsInfoRow(
                icon = GetoIcons.Shield,
                title = "Aegis",
                subtitle = "Advanced Android App Environment & Automation Controller",
            )
            SettingsInfoRow(
                icon = GetoIcons.Security,
                title = "Privacy-First",
                subtitle = "No cloud. No telemetry. No analytics. Fully offline.",
            )
            SettingsInfoRow(
                icon = GetoIcons.Android,
                title = "Platform",
                subtitle = "Built for Android 15, 16 and beyond · Powered by Shizuku",
            )
            SettingsInfoRow(
                icon = GetoIcons.Info,
                title = "Package",
                subtitle = "com.android.geto",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showThemeDialog) {
        ThemeDialog(
            onDismissRequest = { showThemeDialog = false },
            selected = selectedTheme,
            onSelect = { selectedTheme = it },
            onChangeClick = {
                onUpdateTheme(Theme.entries[selectedTheme])
                showThemeDialog = false
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = GetoIcons.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isGranted) GetoIcons.CheckCircle else GetoIcons.Warning,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = if (isGranted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ) {
            Text(
                text = if (isGranted) "Granted" else "Not Granted",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isGranted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private data class AshellCommand(
    val step: String,
    val label: String,
    val command: String,
    val required: Boolean = false,
)

private val ashellCommands = listOf(
    AshellCommand("Step 1 — REQUIRED", "Write Secure Settings", "adb shell pm grant com.android.geto android.permission.WRITE_SECURE_SETTINGS", required = true),
    AshellCommand("Step 1 — REQUIRED", "Dump", "adb shell pm grant com.android.geto android.permission.DUMP", required = true),
    AshellCommand("Step 1 — REQUIRED", "Write Settings", "adb shell pm grant com.android.geto android.permission.WRITE_SETTINGS", required = true),
    AshellCommand("Step 2 — Recommended", "Read Logs", "adb shell pm grant com.android.geto android.permission.READ_LOGS"),
    AshellCommand("Step 2 — Recommended", "Change Configuration", "adb shell pm grant com.android.geto android.permission.CHANGE_CONFIGURATION"),
    AshellCommand("Step 2 — Recommended", "Get Usage Stats", "adb shell appops set com.android.geto GET_USAGE_STATS allow"),
    AshellCommand("Step 2 — Recommended", "Package Usage Stats", "adb shell appops set com.android.geto PACKAGE_USAGE_STATS allow"),
    AshellCommand("Step 2 — Recommended", "Battery Stats", "adb shell pm grant com.android.geto android.permission.BATTERY_STATS"),
    AshellCommand("Step 3 — Optional", "Manage App Ops", "adb shell pm grant com.android.geto android.permission.MANAGE_APP_OPS_MODES"),
    AshellCommand("Step 3 — Optional", "Notification Policy", "adb shell pm grant com.android.geto android.permission.ACCESS_NOTIFICATION_POLICY"),
    AshellCommand("Step 3 — Optional", "Change Network State", "adb shell pm grant com.android.geto android.permission.CHANGE_NETWORK_STATE"),
    AshellCommand("Step 3 — Optional", "Status Bar", "adb shell pm grant com.android.geto android.permission.STATUS_BAR"),
    AshellCommand("Step 3 — Optional", "Read Device Config", "adb shell pm grant com.android.geto android.permission.READ_DEVICE_CONFIG"),
)

@Composable
private fun AshellSetupSection() {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "ASHELL U SETUP GUIDE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = GetoIcons.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Run these commands in AShell U",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${ashellCommands.size} commands · 3 required · tap to ${if (expanded) "collapse" else "expand"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) GetoIcons.ExpandLess else GetoIcons.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        var lastStep = ""
                        ashellCommands.forEach { cmd ->
                            if (cmd.step != lastStep) {
                                lastStep = cmd.step
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (cmd.required) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = cmd.step,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (cmd.required) MaterialTheme.colorScheme.onErrorContainer
                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = cmd.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cmd.command,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AShell command", cmd.command))
                                        Toast.makeText(context, "Copied: ${cmd.label}", Toast.LENGTH_SHORT).show()
                                    },
                                ) {
                                    Icon(
                                        imageVector = GetoIcons.Copy,
                                        contentDescription = "Copy command",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Theme.getTitle() = when (this) {
    Theme.FOLLOW_SYSTEM -> stringResource(R.string.follow_system)
    Theme.LIGHT -> stringResource(R.string.light)
    Theme.DARK -> stringResource(R.string.dark)
    Theme.AMOLED -> stringResource(R.string.amoled)
}
