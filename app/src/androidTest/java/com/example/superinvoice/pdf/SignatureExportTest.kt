package com.example.superinvoice.pdf

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.superinvoice.ui.components.PathState
import com.example.superinvoice.ui.components.toBitmap
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A assinatura é capturada em coordenadas de tela da área de desenho, que
 * é bem maior que o bitmap exportado. Antes os traços eram copiados sem
 * escalar, então tudo abaixo de 300px sumia — e a área de desenho tem
 * ~550px de altura num aparelho comum.
 */
@RunWith(AndroidJUnit4::class)
class SignatureExportTest {

    private fun stroke(x0: Float, y0: Float, x1: Float, y1: Float) = PathState(
        path = Path().apply {
            moveTo(x0, y0)
            lineTo(x1, y1)
        },
        color = Color.Black,
        strokeWidth = 8f
    )

    @Test
    fun keepsInkDrawnBelowTheOldCutoff() {
        // Um traço lá embaixo, na faixa que era descartada.
        val paths = listOf(
            stroke(40f, 60f, 860f, 90f),
            stroke(60f, 430f, 880f, 500f)
        )
        val bitmap = paths.toBitmap(1600, 600)

        val bottomHalfInk = countInk(bitmap, fromY = bitmap.height / 2)
        val topHalfInk = countInk(bitmap, fromY = 0, toY = bitmap.height / 2)

        assertTrue("nada foi desenhado", topHalfInk > 0)
        assertTrue(
            "o traço de baixo foi perdido — era o bug do corte",
            bottomHalfInk > 0
        )
    }

    @Test
    fun fillsTheExportInsteadOfLeavingItMostlyEmpty() {
        // Assinatura pequena num canto: deve ser ampliada até encostar em
        // uma das bordas, sem distorcer — qual das duas depende da
        // proporção do traço.
        val paths = listOf(stroke(10f, 10f, 120f, 60f))
        val bitmap = paths.toBitmap(1600, 600)

        var minX = bitmap.width; var maxX = 0
        var minY = bitmap.height; var maxY = 0
        for (y in 0 until bitmap.height step 4) {
            for (x in 0 until bitmap.width step 4) {
                if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 32) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        val widthCoverage = (maxX - minX).toFloat() / bitmap.width
        val heightCoverage = (maxY - minY).toFloat() / bitmap.height
        val filled = maxOf(widthCoverage, heightCoverage)

        // Antes do conserto o traço saía no tamanho original — ~7% da
        // largura, perdido no canto.
        assertTrue(
            "a assinatura não foi ampliada: %d%% da largura, %d%% da altura".format(
                (widthCoverage * 100).toInt(), (heightCoverage * 100).toInt()
            ),
            filled > 0.95f
        )
    }

    @Test
    fun backgroundIsTransparent() {
        val bitmap = listOf(stroke(100f, 100f, 200f, 200f)).toBitmap(400, 200)
        val corner = bitmap.getPixel(2, 2)
        assertTrue(
            "o fundo opaco aparecia como uma mancha sobre a página branca",
            android.graphics.Color.alpha(corner) == 0
        )
    }

    private fun countInk(
        bitmap: android.graphics.Bitmap,
        fromY: Int = 0,
        toY: Int = bitmap.height
    ): Int {
        var n = 0
        for (y in fromY until toY step 3) {
            for (x in 0 until bitmap.width step 3) {
                if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 32) n++
            }
        }
        return n
    }
}
