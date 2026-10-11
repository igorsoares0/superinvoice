package com.example.superinvoice.data.database.entities

/**
 * Fatura e orçamento moram na mesma tabela `invoices`: compartilham cliente, itens, totais e
 * PDF, e a conversão de orçamento em fatura vira uma cópia da linha com outro tipo
 * (`docs/spec-onda-1.md`, seção 3).
 */
enum class DocumentType {
    INVOICE,
    ESTIMATE
}
