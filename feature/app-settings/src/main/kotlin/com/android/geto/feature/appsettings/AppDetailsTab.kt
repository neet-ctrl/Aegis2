package com.android.geto.feature.appsettings

import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private enum class ComponentType { ACTIVITY, SERVICE, RECEIVER, PROVIDER, PERMISSION }

@Composable
internal fun AppDetailsTab(
    modifier: Modifier = Modifier,
    packageName: String,
    packageInfo: PackageInfo?,
) {
    val context = LocalContext.current

    val uid = packageInfo?.applicationInfo?.uid ?: 0

    val permissionsWithStatus = packageInfo?.requestedPermissions?.mapIndexed { i, perm ->
        val flags = packageInfo.requestedPermissionsFlags?.getOrNull(i) ?: 0
        val isGranted = (flags and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        Triple(perm.substringAfterLast("."), perm, isGranted)
    } ?: emptyList()

    val activities = packageInfo?.activities?.map { Pair(it.name.substringAfterLast("."), it.name) } ?: emptyList()
    val services = packageInfo?.services?.map { Pair(it.name.substringAfterLast("."), it.name) } ?: emptyList()
    val receivers = packageInfo?.receivers?.map { Pair(it.name.substringAfterLast("."), it.name) } ?: emptyList()
    val providers = packageInfo?.providers?.map { Pair(it.name.substringAfterLast("."), it.name) } ?: emptyList()

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
                AppOpsCard(entries = appOpsEntries, context = context, packageName = packageName, uid = uid)
            }
        }

        if (permissionsWithStatus.isNotEmpty()) {
            item {
                PermissionsCard(
                    permissions = permissionsWithStatus,
                    context = context,
                )
            }
        }

        if (activities.isNotEmpty()) {
            item {
                ComponentCard(
                    title = "Activities",
                    count = activities.size,
                    items = activities,
                    componentType = ComponentType.ACTIVITY,
                    packageName = packageName,
                    context = context,
                )
            }
        }

        if (services.isNotEmpty()) {
            item {
                ComponentCard(
                    title = "Services",
                    count = services.size,
                    items = services,
                    componentType = ComponentType.SERVICE,
                    packageName = packageName,
                    context = context,
                )
            }
        }

        if (receivers.isNotEmpty()) {
            item {
                ComponentCard(
                    title = "Broadcast Receivers",
                    count = receivers.size,
                    items = receivers,
                    componentType = ComponentType.RECEIVER,
                    packageName = packageName,
                    context = context,
                )
            }
        }

        if (providers.isNotEmpty()) {
            item {
                ComponentCard(
                    title = "Content Providers",
                    count = providers.size,
                    items = providers,
                    componentType = ComponentType.PROVIDER,
                    packageName = packageName,
                    context = context,
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

private data class AppOpsEntry(
    val label: String,
    val op: String,
    val allowed: Boolean?,
)

private fun buildAppOpsEntries(context: Context, packageName: String, uid: Int): List<AppOpsEntry> {
    if (uid == 0) return emptyList()
    return try {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        listOf(
            AppOpsEntry("Camera", AppOpsManager.OPSTR_CAMERA, checkOp(appOpsManager, AppOpsManager.OPSTR_CAMERA, uid, packageName)),
            AppOpsEntry("Microphone", AppOpsManager.OPSTR_RECORD_AUDIO, checkOp(appOpsManager, AppOpsManager.OPSTR_RECORD_AUDIO, uid, packageName)),
            AppOpsEntry("Fine Location", AppOpsManager.OPSTR_FINE_LOCATION, checkOp(appOpsManager, AppOpsManager.OPSTR_FINE_LOCATION, uid, packageName)),
            AppOpsEntry("Coarse Location", AppOpsManager.OPSTR_COARSE_LOCATION, checkOp(appOpsManager, AppOpsManager.OPSTR_COARSE_LOCATION, uid, packageName)),
            AppOpsEntry("Read Contacts", AppOpsManager.OPSTR_READ_CONTACTS, checkOp(appOpsManager, AppOpsManager.OPSTR_READ_CONTACTS, uid, packageName)),
            AppOpsEntry("Read Call Log", AppOpsManager.OPSTR_READ_CALL_LOG, checkOp(appOpsManager, AppOpsManager.OPSTR_READ_CALL_LOG, uid, packageName)),
            AppOpsEntry("Read SMS", AppOpsManager.OPSTR_READ_SMS, checkOp(appOpsManager, AppOpsManager.OPSTR_READ_SMS, uid, packageName)),
            AppOpsEntry("Write Storage", AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, checkOp(appOpsManager, AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, uid, packageName)),
            AppOpsEntry("Read Storage", AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE, checkOp(appOpsManager, AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE, uid, packageName)),
            AppOpsEntry("System Alert Window", AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, checkOp(appOpsManager, AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, packageName)),
            AppOpsEntry("Usage Stats", AppOpsManager.OPSTR_GET_USAGE_STATS, checkOp(appOpsManager, AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)),
            AppOpsEntry("Mock Location", AppOpsManager.OPSTR_MOCK_LOCATION, checkOp(appOpsManager, AppOpsManager.OPSTR_MOCK_LOCATION, uid, packageName)),
        )
    } catch (_: Exception) {
        emptyList()
    }
}

private fun checkOp(appOpsManager: AppOpsManager, op: String, uid: Int, packageName: String): Boolean? {
    return try {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(op, uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(op, uid, packageName)
        }
        when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> false
            AppOpsManager.MODE_DEFAULT -> null
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun setOpMode(context: Context, op: String, uid: Int, packageName: String, allow: Boolean): Boolean {
    return try {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (allow) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED
        val method = AppOpsManager::class.java.getDeclaredMethod(
            "setMode",
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        method.invoke(appOpsManager, op, uid, packageName, mode)
        true
    } catch (_: Exception) {
        false
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
                        text = "Visibility restricted to own processes on Android 8+",
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
private fun AppOpsCard(
    entries: List<AppOpsEntry>,
    context: Context,
    packageName: String,
    uid: Int,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var localEntries by remember { mutableStateOf(entries) }

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
                    Icon(
                        imageVector = GetoIcons.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = "App Ops",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Real-time operation permission status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "Toggling app ops requires MANAGE_APP_OPS_MODES. This permission cannot be granted via ADB on stock Android — it requires a custom ROM or platform-signed build.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    localEntries.forEachIndexed { i, entry ->
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
                                text = entry.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val (color, text) = when (entry.allowed) {
                                    true -> Pair(MaterialTheme.colorScheme.primary, "ALLOW")
                                    false -> Pair(MaterialTheme.colorScheme.error, "DENY")
                                    null -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "DEFAULT")
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
                                if (entry.allowed != true) {
                                    TextButton(
                                        onClick = {
                                            val ok = setOpMode(context, entry.op, uid, packageName, true)
                                            if (ok) {
                                                localEntries = localEntries.map {
                                                    if (it.op == entry.op) it.copy(allowed = true) else it
                                                }
                                                Toast.makeText(context, "${entry.label} allowed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Need MANAGE_APP_OPS_MODES — grant via ADB first", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    ) {
                                        Text("Allow", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            val ok = setOpMode(context, entry.op, uid, packageName, false)
                                            if (ok) {
                                                localEntries = localEntries.map {
                                                    if (it.op == entry.op) it.copy(allowed = false) else it
                                                }
                                                Toast.makeText(context, "${entry.label} denied", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Need MANAGE_APP_OPS_MODES — grant via ADB first", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    ) {
                                        Text("Deny", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
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
private fun PermissionsCard(
    permissions: List<Triple<String, String, Boolean>>,
    context: Context,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val granted = permissions.count { it.third }
    val denied = permissions.size - granted

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
                    Icon(
                        imageVector = GetoIcons.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = "Permissions",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "$granted granted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "$denied denied",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    permissions.forEachIndexed { i, (shortName, fullName, isGranted) ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = if (isGranted) "GRANTED" else "DENIED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Permission", fullName))
                                        Toast.makeText(context, "Permission copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = GetoIcons.Copy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
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
private fun ComponentCard(
    title: String,
    count: Int,
    items: List<Pair<String, String>>,
    componentType: ComponentType,
    packageName: String,
    context: Context,
) {
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
                    Icon(
                        imageVector = when (componentType) {
                            ComponentType.ACTIVITY -> GetoIcons.PhoneAndroid
                            ComponentType.SERVICE -> GetoIcons.Tune
                            ComponentType.RECEIVER -> GetoIcons.Notifications
                            ComponentType.PROVIDER -> GetoIcons.Dns
                            else -> GetoIcons.Info
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
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
                    items.forEachIndexed { i, (shortName, fullName) ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                        )
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = shortName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = fullName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Component", fullName))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = GetoIcons.Copy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            if (componentType == ComponentType.ACTIVITY || componentType == ComponentType.SERVICE) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent().apply {
                                                    component = ComponentName(packageName, fullName)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                if (componentType == ComponentType.ACTIVITY) {
                                                    context.startActivity(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    if (componentType == ComponentType.ACTIVITY) "Launching $shortName" else "Starting $shortName",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Cannot launch: ${e.message ?: "not exported or permission denied"}",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = GetoIcons.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            if (componentType == ComponentType.ACTIVITY) "Launch" else "Start",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val pm = context.packageManager
                                                pm.setComponentEnabledSetting(
                                                    ComponentName(packageName, fullName),
                                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                                    PackageManager.DONT_KILL_APP,
                                                )
                                                Toast.makeText(context, "$shortName disabled", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                val adbCmd = "pm disable $packageName/$fullName"
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB", adbCmd))
                                                Toast.makeText(
                                                    context,
                                                    "Needs system permission. ADB command copied: $adbCmd",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = GetoIcons.Block,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text("Disable", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
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

            val isDebuggable = packageInfo?.applicationInfo?.let {
                it.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            } ?: false

            val isSystemApp = packageInfo?.applicationInfo?.let {
                it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            } ?: false

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
                "Debuggable APK" to if (isDebuggable) "YES ⚠" else "No",
                "System App" to if (isSystemApp) "Yes" else "No",
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
                        color = if (key == "Debuggable APK" && isDebuggable) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
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
                        text = "Full package declaration",
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
                    Text("Copy All")
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
    sb.appendLine("        android:uid=\"$uid\" />")
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
        packageInfo.requestedPermissions?.forEachIndexed { i, perm ->
            val flags = packageInfo.requestedPermissionsFlags?.getOrNull(i) ?: 0
            val granted = if ((flags and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) " <!-- GRANTED -->" else " <!-- DENIED -->"
            sb.appendLine("    <uses-permission android:name=\"$perm\" />$granted")
        }
        sb.appendLine()
    }

    if (packageInfo.activities != null) {
        sb.appendLine("    <!-- Activities -->")
        packageInfo.activities?.forEach { act ->
            sb.appendLine("    <activity android:name=\"${act.name}\" />")
        }
        sb.appendLine()
    }

    if (packageInfo.services != null) {
        sb.appendLine("    <!-- Services -->")
        packageInfo.services?.forEach { svc ->
            sb.appendLine("    <service android:name=\"${svc.name}\" />")
        }
        sb.appendLine()
    }

    sb.appendLine("</manifest>")
    return sb.toString()
}
