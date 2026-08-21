package com.example.superinvoice.data.pdf

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument

/**
 * Dono das páginas do PDF.
 *
 * O gerador desenhava numa página só, sem nenhuma checagem de limite:
 * tudo abaixo de y=842 era descartado em silêncio, então uma fatura com
 * muitos itens perdia o total e a assinatura. No Modern era pior — o
 * rodapé é pintado em posição fixa e passava por cima do conteúdo.
 *
 * O [canvas] muda quando a página vira, então quem desenha precisa lê-lo
 * de novo a cada uso, nunca guardar numa `val`.
 */
internal class InvoicePager(
    private val document: PdfDocument,
    private val pageWidth: Int,
    private val pageHeight: Int,
    private val margin: Float,
    /** Desenhado por último em cada página — a marca d'água fica por cima. */
    private val onBeforeFinishPage: ((Canvas) -> Unit)? = null
) {
    /** Última linha utilizável antes da margem inferior. */
    val bottom: Float get() = pageHeight - margin

    /** Onde o conteúdo recomeça numa página nova. */
    val top: Float get() = margin

    var pageCount = 1
        private set

    private var page: PdfDocument.Page = newPage()

    val canvas: Canvas get() = page.canvas

    private fun newPage(): PdfDocument.Page =
        document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create()
        )

    private fun endPage() {
        onBeforeFinishPage?.invoke(page.canvas)
        document.finishPage(page)
    }

    /**
     * Garante [needed] pontos de espaço abaixo de [yPos]. Se não couber,
     * vira a página e devolve o topo da nova — passando por
     * [onContinuation], que é onde o cabeçalho da tabela é repetido.
     */
    fun flowTo(
        yPos: Float,
        needed: Float,
        onContinuation: ((Float) -> Float)? = null
    ): Float {
        if (yPos + needed <= bottom) return yPos
        endPage()
        pageCount++
        page = newPage()
        return onContinuation?.invoke(top) ?: top
    }

    /**
     * Para os rodapés de posição fixa: se o conteúdo já chegou na faixa
     * reservada, abre uma página nova em vez de sobrescrever.
     */
    fun reserveFooter(yPos: Float, height: Float): Float {
        val footerTop = bottom - height
        if (yPos <= footerTop) return footerTop
        endPage()
        pageCount++
        page = newPage()
        return footerTop
    }

    fun finish() {
        endPage()
    }
}
