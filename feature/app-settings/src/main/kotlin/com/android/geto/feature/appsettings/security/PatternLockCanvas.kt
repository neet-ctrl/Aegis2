package com.android.geto.feature.appsettings.security

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sqrt

@Composable
fun PatternLockCanvas(
    modifier: Modifier = Modifier,
    selectedDots: List<Int> = emptyList(),
    isError: Boolean = false,
    primaryColor: Color,
    errorColor: Color,
    surfaceColor: Color,
    onPatternChanged: (List<Int>) -> Unit = {},
    onPatternComplete: (List<Int>) -> Unit = {},
) {
    var dotPositions by remember { mutableStateOf(emptyList<Offset>()) }
    var currentDrag by remember { mutableStateOf<Offset?>(null) }
    val mutableSelected = remember(selectedDots) { selectedDots.toMutableList() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    mutableSelected.clear()
                    currentDrag = offset
                    val nearest = nearestDot(offset, dotPositions)
                    if (nearest >= 0) {
                        mutableSelected.add(nearest)
                        onPatternChanged(mutableSelected.toList())
                    }
                },
                onDrag = { change, _ ->
                    currentDrag = change.position
                    val nearest = nearestDot(change.position, dotPositions)
                    if (nearest >= 0 && nearest !in mutableSelected) {
                        mutableSelected.add(nearest)
                        onPatternChanged(mutableSelected.toList())
                    }
                },
                onDragEnd = {
                    currentDrag = null
                    val completed = mutableSelected.size >= 4
                    if (completed) {
                        onPatternComplete(mutableSelected.toList())
                        mutableSelected.clear()
                    } else {
                        mutableSelected.clear()
                        onPatternChanged(emptyList())
                    }
                },
                onDragCancel = {
                    currentDrag = null
                    mutableSelected.clear()
                    onPatternChanged(emptyList())
                },
            )
        },
    ) {
        val cellW = size.width / 3f
        val cellH = size.height / 3f
        val dotRadius = minOf(cellW, cellH) * 0.1f
        val ringRadius = minOf(cellW, cellH) * 0.2f

        val positions = (0 until 9).map { idx ->
            val row = idx / 3
            val col = idx % 3
            Offset(cellW * col + cellW / 2f, cellH * row + cellH / 2f)
        }
        dotPositions = positions

        val activeColor = if (isError) errorColor else primaryColor

        for (i in 1 until selectedDots.size) {
            val from = positions[selectedDots[i - 1]]
            val to = positions[selectedDots[i]]
            drawLine(
                color = activeColor.copy(alpha = 0.55f),
                start = from,
                end = to,
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
        }

        if (selectedDots.isNotEmpty() && currentDrag != null) {
            drawLine(
                color = activeColor.copy(alpha = 0.3f),
                start = positions[selectedDots.last()],
                end = currentDrag!!,
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }

        positions.forEachIndexed { idx, pos ->
            val isSelected = idx in selectedDots
            val dotColor = if (isSelected) activeColor else surfaceColor
            drawCircle(color = dotColor.copy(alpha = 0.12f), radius = ringRadius, center = pos)
            drawCircle(color = dotColor, radius = dotRadius, center = pos)
            if (isSelected) {
                drawCircle(color = activeColor.copy(alpha = 0.35f), radius = ringRadius * 0.65f, center = pos)
            }
        }
    }
}

private fun nearestDot(pos: Offset, dots: List<Offset>, threshold: Float = 90f): Int {
    var minDist = Float.MAX_VALUE
    var nearest = -1
    dots.forEachIndexed { i, dot ->
        val dx = pos.x - dot.x
        val dy = pos.y - dot.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < threshold && dist < minDist) {
            minDist = dist
            nearest = i
        }
    }
    return nearest
}

fun patternToString(dots: List<Int>): String = dots.joinToString(",")
fun stringToPattern(s: String): List<Int> =
    s.split(",").mapNotNull { it.trim().toIntOrNull() }
