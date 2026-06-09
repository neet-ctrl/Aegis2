package com.android.geto.feature.appsettings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.material3.HorizontalDivider
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

private enum class DetectionState { DETECTED, CLEAN, UNKNOWN }

private data class DetectionItem(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val method: String,
    val educationalInfo: String,
    val bypassTip: String,
    val state: DetectionState,
)

private data class DetectionCategory(
    val name: String,
    val icon: ImageVector,
    val items: List<DetectionItem>,
)

private fun isRooted(): Boolean {
    return try {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/data/local/su", "/system/sd/xbin/su",
            "/system/bin/.ext/.su", "/data/adb/magisk",
            "/sbin/.magisk", "/data/adb/ksu",
        )
        if (suPaths.any { java.io.File(it).exists() }) return true
        if (Build.TAGS?.contains("test-keys") == true) return true
        if (java.io.File("/proc/net/unix").readText().contains("magisk")) return true
        false
    } catch (_: Exception) {
        false
    }
}

private fun isXposedInstalled(): Boolean {
    return try {
        val xposedPaths = listOf(
            "/system/framework/XposedBridge.jar",
            "/system/lib/libxposed_art.so",
            "/data/framework/XposedBridge.jar",
        )
        xposedPaths.any { java.io.File(it).exists() }
    } catch (_: Exception) {
        false
    }
}

private fun isFridaRunning(): Boolean {
    return try {
        val proc = Runtime.getRuntime().exec("netstat -tnp")
        val output = proc.inputStream.bufferedReader().readText()
        output.contains(":27042") || output.contains(":27043")
    } catch (_: Exception) {
        false
    }
}

