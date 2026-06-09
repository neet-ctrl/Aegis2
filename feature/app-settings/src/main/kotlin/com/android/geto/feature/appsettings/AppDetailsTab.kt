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

            val rows = listOf(
                "Package" to packageName,
                "Version Name" to (packageInfo?.versionName ?: "—"),
                "Version Code" to versionCode,
                "Target SDK" to (packageInfo?.applicationInfo?.targetSdkVersion?.toString() ?: "—"),
                "Min SDK" to minSdk,
                "UID" to (packageInfo?.applicationInfo?.uid?.toString() ?: "—"),
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
    val manifestText = buildManifestSummary(packageInfo, packageName)

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
                        text = "Package Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Structured info — tap Copy to export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Package Summary", manifestText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Package summary copied", Toast.LENGTH_SHORT).show()
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

private fun buildManifestSummary(packageInfo: PackageInfo?, packageName: String): String {
    if (packageInfo == null) return "{ \"error\": \"Package info unavailable\" }"
    val permissions = packageInfo.requestedPermissions?.size ?: 0
    val activities = packageInfo.activities?.size ?: 0
    val services = packageInfo.services?.size ?: 0
    val receivers = packageInfo.receivers?.size ?: 0
    val providers = packageInfo.providers?.size ?: 0
    val uid = packageInfo.applicationInfo?.uid ?: 0
    val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 0

    return """package="$packageName"
versionName="${packageInfo.versionName ?: "?"}"
uid="$uid"
targetSdkVersion="$targetSdk"
permissionsCount="$permissions"
activitiesCount="$activities"
servicesCount="$services"
receiversCount="$receivers"
providersCount="$providers""""
}
