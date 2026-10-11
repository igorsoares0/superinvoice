package com.example.superinvoice

import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.entities.AdjustmentType.FIXED
import com.example.superinvoice.data.database.entities.AdjustmentType.PERCENT
import com.example.superinvoice.ui.viewmodel.AdjustmentInput
import com.example.superinvoice.ui.viewmodel.InvoiceForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O formulário é o que a criação e a edição gravam: o que a tela mostra tem de ser o que vai
 * para o banco, com o ajuste como digitado (tipo e valor) e o montante já calculado.
 */
class InvoiceFormTest {

    private val hours = ProductService(id = 7, name = "Consultoria", pricePerUnit = 100.0, unit = "h")

    @Test
    fun `percentual digitado com virgula vai para a fatura com tipo, valor e montante`() {
        val form = InvoiceForm()
        form.addLineItem(hours)
        form.setDiscount(AdjustmentInput(PERCENT, "10"))
        form.setTax(AdjustmentInput(PERCENT, "12,5"))
        form.setShipping("15,50")

        val invoice = form.applyTo(Invoice(number = "INV-001", clientId = 1, dueDate = 0L))

        assertEquals(PERCENT, invoice.discountType)
        assertEquals(10.0, invoice.discountValue, 0.0)
        assertEquals(10.0, invoice.discountAmount, 0.0)
        assertEquals(PERCENT, invoice.taxType)
        assertEquals(12.5, invoice.taxValue, 0.0)
        assertEquals(11.25, invoice.taxAmount, 0.0) // 12,5% de (100 − 10)
        assertEquals(15.5, invoice.shipping, 0.0)
        assertEquals(100.0 - 10.0 + 11.25 + 15.5, invoice.totalAmount, 0.0)
    }

    @Test
    fun `item novo herda a unidade do produto e a quantidade aceita decimal`() {
        val form = InvoiceForm()
        form.addLineItem(hours)
        form.updateLineItem(0, 2.5, " hora ")

        val item = form.toInvoiceItems(invoiceId = 3).single()
        assertEquals(2.5, item.quantity, 0.0)
        assertEquals("hora", item.unit)
        assertEquals(250.0, item.lineTotal, 0.0)
        assertEquals(7, item.productServiceId)
        assertTrue(form.hasDecimalQuantity)
    }

    @Test
    fun `passo de quantidade nunca chega a zero`() {
        val form = InvoiceForm()
        form.addLineItem(hours)
        form.updateLineItem(0, 1.5, "h")

        form.stepQuantity(0, -1)
        assertEquals(0.5, form.lineItems.value.single().quantity, 0.0)
        form.stepQuantity(0, -1)
        assertEquals(0.5, form.lineItems.value.single().quantity, 0.0)

        form.stepQuantity(0, 1)
        assertEquals(1.5, form.lineItems.value.single().quantity, 0.0)
        assertEquals(150.0, form.totals.value.total, 0.0)
    }

    @Test
    fun `fatura gravada volta ao formulario com os mesmos ajustes e o mesmo total`() {
        val saved = Invoice(
            number = "INV-002", clientId = 1, dueDate = 0L,
            subtotal = 200.0,
            discountType = FIXED, discountValue = 20.0, discountAmount = 20.0,
            taxType = PERCENT, taxValue = 10.0, taxAmount = 18.0,
            shipping = 5.0, totalAmount = 203.0
        )
        val items = listOf(
            InvoiceItem(
                invoiceId = 2, productServiceId = null, productServiceName = "Produto apagado",
                productServiceDescription = "", pricePerUnit = 80.0, quantity = 2.5, unit = "kg",
                lineTotal = 200.0
            )
        )

        val form = InvoiceForm()
        form.load(saved, items)

        assertEquals(PERCENT, form.tax.value.type)
        assertEquals(10.0, form.tax.value.value, 0.0)
        assertEquals(FIXED, form.discount.value.type)
        assertEquals(203.0, form.totals.value.total, 0.0)
        assertEquals("kg", form.lineItems.value.single().unit)
        // Produto apagado (SET NULL) continua sem vínculo depois de salvar de novo.
        assertNull(form.toInvoiceItems(invoiceId = 2).single().productServiceId)
    }

    @Test
    fun `reset limpa itens, ajustes e frete`() {
        val form = InvoiceForm()
        form.addLineItem(hours)
        form.setTax(AdjustmentInput(PERCENT, "10"))
        form.setShipping("5")

        form.reset()

        assertTrue(form.lineItems.value.isEmpty())
        assertEquals(AdjustmentInput(), form.tax.value)
        assertEquals("", form.shipping.value)
        assertEquals(0.0, form.totals.value.total, 0.0)
        assertFalse(form.hasDecimalQuantity)
    }
}
