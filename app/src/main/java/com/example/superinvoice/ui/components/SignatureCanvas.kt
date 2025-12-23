package com.example.superinvoice.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

data class PathState(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onPathsChanged: (List<PathState>) -> Unit = {}
) {
    val paths = remember { mutableStateListOf<PathState>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    val strokeWidth = with(LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath.clear()
                        currentPath.add(offset)
                    },
                    onDrag = { change, _ ->
                        currentPath.add(change.position)
                    },
                    onDragEnd = {
                        if (currentPath.size > 1) {
                            val path = Path().apply {
                                moveTo(currentPath.first().x, currentPath.first().y)
                                for (i in 1 until currentPath.size) {
                                    lineTo(currentPath[i].x, currentPath[i].y)
                                }
                            }
                            paths.add(PathState(path, Color.Black, strokeWidth))
                            onPathsChanged(paths.toList())
                        }
                        currentPath.clear()
                    }
                )
            }
    ) {
        // Draw all saved paths
        paths.forEach { pathState ->
            drawPath(
                path = pathState.path,
                color = pathState.color,
                style = Stroke(
                    width = pathState.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw current path being drawn
        if (currentPath.size > 1) {
            val path = Path().apply {
                moveTo(currentPath.first().x, currentPath.first().y)
                for (i in 1 until currentPath.size) {
                    lineTo(currentPath[i].x, currentPath[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

fun List<PathState>.toBitmap(width: Int, height: Int): Bitmap {
    val imageBitmap = androidx.compose.ui.graphics.ImageBitmap(width, height)
    val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)

    // Draw white background
    canvas.drawRect(
        0f, 0f, width.toFloat(), height.toFloat(),
        androidx.compose.ui.graphics.Paint().apply {
            color = Color.White
        }
    )

    // Draw all paths
    forEach { pathState ->
        canvas.drawPath(
            path = pathState.path,
            paint = androidx.compose.ui.graphics.Paint().apply {
                color = pathState.color
                strokeWidth = pathState.strokeWidth
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            }
        )
    }

    return imageBitmap.asAndroidBitmap()
}
