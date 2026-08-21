package com.example.superinvoice.data.pdf

import android.graphics.Color
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils

/**
 * As fontes usadas para desenhar a fatura.
 *
 * O documento carrega a marca do negócio do usuário, não a da SuperInvoice —
 * por isso ele tem o próprio conjunto tipográfico, independente do
 * `ui/theme/Type.kt`.
 */
data class InvoiceFontSet(
    val displayRegular: Typeface,
    val displayBold: Typeface,
    val textRegular: Typeface,
    val textBold: Typeface
) {
    companion object {
        /** A fonte do sistema — exatamente o que o gerador usa hoje. */
        val System = InvoiceFontSet(
            displayRegular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
            displayBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
            textRegular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL),
            textBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        )
    }
}

/**
 * A aparência do documento: cores e tipografia.
 *
 * [Default] reproduz o PDF atual pixel a pixel — é o padrão de quem nunca
 * abriu o editor, e é a referência do refactor.
 */
data class InvoiceStyle(
    /** Ação e ênfase: barra do cabeçalho da tabela, wordmark, total. */
    val accent: Int,
    /** Texto principal. */
    val ink: Int,
    /** Rótulos e texto de apoio. */
    val muted: Int,
    /** Filetes e divisores. */
    val rule: Int,
    /** Fundo da página. */
    val paper: Int,
    val fonts: InvoiceFontSet
) {
    /**
     * Derivado, nunca configurável: entre preto e branco, o que tiver
     * maior contraste sobre o destaque.
     *
     * Um corte fixo de luminância erra nos tons médios — sobre o laranja
     * da marca o branco dá 4.05:1 e o preto 5.19:1, então o corte
     * escolheria justamente o pior. Comparar os dois sempre acerta, e
     * segue valendo para qualquer cor que entre na paleta depois.
     */
    val onAccent: Int
        get() {
            val luminance = ColorUtils.calculateLuminance(accent)
            val onWhite = (1.0 + 0.05) / (luminance + 0.05)
            val onBlack = (luminance + 0.05) / 0.05
            return if (onBlack > onWhite) Color.BLACK else Color.WHITE
        }

    companion object {
        /** A fatura como ela é hoje: preto sobre branco, fonte do sistema. */
        val Default = InvoiceStyle(
            accent = InvoiceAccent.Default.argb,
            ink = Color.BLACK,
            muted = Color.GRAY,
            rule = Color.LTGRAY,
            paper = Color.WHITE,
            fonts = InvoiceFontSet.System
        )

        /**
         * O que o usuário escolheu. Só o destaque e a tipografia são
         * configuráveis — tinta, apoio e filetes seguem neutros para a
         * fatura continuar legível impressa em preto e branco.
         */
        fun of(
            accent: InvoiceAccent = InvoiceAccent.Default,
            fonts: InvoiceFontSet = InvoiceFontSet.System
        ): InvoiceStyle = Default.copy(accent = accent.argb, fonts = fonts)
    }
}
