package com.example.superinvoice

import com.example.superinvoice.data.calc.PaymentRules
import com.example.superinvoice.data.database.entities.InvoiceStatus.DRAFT
import com.example.superinvoice.data.database.entities.InvoiceStatus.PAID
import com.example.superinvoice.data.database.entities.InvoiceStatus.PARTIALLY_PAID
import com.example.superinvoice.data.database.entities.InvoiceStatus.SENT
import org.junit.Assert.assertEquals
import org.junit.Test

/** Status e saldo vão para a Home, o PDF e o recibo; um erro aqui faz cobrar quem já pagou. */
class PaymentRulesTest {

    @Test
    fun `pagamento que cobre o total quita a fatura`() {
        assertEquals(PAID, PaymentRules.status(SENT, total = 105.0, amountPaid = 105.0))
        assertEquals(PAID, PaymentRules.status(DRAFT, total = 105.0, amountPaid = 120.0))
    }

    @Test
    fun `pagamento menor que o total deixa parcialmente paga`() {
        assertEquals(PARTIALLY_PAID, PaymentRules.status(DRAFT, total = 105.0, amountPaid = 40.0))
        assertEquals(PARTIALLY_PAID, PaymentRules.status(PAID, total = 150.0, amountPaid = 105.0))
    }

    @Test
    fun `sem pagamento a fatura paga volta para enviada e o rascunho continua rascunho`() {
        assertEquals(SENT, PaymentRules.status(PAID, total = 105.0, amountPaid = 0.0))
        assertEquals(SENT, PaymentRules.status(PARTIALLY_PAID, total = 105.0, amountPaid = 0.0))
        assertEquals(DRAFT, PaymentRules.status(DRAFT, total = 105.0, amountPaid = 0.0))
    }

    @Test
    fun `tres pagamentos de 10 centavos quitam 30 centavos`() {
        val paid = PaymentRules.sum(listOf(0.1, 0.1, 0.1))
        assertEquals(0.3, paid, 0.0)
        assertEquals(PAID, PaymentRules.status(SENT, total = 0.3, amountPaid = paid))
    }

    @Test
    fun `numero do recibo segue a sequencia`() {
        assertEquals("REC-001", PaymentRules.nextReceiptNumber(null))
        assertEquals("REC-013", PaymentRules.nextReceiptNumber("REC-012"))
        assertEquals("REC-1000", PaymentRules.nextReceiptNumber("REC-999"))
    }
}
