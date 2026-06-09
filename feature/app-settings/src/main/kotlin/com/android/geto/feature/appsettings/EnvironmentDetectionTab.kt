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

import android.content.Context
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

private enum class DetectionState { ENABLED, DISABLED, UNKNOWN }

private data class DetectionItem(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val method: String,
    val educationalInfo: String,
    val state: DetectionState,
)

private data class DetectionCategory(
    val name: String,
    val icon: ImageVector,
    val items: List<DetectionItem>,
)

private fun buildDetectionCategories(context: Context): List<DetectionCategory> {
    val cr = context.contentResolver

    val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
    val devOptions = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    val mockLocation = Settings.Secure.getString(cr, Settings.Secure.ALLOW_MOCK_LOCATION) == "1"
    val stayOn = Settings.Global.getInt(cr, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0
    val locationMode = try {
        Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE) != 0
    } catch (_: Exception) { false }

    return listOf(
        DetectionCategory(
            name = "Developer Environment",
            icon = GetoIcons.DeveloperMode,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.DeveloperMode,
                    name = "Developer Options",
                    description = "Whether Android Developer Options menu is enabled",
                    method = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED",
                    educationalInfo = "Apps may use this to detect testing environments or developer devices. Many security-sensitive apps (banking, DRM) refuse to run when developer options are on.",
                    state = if (devOptions) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
                DetectionItem(
                    icon = GetoIcons.Usb,
                    name = "USB Debugging",
                    description = "Whether ADB (USB debugging) is currently active",
                    method = "Settings.Global.ADB_ENABLED",
                    educationalInfo = "USB debugging allows a computer to interact with the device over ADB. Apps check this as an indicator of a developer or testing environment.",
                    state = if (adbEnabled) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Stay Awake",
                    description = "Screen stays on while plugged in (developer setting)",
                    method = "Settings.Global.STAY_ON_WHILE_PLUGGED_IN",
                    educationalInfo = "This developer setting keeps the screen on during charging. Apps can use this as a secondary developer environment indicator.",
                    state = if (stayOn) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
            ),
        ),
        DetectionCategory(
            name = "Mock & Debug",
            icon = GetoIcons.BugReport,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Mock Locations",
                    description = "Whether mock/fake GPS locations are allowed",
                    method = "Settings.Secure.ALLOW_MOCK_LOCATION (API <23) / appops OP_MOCK_LOCATION",
                    educationalInfo = "Mock locations allow apps like GPS spoofers to fake your device position. Apps with geofencing or anti-fraud checks (e.g. ride-sharing, mobile banking) detect this to prevent manipulation.",
                    state = if (mockLocation) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
                DetectionItem(
                    icon = GetoIcons.BugReport,
                    name = "Debugger Attached",
                    description = "Whether a debugger (e.g. Android Studio) is connected to this app",
                    method = "android.os.Debug.isDebuggerConnected()",
                    educationalInfo = "A connected debugger allows real-time inspection and modification of app memory. Apps detect this to prevent reverse engineering or cheating via memory modification.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.Terminal,
                    name = "Emulator Detection",
                    description = "Whether the device appears to be an Android emulator or virtual machine",
                    method = "Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER, QEMU props",
                    educationalInfo = "Apps check for emulator signatures (e.g. 'generic', 'sdk_gphone') in system properties to detect automated testing environments. Checked by gaming anti-cheat and banking apps.",
                    state = if (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator")) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
            ),
        ),
        DetectionCategory(
            name = "System State",
            icon = GetoIcons.Shield,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.Security,
                    name = "Root / Su Binary",
                    description = "Whether root access (su binary, Magisk, KernelSU) is present",
                    method = "Checks /system/xbin/su, Magisk paths, RootBeer, SafetyNet/Play Integrity API",
                    educationalInfo = "Root access bypasses Android's security model. Banking apps, Google Pay, and DRM-protected content use Play Integrity API to verify device integrity and refuse to run on rooted devices.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.Layers,
                    name = "Overlay Apps (SYSTEM_ALERT_WINDOW)",
                    description = "Whether any app is drawing content over other apps",
                    method = "Settings.canDrawOverlays() / OP_SYSTEM_ALERT_WINDOW",
                    educationalInfo = "Overlay apps can capture taps, display fake UI over banking screens (tapjacking attack), or show persistent buttons. Security-sensitive apps check this permission as a fraud prevention measure.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.AccessibilityNew,
                    name = "Accessibility Services",
                    description = "Whether any accessibility services are currently running",
                    method = "Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES",
                    educationalInfo = "Accessibility services have elevated privileges to observe and interact with other apps. While designed for users with disabilities, they can also be used for automation or screen scraping. Many apps flag this.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.VpnKey,
                    name = "VPN Active",
                    description = "Whether a VPN tunnel is currently connected",
                    method = "ConnectivityManager.getActiveNetwork() / NetworkCapabilities.NET_CAPABILITY_NOT_VPN",
                    educationalInfo = "VPNs route traffic through a different server. Apps with geo-restrictions, anti-fraud, or session integrity requirements detect active VPNs. Some banking apps refuse to work over VPN.",
                    state = DetectionState.UNKNOWN,
                ),
            ),
        ),
        DetectionCategory(
            name = "Installation & Source",
            icon = GetoIcons.Store,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.Store,
                    name = "Installation Source",
                    description = "Which store or method was used to install the app being checked",
                    method = "PackageManager.getInstallSourceInfo() / getInstallerPackageName()",
                    educationalInfo = "Apps can verify they were installed from the Play Store (com.android.vending) to prevent sideloaded or modified APKs from running. Checked by DRM-protected and subscription apps.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.Policy,
                    name = "Battery Optimization Exempt",
                    description = "Whether an app is excluded from Doze battery optimization",
                    method = "PowerManager.isIgnoringBatteryOptimizations()",
                    educationalInfo = "Apps exempt from battery optimization run more freely in the background. Some apps request this status for reliability, and others detect it to infer whether the user has modified default settings.",
                    state = DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Location Services",
                    description = "Whether device location is currently enabled",
                    method = "Settings.Secure.LOCATION_MODE / LocationManager.isLocationEnabled()",
                    educationalInfo = "Many apps check if location is enabled before using GPS features. Ride-sharing, navigation, and delivery apps require location to function.",
                    state = if (locationMode) DetectionState.ENABLED else DetectionState.DISABLED,
                ),
            ),
        ),
    )
}

@Composable
internal fun EnvironmentDetectionTab(
    modifier: Modifier = Modifier,
    packageName: String,
) {
    val context = LocalContext.current
    val categories = remember(packageName) { buildDetectionCategories(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
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
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Detection Center shows what environment checks apps commonly perform and your device's current state for each. Educational only — no bypass functionality is provided.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        categories.forEach { category ->
            item {
                DetectionCategoryCard(category = category)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DetectionCategoryCard(category: DetectionCategory) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
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
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    category.items.forEach { item ->
                        DetectionItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionItemCard(item: DetectionItem) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val (stateColor, stateLabel, stateIcon) = when (item.state) {
        DetectionState.ENABLED -> Triple(
            MaterialTheme.colorScheme.error,
            "ACTIVE",
            GetoIcons.Warning,
        )
        DetectionState.DISABLED -> Triple(
            MaterialTheme.colorScheme.primary,
            "INACTIVE",
            GetoIcons.CheckCircle,
        )
        DetectionState.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            "UNKNOWN",
            GetoIcons.Info,
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = stateColor.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = stateIcon,
                            contentDescription = null,
                            tint = stateColor,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = stateColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Method: ${item.method}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (expanded) "Less" else "Why?",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.educationalInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}
