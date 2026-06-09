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

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.IconCompat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.android.geto.broadcastreceiver.RevertSettingsBroadcastReceiver
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.domain.model.AddAppSettingResult
import com.android.geto.domain.model.AddAppSettingResult.FAILED
import com.android.geto.domain.model.AddAppSettingResult.SUCCESS
import com.android.geto.domain.model.AppSetting
import com.android.geto.domain.model.AppSettingTemplate
import com.android.geto.domain.model.AppSettingsResult
import com.android.geto.domain.model.AppSettingsResult.DisabledAppSettings
import com.android.geto.domain.model.AppSettingsResult.EmptyAppSettings
import com.android.geto.domain.model.AppSettingsResult.Failure
import com.android.geto.domain.model.AppSettingsResult.InvalidValues
import com.android.geto.domain.model.AppSettingsResult.NoPermission
import com.android.geto.domain.model.AppSettingsResult.Success
import com.android.geto.domain.model.RequestPinShortcutResult
import com.android.geto.domain.model.RequestPinShortcutResult.SupportedLauncher
import com.android.geto.domain.model.RequestPinShortcutResult.UnsupportedLauncher
import com.android.geto.domain.model.RequestPinShortcutResult.UpdateFailure
import com.android.geto.domain.model.RequestPinShortcutResult.UpdateImmutableShortcuts
import com.android.geto.domain.model.RequestPinShortcutResult.UpdateSuccess
import com.android.geto.domain.model.SecureSetting
import com.android.geto.domain.model.SettingType
import com.android.geto.feature.appsettings.dialog.AppSettingDialog
import com.android.geto.feature.appsettings.dialog.ShortcutDialog
import com.android.geto.feature.appsettings.dialog.TemplateDialog
import com.android.geto.feature.appsettings.dialog.WriteSecureSettingsDialog
import com.android.geto.feature.appsettings.navigation.AppSettingsRouteData
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.ACTION_REVERT_SETTINGS
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.NOTIFICATION_EXTRA_COMPONENT_NAME
import com.android.geto.framework.notificationmanager.AndroidNotificationManagerWrapper.Companion.NOTIFICATION_EXTRA_NOTIFICATION_ID
import com.android.geto.ui.local.LocalLauncherApps
import com.android.geto.ui.local.LocalNotificationManager
import kotlinx.coroutines.FlowPreview

@Composable
internal fun AppSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = hiltViewModel(),
    appSettingsRouteData: AppSettingsRouteData,
    onNavigationIconClick: () -> Unit,
) {
    val appSettingsUiState by viewModel.appSettingsUiState.collectAsStateWithLifecycle()
    val secureSettings by viewModel.secureSettings.collectAsStateWithLifecycle()
    val applyAppSettingsResult by viewModel.applyAppSettingsResult.collectAsStateWithLifecycle()
    val revertAppSettingsResult by viewModel.revertAppSettingsResult.collectAsStateWithLifecycle()
    val addAppSettingResult by viewModel.addAppSettingsResult.collectAsStateWithLifecycle()
    val activityIcon by viewModel.activityIcon.collectAsStateWithLifecycle()
    val requestPinShortcutResult by viewModel.requestPinShortcutResult.collectAsStateWithLifecycle()
    val appSettingTemplates by viewModel.appSettingTemplates.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    AppSettingsScreen(
        modifier = modifier,
        appSettingsRouteData = appSettingsRouteData,
        appSettingsUiState = appSettingsUiState,
        snackbarHostState = snackbarHostState,
        activityIcon = activityIcon,
        secureSettings = secureSettings,
        addAppSettingResult = addAppSettingResult,
        applyAppSettingsResult = applyAppSettingsResult,
        revertAppSettingsResult = revertAppSettingsResult,
        requestPinShortcutResult = requestPinShortcutResult,
        appSettingTemplates = appSettingTemplates,
        onApplyAppSettings = viewModel::applyAppSettings,
        onRevertAppSettings = viewModel::revertAppSettings,
        onCheckAppSetting = viewModel::checkAppSetting,
        onDeleteAppSetting = viewModel::deleteAppSetting,
        onAddAppSetting = viewModel::addAppSetting,
        onRequestPinShortcut = viewModel::requestPinShortcut,
        onGetSecureSettingsByName = viewModel::getSecureSettingsByName,
        onResetApplyAppSettingsResult = viewModel::resetApplyAppSettingsResult,
        onResetRequestPinShortcutResult = viewModel::resetRequestPinShortcutResult,
        onResetRevertAppSettingsResult = viewModel::resetRevertAppSettingsResult,
        onResetAddAppSettingResult = viewModel::resetAddAppSettingResult,
        onNavigationIconClick = onNavigationIconClick,
    )
}

