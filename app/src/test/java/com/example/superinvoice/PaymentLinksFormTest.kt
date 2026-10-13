package com.example.superinvoice

import com.example.superinvoice.data.payment.PixKeyType
import com.example.superinvoice.ui.viewmodel.PaymentLinksErrors
import com.example.superinvoice.ui.viewmodel.PaymentLinksInput
import com.example.superinvoice.ui.viewmodel.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O que é salvo aqui vai para o PDF de todas as faturas seguintes. */
class PaymentLinksFormTest {

    @Test
    fun `tudo vazio e valido`() {
        val (valid, errors) = PaymentLinksInput().validate(businessName = "", businessCity = "")
        assertEquals(PaymentLinksInput(), valid)
        assertEquals(PaymentLinksErrors(), errors)
    }

    @Test
    fun `salva os valores normalizados`() {
        val input = PaymentLinksInput(
            pixKeyType = PixKeyType.PHONE,
            pixKey = "(11) 98765-4321",
            pixMerchantName = " Joao Silva ",
            payPalUsername = "https://paypal.me/joao",
            paymentLinkUrl = "buy.stripe.com/abc"
        )
        val (valid, _) = input.validate(businessName = "", businessCity = "Recife")

        assertEquals("+5511987654321", valid!!.pixKey)
        assertEquals("Joao Silva", valid.pixMerchantName)
        // Cidade vazia continua vazia: o fallback para a do negócio é aplicado na leitura.
        assertEquals("", valid.pixMerchantCity)
        assertEquals("joao", valid.payPalUsername)
        assertEquals("https://buy.stripe.com/abc", valid.paymentLinkUrl)
    }

    @Test
    fun `chave Pix exige nome e cidade, proprios ou do negocio`() {
        val input = PaymentLinksInput(pixKeyType = PixKeyType.EMAIL, pixKey = "joao@exemplo.com")

        val (_, missing) = input.validate(businessName = "", businessCity = "")
        assertTrue(missing.pixMerchantName)
        assertTrue(missing.pixMerchantCity)

        val (valid, _) = input.validate(businessName = "Joao Silva", businessCity = "Recife")
        assertEquals("joao@exemplo.com", valid!!.pixKey)
    }

    @Test
    fun `sem chave Pix nome e cidade nao sao exigidos`() {
        val (valid, errors) = PaymentLinksInput(payPalUsername = "joao")
            .validate(businessName = "", businessCity = "")
        assertEquals(PaymentLinksErrors(), errors)
        assertEquals("joao", valid!!.payPalUsername)
    }

    @Test
    fun `cada campo invalido marca o proprio erro e nada e salvo`() {
        val input = PaymentLinksInput(
            pixKeyType = PixKeyType.CPF,
            pixKey = "123.456.789-00",
            pixMerchantName = "Joao",
            pixMerchantCity = "Recife",
            payPalUsername = "joão",
            paymentLinkUrl = "http://inseguro.com"
        )
        val (valid, errors) = input.validate(businessName = "", businessCity = "")

        assertNull(valid)
        assertEquals(
            PaymentLinksErrors(pixKey = true, payPalUsername = true, paymentLinkUrl = true),
            errors
        )
    }
}
