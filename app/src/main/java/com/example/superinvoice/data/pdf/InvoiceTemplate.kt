package com.example.superinvoice.data.pdf

/** Converte o id salvo no DataStore no template. */
fun invoiceTemplateOf(id: String?): InvoiceTemplate = when (id) {
    "modern" -> InvoiceTemplate.MODERN
    "professional" -> InvoiceTemplate.PROFESSIONAL
    else -> InvoiceTemplate.CLASSIC
}
