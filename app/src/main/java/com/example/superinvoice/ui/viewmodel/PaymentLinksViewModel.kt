package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.payment.PayPalMe
import com.example.superinvoice.data.payment.PaymentLinkUrl
import com.example.superinvoice.data.payment.PixKey
import com.example.superinvoice.data.payment.PixKeyType
import com.example.superinvoice.data.payment.PixPayload
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** O que está digitado na tela de links de pagamento. */
data class PaymentLinksInput(
    val pixKeyType: PixKeyType = PixKeyType.CPF,
    val pixKey: String = "",
    val pixMerchantName: String = "",
    val pixMerchantCity: String = "",
    val payPalUsername: String = "",
    val paymentLinkUrl: String = "",
)

data class PaymentLinksErrors(
    val pixKey: Boolean = false,
    val pixMerchantName: Boolean = false,
    val pixMerchantCity: Boolean = false,
    val payPalUsername: Boolean = false,
    val paymentLinkUrl: Boolean = false,
) {
    val any: Boolean
        get() = pixKey || pixMerchantName || pixMerchantCity || payPalUsername || paymentLinkUrl
}

/**
 * Valida e normaliza tudo de uma vez, sem Android, para dar para testar.
 *
 * Campo vazio é sempre válido: significa "não uso esse meio". Nome e cidade só são exigidos
 * quando há chave Pix, e aí valem os do negócio se o usuário deixar em branco — o mesmo
 * fallback de `SettingsRepository.paymentLinkSettings`.
 *
 * @return a entrada normalizada (null se houver erro) e os erros por campo
 */
fun PaymentLinksInput.validate(
    businessName: String,
    businessCity: String,
): Pair<PaymentLinksInput?, PaymentLinksErrors> {
    val key = pixKey.takeIf { it.isNotBlank() }?.let { PixKey.normalize(pixKeyType, it) }
    val hasPix = pixKey.isNotBlank()
    // Sem nada que sobreviva à limpeza do BR Code (só acento, só emoji), o banco recusaria.
    val nameMissing = hasPix &&
        PixPayload.sanitize(pixMerchantName.ifBlank { businessName }).isEmpty()
    val cityMissing = hasPix &&
        PixPayload.sanitize(pixMerchantCity.ifBlank { businessCity }).isEmpty()
    val payPal = payPalUsername.takeIf { it.isNotBlank() }?.let { PayPalMe.normalizeUsername(it) }
    val link = paymentLinkUrl.takeIf { it.isNotBlank() }?.let { PaymentLinkUrl.normalize(it) }

    val errors = PaymentLinksErrors(
        pixKey = hasPix && key == null,
        pixMerchantName = nameMissing,
        pixMerchantCity = cityMissing,
        payPalUsername = payPalUsername.isNotBlank() && payPal == null,
        paymentLinkUrl = paymentLinkUrl.isNotBlank() && link == null,
    )
    if (errors.any) return null to errors

    return copy(
        pixKey = key ?: "",
        pixMerchantName = pixMerchantName.trim(),
        pixMerchantCity = pixMerchantCity.trim(),
        payPalUsername = payPal ?: "",
        paymentLinkUrl = link ?: "",
    ) to errors
}

data class PaymentLinksUiState(
    val input: PaymentLinksInput = PaymentLinksInput(),
    val errors: PaymentLinksErrors = PaymentLinksErrors(),
    /** Decisão L5: a seção Pix é ruído para quem não fatura em real. */
    val showPix: Boolean = false,
    /** Mostrados como placeholder de nome e cidade do Pix, que caem neles se vazios. */
    val businessName: String = "",
    val businessCity: String = "",
    /** Há imagem em "QR code de pagamento": ela vence o PayPal e o link (decisão L2). */
    val hasUploadedQr: Boolean = false,
) {
    /**
     * O que o QR da fatura vai conter, sem valor, para a prévia da tela. Segue a ordem da
     * decisão L2 (`PaymentLinksBuilder`). Null enquanto algum campo estiver inválido.
     */
    val qrPreview: String?
        get() {
            val valid = input.validate(businessName, businessCity).first ?: return null
            if (showPix && valid.pixKey.isNotBlank()) {
                return PixPayload.build(
                    key = valid.pixKey,
                    name = valid.pixMerchantName.ifBlank { businessName },
                    city = valid.pixMerchantCity.ifBlank { businessCity },
                    amount = null,
                    txid = ""
                )
            }
            if (hasUploadedQr) return null
            if (valid.payPalUsername.isNotBlank()) {
                return PayPalMe.url(valid.payPalUsername, amount = null, currency = "")
            }
            return valid.paymentLinkUrl.ifBlank { null }
        }

    /** PayPal ou link configurado, mas o QR do PDF é a imagem enviada. */
    val uploadedQrWins: Boolean
        get() = hasUploadedQr && input.pixKey.isBlank() &&
            (input.payPalUsername.isNotBlank() || input.paymentLinkUrl.isNotBlank())
}

