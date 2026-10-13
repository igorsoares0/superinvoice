package com.example.superinvoice.util

import android.content.Context
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.payment.PaymentLinks
import online.isdevapps.superinvoice.R

/** Assunto, texto e título do seletor ao compartilhar o PDF de uma fatura ou orçamento. */
data class ShareTexts(val subject: String, val text: String, val chooserTitle: String)

/**
 * Até a Onda 1 esses textos eram fixos em inglês ("Invoice INV-001"), mesmo com o app em
 * português ou espanhol, e o e-mail chegava ao cliente assim.
 */
fun shareTextsFor(
    context: Context,
    invoice: Invoice,
    links: PaymentLinks = PaymentLinks.NONE
): ShareTexts =
    if (invoice.type == DocumentType.ESTIMATE) {
        ShareTexts(
            subject = context.getString(R.string.share_estimate_subject, invoice.number),
            text = context.getString(R.string.share_estimate_text, invoice.number),
            chooserTitle = context.getString(R.string.share_estimate_chooser)
        )
    } else {
        ShareTexts(
            subject = context.getString(R.string.share_invoice_subject, invoice.number),
            text = context.getString(R.string.share_invoice_text, invoice.number) +
                paymentLinesFor(context, links),
            chooserTitle = context.getString(R.string.share_invoice_chooser)
        )
    }

/**
 * Os links de pagamento no fim da mensagem, cada um na sua linha. No WhatsApp e no e-mail eles
 * ficam clicáveis, o que no PDF não dá para garantir (`docs/spec-links-pagamento.md`, 5.5). O
 * copia e cola do Pix vem por último e sozinho na linha: o WhatsApp usa o texto como legenda do
 * PDF, e assim é mais fácil de selecionar.
 */
private fun paymentLinesFor(context: Context, links: PaymentLinks): String {
    val lines = buildList {
        links.payPalUrl?.let { add(context.getString(R.string.share_pay_with_paypal, it)) }
        links.customUrl?.let { add(context.getString(R.string.share_pay_online, it)) }
        links.pixPayload?.let {
            add(context.getString(R.string.share_pix_copy_paste))
            add(it)
        }
    }
    return if (lines.isEmpty()) "" else "\n\n" + lines.joinToString("\n")
}
