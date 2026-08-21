package com.example.superinvoice.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import com.example.superinvoice.data.billing.BillingManager
import com.revenuecat.purchases.Package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import online.isdevapps.superinvoice.R
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val application: Application
) : ViewModel() {

    private val _monthlyPackage = MutableStateFlow<Package?>(null)
    val monthlyPackage: StateFlow<Package?> = _monthlyPackage.asStateFlow()

    private val _annualPackage = MutableStateFlow<Package?>(null)
    val annualPackage: StateFlow<Package?> = _annualPackage.asStateFlow()

    // Começa carregando: onScreenShown dispara na primeira composição, e sem isso o
    // botão de retry apareceria por um frame antes da primeira busca.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    private val hasPlans: Boolean
        get() = _monthlyPackage.value != null || _annualPackage.value != null

    /** Só sobre a busca de offerings — `isLoading` também cobre compra e restore. */
    private var offeringsInFlight = false

    /**
     * Chamado toda vez que o paywall aparece.
     *
     * A navegação do app é um `when` sobre estado, sem NavHost, então este ViewModel
     * vive no escopo da Activity e sobrevive a sair e voltar para a tela. Carregar as
     * offerings no `init` significava que uma falha na primeira abertura (usuário sem
     * rede naquele instante) nunca era tentada de novo. Aqui a gente retenta.
     */
    fun onScreenShown() {
        _purchaseSuccess.value = false
        _errorMessage.value = null
        if (!hasPlans && !offeringsInFlight) {
            loadOfferings()
        }
    }

    fun retry() {
        if (!offeringsInFlight) loadOfferings()
    }

    /** A tela não conseguiu resolver a Activity para abrir a folha do Google Play. */
    fun reportCheckoutUnavailable() {
        _errorMessage.value = application.getString(R.string.checkout_unavailable)
    }

    private fun loadOfferings() {
        offeringsInFlight = true
        _isLoading.value = true
        _errorMessage.value = null
        billingManager.fetchOfferings(
            onSuccess = { monthly, annual ->
                _monthlyPackage.value = monthly
                _annualPackage.value = annual
                offeringsInFlight = false
                _isLoading.value = false
                if (!hasPlans) {
                    // A chamada funcionou mas a offering não trouxe plano nenhum —
                    // configuração faltando no dashboard. Melhor dizer isso do que
                    // mostrar um botão que não faz nada.
                    _errorMessage.value = application.getString(R.string.plans_unavailable)
                }
            },
            onError = { message ->
                offeringsInFlight = false
                _errorMessage.value = message
                _isLoading.value = false
            }
        )
    }

    fun purchase(activity: Activity, pkg: Package) {
        _isLoading.value = true
        _errorMessage.value = null
        billingManager.purchase(
            activity = activity,
            packageToPurchase = pkg,
            onSuccess = { _, _ ->
                _isLoading.value = false
                _purchaseSuccess.value = true
            },
            onError = { error, userCancelled ->
                _isLoading.value = false
                // Fechar a folha do Google Play não é erro; mostrar mensagem vermelha
                // para quem só desistiu é ruído.
                if (!userCancelled) {
                    _errorMessage.value = error.message
                }
            }
        )
    }

    fun restorePurchases() {
        _isLoading.value = true
        _errorMessage.value = null
        billingManager.restorePurchases(
            onSuccess = { _ ->
                _isLoading.value = false
                if (billingManager.isPremium.value) {
                    _purchaseSuccess.value = true
                } else {
                    _errorMessage.value = application.getString(R.string.no_active_subscription_found)
                }
            },
            onError = { message ->
                _errorMessage.value = message
                _isLoading.value = false
            }
        )
    }
}
