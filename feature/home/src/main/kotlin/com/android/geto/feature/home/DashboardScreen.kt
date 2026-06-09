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

import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.geto.designsystem.icon.GetoIcons

@Composable
internal fun DashboardRoute(
    modifier: Modifier = Modifier,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToAutomations: () -> Unit = {},
    onNavigateToQuickToggles: () -> Unit = {},
) {
    DashboardScreen(
        modifier = modifier,
        onNavigateToActivity = onNavigateToActivity,
        onNavigateToAutomations = onNavigateToAutomations,
        onNavigateToQuickToggles = onNavigateToQuickToggles,
    )
}

@Composable
internal fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToAutomations: () -> Unit = {},
    onNavigateToQuickToggles: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HeroBanner() }
        item { QuickTogglesEntryCard(onNavigateToQuickToggles = onNavigateToQuickToggles) }
        item { StatusGrid() }
        item { AshellCommandsPanel() }
        item { StatisticsSection() }
        item { RecentActivitySection(onNavigateToActivity = onNavigateToActivity) }
        item { QuickActionsSection(onNavigateToAutomations = onNavigateToAutomations) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickTogglesEntryCard(onNavigateToQuickToggles: () -> Unit) {
    val context = LocalContext.current
    val hasWriteSecure = ContextCompat.checkSelfPermission(
        context, "android.permission.WRITE_SECURE_SETTINGS",
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = onNavigateToQuickToggles,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GetoIcons.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quick Toggles",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "60 system toggles • Developer tools, Network, Display & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!hasWriteSecure) {
                    Text(
                        text = "ADB setup recommended for full access",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    )
                }
            }
            Icon(
                imageVector = GetoIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HeroBanner() {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        ),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(24.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = GetoIcons.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "AEGIS",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Advanced Android App Environment & Automation Controller",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(label = "Offline First", isActive = true)
                    StatusChip(label = "No Telemetry", isActive = true)
                    StatusChip(label = "No Root Required", isActive = true)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun StatusGrid() {
    val context = LocalContext.current

    val writeSecureGranted = ContextCompat.checkSelfPermission(
        context, "android.permission.WRITE_SECURE_SETTINGS",
    ) == PackageManager.PERMISSION_GRANTED

    val dumpGranted = ContextCompat.checkSelfPermission(
        context, "android.permission.DUMP",
    ) == PackageManager.PERMISSION_GRANTED

    val permissionsReady = writeSecureGranted && dumpGranted
    val permissionsValue = if (permissionsReady) "Granted" else "Setup Needed"

    val activeRules = AegisAutomationStore.getEnabledCount(context)
    val totalAutomations = AegisAutomationStore.getTotalCount(context)
    val lockedCount = getLockedCountFromPrefs(context)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "System Status",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "ADB Perms",
                value = if (writeSecureGranted) "Granted" else "Not Granted",
                icon = GetoIcons.Shield,
                isGood = writeSecureGranted,
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Permissions",
                value = permissionsValue,
                icon = GetoIcons.Lock,
                isGood = permissionsReady,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Active Rules",
                value = if (activeRules == 0) "None" else "$activeRules Active",
                icon = GetoIcons.Tune,
                isGood = true,
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Battery Impact",
                value = "Minimal",
                icon = GetoIcons.Speed,
                isGood = true,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Automations",
                value = if (totalAutomations == 0) "None" else "$totalAutomations Total",
                icon = GetoIcons.Automations,
                isGood = true,
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "App Lock",
                value = if (lockedCount == 0) "None Locked" else "$lockedCount Locked",
                icon = GetoIcons.Security,
                isGood = true,
            )
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    isGood: Boolean,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGood) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGood) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isGood) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatisticsSection() {
    val context = LocalContext.current
    val triggersToday = AegisActivityLog.getTodayCount(context, "trigger")
    val settingsToday = AegisActivityLog.getTodayCount(context, "settings")
    val successRate = AegisActivityLog.getSuccessRate(context)
    val successPct = (successRate * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatRow(
                    icon = GetoIcons.Automations,
                    label = "Automation Triggers Today",
                    value = if (triggersToday == 0) "None" else "$triggersToday",
                    progress = (triggersToday / 100f).coerceIn(0f, 1f),
                    progressLabel = "$triggersToday / 100",
                )
                StatRow(
                    icon = GetoIcons.Analytics,
                    label = "Automation Success Rate",
                    value = if (successRate == 0f) "—" else "$successPct%",
                    progress = successRate,
                    progressLabel = if (successRate == 0f) "No data yet" else "$successPct% success",
                )
                StatRow(
                    icon = GetoIcons.BatteryFull,
                    label = "Estimated Battery Impact",
                    value = "< 0.1%",
                    progress = 0.01f,
                    progressLabel = "Minimal",
                )
                StatRow(
                    icon = GetoIcons.Tune,
                    label = "Rules Applied Today",
                    value = if (settingsToday == 0) "None" else "$settingsToday",
                    progress = (settingsToday / 50f).coerceIn(0f, 1f),
                    progressLabel = if (settingsToday == 0) "No rules applied yet" else "$settingsToday applied",
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float,
    progressLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = progressLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class RecentActivityEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val time: String,
    val tag: String,
)

private fun logEntryIcon(tag: String): ImageVector = when (tag) {
    "trigger" -> GetoIcons.FlashOn
    "settings" -> GetoIcons.Tune
    "error" -> GetoIcons.Error
    "shizuku" -> GetoIcons.Shield
    "system" -> GetoIcons.Shield
    else -> GetoIcons.Info
}

@Composable
private fun RecentActivitySection(onNavigateToActivity: () -> Unit) {
    val context = LocalContext.current

    AegisActivityLog.seedIfEmpty(context)

    val liveEntries = AegisActivityLog.getEntries(context)
        .take(3)
        .map { entry ->
            RecentActivityEntry(
                icon = logEntryIcon(entry.tag),
                title = entry.title,
                subtitle = entry.subtitle,
                time = AegisActivityLog.formatRelativeTime(entry.timestampMs),
                tag = entry.tag,
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onNavigateToActivity) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (liveEntries.isEmpty()) {
            Text(
                text = "No activity yet. Run AShell U commands to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            liveEntries.forEach { entry ->
                RecentActivityCard(entry = entry)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(entry: RecentActivityEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private enum class PermCheckType { STANDARD, WRITE_SETTINGS, APPOPS_USAGE, DRAW_OVERLAYS, NONE }

private data class AshellCommand(
    val step: Int,
    val title: String,
    val description: String,
    val command: String,
    val isRequired: Boolean,
    val permissionName: String? = null,
    val checkType: PermCheckType = PermCheckType.STANDARD,
)

private fun isPermissionGranted(context: Context, command: AshellCommand): Boolean {
    return when (command.checkType) {
        PermCheckType.NONE -> false
        PermCheckType.WRITE_SETTINGS -> Settings.System.canWrite(context)
        PermCheckType.DRAW_OVERLAYS -> Settings.canDrawOverlays(context)
        PermCheckType.APPOPS_USAGE -> {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }
        PermCheckType.STANDARD -> command.permissionName?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }
}

private val ashellCommands = listOf(
    AshellCommand(
        step = 1,
        title = "Write Secure Settings",
        description = "REQUIRED — Core permission for applying per-app system settings.",
        command = "pm grant com.android.geto android.permission.WRITE_SECURE_SETTINGS",
        isRequired = true,
        permissionName = "android.permission.WRITE_SECURE_SETTINGS",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 2,
        title = "Dump Permission",
        description = "REQUIRED — Allows Aegis to read system state, app info and running services.",
        command = "pm grant com.android.geto android.permission.DUMP",
        isRequired = true,
        permissionName = "android.permission.DUMP",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 3,
        title = "Write Settings",
        description = "REQUIRED — Allows modifying system-level settings (brightness, volume, DPI, etc.).",
        command = "pm grant com.android.geto android.permission.WRITE_SETTINGS",
        isRequired = true,
        checkType = PermCheckType.WRITE_SETTINGS,
    ),
    AshellCommand(
        step = 4,
        title = "Read Logs",
        description = "Enables activity monitoring, detection logging, and the Activity Center.",
        command = "pm grant com.android.geto android.permission.READ_LOGS",
        isRequired = false,
        permissionName = "android.permission.READ_LOGS",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 5,
        title = "Change Configuration",
        description = "Required for DPI scaling, locale override, and display configuration changes.",
        command = "pm grant com.android.geto android.permission.CHANGE_CONFIGURATION",
        isRequired = false,
        permissionName = "android.permission.CHANGE_CONFIGURATION",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 6,
        title = "Usage Stats (appops)",
        description = "Enables app usage tracking and launch-based automation triggers.",
        command = "appops set com.android.geto GET_USAGE_STATS allow",
        isRequired = false,
        checkType = PermCheckType.APPOPS_USAGE,
    ),
    AshellCommand(
        step = 7,
        title = "Package Usage Stats",
        description = "Supplements usage stats permission for per-app automation triggers.",
        command = "pm grant com.android.geto android.permission.PACKAGE_USAGE_STATS",
        isRequired = false,
        permissionName = "android.permission.PACKAGE_USAGE_STATS",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 8,
        title = "Battery Stats",
        description = "Enables battery impact monitoring and battery-based automation triggers.",
        command = "pm grant com.android.geto android.permission.BATTERY_STATS",
        isRequired = false,
        permissionName = "android.permission.BATTERY_STATS",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 9,
        title = "Notification Policy",
        description = "Required for Do Not Disturb and per-app notification volume control.",
        command = "pm grant com.android.geto android.permission.ACCESS_NOTIFICATION_POLICY",
        isRequired = false,
        permissionName = "android.permission.ACCESS_NOTIFICATION_POLICY",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 10,
        title = "Network State",
        description = "Enables Wi-Fi preference controls and network-based automation triggers.",
        command = "pm grant com.android.geto android.permission.CHANGE_NETWORK_STATE",
        isRequired = false,
        permissionName = "android.permission.CHANGE_NETWORK_STATE",
        checkType = PermCheckType.STANDARD,
    ),
    AshellCommand(
        step = 11,
        title = "Observe App Usage",
        description = "Enables per-app trigger detection via UsageStatsManager.",
        command = "appops set com.android.geto android:get_usage_stats allow",
        isRequired = false,
        checkType = PermCheckType.APPOPS_USAGE,
    ),
    AshellCommand(
        step = 12,
        title = "Manage Overlay Permission",
        description = "Allows Aegis to draw on top of other apps (for on-screen status).",
        command = "appops set com.android.geto SYSTEM_ALERT_WINDOW allow",
        isRequired = false,
        checkType = PermCheckType.DRAW_OVERLAYS,
    ),
)

@Composable
private fun AshellCommandsPanel() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                    modifier = Modifier.weight(1f),
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
                            imageVector = GetoIcons.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "AShell U — Setup Commands",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Text(
                                    text = "${ashellCommands.size} commands",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                        Text(
                            text = if (expanded) "Tap to collapse" else "Run these in AShell U before using Aegis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) GetoIcons.ExpandLess else GetoIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
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
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = GetoIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Install AShell U from F-Droid, start Shizuku first, then run each command in AShell U. Steps 1–3 (REQUIRED) must be completed for Aegis to function.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RequiredLegendChip(label = "REQUIRED", isRequired = true)
                        RequiredLegendChip(label = "OPTIONAL", isRequired = false)
                    }

                    ashellCommands.forEach { cmd ->
                        CommandItem(
                            command = cmd,
                            context = context,
                            isGranted = isPermissionGranted(context, cmd),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequiredLegendChip(label: String, isRequired: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isRequired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRequired) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isRequired) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommandItem(command: AshellCommand, context: Context, isGranted: Boolean) {
    val grantedColor = Color(0xFF2E7D32)
    val notGrantedColor = MaterialTheme.colorScheme.error

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (command.isRequired) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${command.step}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (command.isRequired) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = command.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = command.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (isGranted) grantedColor.copy(alpha = 0.15f)
                    else notGrantedColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isGranted) grantedColor else notGrantedColor),
                        )
                        Text(
                            text = if (isGranted) "Granted" else "Not granted",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGranted) grantedColor else notGrantedColor,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = command.command,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AShell command", command.command))
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = GetoIcons.Copy,
                            contentDescription = "Copy command",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSection(onNavigateToAutomations: () -> Unit) {
    val context = LocalContext.current
    val activeRules = AegisAutomationStore.getEnabledCount(context)
    val lockedCount = remember { getLockedCountFromPrefs(context) }
    var showLockedAppsSheet by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = GetoIcons.Automations,
                label = "Automation Engine",
                subtitle = if (activeRules == 0) "No active rules" else "$activeRules rule${if (activeRules == 1) "" else "s"} active",
                isActive = activeRules > 0,
                onClick = onNavigateToAutomations,
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = GetoIcons.Security,
                label = "App Lock",
                subtitle = if (lockedCount == 0) "No apps locked" else "$lockedCount app${if (lockedCount == 1) "" else "s"} locked",
                isActive = lockedCount > 0,
                onClick = { showLockedAppsSheet = true },
            )
        }
    }

    if (showLockedAppsSheet) {
        LockedAppsSheet(
            context = context,
            onDismiss = { showLockedAppsSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockedAppsSheet(context: Context, onDismiss: () -> Unit) {
    val lockedPackages = remember { getAllLockedPackagesFromPrefs(context) }
    val pm = context.packageManager

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Locked Apps",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (lockedPackages.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No apps are currently locked. Open any app in the Apps tab and go to its Security tab to set up a lock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                lockedPackages.forEach { pkg ->
                    val appName = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg }
                    val isBlocked = isAppBlockedFromPrefs(context, pkg)
                    val lockLabel = getLockTypeLabelFromPrefs(context, pkg)
                    ListItem(
                        headlineContent = {
                            Text(
                                appName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = if (isBlocked) "Blocked" else "$lockLabel lock",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isBlocked) GetoIcons.Block else GetoIcons.Lock,
                                contentDescription = null,
                                tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        trailingContent = {
                            Text(
                                text = pkg.substringAfterLast("."),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun getLockedCountFromPrefs(context: Context): Int {
    val prefs = context.getSharedPreferences("aegis_app_lock_v1", Context.MODE_PRIVATE)
    return prefs.all.keys.count { key ->
        key.endsWith("_enabled") && prefs.getBoolean(key, false)
    }
}

private fun getAllLockedPackagesFromPrefs(context: Context): List<String> {
    val prefs = context.getSharedPreferences("aegis_app_lock_v1", Context.MODE_PRIVATE)
    return prefs.all.keys
        .filter { it.endsWith("_enabled") && prefs.getBoolean(it, false) }
        .map { it.removeSuffix("_enabled") }
        .distinct()
}

private fun isAppBlockedFromPrefs(context: Context, pkg: String): Boolean {
    val prefs = context.getSharedPreferences("aegis_app_lock_v1", Context.MODE_PRIVATE)
    return prefs.getBoolean("${pkg}_blocked", false)
}

private fun getLockTypeLabelFromPrefs(context: Context, pkg: String): String {
    val prefs = context.getSharedPreferences("aegis_app_lock_v1", Context.MODE_PRIVATE)
    return when (prefs.getString("${pkg}_type", "pattern")) {
        "pin" -> "PIN"
        "password" -> "Password"
        "biometric" -> "Biometric"
        else -> "Pattern"
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
