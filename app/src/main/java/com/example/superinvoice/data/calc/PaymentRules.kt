package com.example.superinvoice.data.calc

import com.example.superinvoice.data.database.entities.InvoiceStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Regras que ligam os pagamentos ao status da fatura (spec da Onda 1, seção 5.2). `PAID` e
 * `PARTIALLY_PAID` são consequência dos pagamentos, nunca escolhidos na tela.
 */
object PaymentRules {

    /**
     * Status depois que `amountPaid` mudou: quitou → `PAID`; parte → `PARTIALLY_PAID`; nada
     * pago → volta para `SENT` se estava paga, senão mantém (um rascunho continua rascunho).
     */
    fun status(current: InvoiceStatus, total: Double, amountPaid: Double): InvoiceStatus {
        val paid = money(amountPaid)
        return when {
            paid.signum() <= 0 ->
                if (current == InvoiceStatus.PAID || current == InvoiceStatus.PARTIALLY_PAID) {
                    InvoiceStatus.SENT
                } else {
                    current
                }
            paid >= money(total) -> InvoiceStatus.PAID
            else -> InvoiceStatus.PARTIALLY_PAID
        }
    }

    /** Soma em centavos exatos: `SUM()` do SQLite em `REAL` daria 0.30000000000000004. */
    fun sum(amounts: List<Double>): Double =
        amounts.fold(BigDecimal.ZERO) { acc, amount -> acc + money(amount) }.toDouble()

    /** REC-001, REC-002…; a sequência é única para o app todo, não por fatura. */
    fun nextReceiptNumber(last: String?): String {
        val next = (last?.substringAfterLast('-')?.toIntOrNull() ?: 0) + 1
        return "REC-" + next.toString().padStart(3, '0')
    }

    private fun money(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
}
