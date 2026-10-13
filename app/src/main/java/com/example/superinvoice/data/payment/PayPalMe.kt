package com.example.superinvoice.data.payment

import java.math.BigDecimal
import java.math.RoundingMode

/** Link `paypal.me` com o valor da fatura já preenchido para o pagador. */
object PayPalMe {

    private val USERNAME = Regex("^[A-Za-z0-9]{1,20}$")

    /**
     * Moedas do app (`getCurrencySymbol`) que o PayPal aceita num pagamento entre países. CNY e
     * INR só valem dentro do próprio país e ARS não é aceita: nessas, o link vai sem valor e o
     * pagador digita. Conferir com a lista do PayPal ao adicionar moeda nova no app.
     */
    private val SUPPORTED = setOf("USD", "EUR", "GBP", "BRL", "JPY", "CAD", "AUD", "CHF", "MXN")

    /** Moedas sem centavos no PayPal: "15000JPY", não "15000.00JPY". */
    private val ZERO_DECIMAL = setOf("JPY")

    /**
     * O usuário costuma colar o link inteiro ou o "@usuario" do app do PayPal. Devolve só o
     * usuário, ou null se não for um usuário válido do PayPal.me.
     */
    fun normalizeUsername(raw: String): String? =
        raw.trim()
            .removePrefix("https://").removePrefix("http://")
            .removePrefix("www.")
            .let { if (it.startsWith("paypal.me/", ignoreCase = true)) it.substring(10) else it }
            .removePrefix("@")
            .substringBefore('/')
            .takeIf { USERNAME.matches(it) }

    /** @param username já normalizado por [normalizeUsername] */
    fun url(username: String, amount: Double?, currency: String): String {
        val base = "https://paypal.me/$username"
        if (amount == null || amount <= 0.0 || currency !in SUPPORTED) return base
        val scale = if (currency in ZERO_DECIMAL) 0 else 2
        // Ponto sempre, como no Pix: o PayPal não entende "150,00".
        val value = BigDecimal.valueOf(amount).setScale(scale, RoundingMode.HALF_UP).toPlainString()
        return "$base/$value$currency"
    }
}
