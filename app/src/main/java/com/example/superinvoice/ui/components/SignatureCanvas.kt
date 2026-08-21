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
import com.example.superinvoice.ui.theme.Paper
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
            .background(Paper) // só a superfície visível; o bitmap exportado segue igual
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

/**
 * Exporta a assinatura desenhada.
 *
 * Os traços são capturados em coordenadas de tela da área de desenho, que
 * é bem maior que o bitmap de destino — antes eles eram copiados sem
 * escalar, então tudo que passasse de [width] x [height] era simplesmente
 * cortado. Aqui a caixa de tinta é medida e encaixada no destino,
 * preservando a proporção.
 *
 * O fundo sai transparente: a assinatura é colada sobre a página branca do
 * PDF, e um retângulo off-white por baixo dela aparecia como uma mancha.
 */
fun List<PathState>.toBitmap(width: Int, height: Int): Bitmap {
    val imageBitmap = androidx.compose.ui.graphics.ImageBitmap(width, height)
    val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)

    if (isEmpty()) return imageBitmap.asAndroidBitmap()

    // Caixa que envolve toda a tinta, folgada pela espessura do traço para
    // as pontas arredondadas não encostarem na borda.
    val pad = maxOf(1f, maxOf { it.strokeWidth } / 2f)
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    forEach { state ->
        val b = state.path.getBounds()
        if (b.left < left) left = b.left
        if (b.top < top) top = b.top
        if (b.right > right) right = b.right
        if (b.bottom > bottom) bottom = b.bottom
    }
    left -= pad; top -= pad; right += pad; bottom += pad

    val inkWidth = (right - left).coerceAtLeast(1f)
    val inkHeight = (bottom - top).coerceAtLeast(1f)
    val scale = minOf(width / inkWidth, height / inkHeight)

    canvas.save()
    // Centraliza o que sobrar depois de encaixar.
    canvas.translate(
        (width - inkWidth * scale) / 2f,
        (height - inkHeight * scale) / 2f
    )
    canvas.scale(scale, scale)
    canvas.translate(-left, -top)

    forEach { pathState ->
        canvas.drawPath(
            path = pathState.path,
            paint = androidx.compose.ui.graphics.Paint().apply {
                color = pathState.color
                strokeWidth = pathState.strokeWidth
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                isAntiAlias = true
            }
        )
    }
    canvas.restore()

    return imageBitmap.asAndroidBitmap()
}
