package com.example.superinvoice.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Lê um número digitado pelo usuário. Em pt-BR e es o teclado decimal mostra vírgula, e
 * `"10,5".toDoubleOrNull()` dá `null`; antes disso o imposto, o desconto e o preço viravam 0
 * em silêncio. Por isso vírgula e ponto valem como separador decimal em qualquer idioma.
 *
 * Com os dois separadores, o último é o decimal e o outro é de milhar ("1.234,56" e
 * "1,234.56" dão 1234.56). Com um só, ele é o decimal. Vazio ou inválido devolve `null`.
 */
fun parseDecimalInput(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val separatorIndex = maxOf(trimmed.lastIndexOf(','), trimmed.lastIndexOf('.'))
    if (separatorIndex < 0) {
        return if (DIGITS.matches(trimmed)) trimmed.toDouble() else null
    }

    val decimalSeparator = trimmed[separatorIndex]
    val groupingSeparator = if (decimalSeparator == ',') '.' else ','
    val integerPart = trimmed.substring(0, separatorIndex)
    val fractionPart = trimmed.substring(separatorIndex + 1)

    // O separador decimal só pode aparecer uma vez ("1,2,3" é ambíguo).
    if (decimalSeparator in integerPart) return null
    val integerDigits = integerPart.replace(groupingSeparator.toString(), "")

    if (!INTEGER.matches(integerDigits) || !FRACTION.matches(fractionPart)) return null
    if (integerDigits.trimStart('-').isEmpty() && fractionPart.isEmpty()) return null

    val sign = if (integerDigits.startsWith('-')) "-" else ""
    val integer = integerDigits.trimStart('-').ifEmpty { "0" }
    return "$sign$integer.${fractionPart.ifEmpty { "0" }}".toDouble()
}

/**
 * Valor guardado devolvido ao campo de texto com o separador do idioma ("10,00" em pt-BR,
 * "10.00" em inglês), que [parseDecimalInput] lê de volta. Zero vira campo vazio.
 *
 * Mostra no mínimo 2 casas, mas não corta as que existirem: um preço 12.345 salvo de novo sem
 * alteração não pode virar 12.35.
 */
fun formatDecimalInput(value: Double, locale: Locale = Locale.getDefault()): String {
    if (value == 0.0) return ""
    val decimal = BigDecimal.valueOf(value)
    val plain = decimal.setScale(maxOf(2, decimal.stripTrailingZeros().scale())).toPlainString()
    return plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}

/**
 * Quantidade de item para exibir: sem casas quando inteira ("2"), com até 3 quando não
 * ("2,5" em pt-BR, "2.5" em inglês).
 */
fun formatQuantity(quantity: Double, locale: Locale = Locale.getDefault()): String {
    val plain = BigDecimal.valueOf(quantity)
        .setScale(3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}

private val DIGITS = Regex("-?\\d+")
private val INTEGER = Regex("-?\\d*")
private val FRACTION = Regex("\\d*")