private fun buildDetectionCategories(context: Context, targetPackageName: String): List<DetectionCategory> {
    val cr = context.contentResolver

    val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
    val devOptions = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    val mockLocationOld = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        Settings.Secure.getString(cr, "mock_location") == "1"
    } else false
    val stayOn = Settings.Global.getInt(cr, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0
    val locationMode = try {
        Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE) != 0
    } catch (_: Exception) { false }

    val isEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
        Build.FINGERPRINT.contains("unknown", ignoreCase = true) ||
        Build.MODEL.contains("google_sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for x86", ignoreCase = true) ||
        Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        "google_sdk" == Build.PRODUCT

    val enabledAccessibilityServices = Settings.Secure.getString(
        cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: ""
    val hasAccessibilityService = enabledAccessibilityServices.isNotEmpty() &&
        enabledAccessibilityServices.lowercase() != "null"

    val isVpnActive = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val caps = cm.getNetworkCapabilities(activeNetwork)
                caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            } else false
        } else false
    } catch (_: Exception) { false }

    val canDrawOverlays = Settings.canDrawOverlays(context)

    val rooted = isRooted()
    val xposedDetected = isXposedInstalled()
    val fridaDetected = isFridaRunning()

    val targetPackageInfo: PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                targetPackageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(targetPackageName, PackageManager.GET_PERMISSIONS)
        }
    } catch (_: Exception) { null }

    val targetPermissions = targetPackageInfo?.requestedPermissions?.toSet() ?: emptySet()
    val targetAppInfo = targetPackageInfo?.applicationInfo
    val isTargetDebuggable = targetAppInfo != null &&
        (targetAppInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val targetInstallSource = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(targetPackageName).installingPackageName ?: "Unknown/Sideloaded"
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(targetPackageName) ?: "Unknown/Sideloaded"
        }
    } catch (_: Exception) { "Unknown" }
    val isFromPlayStore = targetInstallSource.contains("vending", ignoreCase = true)
    val targetTargetSdk = targetAppInfo?.targetSdkVersion ?: 0
    val hasQueriesAllPackages = targetPermissions.any { it.contains("QUERY_ALL_PACKAGES") }
    val hasReadPhoneState = targetPermissions.any { it.contains("READ_PHONE_STATE") }
    val hasNetworkState = targetPermissions.any { it.contains("ACCESS_NETWORK_STATE") }
    val hasBindNotificationListener = targetPermissions.any { it.contains("BIND_NOTIFICATION_LISTENER") }

    return listOf(
        DetectionCategory(
            name = "Developer Environment",
            icon = GetoIcons.DeveloperMode,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.DeveloperMode,
                    name = "Developer Options",
                    description = "Android Developer Options menu is enabled",
                    method = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED",
                    educationalInfo = "Apps use this to detect testing environments. Banking and DRM apps often refuse to run when Developer Options are on. The setting value is 1 (enabled) or 0 (disabled).",
                    bypassTip = "Disable Developer Options in Settings → About Phone (tap Build Number 7x to toggle). Note: you can re-enable after closing the target app.",
                    state = if (devOptions) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Usb,
                    name = "USB Debugging (ADB)",
                    description = "ADB / USB Debugging is currently active",
                    method = "Settings.Global.ADB_ENABLED == 1",
                    educationalInfo = "USB debugging allows a computer to interact with the device. Apps check this as a strong developer/testing indicator. Many security apps block when ADB is on.",
                    bypassTip = "Turn off USB Debugging in Settings → Developer Options → USB Debugging. You can re-enable it after.",
                    state = if (adbEnabled) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Stay Awake (Plugged In)",
                    description = "Screen stays on while charging (developer setting)",
                    method = "Settings.Global.STAY_ON_WHILE_PLUGGED_IN != 0",
                    educationalInfo = "This developer setting keeps the screen on during charging. Used as a secondary developer environment indicator. Rarely checked alone but combined with other signals.",
                    bypassTip = "Disable in Settings → Developer Options → Stay awake.",
                    state = if (stayOn) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.BugReport,
                    name = "Android Emulator",
                    description = "Device appears to be an Android emulator or VM",
                    method = "Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER checks",
                    educationalInfo = "Apps check Build properties for emulator signatures (e.g. 'generic', 'sdk_gphone'). Anti-cheat and banking apps use this to detect automated environments.",
                    bypassTip = "Use a real device. In emulators, spoofing Build properties is possible via Xposed modules but risks further detection.",
                    state = if (isEmulator) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
            ),
        ),
        DetectionCategory(
            name = "Root & Tampering",
            icon = GetoIcons.Shield,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.Security,
                    name = "Root / Su Binary",
                    description = "Root access (su binary, Magisk, KernelSU) detected on device",
                    method = "File checks: /system/xbin/su, /sbin/.magisk, /data/adb/magisk, Build.TAGS test-keys",
                    educationalInfo = "Root bypasses Android's security model. Banking apps, Google Pay, and DRM content use Play Integrity API to refuse to run on rooted devices. Magisk can hide root but detection keeps improving.",
                    bypassTip = "Use Magisk Hide / DenyList to hide root from the target app. Enable Zygisk + DenyList in Magisk settings, then add the target app.",
                    state = if (rooted) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Layers,
                    name = "Xposed / LSPosed Framework",
                    description = "Xposed hooking framework files detected",
                    method = "File checks: /system/framework/XposedBridge.jar, libxposed_art.so",
                    educationalInfo = "Xposed/LSPosed lets modules intercept and modify any app's code at runtime. Apps check for Xposed files and may detect hooks in their own class stack.",
                    bypassTip = "Use LSPosed's scope to limit injection. Ensure XposedBridge is not in global scope for the target app. Some apps also check java.lang.Throwable stack traces for XposedBridge.",
                    state = if (xposedDetected) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Terminal,
                    name = "Build Integrity (Test-Keys)",
                    description = "Device firmware signed with test-keys instead of release-keys",
                    method = "Build.TAGS.contains(\"test-keys\")",
                    educationalInfo = "Release builds are signed with release-keys. Custom ROMs or unlocked bootloaders often use test-keys, indicating unofficial firmware. This is a strong root/custom ROM indicator.",
                    bypassTip = "Cannot be bypassed on custom ROMs without spoofing Build.TAGS via Xposed.",
                    state = if (Build.TAGS?.contains("test-keys") == true) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
            ),
        ),
        DetectionCategory(
            name = "Runtime & Hook Detection",
            icon = GetoIcons.BugReport,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.BugReport,
                    name = "Debugger Attached",
                    description = "Android Studio or another debugger is connected to an app",
                    method = "android.os.Debug.isDebuggerConnected() / waitingForDebugger()",
                    educationalInfo = "A connected debugger allows real-time inspection of app memory. Apps detect this to prevent reverse engineering. Note: Aegis can read this state for itself only.",
                    bypassTip = "Detach the debugger before running the target app. Anti-debugger hooks in Frida can also spoof the result.",
                    state = if (android.os.Debug.isDebuggerConnected()) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Mock Locations",
                    description = "Fake GPS / mock location provider is active",
                    method = "Settings.Secure.ALLOW_MOCK_LOCATION (API<23), AppOps.OP_MOCK_LOCATION",
                    educationalInfo = "Mock locations allow GPS spoofers to fake device position. Geofencing apps, ride-sharing, and banking detect this to prevent manipulation. On API 23+, checked via AppOps.",
                    bypassTip = "Disable mock locations in Developer Options. On Android 6+, remove the mock provider app or revoke its AppOps MOCK_LOCATION permission.",
                    state = if (mockLocationOld) DetectionState.DETECTED else DetectionState.UNKNOWN,
                ),
                DetectionItem(
                    icon = GetoIcons.Terminal,
                    name = "Frida Instrumentation Server",
                    description = if (fridaDetected) "Frida server detected on default port (27042/27043)"
                    else "Frida server not detected on default ports",
                    method = "Port scan: 27042, 27043 (default Frida server ports)",
                    educationalInfo = "Frida is a dynamic instrumentation toolkit used for hooking app functions at runtime. Apps can check for open Frida ports or look for Frida gadget libraries in memory.",
                    bypassTip = "Run Frida server on a non-default port. Use Frida gadget embedded in the app instead of standalone server.",
                    state = if (fridaDetected) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
            ),
        ),
        DetectionCategory(
            name = "System State",
            icon = GetoIcons.Shield,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.AccessibilityNew,
                    name = "Accessibility Services Active",
                    description = if (hasAccessibilityService) "One or more accessibility services are running" else "No accessibility services detected",
                    method = "Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES",
                    educationalInfo = "Accessibility services have elevated privileges to observe and interact with other apps. While designed for disabilities, they can be used for automation or screen scraping. Many apps flag any active accessibility service.",
                    bypassTip = "Disable non-essential accessibility services in Settings → Accessibility. Aegis Lock is an accessibility service — the target app may flag it.",
                    state = if (hasAccessibilityService) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.VpnKey,
                    name = "VPN Active",
                    description = if (isVpnActive) "A VPN tunnel is currently connected" else "No VPN detected",
                    method = "ConnectivityManager.getNetworkCapabilities() — NET_CAPABILITY_NOT_VPN",
                    educationalInfo = "VPNs route traffic through a different server. Apps with geo-restrictions, anti-fraud, or session integrity requirements detect active VPNs. Some banking apps refuse to work over VPN.",
                    bypassTip = "Disconnect the VPN before using the target app. Some VPN apps offer per-app exclusion (split tunneling) to route target app traffic directly.",
                    state = if (isVpnActive) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Layers,
                    name = "Screen Overlay (SYSTEM_ALERT_WINDOW)",
                    description = if (canDrawOverlays) "Aegis has SYSTEM_ALERT_WINDOW permission active" else "No overlay active for Aegis",
                    method = "Settings.canDrawOverlays(context) per-app check",
                    educationalInfo = "Overlay apps can capture taps or display fake UI over banking screens (tapjacking). Security apps check if ANY app has draw-over-apps permission active. Aegis uses this for certain features.",
                    bypassTip = "Revoke SYSTEM_ALERT_WINDOW from any app that might trigger detection. In Settings → Apps → Special app access → Display over other apps.",
                    state = if (canDrawOverlays) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.PhoneAndroid,
                    name = "Location Services",
                    description = if (locationMode) "Device location is currently enabled" else "Location services are OFF",
                    method = "Settings.Secure.LOCATION_MODE / LocationManager.isLocationEnabled()",
                    educationalInfo = "Many apps check if location is enabled before using GPS. Ride-sharing, navigation, and delivery apps require location. Some fraud-detection systems also check for sudden location changes.",
                    bypassTip = "Enable or disable location as needed. For spoofing, enable mock locations via Developer Options.",
                    state = if (locationMode) DetectionState.CLEAN else DetectionState.UNKNOWN,
                ),
            ),
        ),
        DetectionCategory(
            name = "App Security Profile — ${targetPackageName.substringAfterLast(".")}",
            icon = GetoIcons.Policy,
            items = listOf(
                DetectionItem(
                    icon = GetoIcons.Store,
                    name = "Installation Source",
                    description = "App installed from: $targetInstallSource",
                    method = "PackageManager.getInstallSourceInfo() / getInstallerPackageName()",
                    educationalInfo = "Apps verify they were installed from Play Store (com.android.vending) to prevent sideloaded or modified APKs from running. Non-Play installs often trigger security checks.",
                    bypassTip = "Cannot fake install source without system permission. Some root tools can spoof this value. For testing, install through Play Store.",
                    state = if (isFromPlayStore) DetectionState.CLEAN else DetectionState.DETECTED,
                ),
                DetectionItem(
                    icon = GetoIcons.BugReport,
                    name = "App Debuggable Flag",
                    description = if (isTargetDebuggable) "⚠ This app's APK is built in debug mode" else "Release build — not debuggable",
                    method = "ApplicationInfo.FLAG_DEBUGGABLE check on target APK",
                    educationalInfo = "A debuggable APK allows attaching Android debugger. Production apps should NOT be debuggable. If this is debug, it may be a modified or cracked APK. This may also mean the app's security is reduced.",
                    bypassTip = "This is about the target app itself, not your device. A debuggable production APK may indicate it has been tampered with.",
                    state = if (isTargetDebuggable) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Security,
                    name = "Device Fingerprinting Permissions",
                    description = if (hasReadPhoneState) "App requests READ_PHONE_STATE (device ID collection)" else "No phone state permission requested",
                    method = "PackageInfo.requestedPermissions analysis",
                    educationalInfo = "READ_PHONE_STATE grants access to IMEI, device ID, and call state. Combined with other IDs, apps build a device fingerprint that persists across reinstalls. Used for fraud detection and licensing.",
                    bypassTip = "Deny READ_PHONE_STATE in Settings → Apps → [App] → Permissions. Alternatively, revoke via ADB: adb shell pm revoke $targetPackageName android.permission.READ_PHONE_STATE",
                    state = if (hasReadPhoneState) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Apps,
                    name = "Package Query Permission (QUERY_ALL_PACKAGES)",
                    description = if (hasQueriesAllPackages) "App can list ALL installed apps on device" else "App does not have QUERY_ALL_PACKAGES",
                    method = "PackageInfo.requestedPermissions: QUERY_ALL_PACKAGES",
                    educationalInfo = "This permission (added in Android 11) lets an app see all other installed apps. Used to detect Magisk Manager, banking apps, overlay apps, and other security-relevant installed packages.",
                    bypassTip = "Cannot prevent QUERY_ALL_PACKAGES if granted. You can try to hide app names via Magisk module that spoofs PM responses. Revoke the permission on rooted devices.",
                    state = if (hasQueriesAllPackages) DetectionState.DETECTED else DetectionState.CLEAN,
                ),
                DetectionItem(
                    icon = GetoIcons.Speed,
                    name = "Target SDK Level",
                    description = "App targets SDK $targetTargetSdk — ${if (targetTargetSdk >= 30) "uses modern security APIs" else "older API level"}",
                    method = "ApplicationInfo.targetSdkVersion",
                    educationalInfo = "Apps targeting SDK 29+ have access to Play Integrity API (replacement for SafetyNet). Higher target SDK = stronger access to modern security checks like attestation, integrity verdict, and boot state.",
                    bypassTip = "Cannot change app's target SDK. Passing Play Integrity requires proper device attestation — passing on modified devices requires GMS spoofing.",
                    state = if (targetTargetSdk >= 29) DetectionState.DETECTED else DetectionState.UNKNOWN,
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
    val categories = remember(packageName) { buildDetectionCategories(context, packageName) }

    val detectedCount = categories.sumOf { cat -> cat.items.count { it.state == DetectionState.DETECTED } }
    val totalChecks = categories.sumOf { cat -> cat.items.count { it.state != DetectionState.UNKNOWN } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DetectionSummaryBanner(detectedCount = detectedCount, totalChecks = totalChecks)
        }

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
                        text = "Detection Center shows your device state + app-specific security signals. " +
                            "DETECTED (red) means an active detection vector that apps commonly check. " +
                            "CLEAN (blue) means the vector is not active. Tap any item for details + bypass tips.",
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
private fun DetectionSummaryBanner(detectedCount: Int, totalChecks: Int) {
    val isClean = detectedCount == 0
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isClean) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isClean) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isClean) GetoIcons.CheckCircle else GetoIcons.Warning,
                    contentDescription = null,
                    tint = if (isClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column {
                Text(
                    text = if (isClean) "No Risks Detected" else "$detectedCount Risk(s) Detected",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "$totalChecks checks run — tap items for bypass tips",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isClean) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DetectionCategoryCard(category: DetectionCategory) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    val detectedCount = category.items.count { it.state == DetectionState.DETECTED }

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
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (detectedCount > 0) {
                            Text(
                                text = "$detectedCount active risk(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
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
        DetectionState.DETECTED -> Triple(
            MaterialTheme.colorScheme.error,
            "DETECTED",
            GetoIcons.Warning,
        )
        DetectionState.CLEAN -> Triple(
            MaterialTheme.colorScheme.primary,
            "CLEAN",
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
        color = if (item.state == DetectionState.DETECTED)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
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
                        tint = stateColor,
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

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Method: ${item.method}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (expanded) "Less" else "Details + Bypass",
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Why apps check this",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.educationalInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    if (item.bypassTip.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = GetoIcons.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Bypass / Mitigation",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.bypassTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
