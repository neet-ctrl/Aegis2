package com.android.geto.feature.appsettings

import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.geto.designsystem.icon.GetoIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AppInfoHeader(
    modifier: Modifier = Modifier,
    icon: ByteArray?,
    label: String,
    packageName: String,
    packageInfo: PackageInfo?,
) {
    val uid = packageInfo?.applicationInfo?.uid ?: 0
    val versionName = packageInfo?.versionName ?: "—"
    val targetSdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0
    val installDate = packageInfo?.firstInstallTime?.let { formatDate(it) } ?: "—"
    val updateDate = packageInfo?.lastUpdateTime?.let { formatDate(it) } ?: "—"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    AsyncImage(
                        model = icon,
                        contentDescription = label,
                        modifier = Modifier.size(52.dp),
                    )
                } else {
                    Icon(
                        imageVector = GetoIcons.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppInfoChip(label = "UID $uid")
                    AppInfoChip(label = "v$versionName")
                    AppInfoChip(label = "SDK $targetSdk")
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(
                            text = "Installed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = installDate,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column {
                        Text(
                            text = "Updated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = updateDate,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
internal fun AppActionButtons(
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit,
    onAppSystemSettings: () -> Unit,
    onShareApp: () -> Unit,
    onSaveIcon: () -> Unit,
    onSaveApk: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = { Icon(GetoIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = "Open",
            onClick = onOpenApp,
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = { Icon(GetoIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = "System",
            onClick = onAppSystemSettings,
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = { Icon(GetoIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = "Share",
            onClick = onShareApp,
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = { Icon(GetoIcons.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = "Icon",
            onClick = onSaveIcon,
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = { Icon(GetoIcons.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = "APK",
            onClick = onSaveApk,
        )
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 10.dp,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
