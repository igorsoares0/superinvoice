package com.example.superinvoice.data.pdf

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import online.isdevapps.superinvoice.R

/**
 * As tipografias oferecidas para a fatura.
 *
 * [System] é o padrão e reproduz o documento atual. As outras três já
 * estão no APK: Space Grotesk e DM Sans vieram com a migração da
 * interface, e a Crimson Pro entrou aqui por ser a serifada mais estreita
 * entre as candidatas — largura importa porque a coluna de descrição tem
 * pouco mais de 180pt.
 *
 * O [id] é o que vai para o DataStore.
 */
enum class InvoiceFontChoice(val id: String) {
    /** A fonte do aparelho — o documento como sempre foi. */
    System("system"),

    /** Space Grotesk nos títulos, DM Sans no corpo: a cara do app. */
    GroteskSans("grotesk_sans"),

    /** DM Sans em tudo, mais sóbrio. */
    Sans("sans"),

    /** Crimson Pro: serifada, para quem quer o tom de documento formal. */
    Serif("serif");

    fun fonts(context: Context): InvoiceFontSet = when (this) {
        System -> InvoiceFontSet.System

        GroteskSans -> InvoiceFontSet(
            displayRegular = font(context, R.font.space_grotesk_regular),
            displayBold = font(context, R.font.space_grotesk_medium),
            textRegular = font(context, R.font.dm_sans_regular),
            textBold = font(context, R.font.dm_sans_medium)
        )

        Sans -> InvoiceFontSet(
            displayRegular = font(context, R.font.dm_sans_regular),
            displayBold = font(context, R.font.dm_sans_medium),
            textRegular = font(context, R.font.dm_sans_regular),
            textBold = font(context, R.font.dm_sans_medium)
        )

        Serif -> InvoiceFontSet(
            displayRegular = font(context, R.font.crimson_pro_regular),
            displayBold = font(context, R.font.crimson_pro_semibold),
            textRegular = font(context, R.font.crimson_pro_regular),
            textBold = font(context, R.font.crimson_pro_semibold)
        )
    }

    companion object {
        val Default = System

        fun from(id: String?): InvoiceFontChoice =
            entries.firstOrNull { it.id == id } ?: Default
    }
}

/** Se o recurso falhar por qualquer motivo, a fatura sai na fonte do sistema. */
private fun font(context: Context, resId: Int): Typeface =
    runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.DEFAULT
