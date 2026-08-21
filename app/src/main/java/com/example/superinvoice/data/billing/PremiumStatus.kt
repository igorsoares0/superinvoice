package com.example.superinvoice.data.billing

/**
 * O acesso premium do usuário.
 *
 * [Unknown] existe porque o RevenueCat resolve o CustomerInfo de forma
 * assíncrona: tratar "ainda não sei" como "não é premium" fazia o assinante
 * receber PDF com marca d'água nos primeiros segundos depois de abrir o app.
 *
 * Quem toma decisão irreversível — renderizar PDF, liberar a criação de fatura —
 * deve esperar o status resolver com [BillingManager.awaitPremiumStatus]. Quem só
 * mostra ou esconde algo na tela pode usar [BillingManager.isPremium].
 */
enum class PremiumStatus {
    Unknown,
    Free,
    Premium;

    val isPremium: Boolean get() = this == Premium
}
