package com.android.geto.lock

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.designsystem.theme.GetoTheme
import com.android.geto.feature.appsettings.security.AppLockManager
import com.android.geto.feature.appsettings.security.LockType
import com.android.geto.feature.appsettings.security.LockVerifyResult
import com.android.geto.feature.appsettings.security.PatternLockCanvas
import com.android.geto.feature.appsettings.security.PinDotRow
import com.android.geto.feature.appsettings.security.PinNumericKeypad
import com.android.geto.feature.appsettings.security.patternToString

class AppLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }

        val lockType = AppLockManager.getLockType(this, packageName)
        val isBlocked = AppLockManager.getConfig(this, packageName).isBlocked

        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            GetoTheme {
                if (isBlocked) {
                    BlockedScreen(
                        packageName = packageName,
                        onExit = { moveTaskToBack(true); finish() },
                    )
                } else {
                    LockScreen(
                        packageName = packageName,
                        lockType = lockType,
                        onUnlocked = {
                            AppLockService.addUnlocked(packageName)
                            finish()
                        },
                    )
                }
            }
        }

        if (!isBlocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            AppLockManager.isBiometricEnabled(this, packageName)
        ) {
            triggerBiometric(packageName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun triggerBiometric(packageName: String) {
        val prompt = BiometricPrompt.Builder(this)
            .setTitle("Unlock ${packageName.substringAfterLast(".")}")
            .setSubtitle("Use biometric to unlock")
            .setNegativeButton("Use ${AppLockManager.getLockType(this, packageName).label}", mainExecutor) { _, _ -> }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockService.addUnlocked(packageName)
                    finish()
                }
                override fun onAuthenticationFailed() {
                    Toast.makeText(this@AppLockActivity, "Biometric failed — use ${AppLockManager.getLockType(this@AppLockActivity, packageName).label}", Toast.LENGTH_SHORT).show()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
            },
        )
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_locked_package"

        fun createIntent(context: Context, packageName: String): Intent =
            Intent(context, AppLockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}

@Composable
private fun LockScreen(
    packageName: String,
    lockType: LockType,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(emptyList<Int>()) }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun verify(credential: String) {
        when (AppLockManager.verifyCredential(context, packageName, credential)) {
            LockVerifyResult.Success -> onUnlocked()
            else -> { isError = true; pin = ""; pattern = emptyList() }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GetoIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "This app is protected by Aegis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isError) {
                Text(
                    text = "Incorrect ${lockType.label} — try again",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(20.dp),
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
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isError,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { verify(password) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Unlock") }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun BlockedScreen(
    packageName: String,
    onExit: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GetoIcons.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "App Blocked",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "This app has been blocked by Aegis. Unblock it in Aegis → App Settings → Security.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Go Back")
            }
        }
    }
}
