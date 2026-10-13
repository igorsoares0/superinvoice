package com.example.superinvoice.data.payment

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Locale

/**
 * BR Code estático do Pix (o "copia e cola"), no formato EMV do Manual de Padrões para
 * Iniciação do Pix do BACEN. Gerado no aparelho, sem PSP: o pagamento cai direto na chave, e o
 * usuário continua registrando o pagamento à mão.
 *
 * Cada campo é `ID (2) + tamanho (2) + valor`. Regras em `docs/spec-links-pagamento.md`, 5.1.
 */
object PixPayload {

    private const val GUI = "br.gov.bcb.pix"
    private const val MAX_MERCHANT_ACCOUNT = 99
    private const val MAX_NAME = 25
    private const val MAX_CITY = 15
    private const val MAX_TXID = 25
    private const val MAX_AMOUNT = 13

    /**
     * @param key chave já normalizada por [PixKey.normalize]
     * @param amount valor a cobrar; nulo ou zero omite o campo e o pagador digita o valor
     * @param txid identificador livre; só letras e dígitos ASCII são mantidos
     * @return o payload, ou null se algum campo obrigatório ficar vazio ou passar do limite
     */
    fun build(key: String, name: String, city: String, amount: Double?, txid: String): String? {
        val merchantAccount = field("00", GUI) + field("01", key)
        if (key.isBlank() || merchantAccount.length > MAX_MERCHANT_ACCOUNT) return null

        val cleanName = sanitize(name).take(MAX_NAME).trimEnd()
        val cleanCity = sanitize(city).take(MAX_CITY).trimEnd()
        if (cleanName.isEmpty() || cleanCity.isEmpty()) return null

        val amountText = amount?.takeIf { it > 0.0 }?.let { formatAmount(it) }
        if (amountText != null && amountText.length > MAX_AMOUNT) return null

        val cleanTxid = txid.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }
            .take(MAX_TXID)
            .ifEmpty { "***" }

        val payload = buildString {
            append(field("00", "01"))
            append(field("26", merchantAccount))
            append(field("52", "0000"))
            append(field("53", "986"))
            amountText?.let { append(field("54", it)) }
            append(field("58", "BR"))
            append(field("59", cleanName))
            append(field("60", cleanCity))
            append(field("62", field("05", cleanTxid)))
            // O CRC cobre o próprio ID e tamanho do campo 63.
            append("6304")
        }
        return payload + crc16(payload)
    }

    /**
     * Sempre com ponto. `String.format("%.2f")` e `formatDecimalInput` seguem o locale e, em
     * pt-BR e es, dão vírgula: o banco rejeita o código.
     */
    internal fun formatAmount(amount: Double): String =
        BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString()

    /**
     * Sem acento e só ASCII imprimível: vários apps de banco rejeitam o código ou mostram o nome
     * truncado quando há caractere fora disso.
     */
    internal fun sanitize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .filter { it in ' '..'~' }
            .replace(Regex("\\s+"), " ")
            .trim()

    /** CRC16-CCITT-FALSE: polinômio 0x1021, valor inicial 0xFFFF, 4 hex maiúsculos. */
    internal fun crc16(text: String): String {
        var crc = 0xFFFF
        for (byte in text.toByteArray(Charsets.US_ASCII)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return Integer.toHexString(crc).uppercase(Locale.ROOT).padStart(4, '0')
    }

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(2, '0') + value
}
