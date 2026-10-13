package com.example.superinvoice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.payment.PaymentLinks
import com.example.superinvoice.util.shareTextsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A mensagem de compartilhamento é onde os links ficam clicáveis (spec de links, 5.5). */
@RunWith(AndroidJUnit4::class)
class ShareTextsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val invoice = Invoice(number = "INV-042", clientId = 1, dueDate = 0L)

    @Test
    fun semLinksOTextoNaoMuda() {
        val plain = shareTextsFor(context, invoice).text
        assertEquals(plain, shareTextsFor(context, invoice, PaymentLinks.NONE).text)
        assertTrue(plain.lines().size == 1)
    }

    @Test
    fun copiaEColaDoPixVemPorUltimoSozinhoNaLinha() {
        val links = PaymentLinks(
            pixKey = "joao@exemplo.com",
            pixPayload = "00020126PIX6304ABCD",
            payPalUrl = "https://paypal.me/joao/150.00BRL",
            customUrl = "https://buy.stripe.com/abc"
        )
        val lines = shareTextsFor(context, invoice, links).text.lines()

        assertEquals("", lines[1])
        assertTrue(lines[2].endsWith("https://paypal.me/joao/150.00BRL"))
        assertTrue(lines[3].endsWith("https://buy.stripe.com/abc"))
        assertEquals("00020126PIX6304ABCD", lines.last())
        assertEquals(6, lines.size)
    }
}