@HiltViewModel
class PaymentLinksViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentLinksUiState())
    val state: StateFlow<PaymentLinksUiState> = _state.asStateFlow()

    init {
        // Leitura única: com `collect`, uma gravação do DataStore no meio da digitação
        // apagaria o que o usuário acabou de escrever.
        viewModelScope.launch {
            val pixKey = settingsRepository.pixKey.first()
            _state.value = PaymentLinksUiState(
                input = PaymentLinksInput(
                    pixKeyType = settingsRepository.pixKeyType.first(),
                    pixKey = pixKey,
                    pixMerchantName = settingsRepository.pixMerchantName.first(),
                    pixMerchantCity = settingsRepository.pixMerchantCity.first(),
                    payPalUsername = settingsRepository.payPalUsername.first(),
                    paymentLinkUrl = settingsRepository.paymentLinkUrl.first(),
                ),
                showPix = settingsRepository.currency.first() == "BRL" || pixKey.isNotBlank(),
                businessName = settingsRepository.businessName.first(),
                businessCity = settingsRepository.businessCity.first(),
                hasUploadedQr = settingsRepository.paymentQrCodePath.first().isNotEmpty(),
            )
        }
    }

    fun setPixKeyType(value: PixKeyType) = edit({ it.copy(pixKeyType = value) }) {
        it.copy(pixKey = false)
    }

    fun setPixKey(value: String) = edit({ it.copy(pixKey = value) }) { it.copy(pixKey = false) }

    fun setPixMerchantName(value: String) = edit({ it.copy(pixMerchantName = value) }) {
        it.copy(pixMerchantName = false)
    }

    fun setPixMerchantCity(value: String) = edit({ it.copy(pixMerchantCity = value) }) {
        it.copy(pixMerchantCity = false)
    }

    fun setPayPalUsername(value: String) = edit({ it.copy(payPalUsername = value) }) {
        it.copy(payPalUsername = false)
    }

    fun setPaymentLinkUrl(value: String) = edit({ it.copy(paymentLinkUrl = value) }) {
        it.copy(paymentLinkUrl = false)
    }

    /** Erro de campo some quando o usuário mexe nele, não só no próximo "Salvar". */
    private fun edit(
        input: (PaymentLinksInput) -> PaymentLinksInput,
        clearError: (PaymentLinksErrors) -> PaymentLinksErrors,
    ) {
        _state.update { it.copy(input = input(it.input), errors = clearError(it.errors)) }
    }

    fun save(onSuccess: () -> Unit) {
        val current = _state.value
        val (valid, errors) = current.input.validate(current.businessName, current.businessCity)
        _state.update { it.copy(errors = errors) }
        if (valid == null) return

        viewModelScope.launch {
            settingsRepository.savePaymentLinks(
                pixKeyType = valid.pixKeyType,
                pixKey = valid.pixKey,
                pixMerchantName = valid.pixMerchantName,
                pixMerchantCity = valid.pixMerchantCity,
                payPalUsername = valid.payPalUsername,
                paymentLinkUrl = valid.paymentLinkUrl
            )
            analyticsManager.logPaymentLinksSaved(
                hasPix = valid.pixKey.isNotBlank(),
                hasPayPal = valid.payPalUsername.isNotBlank(),
                hasCustomLink = valid.paymentLinkUrl.isNotBlank()
            )
            onSuccess()
        }
    }

    /**
     * Um Pix de R$ 1,00 com o que está na tela, para o usuário colar no app do banco e conferir
     * nome e chave antes de mandar a primeira fatura (spec, 5.3). Null se faltar chave ou algum
     * campo for inválido; os erros ficam marcados na tela.
     */
    fun testPixPayload(): String? {
        val current = _state.value
        val (valid, errors) = current.input.validate(current.businessName, current.businessCity)
        val missingKey = current.input.pixKey.isBlank()
        _state.update { it.copy(errors = errors.copy(pixKey = errors.pixKey || missingKey)) }
        if (valid == null || missingKey) return null

        return PixPayload.build(
            key = valid.pixKey,
            name = valid.pixMerchantName.ifBlank { current.businessName },
            city = valid.pixMerchantCity.ifBlank { current.businessCity },
            amount = 1.0,
            txid = "TESTE"
        )
    }
}
