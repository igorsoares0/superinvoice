package com.example.superinvoice.data.payment

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Matriz do QR de pagamento, desenhada pelo PDF e pela prévia na tela de configurações. Sem
 * Android: o PDF desenha cada trecho escuro como retângulo, vetorial, em vez de um bitmap que
 * borra ao imprimir (o problema que o `drawImageAt` resolve para a imagem enviada).
 */
object PaymentQr {

    /**
     * Correção `M` (15%): aguenta dobra e mancha no papel sem deixar o QR do Pix com valor
     * denso demais. Margem de 4 módulos, o mínimo da norma para a câmera achar o código.
     *
     * Sem `CHARACTER_SET`: o conteúdo é ASCII (BR Code, URL), e declarar UTF-8 acrescenta um
     * segmento ECI que alguns leitores de app de banco não entendem.
     *
     * @return null se o conteúdo não couber num QR (não acontece com Pix nem URL normal)
     */
    fun encode(content: String): BitMatrix? = runCatching {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 4
            )
        )
    }.getOrNull()

    /**
     * Cada sequência de módulos escuros de uma linha, como `[start, end)`. Um retângulo por
     * sequência em vez de um por módulo: o PDF fica menor e não aparecem frestas entre módulos
     * vizinhos em alguns leitores de PDF.
     */
    fun forEachDarkRun(matrix: BitMatrix, block: (row: Int, start: Int, end: Int) -> Unit) {
        for (row in 0 until matrix.height) {
            var col = 0
            while (col < matrix.width) {
                if (!matrix[col, row]) {
                    col++
                    continue
                }
                val start = col
                while (col < matrix.width && matrix[col, row]) col++
                block(row, start, col)
            }
        }
    }
}
