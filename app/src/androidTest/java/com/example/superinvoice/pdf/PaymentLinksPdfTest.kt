package com.example.superinvoice.pdf

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.payment.PaymentLinkSettings
import com.example.superinvoice.data.payment.PaymentLinks
import com.example.superinvoice.data.payment.PaymentLinksBuilder
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.pdf.InvoiceTemplate
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Links de pagamento no PDF (`docs/spec-links-pagamento.md`, seções 5.4 e 6). */
@RunWith(AndroidJUnit4::class)
class PaymentLinksPdfTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val generator = InvoicePdfGenerator(context)

    private val invoice = Invoice(
        id = 1,
        number = "INV-042",
        clientId = 1,
        createdDate = 1735689600000L,
        dueDate = 1735603200000L,
        subtotal = 1100.0,
        totalAmount = 1100.0,
        status = InvoiceStatus.SENT,
        currency = "BRL"
    )

    private val settings = PaymentLinkSettings(
        pixKey = "123e4567-e12b-12d1-a456-426655440000",
        pixMerchantName = "Oficina Tipografica",
        pixMerchantCity = "Sao Paulo",
        payPalUsername = "oficina",
        paymentLinkUrl = "https://buy.stripe.com/test_00000000000000000000000000000000000000000000"
    )

    private val links = PaymentLinksBuilder.forInvoice(invoice, settings, hasUploadedQr = false)

    /** Sem nenhum dado bancário: antes dos links, o bloco de pagamento não aparecia. */
    private val noBank = InvoicePdfGenerator.PaymentInfo()

    @Test
    fun soPixSemBancoAindaDesenhaOBloco() {
        for (template in InvoiceTemplate.entries) {
            val without = hash(render(template, PaymentLinks.NONE, isPremium = true))
            val with = hash(render(template, links, isPremium = true))
            assertNotEquals("$template sem bloco de pagamento", without, with)
        }
    }

    @Test
    fun qrDoPdfLeOPixComOValor() {
        for (template in InvoiceTemplate.entries) {
            val bitmap = render(template, links, isPremium = true, scale = 3)
            assertEquals("$template", links.pixPayload, readQr(bitmap))
            bitmap.recycle()
        }
    }

    @Test
    fun semPremiumSaiSoOTexto() {
        val textOnly = links.copy(qrContent = null)
        for (template in InvoiceTemplate.entries) {
            assertEquals(
                "$template: o QR gerado não pode sair sem premium",
                hash(render(template, textOnly, isPremium = false)),
                hash(render(template, links, isPremium = false))
            )
        }
    }

    @Test
    fun muitosItensNaoQuebramNenhumTemplate() {
        val items = (1..25).map { item(it) }
        for (template in InvoiceTemplate.entries) {
            render(template, links, isPremium = true, items = items).recycle()
        }
    }

    private fun render(
        template: InvoiceTemplate,
        paymentLinks: PaymentLinks,
        isPremium: Boolean,
        scale: Int = 2,
        items: List<InvoiceItem> = listOf(item(1)),
    ): Bitmap = requireNotNull(
        generator.generateInvoicePreviewBitmap(
            invoice = invoice,
            client = Client(id = 1, name = "Studio Vinte", email = "", phone = ""),
            items = items,
            businessInfo = InvoicePdfGenerator.BusinessInfo(businessName = "Oficina Tipográfica"),
            paymentInfo = noBank,
            currency = invoice.currency,
            paymentLinks = paymentLinks,
            template = template,
            scale = scale,
            isPremium = isPremium
        )
    ) { "$template devolveu null" }

    private fun item(id: Int) = InvoiceItem(
        id = id,
        invoiceId = 1,
        productServiceId = null,
        productServiceName = "Item $id",
        productServiceDescription = "",
        pricePerUnit = 1100.0,
        quantity = 1.0,
        lineTotal = 1100.0
    )

    private fun readQr(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(source)),
            mapOf(DecodeHintType.TRY_HARDER to true)
        ).text
    }

    private fun hash(bitmap: Bitmap): String {
        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        bitmap.recycle()
        return MessageDigest.getInstance("SHA-256").digest(buffer.array())
            .joinToString("") { "%02x".format(it) }
    }
}
