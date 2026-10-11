package com.example.superinvoice.data.database.entities

/**
 * `PAID` e `PARTIALLY_PAID` são consequência dos pagamentos registrados, não um toggle.
 * "Vencida" não é gravada: é derivada de `dueDate` na leitura, para nenhum job precisar
 * atualizar status à meia-noite.
 */
enum class InvoiceStatus {
    // Faturas
    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    CANCELLED,

    // Orçamentos
    ACCEPTED,
    DECLINED,
    CONVERTED,

    /** Só existe para o [com.example.superinvoice.data.database.Converters] ler valores da v4. */
    @Deprecated("Derivado da data, nunca gravado")
    OVERDUE
}
