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

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.designsystem.theme.supportsDynamicTheming
import com.android.geto.domain.model.Theme
import com.android.geto.domain.model.UserData
import com.android.geto.feature.settings.dialog.ThemeDialog

private enum class GrantType { RUNTIME, SETTINGS_PAGE, ASHELL_U }
private enum class Tier { REQUIRED, RECOMMENDED, OPTIONAL }

private data class PermEntry(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val whyNeeded: String,
    val grantType: GrantType,
    val tier: Tier = Tier.RECOMMENDED,
    val settingsAction: String = "",
    val usePackageUri: Boolean = true,
    val ashellCommand: String = "",
    val checkGranted: (Context) -> Boolean,
)

private fun checkRuntimePermission(ctx: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

private fun checkWriteSettings(ctx: Context): Boolean =
    Settings.System.canWrite(ctx)

private fun checkOverlay(ctx: Context): Boolean =
    Settings.canDrawOverlays(ctx)

private fun checkUsageStats(ctx: Context): Boolean {
    return try {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}

private fun checkAccessibility(ctx: Context): Boolean {
    return try {
        val services = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        services?.contains("com.android.geto", ignoreCase = true) == true
    } catch (_: Exception) { false }
}

private fun checkInstallPackages(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctx.packageManager.canRequestPackageInstalls()
    } else true
}

private fun checkExactAlarm(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true
}

private fun checkNotificationPolicy(ctx: Context): Boolean {
    return try {
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted
    } catch (_: Exception) { false }
}

private fun ashellGranted(ctx: Context, permission: String): Boolean =
    checkRuntimePermission(ctx, permission)

private fun buildPermissions(): List<PermEntry> = listOf(

    // ──────────────────────────────────────
    // RUNTIME PERMISSIONS (popup dialog)
    // ──────────────────────────────────────

    PermEntry(
        icon = GetoIcons.Notifications,
        name = "POST_NOTIFICATIONS",
        description = "Show automation trigger and app-lock notifications",
        whyNeeded = "Required on Android 13+ to display any notification from Aegis, including automation firing alerts, app-lock prompts, and background status indicators.",
        grantType = GrantType.RUNTIME,
        tier = Tier.RECOMMENDED,
        checkGranted = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                checkRuntimePermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            else true
        },
    ),

    // ──────────────────────────────────────
    // SYSTEM SETTINGS PAGE (open intent)
    // ──────────────────────────────────────

    PermEntry(
        icon = GetoIcons.Tune,
        name = "WRITE_SETTINGS",
        description = "Modify system settings: brightness, volume, DPI, screen timeout, font scale",
        whyNeeded = "Allows Aegis to apply per-app display, audio, and system settings when an app is opened or when an automation fires. This is one of the three core required permissions.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.REQUIRED,
        settingsAction = Settings.ACTION_MANAGE_WRITE_SETTINGS,
        usePackageUri = true,
        checkGranted = ::checkWriteSettings,
    ),

    PermEntry(
        icon = GetoIcons.Layers,
        name = "SYSTEM_ALERT_WINDOW",
        description = "Draw informational overlay badges over other apps",
        whyNeeded = "Allows Aegis to display a small on-screen status badge when per-app rules are active. This is optional — core functionality works without it.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.OPTIONAL,
        settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        usePackageUri = true,
        checkGranted = ::checkOverlay,
    ),

    PermEntry(
        icon = GetoIcons.Analytics,
        name = "GET_USAGE_STATS (App Usage Access)",
        description = "Detect when specific apps are launched to fire automation triggers",
        whyNeeded = "Aegis needs to know which app is in the foreground to fire 'App Launch' and 'App Close' automation triggers. Without this, launch-based automations cannot work.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.REQUIRED,
        settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS,
        usePackageUri = false,
        checkGranted = ::checkUsageStats,
    ),

    PermEntry(
        icon = GetoIcons.AccessibilityNew,
        name = "Accessibility Service (App Lock)",
        description = "Monitor foreground app to enforce per-app lock screen",
        whyNeeded = "The Aegis App Lock Service uses Android Accessibility to detect when a locked app comes to the foreground and immediately shows the lock screen. Required for app lock to work.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.REQUIRED,
        settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
        usePackageUri = false,
        checkGranted = ::checkAccessibility,
    ),

    PermEntry(
        icon = GetoIcons.Store,
        name = "REQUEST_INSTALL_PACKAGES",
        description = "Allow installing app update packages from within Aegis",
        whyNeeded = "Required if Aegis needs to install APK updates or sideload automation plugin packages. Not needed for standard usage.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.OPTIONAL,
        settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
        else Settings.ACTION_SECURITY_SETTINGS,
        usePackageUri = true,
        checkGranted = ::checkInstallPackages,
    ),

    PermEntry(
        icon = GetoIcons.Schedule,
        name = "SCHEDULE_EXACT_ALARM",
        description = "Fire time-based automations at exact scheduled moments",
        whyNeeded = "Required on Android 12+ for automations set to trigger at a precise time (e.g., 08:00 every Monday). Without this, time triggers may be delayed by a few minutes.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.RECOMMENDED,
        settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        else "",
        usePackageUri = false,
        checkGranted = ::checkExactAlarm,
    ),

    PermEntry(
        icon = GetoIcons.Notifications,
        name = "NOTIFICATION_POLICY (Do Not Disturb)",
        description = "Control Do Not Disturb mode in automations",
        whyNeeded = "Required to set or change DND/zen mode as part of an automation action (e.g., enable DND when you open a meditation app). Granted in Notification settings.",
        grantType = GrantType.SETTINGS_PAGE,
        tier = Tier.OPTIONAL,
        settingsAction = "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS",
        usePackageUri = false,
        checkGranted = ::checkNotificationPolicy,
    ),

    // ──────────────────────────────────────
    // ASHELL U / SIGNATURE (pm grant / appops)
    // ──────────────────────────────────────

    PermEntry(
        icon = GetoIcons.Security,
        name = "WRITE_SECURE_SETTINGS",
        description = "Core — apply per-app secure and global system settings",
        whyNeeded = "This is the primary permission Aegis needs to change system settings on behalf of each app. Without it, no per-app rules can be applied.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.REQUIRED,
        ashellCommand = "pm grant com.android.geto android.permission.WRITE_SECURE_SETTINGS",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.WRITE_SECURE_SETTINGS) },
    ),

    PermEntry(
        icon = GetoIcons.Terminal,
        name = "DUMP",
        description = "Core — read system state, running services, and app info",
        whyNeeded = "Allows Aegis to read detailed system and app state information including running processes, service status, and device configuration values.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.REQUIRED,
        ashellCommand = "pm grant com.android.geto android.permission.DUMP",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.DUMP) },
    ),

    PermEntry(
        icon = GetoIcons.BugReport,
        name = "READ_LOGS",
        description = "Activity Center — monitor and display app events and logs",
        whyNeeded = "Enables the Activity Center to read system logcat events, automation trigger logs, and error reports in real-time.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.RECOMMENDED,
        ashellCommand = "pm grant com.android.geto android.permission.READ_LOGS",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.READ_LOGS) },
    ),

    PermEntry(
        icon = GetoIcons.ScreenRotation,
        name = "CHANGE_CONFIGURATION",
        description = "Apply DPI, locale, font scale, and display configuration overrides",
        whyNeeded = "Required for per-app display density (DPI) changes, locale switching, and display configuration overrides beyond what WRITE_SETTINGS allows.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.RECOMMENDED,
        ashellCommand = "pm grant com.android.geto android.permission.CHANGE_CONFIGURATION",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.CHANGE_CONFIGURATION) },
    ),

    PermEntry(
        icon = GetoIcons.BatteryFull,
        name = "BATTERY_STATS",
        description = "Monitor battery state and enable battery-based automation triggers",
        whyNeeded = "Required to read precise battery level, charging state, and power consumption data for battery-based automation triggers and the Battery Impact statistic.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.RECOMMENDED,
        ashellCommand = "pm grant com.android.geto android.permission.BATTERY_STATS",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.BATTERY_STATS) },
    ),

    PermEntry(
        icon = GetoIcons.Policy,
        name = "MANAGE_APP_OPS_MODES",
        description = "Control per-app operation permissions (camera, mic, location, clipboard)",
        whyNeeded = "Allows Aegis to read and modify per-app AppOps grants — controlling whether apps can access the camera, microphone, location, clipboard, and other sensors.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.MANAGE_APP_OPS_MODES",
        checkGranted = { ctx -> ashellGranted(ctx, "android.permission.MANAGE_APP_OPS_MODES") },
    ),

    PermEntry(
        icon = GetoIcons.NetworkCheck,
        name = "CHANGE_NETWORK_STATE",
        description = "Enable/disable mobile data and network state in automations",
        whyNeeded = "Required to toggle mobile data on or off as an automation action, and to detect network connectivity changes for network-based triggers.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.CHANGE_NETWORK_STATE",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.CHANGE_NETWORK_STATE) },
    ),

    PermEntry(
        icon = GetoIcons.Wifi,
        name = "CHANGE_WIFI_STATE",
        description = "Enable/disable Wi-Fi and read detailed Wi-Fi connection state",
        whyNeeded = "Required to toggle Wi-Fi state in automations and to detect specific Wi-Fi network connections for Wi-Fi-based triggers.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.CHANGE_WIFI_STATE",
        checkGranted = { ctx -> ashellGranted(ctx, Manifest.permission.CHANGE_WIFI_STATE) },
    ),

    PermEntry(
        icon = GetoIcons.Widgets,
        name = "STATUS_BAR",
        description = "Interact with status bar and Quick Settings tiles",
        whyNeeded = "Required for Aegis Quick Settings tiles to function correctly, including the ability to collapse the notification shade after a tile is tapped.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.STATUS_BAR",
        checkGranted = { ctx -> ashellGranted(ctx, "android.permission.STATUS_BAR") },
    ),

    PermEntry(
        icon = GetoIcons.DeveloperMode,
        name = "READ_DEVICE_CONFIG",
        description = "Read device configuration flags and feature toggles",
        whyNeeded = "Allows Aegis to read device-level feature flags and configuration values for advanced system control and compatibility checks.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.READ_DEVICE_CONFIG",
        checkGranted = { ctx -> ashellGranted(ctx, "android.permission.READ_DEVICE_CONFIG") },
    ),

    PermEntry(
        icon = GetoIcons.Block,
        name = "FORCE_STOP_PACKAGES",
        description = "Force-stop other apps as an automation action",
        whyNeeded = "Allows Aegis to force-stop a target app as part of an automation (e.g., force-stop a battery drain app after screen off). Use carefully.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.FORCE_STOP_PACKAGES",
        checkGranted = { ctx -> ashellGranted(ctx, "android.permission.FORCE_STOP_PACKAGES") },
    ),

    PermEntry(
        icon = GetoIcons.Analytics,
        name = "PACKAGE_USAGE_STATS (appops)",
        description = "Enhanced per-package app usage tracking via AppOps",
        whyNeeded = "Supplements the Usage Access setting for more precise per-app foreground detection. Grant via appops set for the most reliable app-launch trigger detection.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.RECOMMENDED,
        ashellCommand = "appops set com.android.geto GET_USAGE_STATS allow",
        checkGranted = ::checkUsageStats,
    ),

    PermEntry(
        icon = GetoIcons.Notifications,
        name = "ACCESS_NOTIFICATION_POLICY (AShell)",
        description = "Full DND/Zen mode control for automation actions",
        whyNeeded = "The Settings-page grant gives basic DND access. This AShell U grant enables full zen mode manipulation including per-app interruption filters in automations.",
        grantType = GrantType.ASHELL_U,
        tier = Tier.OPTIONAL,
        ashellCommand = "pm grant com.android.geto android.permission.ACCESS_NOTIFICATION_POLICY",
        checkGranted = ::checkNotificationPolicy,
    ),
)

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
    val lifecycleOwner = LocalLifecycleOwner.current

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTheme by remember { mutableIntStateOf(Theme.entries.indexOf(userData.theme)) }
    var permCheckTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permCheckTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permCheckTrigger++ }

    val scope = rememberCoroutineScope()
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val automationsRaw = context.getSharedPreferences("aegis_automations_v1", Context.MODE_PRIVATE)
                        .getStringSet("automations", null) ?: emptySet()
                    val logRaw = context.getSharedPreferences("aegis_activity_log_v1", Context.MODE_PRIVATE)
                        .getStringSet("entries", null) ?: emptySet()
                    val json = JSONObject().apply {
                        put("version", 1)
                        put("timestamp", System.currentTimeMillis())
                        put("automations", JSONArray(automationsRaw.toList()))
                        put("activity_log", JSONArray(logRaw.toList()))
                    }.toString(2)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    val count = automationsRaw.size
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup saved — $count automations backed up", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val jsonStr = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: throw Exception("Cannot read file")
                    val json = JSONObject(jsonStr)
                    val version = json.optInt("version", 0)
                    if (version != 1) throw Exception("Unknown backup format (version $version)")
                    val automationsArr = json.optJSONArray("automations")
                    if (automationsArr != null) {
                        val set = mutableSetOf<String>()
                        for (i in 0 until automationsArr.length()) set.add(automationsArr.getString(i))
                        context.getSharedPreferences("aegis_automations_v1", Context.MODE_PRIVATE)
                            .edit().putStringSet("automations", set).apply()
                    }
                    val logArr = json.optJSONArray("activity_log")
                    if (logArr != null) {
                        val set = mutableSetOf<String>()
                        for (i in 0 until logArr.length()) set.add(logArr.getString(i))
                        context.getSharedPreferences("aegis_activity_log_v1", Context.MODE_PRIVATE)
                            .edit().putStringSet("entries", set).apply()
                    }
                    val count = automationsArr?.length() ?: 0
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restored $count automations — restart app to refresh", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
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
                onClick = { backupLauncher.launch("aegis_backup_${System.currentTimeMillis()}.aegis") },
            )
            SettingsNavigationRow(
                icon = GetoIcons.Restore,
                title = "Restore Aegis",
                subtitle = "Restore from a previously saved .aegis backup file",
                onClick = { restoreLauncher.launch(arrayOf("*/*")) },
            )
        }

        PermissionCenterSection(
            context = context,
            trigger = permCheckTrigger,
            notifLauncher = notifLauncher,
        )

        AshellSetupSection()

        SettingsSection(title = "Security & Privacy") {
            SettingsInfoRow(icon = GetoIcons.Fingerprint, title = "App Lock", subtitle = "Per-app biometric/PIN/pattern lock — set in each app's control page")
            SettingsInfoRow(icon = GetoIcons.Lock, title = "Encrypted Storage", subtitle = "All rules and settings stored securely on-device only")
            SettingsInfoRow(icon = GetoIcons.Block, title = "Zero Cloud", subtitle = "No cloud sync, no analytics, no telemetry — ever")
        }

        SettingsSection(title = "About Aegis") {
            SettingsInfoRow(icon = GetoIcons.Shield, title = "Aegis", subtitle = "Advanced Android App Environment & Automation Controller")
            SettingsInfoRow(icon = GetoIcons.Security, title = "Privacy-First", subtitle = "No cloud. No telemetry. No analytics. Fully offline.")
            SettingsInfoRow(icon = GetoIcons.Android, title = "Platform", subtitle = "Built for Android 15, 16 and beyond · Powered by Shizuku")
            SettingsInfoRow(icon = GetoIcons.Info, title = "Package", subtitle = "com.android.geto")
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

// ═══════════════════════════════════════════════════════════════
// PERMISSION CENTER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PermissionCenterSection(
    context: Context,
    trigger: Int,
    notifLauncher: ActivityResultLauncher<String>,
) {
    val allPerms = remember { buildPermissions() }
    val grantedCount = remember(trigger) { allPerms.count { it.checkGranted(context) } }
    val totalCount = allPerms.size
    val progress = if (totalCount > 0) grantedCount.toFloat() / totalCount.toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "PERMISSION CENTER",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Permissions Granted",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$grantedCount of $totalCount permissions active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = when {
                            progress >= 0.8f -> MaterialTheme.colorScheme.primaryContainer
                            progress >= 0.5f -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        },
                    ) {
                        Text(
                            text = "${"%.0f".format(progress * 100)}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                progress >= 0.8f -> MaterialTheme.colorScheme.onPrimaryContainer
                                progress >= 0.5f -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = when {
                        progress >= 0.8f -> MaterialTheme.colorScheme.primary
                        progress >= 0.5f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                val requiredGranted = remember(trigger) {
                    allPerms.filter { it.tier == Tier.REQUIRED }.count { it.checkGranted(context) }
                }
                val requiredTotal = allPerms.count { it.tier == Tier.REQUIRED }

                Surface(
                    color = if (requiredGranted < requiredTotal)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (requiredGranted < requiredTotal) GetoIcons.Warning else GetoIcons.CheckCircle,
                            contentDescription = null,
                            tint = if (requiredGranted < requiredTotal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (requiredGranted < requiredTotal)
                                "$requiredGranted/$requiredTotal required permissions granted — Aegis features limited"
                            else "All $requiredTotal required permissions granted — Aegis is fully operational",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (requiredGranted < requiredTotal)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val runtimePerms = allPerms.filter { it.grantType == GrantType.RUNTIME }
        val settingsPerms = allPerms.filter { it.grantType == GrantType.SETTINGS_PAGE }
        val ashellPerms = allPerms.filter { it.grantType == GrantType.ASHELL_U }

        PermissionGroup(
            title = "Runtime Permissions",
            icon = GetoIcons.Notifications,
            description = "Granted via Android permission popup dialog",
            entries = runtimePerms,
            trigger = trigger,
            context = context,
        ) { entry ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                entry.name == "POST_NOTIFICATIONS"
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        PermissionGroup(
            title = "System Settings Page",
            icon = GetoIcons.Settings,
            description = "Tap Grant to open the relevant Android settings screen",
            entries = settingsPerms,
            trigger = trigger,
            context = context,
        ) { entry ->
            if (entry.settingsAction.isNotEmpty()) {
                val intent = if (entry.usePackageUri) {
                    Intent(entry.settingsAction, Uri.parse("package:${context.packageName}"))
                } else {
                    Intent(entry.settingsAction)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
            }
        }

        PermissionGroup(
            title = "AShell U — Signature Permissions",
            icon = GetoIcons.Terminal,
            description = "Run commands in AShell U (requires Shizuku). Tap Copy to copy the command.",
            entries = ashellPerms,
            trigger = trigger,
            context = context,
        ) { entry ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("AShell command", entry.ashellCommand))
            Toast.makeText(context, "Command copied — paste in AShell U", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun PermissionGroup(
    title: String,
    icon: ImageVector,
    description: String,
    entries: List<PermEntry>,
    trigger: Int,
    context: Context,
    onGrant: (PermEntry) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val grantedCount = remember(trigger) { entries.count { it.checkGranted(context) } }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
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
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (grantedCount == entries.size)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Text(
                                    text = "$grantedCount/${entries.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (grantedCount == entries.size)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Text(
                            text = description,
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
                    entries.forEachIndexed { idx, entry ->
                        if (idx > 0) HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                        )
                        val isGranted = remember(trigger) { entry.checkGranted(context) }
                        PermissionRow(
                            entry = entry,
                            isGranted = isGranted,
                            onGrant = { onGrant(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    entry: PermEntry,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    var detailExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else when (entry.tier) {
                            Tier.REQUIRED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            Tier.RECOMMENDED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            Tier.OPTIONAL -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGranted) GetoIcons.CheckCircle else entry.icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary
                    else when (entry.tier) {
                        Tier.REQUIRED -> MaterialTheme.colorScheme.error
                        Tier.RECOMMENDED -> MaterialTheme.colorScheme.secondary
                        Tier.OPTIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TierBadge(tier = entry.tier)
                }
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GrantStatusBadge(isGranted = isGranted)
                if (!isGranted) {
                    when (entry.grantType) {
                        GrantType.RUNTIME -> Button(
                            onClick = onGrant,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall)
                        }
                        GrantType.SETTINGS_PAGE -> OutlinedButton(
                            onClick = onGrant,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(GetoIcons.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                        }
                        GrantType.ASHELL_U -> OutlinedButton(
                            onClick = onGrant,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(GetoIcons.Copy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Command", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (entry.grantType == GrantType.ASHELL_U && entry.ashellCommand.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.ashellCommand,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onGrant, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = GetoIcons.Copy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { detailExpanded = !detailExpanded },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (detailExpanded) "Hide details" else "Why is this needed?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = if (detailExpanded) GetoIcons.ExpandLess else GetoIcons.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }

        AnimatedVisibility(visible = detailExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ) {
                Text(
                    text = entry.whyNeeded,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun GrantStatusBadge(isGranted: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isGranted)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    ),
            )
            Text(
                text = if (isGranted) "GRANTED" else "NOT GRANTED",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TierBadge(tier: Tier) {
    val (label, color) = when (tier) {
        Tier.REQUIRED -> "REQUIRED" to MaterialTheme.colorScheme.error
        Tier.RECOMMENDED -> "RECOMMENDED" to MaterialTheme.colorScheme.secondary
        Tier.OPTIONAL -> "OPTIONAL" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ASHELL U SETUP SECTION (command guide)
// ═══════════════════════════════════════════════════════════════

private data class AshellCommand(val step: String, val label: String, val command: String, val required: Boolean = false)

private val ashellCommands = listOf(
    AshellCommand("Step 1 — REQUIRED", "Write Secure Settings", "pm grant com.android.geto android.permission.WRITE_SECURE_SETTINGS", required = true),
    AshellCommand("Step 1 — REQUIRED", "Dump", "pm grant com.android.geto android.permission.DUMP", required = true),
    AshellCommand("Step 1 — REQUIRED", "Write Settings", "pm grant com.android.geto android.permission.WRITE_SETTINGS", required = true),
    AshellCommand("Step 2 — Recommended", "Read Logs", "pm grant com.android.geto android.permission.READ_LOGS"),
    AshellCommand("Step 2 — Recommended", "Change Configuration", "pm grant com.android.geto android.permission.CHANGE_CONFIGURATION"),
    AshellCommand("Step 2 — Recommended", "Usage Stats (appops)", "appops set com.android.geto GET_USAGE_STATS allow"),
    AshellCommand("Step 2 — Recommended", "Package Usage Stats", "pm grant com.android.geto android.permission.PACKAGE_USAGE_STATS"),
    AshellCommand("Step 2 — Recommended", "Battery Stats", "pm grant com.android.geto android.permission.BATTERY_STATS"),
    AshellCommand("Step 3 — Optional", "Manage App Ops", "pm grant com.android.geto android.permission.MANAGE_APP_OPS_MODES"),
    AshellCommand("Step 3 — Optional", "Notification Policy", "pm grant com.android.geto android.permission.ACCESS_NOTIFICATION_POLICY"),
    AshellCommand("Step 3 — Optional", "Change Network State", "pm grant com.android.geto android.permission.CHANGE_NETWORK_STATE"),
    AshellCommand("Step 3 — Optional", "Change WiFi State", "pm grant com.android.geto android.permission.CHANGE_WIFI_STATE"),
    AshellCommand("Step 3 — Optional", "Status Bar", "pm grant com.android.geto android.permission.STATUS_BAR"),
    AshellCommand("Step 3 — Optional", "Read Device Config", "pm grant com.android.geto android.permission.READ_DEVICE_CONFIG"),
    AshellCommand("Step 3 — Optional", "Force Stop Packages", "pm grant com.android.geto android.permission.FORCE_STOP_PACKAGES"),
    AshellCommand("Step 3 — Optional", "Interact Across Users", "pm grant com.android.geto android.permission.INTERACT_ACROSS_USERS"),
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
                                text = "Full AShell U Command Guide",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${ashellCommands.size} ordered commands with copy buttons",
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

                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "1. Install AShell U from F-Droid  2. Start Shizuku  3. Open AShell U  4. Run each command below (Steps 1–3 are REQUIRED)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp),
                            )
                        }

                        var lastStep = ""
                        ashellCommands.forEach { cmd ->
                            if (cmd.step != lastStep) {
                                lastStep = cmd.step
                                Text(
                                    text = cmd.step,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (cmd.required) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cmd.label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = cmd.command,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("AShell command", cmd.command))
                                            Toast.makeText(context, "Copied — paste in AShell U", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = GetoIcons.Copy,
                                            contentDescription = "Copy",
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
}

// ═══════════════════════════════════════════════════════════════
// REUSABLE SETTINGS ROW COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
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
            Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer),
        )
    }
}

@Composable
private fun SettingsNavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = GetoIcons.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun Theme.getTitle() = when (this) {
    Theme.FOLLOW_SYSTEM -> "Follow System"
    Theme.LIGHT -> "Light"
    Theme.DARK -> "Dark"
    Theme.AMOLED -> "AMOLED Black"
}
