package com.android.geto.feature.appsettings.security

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SetupStep { CHOOSE_TYPE, ENTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupSheet(
    packageName: String,
    isChanging: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSetupComplete: (LockType, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(SetupStep.CHOOSE_TYPE) }
    var chosenType by remember { mutableStateOf(LockType.PIN) }
    var pinEnter by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var patternEnter by remember { mutableStateOf(emptyList<Int>()) }
    var patternConfirm by remember { mutableStateOf(emptyList<Int>()) }
    var passwordEnter by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }

    fun reset() {
        pinEnter = ""; pinConfirm = ""; patternEnter = emptyList(); patternConfirm = emptyList()
        passwordEnter = ""; passwordConfirm = ""; isError = false; errorMsg = ""; isConfirmStep = false
    }

    fun tryFinish() {
        when (chosenType) {
            LockType.PIN -> {
                if (pinEnter != pinConfirm) {
                    isError = true; errorMsg = "PINs don't match — try again"
                    scope.launch { delay(700); pinEnter = ""; pinConfirm = ""; isError = false; isConfirmStep = false; errorMsg = "" }
                } else onSetupComplete(LockType.PIN, pinEnter)
            }
            LockType.PATTERN -> {
                if (patternToString(patternEnter) != patternToString(patternConfirm)) {
                    isError = true; errorMsg = "Patterns don't match — try again"
                    scope.launch { delay(700); patternConfirm = emptyList(); isError = false; isConfirmStep = false; errorMsg = "" }
                } else onSetupComplete(LockType.PATTERN, patternToString(patternEnter))
            }
            LockType.PASSWORD -> {
                when {
                    passwordEnter.length < 4 -> { isError = true; errorMsg = "At least 4 characters required" }
                    passwordEnter != passwordConfirm -> {
                        isError = true; errorMsg = "Passwords don't match — try again"
                        scope.launch { delay(700); passwordConfirm = ""; isError = false; isConfirmStep = false; errorMsg = "" }
                    }
                    else -> onSetupComplete(LockType.PASSWORD, passwordEnter)
                }
            }
            else -> {}
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
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isChanging) "Change App Lock" else "Set Up App Lock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (step == SetupStep.CHOOSE_TYPE) 0.4f else if (!isConfirmStep) 0.7f else 1f },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step to isConfirmStep,
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                label = "setup_step",
            ) { (currentStep, inConfirm) ->
                when (currentStep) {
                    SetupStep.CHOOSE_TYPE -> LockTypeChooser(
                        chosen = chosenType,
                        onChoose = { chosenType = it },
                        onContinue = { step = SetupStep.ENTER; reset() },
                    )
                    SetupStep.ENTER -> when (chosenType) {
                        LockType.PIN -> PinSetupStep(
                            pinEnter = pinEnter,
                            pinConfirm = pinConfirm,
                            isConfirmStep = inConfirm,
                            isError = isError,
                            errorMsg = errorMsg,
                            onEnterChanged = { pinEnter = it; isError = false },
                            onConfirmChanged = { pinConfirm = it; isError = false },
                            onEnterComplete = { isConfirmStep = true },
                            onConfirmComplete = { pinConfirm = it; tryFinish() },
                            onBack = { step = SetupStep.CHOOSE_TYPE; reset() },
                            onReEnter = { isConfirmStep = false; pinConfirm = "" },
                        )
                        LockType.PATTERN -> PatternSetupStep(
                            patternEnter = patternEnter,
                            patternConfirm = patternConfirm,
                            isConfirmStep = inConfirm,
                            isError = isError,
                            errorMsg = errorMsg,
                            onEnterChanged = { patternEnter = it; isError = false },
                            onConfirmChanged = { patternConfirm = it; isError = false },
                            onEnterComplete = { patternEnter = it; isConfirmStep = true },
                            onConfirmComplete = { patternConfirm = it; tryFinish() },
                            onBack = { step = SetupStep.CHOOSE_TYPE; reset() },
                            onReEnter = { isConfirmStep = false; patternConfirm = emptyList() },
                        )
                        LockType.PASSWORD -> PasswordSetupStep(
                            passwordEnter = passwordEnter,
                            passwordConfirm = passwordConfirm,
                            isConfirmStep = inConfirm,
                            isError = isError,
                            errorMsg = errorMsg,
                            onEnterChanged = { passwordEnter = it; isError = false },
                            onConfirmChanged = { passwordConfirm = it; isError = false },
                            onContinue = {
                                if (passwordEnter.length >= 4) isConfirmStep = true
                                else { isError = true; errorMsg = "At least 4 characters required" }
                            },
                            onFinish = { tryFinish() },
                            onBack = { step = SetupStep.CHOOSE_TYPE; reset() },
                            onReEnter = { isConfirmStep = false; passwordConfirm = "" },
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun LockTypeChooser(
    chosen: LockType,
    onChoose: (LockType) -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose a lock type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        listOf(LockType.PIN, LockType.PATTERN, LockType.PASSWORD).forEach { type ->
            val selected = chosen == type
            val icon = when (type) {
                LockType.PIN -> GetoIcons.Fingerprint
                LockType.PATTERN -> GetoIcons.Security
                LockType.PASSWORD -> GetoIcons.Lock
                else -> GetoIcons.Shield
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onChoose(type) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Continue") }
    }
}

@Composable
private fun PinSetupStep(
    pinEnter: String,
    pinConfirm: String,
    isConfirmStep: Boolean,
    isError: Boolean,
    errorMsg: String,
    onEnterChanged: (String) -> Unit,
    onConfirmChanged: (String) -> Unit,
    onEnterComplete: (String) -> Unit,
    onConfirmComplete: (String) -> Unit,
    onBack: () -> Unit,
    onReEnter: () -> Unit,
) {
    val currentPin = if (isConfirmStep) pinConfirm else pinEnter
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (isConfirmStep) "Re-enter your PIN to confirm" else "Enter a 6-digit PIN",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
        )
        if (errorMsg.isNotEmpty()) Text(text = errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Start))
        PinDotRow(pin = currentPin, maxLength = 6, isError = isError)
        PinNumericKeypad(
            pin = currentPin,
            maxLength = 6,
            onPinChanged = { if (isConfirmStep) onConfirmChanged(it) else onEnterChanged(it) },
            onPinComplete = { if (isConfirmStep) onConfirmComplete(it) else onEnterComplete(it) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Change type") }
            if (isConfirmStep) TextButton(onClick = onReEnter, modifier = Modifier.weight(1f)) { Text("Re-enter") }
        }
    }
}

@Composable
private fun PatternSetupStep(
    patternEnter: List<Int>,
    patternConfirm: List<Int>,
    isConfirmStep: Boolean,
    isError: Boolean,
    errorMsg: String,
    onEnterChanged: (List<Int>) -> Unit,
    onConfirmChanged: (List<Int>) -> Unit,
    onEnterComplete: (List<Int>) -> Unit,
    onConfirmComplete: (List<Int>) -> Unit,
    onBack: () -> Unit,
    onReEnter: () -> Unit,
) {
    val currentPattern = if (isConfirmStep) patternConfirm else patternEnter
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (isConfirmStep) "Redraw to confirm" else "Draw a pattern (min 4 dots)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
        )
        if (errorMsg.isNotEmpty()) Text(text = errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Start))
        Text(
            text = if (currentPattern.isEmpty()) "Touch and drag through the dots" else "${currentPattern.size} dots selected",
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PatternLockCanvas(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
            selectedDots = currentPattern,
            isError = isError,
            primaryColor = MaterialTheme.colorScheme.primary,
            errorColor = MaterialTheme.colorScheme.error,
            surfaceColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onPatternChanged = { if (isConfirmStep) onConfirmChanged(it) else onEnterChanged(it) },
            onPatternComplete = { if (isConfirmStep) onConfirmComplete(it) else onEnterComplete(it) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Change type") }
            if (isConfirmStep) TextButton(onClick = onReEnter, modifier = Modifier.weight(1f)) { Text("Redraw") }
        }
    }
}

@Composable
private fun PasswordSetupStep(
    passwordEnter: String,
    passwordConfirm: String,
    isConfirmStep: Boolean,
    isError: Boolean,
    errorMsg: String,
    onEnterChanged: (String) -> Unit,
    onConfirmChanged: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onReEnter: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isConfirmStep) { focusRequester.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isConfirmStep) "Confirm your password" else "Create a password",
            style = MaterialTheme.typography.titleMedium,
        )
        if (errorMsg.isNotEmpty()) Text(text = errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(
            value = if (isConfirmStep) passwordConfirm else passwordEnter,
            onValueChange = { if (isConfirmStep) onConfirmChanged(it) else onEnterChanged(it) },
            label = { Text(if (isConfirmStep) "Re-enter password" else "Create password (min 4 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (isConfirmStep) onFinish() else onContinue() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = if (isConfirmStep) onReEnter else onBack, modifier = Modifier.weight(1f)) {
                Text(if (isConfirmStep) "Re-enter" else "Change type")
            }
            Button(
                onClick = if (isConfirmStep) onFinish else onContinue,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (isConfirmStep) "Confirm" else "Continue") }
        }
    }
}
