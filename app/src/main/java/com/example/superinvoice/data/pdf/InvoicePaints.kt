package com.example.superinvoice.data.pdf

import android.graphics.Paint

/**
 * Fábrica de `Paint` para o gerador de PDF.
 *
 * O arquivo tinha 47 objetos `Paint` para 9 papéis reais — `bodyPaint`,
 * `tableBodyPaint`, `totalLabelPaint` e `totalsLabelPaint` eram o mesmo
 * objeto descrito quatro vezes, e um deles era alocado dentro do laço de
 * itens. Aqui cada combinação é criada uma vez e reaproveitada.
 *
 * `isAntiAlias` fica desligado por padrão porque é assim que o gerador
 * desenha hoje, e a Fase A precisa sair idêntica ao PDF atual. Ligar é uma
 * mudança visual deliberada, para a fase seguinte.
 */
class InvoicePaints(val style: InvoiceStyle) {

    private val fonts = style.fonts

    enum class Family { Text, Display }

    enum class Weight { Regular, Bold }

    private data class TextKey(
        val size: Float,
        val family: Family,
        val weight: Weight,
        val color: Int,
        val align: Paint.Align,
        val tracking: Float
    )

    private data class StrokeKey(val color: Int, val width: Float)

    private val textCache = HashMap<TextKey, Paint>()
    private val strokeCache = HashMap<StrokeKey, Paint>()
    private val fillCache = HashMap<Int, Paint>()

    /** Texto: corpo, rótulo, valor, ênfase — tudo passa por aqui. */
    fun text(
        size: Float,
        family: Family = Family.Text,
        weight: Weight = Weight.Regular,
        color: Int = style.ink,
        align: Paint.Align = Paint.Align.LEFT,
        tracking: Float = 0f
    ): Paint = textCache.getOrPut(TextKey(size, family, weight, color, align, tracking)) {
        Paint().apply {
            this.color = color
            textSize = size
            typeface = when (family) {
                Family.Text -> when (weight) {
                    Weight.Regular -> fonts.textRegular
                    Weight.Bold -> fonts.textBold
                }

                Family.Display -> when (weight) {
                    Weight.Regular -> fonts.displayRegular
                    Weight.Bold -> fonts.displayBold
                }
            }
            textAlign = align
            if (tracking != 0f) letterSpacing = tracking
        }
    }

    /** Régua e divisor. */
    fun stroke(color: Int = style.rule, width: Float = 1f): Paint =
        strokeCache.getOrPut(StrokeKey(color, width)) {
            Paint().apply {
                this.color = color
                strokeWidth = width
            }
        }

    /** Superfície preenchida — hoje só a barra do cabeçalho do Professional. */
    fun fill(color: Int): Paint = fillCache.getOrPut(color) {
        Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
    }

    /**
     * O avanço de uma linha, ajustado à fonte escolhida.
     *
     * Os avanços de `yPos` do gerador são floats cravados e nenhum deriva
     * do texto. Alguns são deliberadamente mais apertados que a caixa da
     * fonte — a sublinha da descrição do item avança 6f para um texto de
     * 8f, e o wordmark de 48f avança 30f. Por isso o ajuste é
     * proporcional, e não um piso: mede quanto a fonte escolhida é mais
     * alta que a de referência (a do sistema, para a qual o espaçamento
     * foi desenhado) e escala na mesma medida. Com a fonte de referência a
     * razão é exatamente 1 e nada muda.
     */
    fun advance(paint: Paint, designed: Float): Float {
        val reference = referenceLineHeight(paint)
        if (reference <= 0f) return designed
        return designed * (lineHeight(paint) / reference)
    }

    private val referenceCache = HashMap<Long, Float>()

    private fun referenceLineHeight(paint: Paint): Float {
        val bold = paint.typeface?.isBold == true
        val key = paint.textSize.toRawBits().toLong() shl 1 or if (bold) 1L else 0L
        return referenceCache.getOrPut(key) {
            val reference = Paint().apply {
                textSize = paint.textSize
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    if (bold) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
            }
            lineHeight(reference)
        }
    }

    /**
     * Altura de uma linha de texto para esta fonte, no lugar dos avanços
     * de `yPos` cravados em float. Sem isso, trocar a tipografia sobrepõe
     * as linhas.
     */
    fun lineHeight(paint: Paint, multiplier: Float = 1f): Float {
        val metrics = paint.fontMetrics
        return (metrics.descent - metrics.ascent + metrics.leading) * multiplier
    }

    /**
     * Espaço reservado entre um rótulo e seu valor.
     *
     * O gerador usava offsets cegos — `rightX + 100f` no Classic, `+ 80f`
     * no Professional, que é o ponto mais apertado do arquivo. Se a fonte
     * escolhida escrever "Date Issued:" mais largo que isso, o rótulo
     * invade o valor. Aqui o projetado vira um piso.
     */
    fun labelGap(paint: Paint, labels: List<String>, designed: Float): Float {
        val widest = labels.maxOfOrNull { paint.measureText(it) } ?: 0f
        return maxOf(designed, widest + 8f)
    }

    /**
     * Corta o texto com reticências para caber em [maxWidth].
     *
     * As colunas do gerador têm x fixo e nenhuma medição, então nome de
     * produto ou endereço comprido atravessa a coluna vizinha — isso já
     * acontece hoje, antes de qualquer troca de fonte.
     */
    fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val room = maxWidth - paint.measureText(ellipsis)
        if (room <= 0f) return ellipsis
        var end = paint.breakText(text, true, room, null)
        while (end > 0 && text[end - 1] == ' ') end--
        return text.substring(0, end).trimEnd() + ellipsis
    }
}
