package com.example.superinvoice.data.payment

import java.util.Locale

/**
 * O tipo é escolhido pelo usuário, não adivinhado: celular sem DDI e CPF têm os dois 11 dígitos
 * (`docs/spec-links-pagamento.md`, seção 5.1).
 */
enum class PixKeyType { CPF, CNPJ, EMAIL, PHONE, EVP }

object PixKey {

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val EVP = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    private const val MAX_EMAIL_LENGTH = 77

    /**
     * A chave no formato que o BR Code espera, ou null se não for válida para o tipo.
     * Aceita a chave com máscara ("123.456.789-09", "(11) 98765-4321") porque é assim que o
     * usuário copia do app do banco.
     */
    fun normalize(type: PixKeyType, raw: String): String? {
        val trimmed = raw.trim()
        return when (type) {
            PixKeyType.CPF -> digitsOnly(trimmed).takeIf { it.length == 11 && isValidCpf(it) }
            PixKeyType.CNPJ -> digitsOnly(trimmed).takeIf { it.length == 14 && isValidCnpj(it) }
            PixKeyType.EMAIL -> trimmed.lowercase(Locale.ROOT)
                .takeIf { it.length <= MAX_EMAIL_LENGTH && EMAIL.matches(it) }
            PixKeyType.PHONE -> normalizePhone(trimmed)
            PixKeyType.EVP -> trimmed.lowercase(Locale.ROOT).takeIf { EVP.matches(it) }
        }
    }

    /**
     * `+55` + DDD + número. Sem DDI o número tem 10 ou 11 dígitos; com DDI, 12 ou 13. O DDD 55
     * existe (RS), então "55 98765-4321" é número local, e só o tamanho decide.
     */
    private fun normalizePhone(raw: String): String? {
        val digits = digitsOnly(raw)
        val withCountry = when {
            raw.startsWith("+") -> digits.takeIf { it.startsWith("55") && it.length in 12..13 }
            digits.length in 10..11 -> "55$digits"
            digits.length in 12..13 && digits.startsWith("55") -> digits
            else -> null
        }
        return withCountry?.let { "+$it" }
    }

    private fun digitsOnly(value: String): String = value.filter { it in '0'..'9' }

    private fun isValidCpf(cpf: String): Boolean {
        if (cpf.all { it == cpf[0] }) return false
        val digits = cpf.map { it - '0' }
        return checkDigit(digits.take(9), weights = (10 downTo 2).toList()) == digits[9] &&
            checkDigit(digits.take(10), weights = (11 downTo 2).toList()) == digits[10]
    }

    private fun isValidCnpj(cnpj: String): Boolean {
        if (cnpj.all { it == cnpj[0] }) return false
        val digits = cnpj.map { it - '0' }
        val first = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        return checkDigit(digits.take(12), first) == digits[12] &&
            checkDigit(digits.take(13), listOf(6) + first) == digits[13]
    }

    /** Módulo 11, comum a CPF e CNPJ: resto menor que 2 dá dígito 0. */
    private fun checkDigit(digits: List<Int>, weights: List<Int>): Int {
        val rest = digits.zip(weights).sumOf { (d, w) -> d * w } % 11
        return if (rest < 2) 0 else 11 - rest
    }
}
