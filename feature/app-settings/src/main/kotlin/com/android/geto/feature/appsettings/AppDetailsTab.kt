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

import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

@Composable
internal fun AppDetailsTab(
    modifier: Modifier = Modifier,
    packageName: String,
    packageInfo: PackageInfo?,
) {
    val context = LocalContext.current

    val permissions = packageInfo?.requestedPermissions?.toList() ?: emptyList()
    val activities = packageInfo?.activities?.map { it.name.substringAfterLast(".") } ?: emptyList()
    val services = packageInfo?.services?.map { it.name.substringAfterLast(".") } ?: emptyList()
    val receivers = packageInfo?.receivers?.map { it.name.substringAfterLast(".") } ?: emptyList()
    val providers = packageInfo?.providers?.map { it.name.substringAfterLast(".") } ?: emptyList()

    val versionCode = if (packageInfo != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
    } else "—"

    val minSdk = if (packageInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        packageInfo.applicationInfo?.minSdkVersion?.toString() ?: "—"
    } else "—"

    val uid = packageInfo?.applicationInfo?.uid ?: 0

    val appOpsEntries = remember(packageName, uid) {
        buildAppOpsEntries(context, packageName, uid)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PackageInfoCard(
                packageName = packageName,
                packageInfo = packageInfo,
                versionCode = versionCode,
                minSdk = minSdk,
                context = context,
            )
        }

        item {
            RunningStateCard(packageName = packageName, context = context)
        }

        if (appOpsEntries.isNotEmpty()) {
            item {
                AppOpsCard(entries = appOpsEntries)
            }
        }

        if (permissions.isNotEmpty()) {
            item {
                CollapsibleComponentSection(
                    title = "Permissions",
                    count = permissions.size,
                    items = permissions,
                    itemTextFn = { it.substringAfterLast(".") },
                    fullNameFn = { it },
                )
            }
        }

        if (activities.isNotEmpty()) {
            item {
                CollapsibleComponentSection(
                    title = "Activities",
                    count = activities.size,
                    items = activities,
                )
            }
        }

        if (services.isNotEmpty()) {
            item {
                CollapsibleComponentSection(
                    title = "Services",
                    count = services.size,
                    items = services,
                )
            }
        }

        if (receivers.isNotEmpty()) {
            item {
                CollapsibleComponentSection(
                    title = "Broadcast Receivers",
                    count = receivers.size,
                    items = receivers,
                )
            }
        }

        if (providers.isNotEmpty()) {
            item {
                CollapsibleComponentSection(
                    title = "Content Providers",
                    count = providers.size,
                    items = providers,
                )
            }
        }

        item {
            ManifestExportCard(
                context = context,
                packageInfo = packageInfo,
                packageName = packageName,
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun buildAppOpsEntries(context: Context, packageName: String, uid: Int): List<Pair<String, Boolean?>> {
    if (uid == 0) return emptyList()
    return try {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        listOf(
            "Camera" to checkOp(appOpsManager, AppOpsManager.OPSTR_CAMERA, uid, packageName),
            "Microphone" to checkOp(appOpsManager, AppOpsManager.OPSTR_RECORD_AUDIO, uid, packageName),
            "Fine Location" to checkOp(appOpsManager, AppOpsManager.OPSTR_FINE_LOCATION, uid, packageName),
            "Coarse Location" to checkOp(appOpsManager, AppOpsManager.OPSTR_COARSE_LOCATION, uid, packageName),
            "Read Contacts" to checkOp(appOpsManager, AppOpsManager.OPSTR_READ_CONTACTS, uid, packageName),
            "Read Call Log" to checkOp(appOpsManager, AppOpsManager.OPSTR_READ_CALL_LOG, uid, packageName),
            "Read SMS" to checkOp(appOpsManager, AppOpsManager.OPSTR_READ_SMS, uid, packageName),
            "Write External Storage" to checkOp(appOpsManager, AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, uid, packageName),
            "Read External Storage" to checkOp(appOpsManager, AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE, uid, packageName),
            "System Alert Window" to checkOp(appOpsManager, AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, packageName),
            "Usage Stats" to checkOp(appOpsManager, AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName),
        )
    } catch (_: Exception) {
        emptyList()
    }
}

private fun checkOp(appOpsManager: AppOpsManager, op: String, uid: Int, packageName: String): Boolean? {
    return try {
        appOpsManager.checkOpNoThrow(op, uid, packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun RunningStateCard(packageName: String, context: Context) {
    val isRunning = remember(packageName) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            val processes = am.runningAppProcesses
            processes?.any { it.processName == packageName || it.pkgList?.contains(packageName) == true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = GetoIcons.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = "Running State",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "On Android 8+, process visibility is restricted to your own app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = if (isRunning) "Running" else "Stopped",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AppOpsCard(entries: List<Pair<String, Boolean?>>) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                    Text(
                        text = "App Ops",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "${entries.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    entries.forEachIndexed { i, (label, allowed) ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val (color, text) = when (allowed) {
                                true -> Pair(MaterialTheme.colorScheme.primary, "ALLOW")
                                false -> Pair(MaterialTheme.colorScheme.error, "DENY")
                                null -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "N/A")
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = color.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PackageInfoCard(
    packageName: String,
    packageInfo: PackageInfo?,
    versionCode: String,
    minSdk: String,
    context: Context,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Package Info",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Package Name", packageName)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Package name copied", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        imageVector = GetoIcons.Copy,
                        contentDescription = "Copy package name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val installDate = packageInfo?.firstInstallTime?.let {
                java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
            } ?: "—"

            val updateDate = packageInfo?.lastUpdateTime?.let {
                java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
            } ?: "—"

            val rows = listOf(
                "Package" to packageName,
                "Version Name" to (packageInfo?.versionName ?: "—"),
                "Version Code" to versionCode,
                "Target SDK" to (packageInfo?.applicationInfo?.targetSdkVersion?.toString() ?: "—"),
                "Min SDK" to minSdk,
                "UID" to (packageInfo?.applicationInfo?.uid?.toString() ?: "—"),
                "First Installed" to installDate,
                "Last Updated" to updateDate,
                "Install Source" to getInstallSource(context, packageName),
            )

            rows.forEachIndexed { i, (key, value) ->
                if (i > 0) HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = if (key == "Package") FontFamily.Monospace else null,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun getInstallSource(context: Context, packageName: String): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = context.packageManager.getInstallSourceInfo(packageName)
            info.installingPackageName ?: "Unknown / Sideloaded"
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(packageName) ?: "Unknown / Sideloaded"
        }
    } catch (_: Exception) {
        "—"
    }
}

@Composable
private fun CollapsibleComponentSection(
    title: String,
    count: Int,
    items: List<String>,
    itemTextFn: (String) -> String = { it },
    fullNameFn: (String) -> String = { it },
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                        )
                        Text(
                            text = itemTextFn(item),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ManifestExportCard(
    context: Context,
    packageInfo: PackageInfo?,
    packageName: String,
) {
    val manifestText = buildFullManifestSummary(packageInfo, packageName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Manifest Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Full package declaration — tap Copy to export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Package Summary", manifestText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Manifest summary copied", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        imageVector = GetoIcons.Copy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Copy")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(12.dp),
            ) {
                Text(
                    text = manifestText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun buildFullManifestSummary(packageInfo: PackageInfo?, packageName: String): String {
    if (packageInfo == null) return "<manifest package=\"$packageName\"\n    error=\"Package info unavailable\" />"

    val permissions = packageInfo.requestedPermissions?.size ?: 0
    val activities = packageInfo.activities?.size ?: 0
    val services = packageInfo.services?.size ?: 0
    val receivers = packageInfo.receivers?.size ?: 0
    val providers = packageInfo.providers?.size ?: 0
    val uid = packageInfo.applicationInfo?.uid ?: 0
    val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 0
    val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        packageInfo.applicationInfo?.minSdkVersion ?: 0
    } else 0
    val installSource = "see Package Info card"

    val sb = StringBuilder()
    sb.appendLine("<manifest")
    sb.appendLine("    package=\"$packageName\"")
    sb.appendLine("    android:versionName=\"${packageInfo.versionName ?: "?"}\"")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        sb.appendLine("    android:versionCode=\"${packageInfo.longVersionCode}\"")
    } else {
        @Suppress("DEPRECATION")
        sb.appendLine("    android:versionCode=\"${packageInfo.versionCode}\"")
    }
    sb.appendLine()
    sb.appendLine("    <!-- SDK -->")
    sb.appendLine("    <uses-sdk")
    sb.appendLine("        android:minSdkVersion=\"$minSdk\"")
    sb.appendLine("        android:targetSdkVersion=\"$targetSdk\" />")
    sb.appendLine()
    sb.appendLine("    <!-- App Identity -->")
    sb.appendLine("    <application-info")
    sb.appendLine("        android:uid=\"$uid\"")
    sb.appendLine("        android:installSource=\"$installSource\" />")
    sb.appendLine()
    sb.appendLine("    <!-- Component Counts -->")
    sb.appendLine("    <!-- permissions: $permissions -->")
    sb.appendLine("    <!-- activities:  $activities -->")
    sb.appendLine("    <!-- services:    $services -->")
    sb.appendLine("    <!-- receivers:   $receivers -->")
    sb.appendLine("    <!-- providers:   $providers -->")
    sb.appendLine()

    if (packageInfo.requestedPermissions != null) {
        sb.appendLine("    <!-- Declared Permissions -->")
        packageInfo.requestedPermissions?.take(30)?.forEach { perm ->
            sb.appendLine("    <uses-permission android:name=\"$perm\" />")
        }
        if ((packageInfo.requestedPermissions?.size ?: 0) > 30) {
            sb.appendLine("    <!-- ... and ${(packageInfo.requestedPermissions?.size ?: 0) - 30} more -->")
        }
        sb.appendLine()
    }

    if (packageInfo.activities != null) {
        sb.appendLine("    <!-- Activities -->")
        packageInfo.activities?.take(10)?.forEach { act ->
            sb.appendLine("    <activity android:name=\"${act.name}\" />")
        }
        if ((packageInfo.activities?.size ?: 0) > 10) {
            sb.appendLine("    <!-- ... and ${(packageInfo.activities?.size ?: 0) - 10} more -->")
        }
        sb.appendLine()
    }

    sb.appendLine("</manifest>")
    return sb.toString()
}
