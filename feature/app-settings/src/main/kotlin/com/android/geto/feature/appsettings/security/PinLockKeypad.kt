package com.android.geto.feature.appsettings.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.geto.designsystem.icon.GetoIcons

@Composable
fun PinDotRow(
    pin: String,
    maxLength: Int = 6,
    isError: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { i ->
            val filled = i < pin.length
            val targetColor = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
            val dotColor by animateColorAsState(
                targetValue = if (filled) targetColor else targetColor.copy(alpha = 0.2f),
                label = "dot_color_$i",
            )
            val dotSize by animateDpAsState(
                targetValue = if (filled) 16.dp else 13.dp,
                label = "dot_size_$i",
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

@Composable
fun PinNumericKeypad(
    pin: String,
    maxLength: Int = 6,
    onPinChanged: (String) -> Unit,
    onPinComplete: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(68.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (key == "⌫") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (key) {
                                            "⌫" -> if (pin.isNotEmpty()) onPinChanged(pin.dropLast(1))
                                            else -> if (pin.length < maxLength) {
                                                val newPin = pin + key
                                                onPinChanged(newPin)
                                                if (newPin.length == maxLength) onPinComplete(newPin)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = GetoIcons.Close,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
