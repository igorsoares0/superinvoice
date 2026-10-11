package com.example.superinvoice.util

import android.content.Context
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.DocumentType
import online.isdevapps.superinvoice.R

/** Assunto, texto e título do seletor ao compartilhar o PDF de uma fatura ou orçamento. */
data class ShareTexts(val subject: String, val text: String, val chooserTitle: String)

/**
 * Até a Onda 1 esses textos eram fixos em inglês ("Invoice INV-001"), mesmo com o app em
 * português ou espanhol, e o e-mail chegava ao cliente assim.
 */
fun shareTextsFor(context: Context, invoice: Invoice): ShareTexts =
    if (invoice.type == DocumentType.ESTIMATE) {
        ShareTexts(
            subject = context.getString(R.string.share_estimate_subject, invoice.number),
            text = context.getString(R.string.share_estimate_text, invoice.number),
            chooserTitle = context.getString(R.string.share_estimate_chooser)
        )
    } else {
        ShareTexts(
            subject = context.getString(R.string.share_invoice_subject, invoice.number),
            text = context.getString(R.string.share_invoice_text, invoice.number),
            chooserTitle = context.getString(R.string.share_invoice_chooser)
        )
    }
