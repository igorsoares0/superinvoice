package com.example.superinvoice.util

import androidx.annotation.StringRes
import com.example.superinvoice.data.database.entities.PaymentMethod
import online.isdevapps.superinvoice.R

/** Nome do método na língua do app; usado na tela e no recibo em PDF. */
@get:StringRes
val PaymentMethod.labelRes: Int
    get() = when (this) {
        PaymentMethod.CASH -> R.string.payment_method_cash
        PaymentMethod.BANK_TRANSFER -> R.string.payment_method_bank_transfer
        PaymentMethod.CARD -> R.string.payment_method_card
        PaymentMethod.PIX -> R.string.payment_method_pix
        PaymentMethod.PAYPAL -> R.string.payment_method_paypal
        PaymentMethod.CHECK -> R.string.payment_method_check
        PaymentMethod.OTHER -> R.string.payment_method_other
    }
