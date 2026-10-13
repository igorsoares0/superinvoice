package com.example.superinvoice

import com.example.superinvoice.data.payment.PixKey
import com.example.superinvoice.data.payment.PixKeyType
import com.example.superinvoice.data.payment.PixPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Um código Pix errado não dá erro no app: o banco do pagador é que recusa, na frente do
 * cliente do usuário. Por isso o formato é conferido contra o exemplo oficial do BACEN.
 */
class PixPayloadTest {

    private val evp = "123e4567-e12b-12d1-a456-426655440000"

    @Test
    fun `reproduz o exemplo estatico do manual do BACEN`() {
        val expected = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
            "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D"
        assertEquals(expected, PixPayload.build(evp, "Fulano de Tal", "BRASILIA", null, ""))
    }

    @Test
    fun `valor e txid entram nos campos 54 e 62 e o CRC fecha`() {
        val payload = PixPayload.build(evp, "Fulano de Tal", "BRASILIA", 150.0, "INV-012")!!

        assertTrue(payload.contains("5406150.00"))
        assertTrue(payload.contains("62100506INV012"))
        assertCrcMatches(payload)
    }

    @Test
    fun `valor sai com ponto mesmo com o aparelho em pt-BR`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pt-BR"))
            val payload = PixPayload.build(evp, "Fulano", "Recife", 1234.5, "1")!!
            assertTrue(payload.contains("54071234.50"))
            assertFalse(payload.contains(","))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `valor arredonda meio centavo para cima`() {
        assertEquals("10.01", PixPayload.formatAmount(10.005))
        assertEquals("0.30", PixPayload.formatAmount(0.1 + 0.2))
    }

    @Test
    fun `valor zero ou nulo omite o campo 54`() {
        val zero = PixPayload.build(evp, "Fulano", "Recife", 0.0, "1")!!
        val none = PixPayload.build(evp, "Fulano", "Recife", null, "1")!!
        assertEquals(none, zero)
        // Sem o 54, a moeda (53) é seguida direto do país (58).
        assertTrue(none.contains("53039865802BR"))
    }

    @Test
    fun `nome e cidade perdem acento e sao cortados em 25 e 15`() {
        val payload = PixPayload.build(
            evp, "José da Conceição Ltda Serviços Gerais", "São José dos Campos", null, "1"
        )!!

        assertTrue(payload.contains("5925Jose da Conceicao Ltda Se"))
        assertTrue(payload.contains("6015Sao Jose dos Ca"))
        assertCrcMatches(payload)
    }

    @Test
    fun `txid fica so com letras e digitos ate 25 caracteres`() {
        val payload = PixPayload.build(evp, "Fulano", "Recife", null, "INV-0001/2026-ABCDEFGHIJKLMNOPQ")!!
        assertTrue(payload.contains("62290525INV00012026ABCDEFGHIJKLMN6304"))
    }

    @Test
    fun `campo obrigatorio vazio ou grande demais devolve null`() {
        assertNull(PixPayload.build(evp, "", "Recife", null, "1"))
        assertNull(PixPayload.build(evp, "Fulano", "  ", null, "1"))
        // Nome só com caracteres fora do ASCII fica vazio depois de limpo.
        assertNull(PixPayload.build(evp, "日本商店", "Recife", null, "1"))
        assertNull(PixPayload.build("", "Fulano", "Recife", null, "1"))
        assertNull(PixPayload.build("a".repeat(80), "Fulano", "Recife", null, "1"))
    }

    @Test
    fun `cpf e cnpj aceitam mascara e conferem o digito`() {
        assertEquals("52998224725", PixKey.normalize(PixKeyType.CPF, "529.982.247-25"))
        assertNull(PixKey.normalize(PixKeyType.CPF, "529.982.247-26"))
        assertNull(PixKey.normalize(PixKeyType.CPF, "111.111.111-11"))
        assertNull(PixKey.normalize(PixKeyType.CPF, "5299822472"))

        assertEquals("11222333000181", PixKey.normalize(PixKeyType.CNPJ, "11.222.333/0001-81"))
        assertNull(PixKey.normalize(PixKeyType.CNPJ, "11.222.333/0001-82"))
    }

    @Test
    fun `celular ganha o DDI 55 e numero estrangeiro e recusado`() {
        assertEquals("+5511987654321", PixKey.normalize(PixKeyType.PHONE, "(11) 98765-4321"))
        assertEquals("+5511987654321", PixKey.normalize(PixKeyType.PHONE, "+55 11 98765-4321"))
        assertEquals("+5511987654321", PixKey.normalize(PixKeyType.PHONE, "5511987654321"))
        assertEquals("+551133334444", PixKey.normalize(PixKeyType.PHONE, "(11) 3333-4444"))
        // DDD 55 (RS) sem DDI: 11 dígitos, então é número local.
        assertEquals("+5555987654321", PixKey.normalize(PixKeyType.PHONE, "(55) 98765-4321"))
        assertNull(PixKey.normalize(PixKeyType.PHONE, "+1 415 555 0100"))
        assertNull(PixKey.normalize(PixKeyType.PHONE, "98765-4321"))
    }

    @Test
    fun `email e chave aleatoria ficam minusculos`() {
        assertEquals("joao@exemplo.com", PixKey.normalize(PixKeyType.EMAIL, " Joao@Exemplo.COM "))
        assertNull(PixKey.normalize(PixKeyType.EMAIL, "joao@exemplo"))
        assertNull(PixKey.normalize(PixKeyType.EMAIL, "joao exemplo@x.com"))

        assertEquals(evp, PixKey.normalize(PixKeyType.EVP, evp.uppercase()))
        assertNull(PixKey.normalize(PixKeyType.EVP, "123e4567e12b12d1a456426655440000"))
    }

    private fun assertCrcMatches(payload: String) {
        val body = payload.dropLast(4)
        assertTrue(body.endsWith("6304"))
        assertEquals(PixPayload.crc16(body), payload.takeLast(4))
    }
}
