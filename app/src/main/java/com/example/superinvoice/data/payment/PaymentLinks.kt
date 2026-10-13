package com.example.superinvoice.data.payment

import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import java.net.URI
import java.util.Locale

/**
 * O que o usuário configurou na tela de links de pagamento, já normalizado ao salvar.
 * [pixMerchantName] e [pixMerchantCity] chegam aqui já com o fallback para o nome e a cidade
 * do negócio aplicado.
 */
data class PaymentLinkSettings(
    val pixKey: String = "",
    val pixMerchantName: String = "",
    val pixMerchantCity: String = "",
    val payPalUsername: String = "",
    val paymentLinkUrl: String = "",
)

/** De onde vem o QR do bloco de pagamento (decisão L2). Também vai para o analytics. */
enum class PaymentQrSource { NONE, UPLOADED, PIX, PAYPAL, CUSTOM_LINK }

/** Os meios de pagamento de uma fatura. Tudo nulo = nada a mostrar além dos dados bancários. */
data class PaymentLinks(
    /** A chave, para a linha "Pix:" do PDF. */
    val pixKey: String? = null,
    /** O copia e cola, com o saldo devedor. */
    val pixPayload: String? = null,
    val payPalUrl: String? = null,
    val customUrl: String? = null,
    val qrSource: PaymentQrSource = PaymentQrSource.NONE,
    /** Conteúdo do QR a gerar; null quando [qrSource] é `UPLOADED` (imagem) ou `NONE`. */
    val qrContent: String? = null,
) {
    val hasAny: Boolean get() = pixKey != null || payPalUrl != null || customUrl != null

    /**
     * Decisão L1: o QR gerado é premium. Sem premium ele não sai, e a imagem enviada pelo
     * usuário (grátis) volta a valer no lugar dele. As linhas de texto continuam.
     */
    fun forPlan(isPremium: Boolean, hasUploadedQr: Boolean): PaymentLinks =
        if (isPremium || qrContent == null) {
            this
        } else {
            copy(
                qrContent = null,
                qrSource = if (hasUploadedQr) PaymentQrSource.UPLOADED else PaymentQrSource.NONE
            )
        }

    companion object {
        val NONE = PaymentLinks()
    }
}

object PaymentLinksBuilder {

    private const val PIX_CURRENCY = "BRL"

    /**
     * Fatura paga ou cancelada e orçamento não ganham link (decisões L3 e L4). O valor cobrado é
     * o saldo, não o total, para quem já pagou uma parte.
     *
     * @param hasUploadedQr o usuário enviou uma imagem de QR em "QR code de pagamento"
     */
    fun forInvoice(
        invoice: Invoice,
        settings: PaymentLinkSettings,
        hasUploadedQr: Boolean,
    ): PaymentLinks {
        val uploaded = if (hasUploadedQr) {
            PaymentLinks(qrSource = PaymentQrSource.UPLOADED)
        } else {
            PaymentLinks.NONE
        }
        if (invoice.type == DocumentType.ESTIMATE) return uploaded
        if (invoice.status == InvoiceStatus.CANCELLED) return uploaded

        val balance = InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)
        if (balance <= 0.0) return uploaded

        val pixKey = settings.pixKey.takeIf { it.isNotBlank() && invoice.currency == PIX_CURRENCY }
        val pixPayload = pixKey?.let {
            PixPayload.build(
                key = it,
                name = settings.pixMerchantName,
                city = settings.pixMerchantCity,
                amount = balance,
                txid = invoice.number
            )
        }
        val payPalUrl = settings.payPalUsername.takeIf { it.isNotBlank() }
            ?.let { PayPalMe.url(it, balance, invoice.currency) }
        val customUrl = settings.paymentLinkUrl.takeIf { it.isNotBlank() }

        // Ordem da decisão L2: só cabe um QR no bloco.
        val (qrSource, qrContent) = when {
            pixPayload != null -> PaymentQrSource.PIX to pixPayload
            hasUploadedQr -> PaymentQrSource.UPLOADED to null
            payPalUrl != null -> PaymentQrSource.PAYPAL to payPalUrl
            customUrl != null -> PaymentQrSource.CUSTOM_LINK to customUrl
            else -> PaymentQrSource.NONE to null
        }

        return PaymentLinks(
            // Sem payload válido (nome ou cidade vazios), a linha "Pix:" também não aparece:
            // o pagador não teria como conferir para quem está pagando.
            pixKey = pixKey.takeIf { pixPayload != null },
            pixPayload = pixPayload,
            payPalUrl = payPalUrl,
            customUrl = customUrl,
            qrSource = qrSource,
            qrContent = qrContent
        )
    }
}

object PaymentLinkUrl {

    /**
     * Link de pagamento de verdade tem bem menos que isso. Acima, o QR fica denso demais para a
     * câmera do pagador (e a partir de ~2.300 caracteres nem cabe num QR).
     */
    private const val MAX_LENGTH = 500

    /**
     * Só `https://`: um link de pagamento sem TLS é sinal de golpe para o pagador. Sem esquema,
     * o `https://` é acrescentado, porque é comum colar "buy.stripe.com/…".
     *
     * @return a URL pronta para salvar, ou null se não for válida
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH || trimmed.any { it.isWhitespace() }) {
            return null
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        val withScheme = when {
            lower.startsWith("https://") -> trimmed
            "://" in lower -> return null
            else -> "https://$trimmed"
        }
        val host = runCatching { URI(withScheme).host }.getOrNull() ?: return null
        return withScheme.takeIf { '.' in host && !host.startsWith('.') && !host.endsWith('.') }
    }
}
