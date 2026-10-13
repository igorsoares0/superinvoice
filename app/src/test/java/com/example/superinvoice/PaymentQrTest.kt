package com.example.superinvoice

import com.example.superinvoice.data.payment.PaymentLinks
import com.example.superinvoice.data.payment.PaymentQr
import com.example.superinvoice.data.payment.PaymentQrSource
import com.example.superinvoice.data.payment.PixPayload
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.Decoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** O QR é a única parte do link que o pagador usa sem digitar: precisa ler de volta igual. */
class PaymentQrTest {

    private val pix = PixPayload.build(
        key = "123e4567-e12b-12d1-a456-426655440000",
        name = "Oficina Tipografica",
        city = "Sao Paulo",
        amount = 1100.0,
        txid = "INV042"
    )!!

    @Test
    fun `QR do Pix le de volta o mesmo payload`() {
        assertEquals(pix, decode(PaymentQr.encode(pix)!!))
    }

    @Test
    fun `QR de URL le de volta a mesma URL`() {
        val url = "https://paypal.me/joao/1100.00BRL"
        assertEquals(url, decode(PaymentQr.encode(url)!!))
    }

    @Test
    fun `QR tem margem de 4 modulos`() {
        val matrix = PaymentQr.encode(pix)!!
        val (left, top) = matrix.enclosingRectangle
        assertEquals(4, left)
        assertEquals(4, top)
    }

    @Test
    fun `os trechos escuros cobrem exatamente os modulos escuros`() {
        val matrix = PaymentQr.encode(pix)!!
        val redrawn = BitMatrix(matrix.width, matrix.height)
        PaymentQr.forEachDarkRun(matrix) { row, start, end ->
            redrawn.setRegion(start, row, end - start, 1)
        }
        assertEquals(matrix, redrawn)
    }

    @Test
    fun `sem premium o QR gerado sai e a imagem enviada volta`() {
        val links = PaymentLinks(
            pixKey = "joao@exemplo.com",
            pixPayload = pix,
            qrSource = PaymentQrSource.PIX,
            qrContent = pix
        )

        val free = links.forPlan(isPremium = false, hasUploadedQr = true)
        assertNull(free.qrContent)
        assertEquals(PaymentQrSource.UPLOADED, free.qrSource)
        // As linhas de texto continuam (decisão L1).
        assertEquals("joao@exemplo.com", free.pixKey)

        assertEquals(PaymentQrSource.NONE, links.forPlan(isPremium = false, hasUploadedQr = false).qrSource)
        assertSame(links, links.forPlan(isPremium = true, hasUploadedQr = true))
    }

    /** O `Decoder` lê a matriz sem a margem, como sai do detector de uma câmera. */
    private fun decode(matrix: BitMatrix): String {
        val (left, top, width, height) = matrix.enclosingRectangle
        val cropped = BitMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix[left + x, top + y]) cropped.set(x, y)
            }
        }
        return Decoder().decode(cropped).text
    }
}