@VisibleForTesting
@Composable
internal fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    appSettingsRouteData: AppSettingsRouteData,
    appSettingsUiState: AppSettingsUiState,
    snackbarHostState: SnackbarHostState,
    activityIcon: ByteArray?,
    secureSettings: List<SecureSetting>,
    addAppSettingResult: AddAppSettingResult?,
    applyAppSettingsResult: AppSettingsResult?,
    revertAppSettingsResult: AppSettingsResult?,
    requestPinShortcutResult: RequestPinShortcutResult?,
    appSettingTemplates: List<AppSettingTemplate>,
    onApplyAppSettings: () -> Unit,
    onRevertAppSettings: () -> Unit,
    onCheckAppSetting: (appSetting: AppSetting) -> Unit,
    onDeleteAppSetting: (appSetting: AppSetting) -> Unit,
    onAddAppSetting: (AppSetting) -> Unit,
    onRequestPinShortcut: (
        icon: ByteArray?,
        shortLabel: String,
        longLabel: String,
    ) -> Unit,
    onGetSecureSettingsByName: (settingType: SettingType, text: String) -> Unit,
    onResetApplyAppSettingsResult: () -> Unit,
    onResetRequestPinShortcutResult: () -> Unit,
    onResetRevertAppSettingsResult: () -> Unit,
    onResetAddAppSettingResult: () -> Unit,
    onNavigationIconClick: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAppSettingDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showWriteSecureSettingsDialog by remember { mutableStateOf(false) }
    var prefillKey by remember { mutableStateOf("") }
    var prefillSettingTypeName by remember { mutableStateOf("SYSTEM") }
    var prefillLabel by remember { mutableStateOf("") }

    val packageName = remember(appSettingsRouteData.componentName) {
        appSettingsRouteData.componentName.substringBefore("/")
    }

    val packageInfo: PackageInfo? = remember(packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_PERMISSIONS or
                            PackageManager.GET_ACTIVITIES or
                            PackageManager.GET_SERVICES or
                            PackageManager.GET_RECEIVERS or
                            PackageManager.GET_PROVIDERS).toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    val saveIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri != null && activityIcon != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(activityIcon) }
                Toast.makeText(context, "Icon saved", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to save icon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
    ) { uri ->
        if (uri != null) {
            val apkPath = packageInfo?.applicationInfo?.let { it.publicSourceDir ?: it.sourceDir }
            if (apkPath != null) {
                try {
                    java.io.File(apkPath).inputStream().use { input ->
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    Toast.makeText(context, "APK saved successfully", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to save APK", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "APK path unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppSettingsLaunchedEffects(
        appSettingsRouteData = appSettingsRouteData,
        snackbarHostState = snackbarHostState,
        activityIcon = activityIcon,
        addAppSettingResult = addAppSettingResult,
        applyAppSettingsResult = applyAppSettingsResult,
        revertAppSettingsResult = revertAppSettingsResult,
        requestPinShortcutResult = requestPinShortcutResult,
        onResetApplyAppSettingsResult = onResetApplyAppSettingsResult,
        onResetRevertAppSettingsResult = onResetRevertAppSettingsResult,
        onResetRequestPinShortcutResult = onResetRequestPinShortcutResult,
        onResetAddAppSettingResult = onResetAddAppSettingResult,
        onShowWriteSecureSettingsDialog = { showWriteSecureSettingsDialog = true },
    )

    AppSettingsDialogs(
        appSettingTemplates = appSettingTemplates,
        componentName = appSettingsRouteData.componentName,
        icon = activityIcon,
        appSettings = (appSettingsUiState as? AppSettingsUiState.Success)?.appSettings ?: emptyList(),
        appLabel = appSettingsRouteData.activityLabel,
        secureSettings = secureSettings,
        showAppSettingDialog = showAppSettingDialog,
        showShortcutDialog = showShortcutDialog,
        showTemplateDialog = showTemplateDialog,
        showWriteSecureSettingsDialog = showWriteSecureSettingsDialog,
        prefillKey = prefillKey,
        prefillSettingTypeName = prefillSettingTypeName,
        prefillLabel = prefillLabel,
        onAddAppSetting = onAddAppSetting,
        onDismissAppSettingDialog = { showAppSettingDialog = false },
        onDismissShortcutDialog = { showShortcutDialog = false },
        onDismissTemplateDialog = { showTemplateDialog = false },
        onDismissWriteSecureSettingsDialog = { showWriteSecureSettingsDialog = false },
        onGetSecureSettingsByName = onGetSecureSettingsByName,
        onRequestPinShortcut = onRequestPinShortcut,
    )

    Scaffold(
        topBar = {
            AppSettingsTopAppBar(
                title = appSettingsRouteData.activityLabel,
                onNavigationIconClick = onNavigationIconClick,
                onSaveIconClick = {
                    saveIconLauncher.launch(
                        "${appSettingsRouteData.activityLabel.replace(" ", "_")}_icon.png",
                    )
                },
                onSaveApkClick = {
                    saveApkLauncher.launch(
                        "${appSettingsRouteData.activityLabel.replace(" ", "_")}.apk",
                    )
                },
                onShareClick = {
                    val apkPath = packageInfo?.applicationInfo?.let { it.publicSourceDir ?: it.sourceDir }
                    if (apkPath != null) {
                        try {
                            val apkFile = java.io.File(apkPath)
                            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile,
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share APK — ${appSettingsRouteData.activityLabel}"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot share APK: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "APK path unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        },
        bottomBar = {
            if (selectedTab == 0) {
                AppSettingsBottomAppBar(
                    onRefreshIconClick = onRevertAppSettings,
                    onSettingsIconClick = {
                        prefillKey = ""
                        prefillSettingTypeName = "SYSTEM"
                        prefillLabel = ""
                        showAppSettingDialog = true
                    },
                    onShortcutIconClick = { showShortcutDialog = true },
                    onSettingsSuggestIconClick = { showTemplateDialog = true },
                    onFloatingActionButtonClick = onApplyAppSettings,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
        ) {
            AppInfoHeader(
                icon = activityIcon,
                label = appSettingsRouteData.activityLabel,
                packageName = packageName,
                packageInfo = packageInfo,
            )

            AppActionButtons(
                onOpenApp = {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.let {
                        context.startActivity(it)
                    } ?: Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                },
                onAppSystemSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        },
                    )
                },
                onShareApp = {
                    val apkPath = packageInfo?.applicationInfo?.let { it.publicSourceDir ?: it.sourceDir }
                    if (apkPath != null) {
                        try {
                            val apkFile = java.io.File(apkPath)
                            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile,
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share APK — ${appSettingsRouteData.activityLabel}"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot share APK: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "APK path unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                onSaveIcon = {
                    saveIconLauncher.launch(
                        "${appSettingsRouteData.activityLabel.replace(" ", "_")}_icon.png",
                    )
                },
                onSaveApk = {
                    saveApkLauncher.launch(
                        "${appSettingsRouteData.activityLabel.replace(" ", "_")}.apk",
                    )
                },
            )

            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
            ) {
                listOf("Rules", "Details", "Controls", "Security", "Detect").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (selectedTab) {
                    0 -> when (appSettingsUiState) {
                        AppSettingsUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        is AppSettingsUiState.Success -> {
                            if (appSettingsUiState.appSettings.isNotEmpty()) {
                                SuccessState(
                                    appSettingsUiState = appSettingsUiState,
                                    onCheckAppSetting = onCheckAppSetting,
                                    onDeleteAppSettingsItem = onDeleteAppSetting,
                                )
                            } else {
                                RulesEmptyGuide()
                            }
                        }
                    }
                    1 -> AppDetailsTab(
                        packageName = packageName,
                        packageInfo = packageInfo,
                    )
                    2 -> AppControlsTab(
                        onAddRule = { key, typeName, label ->
                            prefillKey = key
                            prefillSettingTypeName = typeName
                            prefillLabel = label
                            showAppSettingDialog = true
                        },
                    )
                    3 -> AppSecurityTab(
                        packageName = packageName,
                    )
                    4 -> EnvironmentDetectionTab(
                        packageName = packageName,
                    )
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun AppSettingsLaunchedEffects(
    appSettingsRouteData: AppSettingsRouteData,
    snackbarHostState: SnackbarHostState,
    activityIcon: ByteArray?,
    addAppSettingResult: AddAppSettingResult?,
    applyAppSettingsResult: AppSettingsResult?,
    revertAppSettingsResult: AppSettingsResult?,
    requestPinShortcutResult: RequestPinShortcutResult?,
    onResetApplyAppSettingsResult: () -> Unit,
    onResetRevertAppSettingsResult: () -> Unit,
    onResetRequestPinShortcutResult: () -> Unit,
    onResetAddAppSettingResult: () -> Unit,
    onShowWriteSecureSettingsDialog: () -> Unit,
) {
    val context = LocalContext.current
    val androidLauncherAppsWrapper = LocalLauncherApps.current
    val androidNotificationManagerWrapper = LocalNotificationManager.current

    val appSettingsDisabled = stringResource(id = R.string.app_settings_disabled)
    val emptyAppSettingsList = stringResource(id = R.string.empty_app_settings_list)
    val getoSettings = stringResource(id = R.string.geto_settings)
    val applySuccess = stringResource(id = R.string.apply_success)
    val applyFailure = stringResource(id = R.string.apply_failure)
    val revertFailure = stringResource(id = R.string.revert_failure)
    val revertSuccess = stringResource(id = R.string.revert_success)
    val shortcutUpdateImmutableShortcuts = stringResource(id = R.string.shortcut_update_immutable_shortcuts)
    val shortcutUpdateFailed = stringResource(id = R.string.shortcut_update_failed)
    val shortcutUpdateSuccess = stringResource(id = R.string.shortcut_update_success)
    val supportedLauncher = stringResource(id = R.string.supported_launcher)
    val unsupportedLauncher = stringResource(id = R.string.unsupported_launcher)
    val invalidValues = stringResource(R.string.settings_has_invalid_values)
    val appSettingAddSuccess = stringResource(R.string.app_setting_added_successfully)
    val appSettingAddFailed = stringResource(R.string.app_setting_already_exists)

    LaunchedEffect(key1 = applyAppSettingsResult) {
        when (applyAppSettingsResult) {
            DisabledAppSettings -> snackbarHostState.showSnackbar(message = appSettingsDisabled)
            EmptyAppSettings -> snackbarHostState.showSnackbar(message = emptyAppSettingsList)
            Failure -> snackbarHostState.showSnackbar(message = applyFailure)
            NoPermission -> onShowWriteSecureSettingsDialog()
            Success -> {
                val notificationId = appSettingsRouteData.componentName.hashCode()
                androidNotificationManagerWrapper.notify(
                    id = notificationId,
                    notification = getNotification(
                        context = context,
                        notificationId = notificationId,
                        componentName = appSettingsRouteData.componentName,
                        icon = activityIcon,
                        contentTitle = getoSettings,
                        contentText = applySuccess,
                    ),
                )
                val packageName = appSettingsRouteData.componentName.substringBefore("/")
                context.getSharedPreferences("aegis_pending_revert", Context.MODE_PRIVATE)
                    .edit()
                    .putString(packageName, "${appSettingsRouteData.componentName}|$notificationId")
                    .apply()
                androidLauncherAppsWrapper.startMainActivity(componentName = appSettingsRouteData.componentName)
            }
            InvalidValues -> snackbarHostState.showSnackbar(message = invalidValues)
            null -> Unit
        }
        onResetApplyAppSettingsResult()
    }

    LaunchedEffect(key1 = revertAppSettingsResult) {
        when (revertAppSettingsResult) {
            DisabledAppSettings -> snackbarHostState.showSnackbar(message = appSettingsDisabled)
            EmptyAppSettings -> snackbarHostState.showSnackbar(message = emptyAppSettingsList)
            Failure -> snackbarHostState.showSnackbar(message = revertFailure)
            NoPermission -> onShowWriteSecureSettingsDialog()
            Success -> {
                snackbarHostState.showSnackbar(message = revertSuccess)
                val packageName = appSettingsRouteData.componentName.substringBefore("/")
                context.getSharedPreferences("aegis_pending_revert", Context.MODE_PRIVATE)
                    .edit()
                    .remove(packageName)
                    .apply()
            }
            InvalidValues -> snackbarHostState.showSnackbar(message = invalidValues)
            null -> Unit
        }
        onResetRevertAppSettingsResult()
    }

    LaunchedEffect(key1 = requestPinShortcutResult) {
        when (requestPinShortcutResult) {
            SupportedLauncher -> snackbarHostState.showSnackbar(message = supportedLauncher)
            UnsupportedLauncher -> snackbarHostState.showSnackbar(message = unsupportedLauncher)
            UpdateFailure -> snackbarHostState.showSnackbar(message = shortcutUpdateFailed)
            UpdateSuccess -> snackbarHostState.showSnackbar(message = shortcutUpdateSuccess)
            UpdateImmutableShortcuts -> snackbarHostState.showSnackbar(message = shortcutUpdateImmutableShortcuts)
            null -> Unit
        }
        onResetRequestPinShortcutResult()
    }

    LaunchedEffect(key1 = addAppSettingResult) {
        when (addAppSettingResult) {
            SUCCESS -> snackbarHostState.showSnackbar(message = appSettingAddSuccess)
            FAILED -> snackbarHostState.showSnackbar(message = appSettingAddFailed)
            null -> Unit
        }
        onResetAddAppSettingResult()
    }
}

@Composable
private fun AppSettingsDialogs(
    appSettingTemplates: List<AppSettingTemplate>,
    componentName: String,
    icon: ByteArray?,
    appSettings: List<AppSetting>,
    appLabel: String,
    secureSettings: List<SecureSetting>,
    showAppSettingDialog: Boolean,
    showShortcutDialog: Boolean,
    showTemplateDialog: Boolean,
    showWriteSecureSettingsDialog: Boolean,
    prefillKey: String,
    prefillSettingTypeName: String,
    prefillLabel: String,
    onAddAppSetting: (AppSetting) -> Unit,
    onDismissAppSettingDialog: () -> Unit,
    onDismissShortcutDialog: () -> Unit,
    onDismissTemplateDialog: () -> Unit,
    onDismissWriteSecureSettingsDialog: () -> Unit,
    onGetSecureSettingsByName: (settingType: SettingType, text: String) -> Unit,
    onRequestPinShortcut: (ByteArray?, String, String) -> Unit,
) {
    if (showAppSettingDialog) {
        val initialTypeIndex = SettingType.entries.indexOfFirst { it.name == prefillSettingTypeName }.coerceAtLeast(0)
        AppSettingDialog(
            componentName = componentName,
            secureSettings = secureSettings,
            initialLabel = prefillLabel,
            initialKey = prefillKey,
            initialSettingTypeIndex = initialTypeIndex,
            onAddAppSetting = onAddAppSetting,
            onDismissRequest = onDismissAppSettingDialog,
            onGetSecureSettingsByName = onGetSecureSettingsByName,
        )
    }
    if (showShortcutDialog) {
        ShortcutDialog(
            icon = icon,
            appLabel = appLabel,
            appSettings = appSettings,
            onDismissRequest = onDismissShortcutDialog,
            onRequestPinShortcut = onRequestPinShortcut,
        )
    }
    if (showTemplateDialog) {
        TemplateDialog(
            appSettingTemplates = appSettingTemplates,
            componentName = componentName,
            onAddAppSetting = onAddAppSetting,
            onDismissRequest = onDismissTemplateDialog,
        )
    }
    if (showWriteSecureSettingsDialog) {
        WriteSecureSettingsDialog(onDismissRequest = onDismissWriteSecureSettingsDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    onNavigationIconClick: () -> Unit,
    onSaveIconClick: () -> Unit,
    onSaveApkClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = GetoIcons.Back,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = GetoIcons.ArrowForward,
                    contentDescription = "Share APK file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSaveIconClick) {
                Icon(
                    imageVector = GetoIcons.SaveAlt,
                    contentDescription = "Save app icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSaveApkClick) {
                Icon(
                    imageVector = GetoIcons.FileDownload,
                    contentDescription = "Save APK file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun AppSettingsBottomAppBar(
    onRefreshIconClick: () -> Unit,
    onSettingsIconClick: () -> Unit,
    onShortcutIconClick: () -> Unit,
    onSettingsSuggestIconClick: () -> Unit,
    onFloatingActionButtonClick: () -> Unit,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            AppSettingsBottomAppBarActions(
                onRefreshIconClick = onRefreshIconClick,
                onSettingsIconClick = onSettingsIconClick,
                onShortcutIconClick = onShortcutIconClick,
                onSettingsSuggestIconClick = onSettingsSuggestIconClick,
            )
        },
        floatingActionButton = {
            AppSettingsFloatingActionButton(onClick = onFloatingActionButtonClick)
        },
    )
}

@Composable
private fun AppSettingsFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            imageVector = GetoIcons.PlayArrow,
            contentDescription = "Apply settings",
        )
    }
}

@Composable
private fun AppSettingsBottomAppBarActions(
    onRefreshIconClick: () -> Unit,
    onSettingsIconClick: () -> Unit,
    onShortcutIconClick: () -> Unit,
    onSettingsSuggestIconClick: () -> Unit,
) {
    IconButton(onClick = onRefreshIconClick) {
        Icon(
            imageVector = GetoIcons.Refresh,
            contentDescription = "Revert settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IconButton(onClick = onSettingsIconClick) {
        Icon(
            imageVector = GetoIcons.Add,
            contentDescription = "Add setting",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IconButton(onClick = onShortcutIconClick) {
        Icon(
            imageVector = GetoIcons.Shortcut,
            contentDescription = "Add shortcut",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IconButton(onClick = onSettingsSuggestIconClick) {
        Icon(
            imageVector = GetoIcons.SettingsSuggest,
            contentDescription = "Suggest template",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuccessState(
    appSettingsUiState: AppSettingsUiState.Success,
    onCheckAppSetting: (AppSetting) -> Unit,
    onDeleteAppSettingsItem: (AppSetting) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = appSettingsUiState.appSettings) { appSetting ->
            AppSettingItem(
                appSetting = appSetting,
                onCheckAppSetting = onCheckAppSetting,
                onDeleteAppSetting = onDeleteAppSettingsItem,
            )
        }
    }
}

@Composable
private fun LazyItemScope.AppSettingItem(
    appSetting: AppSetting,
    onCheckAppSetting: (AppSetting) -> Unit,
    onDeleteAppSetting: (AppSetting) -> Unit,
) {
    ListItem(
        modifier = Modifier.animateItem(),
        headlineContent = {
            Text(
                text = appSetting.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
        },
        supportingContent = {
            Text(
                text = appSetting.key,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Checkbox(
                checked = appSetting.enabled,
                onCheckedChange = { onCheckAppSetting(appSetting) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        trailingContent = {
            IconButton(onClick = { onDeleteAppSetting(appSetting) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun RulesEmptyGuide(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = GetoIcons.SettingsSuggest,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Rules Yet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap + in the bottom bar to add your first rule",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "What is a Rule?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A rule changes a system setting when you launch this app (▶ Apply) and restores it when you revert (↺). For example: set brightness to 50 when you open a reading app, then restore it to 128 when done.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item {
            Text(
                text = "FIELDS EXPLAINED",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        val fields = listOf(
            Triple("Label", "A friendly name you choose — shown in the list.", "e.g.  Low brightness for reading"),
            Triple("Setting Type", "SYSTEM = general device settings\nSECURE = privacy / accessibility settings\nGLOBAL = device-wide / network settings", "e.g.  SYSTEM for brightness, SECURE for location"),
            Triple("Key", "The actual Android setting name. Type to search — the dropdown shows matching keys and fills the current value automatically.", "e.g.  screen_brightness"),
            Triple("Value on Launch", "What the setting changes TO when you tap ▶ Apply.", "e.g.  50  (dim the screen)"),
            Triple("Value on Revert", "What the setting restores TO when you tap ↺. Auto-filled when you pick a key from the dropdown.", "e.g.  128  (your normal brightness)"),
        )

        items(fields) { (name, desc, example) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Bottom Bar Buttons",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    listOf(
                        "▶  (Play)" to "Apply all enabled rules and launch the app",
                        "↺  (Refresh)" to "Revert all settings to their original values",
                        "+  (Add)" to "Open the rule dialog to add a new setting rule",
                        "⊞  (Shortcut)" to "Pin a launcher shortcut that applies rules on tap",
                        "✦  (Templates)" to "Pick from pre-built rule templates to add quickly",
                        "☑  (Checkbox)" to "Enable or disable a rule without deleting it",
                        "🗑  (Delete)" to "Permanently remove a rule",
                    ).forEach { (btn, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = btn,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.width(84.dp),
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Full Example — Dim screen for YouTube",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    val rows = listOf(
                        "Label" to "Dim for YouTube",
                        "Type" to "SYSTEM",
                        "Key" to "screen_brightness",
                        "Value on Launch" to "40",
                        "Value on Revert" to "180",
                    )
                    rows.forEach { (field, value) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = field,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.width(120.dp),
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Result: tapping ▶ dims the screen to 40 and opens YouTube. Tapping ↺ restores brightness to 180.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private fun getNotification(
    context: Context,
    notificationId: Int,
    componentName: String,
    icon: ByteArray?,
    contentTitle: String,
    contentText: String,
): Notification {
    val revertIntent = Intent(context, RevertSettingsBroadcastReceiver::class.java).apply {
        action = ACTION_REVERT_SETTINGS
        putExtra(NOTIFICATION_EXTRA_COMPONENT_NAME, componentName)
        putExtra(NOTIFICATION_EXTRA_NOTIFICATION_ID, notificationId)
    }

    val revertPendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        revertIntent,
        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
    )

    return NotificationCompat.Builder(
        context,
        AndroidNotificationManagerWrapper.NOTIFICATION_CHANNEL_ID,
    )
        .setSmallIcon(
            if (icon != null) {
                IconCompat.createWithData(icon, 0, icon.size)
            } else {
                IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info)
            },
        )
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .addAction(
            android.R.drawable.ic_menu_revert,
            "Revert",
            revertPendingIntent,
        )
        .setAutoCancel(true)
        .build()
}
