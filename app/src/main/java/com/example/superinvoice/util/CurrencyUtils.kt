package com.example.superinvoice.util

fun getCurrencySymbol(currencyCode: String): String {
    return when (currencyCode) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "BRL" -> "R$"
        "JPY" -> "¥"
        "CAD" -> "C$"
        "AUD" -> "A$"
        "CHF" -> "CHF"
        "CNY" -> "¥"
        "INR" -> "₹"
        "MXN" -> "MX$"
        "ARS" -> "AR$"
        else -> "$"
    }
}
