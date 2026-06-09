package com.android.geto.feature.appsettings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.feature.appsettings.security.AppLockConfig
import com.android.geto.feature.appsettings.security.AppLockManager
import com.android.geto.feature.appsettings.security.AppLockSetupSheet
import com.android.geto.feature.appsettings.security.LockType
import com.android.geto.feature.appsettings.security.LockVerifyResult
import com.android.geto.feature.appsettings.security.PatternLockCanvas
import com.android.geto.feature.appsettings.security.PinDotRow
import com.android.geto.feature.appsettings.security.PinNumericKeypad
import com.android.geto.feature.appsettings.security.patternToString
import kotlinx.coroutines.launch

@Composable
private fun AccessibilityServiceCard(context: Context) {
    val enabledServices = remember {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
    }
    val isEnabled = remember(enabledServices) {
        enabledServices.split(":").any { it.contains("AppLockService", ignoreCase = true) }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GetoIcons.AccessibilityNew,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Aegis Lock is Active" else "Aegis Lock Not Enabled",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = if (isEnabled)
                        "Accessibility service running — app launch interception active"
                    else
                        "Enable the Accessibility Service to activate per-app lock enforcement",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                )
            }
            if (!isEnabled) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.size(width = 80.dp, height = 34.dp),
                ) {
                    Text("Enable", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSecurityTab(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(AppLockManager.getConfig(context, packageName)) }
    var showSetupSheet by rememberSaveable { mutableStateOf(false) }
    var isChanging by rememberSaveable { mutableStateOf(false) }
    var showVerifyBeforeChange by rememberSaveable { mutableStateOf(false) }
    var showVerifyBeforeRemove by rememberSaveable { mutableStateOf(false) }
    val setupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun reload() { config = AppLockManager.getConfig(context, packageName) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccessibilityServiceCard(context = context)

        SecurityStatusCard(config = config)

        if (config.isEnabled || config.hasCredential) {
            SecurityCard(
                icon = GetoIcons.Lock,
                title = "App Lock Active",
                subtitle = "Protected with ${config.lockType.label}",
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { isChanging = true; showVerifyBeforeChange = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Change") }
                    Button(
                        onClick = { showVerifyBeforeRemove = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Remove") }
                }
            }
        } else {
            SecurityCard(
                icon = GetoIcons.LockOpen,
                title = "No Lock Set",
                subtitle = "Tap below to protect this app",
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Button(
                    onClick = { isChanging = false; showSetupSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(GetoIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set Up App Lock")
                }
            }
        }

        SecurityCard(
            icon = GetoIcons.Fingerprint,
            title = "Biometric Unlock",
            subtitle = if (config.isBiometricEnabled) "Fingerprint / Face ID enabled"
            else "Use fingerprint to unlock faster",
            iconTint = if (config.isBiometricEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            iconBackground = if (config.isBiometricEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fingerprint / Face",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = if (config.isEnabled) "Alongside ${config.lockType.label} as fallback"
                        else "Set up a lock first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = config.isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        if (!config.isEnabled && !config.hasCredential) {
                            Toast.makeText(context, "Set up a lock first", Toast.LENGTH_SHORT).show()
                        } else {
                            AppLockManager.setBiometric(context, packageName, enabled)
                            reload()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        SecurityCard(
            icon = GetoIcons.Block,
            title = "Block App",
            subtitle = if (config.isBlocked) "App is completely blocked — no access"
            else "Prevent the app from launching at all",
            iconTint = if (config.isBlocked) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            iconBackground = if (config.isBlocked) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Block Access",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "Returns to home screen immediately on launch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = config.isBlocked,
                    onCheckedChange = { blocked ->
                        AppLockManager.setBlocked(context, packageName, blocked)
                        reload()
                        if (blocked) Toast.makeText(context, "App blocked", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onError,
                        checkedTrackColor = MaterialTheme.colorScheme.error,
                    ),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = GetoIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Aegis uses Android's Accessibility Service to intercept app launches. Enable \"Aegis Lock\" in Android Settings → Accessibility to activate per-app enforcement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showVerifyBeforeChange || showVerifyBeforeRemove) {
        VerifyCredentialSheet(
            packageName = packageName,
            title = if (showVerifyBeforeChange) "Verify to change lock" else "Verify to remove lock",
            lockType = config.lockType,
            onVerified = {
                if (showVerifyBeforeChange) {
                    showVerifyBeforeChange = false
                    showSetupSheet = true
                } else {
                    showVerifyBeforeRemove = false
                    AppLockManager.clearLock(context, packageName)
                    reload()
                    Toast.makeText(context, "App lock removed", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showVerifyBeforeChange = false; showVerifyBeforeRemove = false },
        )
    }

    if (showSetupSheet) {
        AppLockSetupSheet(
            packageName = packageName,
            isChanging = isChanging,
            sheetState = setupSheetState,
            onDismiss = { showSetupSheet = false },
            onSetupComplete = { lockType, credential ->
                AppLockManager.setCredential(context, packageName, credential)
                AppLockManager.saveConfig(
                    context,
                    AppLockConfig(
                        packageName = packageName,
                        lockType = lockType,
                        isEnabled = true,
                        isBiometricEnabled = config.isBiometricEnabled,
                        isBlocked = config.isBlocked,
                        hasCredential = true,
                    ),
                )
                reload()
                scope.launch { setupSheetState.hide() }.invokeOnCompletion { showSetupSheet = false }
                Toast.makeText(context, "${lockType.label} lock saved", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun SecurityStatusCard(config: AppLockConfig) {
    val (bg, icon, tint, label, sub) = when {
        config.isBlocked -> StatusInfo(
            bg = MaterialTheme.colorScheme.errorContainer,
            icon = GetoIcons.Block,
            tint = MaterialTheme.colorScheme.error,
            label = "BLOCKED",
            sub = "App access completely denied",
        )
        config.isEnabled -> StatusInfo(
            bg = MaterialTheme.colorScheme.primaryContainer,
            icon = GetoIcons.Lock,
            tint = MaterialTheme.colorScheme.primary,
            label = "PROTECTED",
            sub = "${config.lockType.label}${if (config.isBiometricEnabled) " + Biometric" else ""}",
        )
        else -> StatusInfo(
            bg = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = GetoIcons.LockOpen,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = "UNPROTECTED",
            sub = "No lock or block active",
        )
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tint,
                )
                Text(text = sub, style = MaterialTheme.typography.bodySmall, color = tint.copy(alpha = 0.8f))
            }
        }
    }
}

private data class StatusInfo(val bg: Color, val icon: ImageVector, val tint: Color, val label: String, val sub: String)

@Composable
private fun SecurityCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    iconBackground: Color,
    content: @Composable () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyCredentialSheet(
    packageName: String,
    title: String,
    lockType: LockType,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(emptyList<Int>()) }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun verify(credential: String) {
        when (AppLockManager.verifyCredential(context, packageName, credential)) {
            LockVerifyResult.Success -> onVerified()
            else -> { isError = true; pin = ""; pattern = emptyList() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            if (isError) {
                Text(
                    text = "Incorrect ${lockType.label} — try again",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            when (lockType) {
                LockType.PIN -> {
                    PinDotRow(pin = pin, maxLength = 6, isError = isError)
                    PinNumericKeypad(
                        pin = pin,
                        maxLength = 6,
                        onPinChanged = { pin = it; isError = false },
                        onPinComplete = { verify(it) },
                    )
                }
                LockType.PATTERN -> {
                    Text(
                        text = if (pattern.isEmpty()) "Draw your pattern" else "${pattern.size} dots",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PatternLockCanvas(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp),
                        selectedDots = pattern,
                        isError = isError,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        errorColor = MaterialTheme.colorScheme.error,
                        surfaceColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onPatternChanged = { pattern = it; isError = false },
                        onPatternComplete = { verify(patternToString(it)) },
                    )
                }
                LockType.PASSWORD -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; isError = false },
                        label = { Text("Enter password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isError,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { verify(password) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { verify(password) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Verify") }
                }
                else -> {}
            }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    }
}
