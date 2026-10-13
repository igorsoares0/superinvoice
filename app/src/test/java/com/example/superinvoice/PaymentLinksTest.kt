package com.example.superinvoice

import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.payment.PayPalMe
import com.example.superinvoice.data.payment.PaymentLinkSettings
import com.example.superinvoice.data.payment.PaymentLinkUrl
import com.example.superinvoice.data.payment.PaymentLinks
import com.example.superinvoice.data.payment.PaymentLinksBuilder
import com.example.superinvoice.data.payment.PaymentQrSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Link com valor errado ou em fatura já paga faz o cliente do usuário pagar de novo. */
class PaymentLinksTest {

    private val settings = PaymentLinkSettings(
        pixKey = "joao@exemplo.com",
        pixMerchantName = "Joao Silva",
        pixMerchantCity = "Recife",
        payPalUsername = "joao",
        paymentLinkUrl = "https://buy.stripe.com/abc"
    )

    private val invoice = Invoice(
        number = "INV-012",
        clientId = 1,
        dueDate = 0L,
        totalAmount = 150.0,
        status = InvoiceStatus.SENT,
        currency = "BRL"
    )

    @Test
    fun `fatura em BRL com Pix usa o Pix no QR`() {
        val links = PaymentLinksBuilder.forInvoice(invoice, settings, hasUploadedQr = true)

        assertEquals(PaymentQrSource.PIX, links.qrSource)
        assertEquals(links.pixPayload, links.qrContent)
        assertEquals("joao@exemplo.com", links.pixKey)
        assertTrue(links.pixPayload!!.contains("5406150.00"))
        assertEquals("https://paypal.me/joao/150.00BRL", links.payPalUrl)
        assertEquals("https://buy.stripe.com/abc", links.customUrl)
    }

    @Test
    fun `parcialmente paga cobra so o saldo`() {
        val partial = invoice.copy(amountPaid = 50.0, status = InvoiceStatus.PARTIALLY_PAID)
        val links = PaymentLinksBuilder.forInvoice(partial, settings, hasUploadedQr = false)

        assertTrue(links.pixPayload!!.contains("5406100.00"))
        assertEquals("https://paypal.me/joao/100.00BRL", links.payPalUrl)
    }

    @Test
    fun `fatura paga, cancelada ou orcamento nao ganham link`() {
        val paid = invoice.copy(amountPaid = 150.0, status = InvoiceStatus.PAID)
        val cancelled = invoice.copy(status = InvoiceStatus.CANCELLED)
        val estimate = invoice.copy(type = DocumentType.ESTIMATE, number = "EST-001")

        for (doc in listOf(paid, cancelled, estimate)) {
            assertEquals(PaymentLinks.NONE, PaymentLinksBuilder.forInvoice(doc, settings, false))
            // A imagem enviada pelo usuário continua: é QR fixo, sem valor.
            val withImage = PaymentLinksBuilder.forInvoice(doc, settings, hasUploadedQr = true)
            assertEquals(PaymentQrSource.UPLOADED, withImage.qrSource)
            assertNull(withImage.payPalUrl)
        }
    }

    @Test
    fun `fatura em outra moeda ignora o Pix`() {
        val usd = invoice.copy(currency = "USD")
        val links = PaymentLinksBuilder.forInvoice(usd, settings, hasUploadedQr = false)

        assertNull(links.pixKey)
        assertNull(links.pixPayload)
        assertEquals(PaymentQrSource.PAYPAL, links.qrSource)
        assertEquals("https://paypal.me/joao/150.00USD", links.qrContent)
    }

    @Test
    fun `sem Pix a imagem enviada vence o PayPal e o link`() {
        val links = PaymentLinksBuilder.forInvoice(
            invoice, settings.copy(pixKey = ""), hasUploadedQr = true
        )

        assertEquals(PaymentQrSource.UPLOADED, links.qrSource)
        assertNull(links.qrContent)
        assertEquals("https://paypal.me/joao/150.00BRL", links.payPalUrl)
    }

    @Test
    fun `so com link generico o QR e o link`() {
        val onlyLink = PaymentLinkSettings(paymentLinkUrl = "https://buy.stripe.com/abc")
        val links = PaymentLinksBuilder.forInvoice(invoice, onlyLink, hasUploadedQr = false)

        assertEquals(PaymentQrSource.CUSTOM_LINK, links.qrSource)
        assertEquals("https://buy.stripe.com/abc", links.qrContent)
    }

    @Test
    fun `Pix sem nome do recebedor some inteiro e o QR passa para o proximo`() {
        val links = PaymentLinksBuilder.forInvoice(
            invoice, settings.copy(pixMerchantName = ""), hasUploadedQr = false
        )

        assertNull(links.pixKey)
        assertNull(links.pixPayload)
        assertEquals(PaymentQrSource.PAYPAL, links.qrSource)
    }

    @Test
    fun `nada configurado nao gera nada`() {
        assertEquals(
            PaymentLinks.NONE,
            PaymentLinksBuilder.forInvoice(invoice, PaymentLinkSettings(), hasUploadedQr = false)
        )
    }

    @Test
    fun `usuario do PayPal colado como link ou arroba`() {
        assertEquals("joao", PayPalMe.normalizeUsername("https://www.paypal.me/joao/10"))
        assertEquals("Joao", PayPalMe.normalizeUsername("PayPal.me/Joao"))
        assertEquals("joao", PayPalMe.normalizeUsername(" @joao "))
        assertNull(PayPalMe.normalizeUsername("joão"))
        assertNull(PayPalMe.normalizeUsername(""))
    }

    @Test
    fun `PayPal sem centavos em JPY e sem valor em moeda nao aceita`() {
        assertEquals("https://paypal.me/joao/15000JPY", PayPalMe.url("joao", 15000.4, "JPY"))
        assertEquals("https://paypal.me/joao", PayPalMe.url("joao", 150.0, "ARS"))
        assertEquals("https://paypal.me/joao", PayPalMe.url("joao", null, "USD"))
    }

    @Test
    fun `PayPal usa ponto mesmo com o aparelho em espanhol`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("https://paypal.me/joao/1234.50EUR", PayPalMe.url("joao", 1234.5, "EUR"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `link generico so com https`() {
        assertEquals("https://buy.stripe.com/abc", PaymentLinkUrl.normalize(" buy.stripe.com/abc "))
        assertEquals("HTTPS://mpago.la/1x", PaymentLinkUrl.normalize("HTTPS://mpago.la/1x"))
        assertNull(PaymentLinkUrl.normalize("http://buy.stripe.com/abc"))
        assertNull(PaymentLinkUrl.normalize("ftp://exemplo.com"))
        assertNull(PaymentLinkUrl.normalize("meu link"))
        assertNull(PaymentLinkUrl.normalize("localhost"))
        assertNull(PaymentLinkUrl.normalize(""))
    }

    @Test
    fun `link longo demais para um QR legivel e recusado`() {
        val base = "https://buy.stripe.com/"
        assertEquals(base + "a".repeat(477), PaymentLinkUrl.normalize(base + "a".repeat(477)))
        assertNull(PaymentLinkUrl.normalize(base + "a".repeat(478)))
    }
}
