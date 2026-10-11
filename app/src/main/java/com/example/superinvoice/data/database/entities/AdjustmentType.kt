package com.example.superinvoice.data.database.entities

/**
 * Como o usuário informou desconto ou imposto: percentual sobre a base ou valor fixo na moeda.
 * Fica junto das entidades porque vira coluna de `invoices` na versão 5 do banco
 * (`docs/spec-onda-1.md`, seção 3.1).
 */
enum class AdjustmentType {
    PERCENT,
    FIXED
}
