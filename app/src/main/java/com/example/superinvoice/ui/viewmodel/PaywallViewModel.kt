package com.example.superinvoice.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.analytics.PaywallSource
import com.example.superinvoice.data.analytics.PlanType
import com.example.superinvoice.data.billing.BillingManager
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import online.isdevapps.superinvoice.R
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val analyticsManager: AnalyticsManager,
    private val crashReporter: CrashReporter,
    private val application: Application
) : ViewModel() {

    /**
     * De onde esta exibição do paywall veio. A tela vive no escopo da Activity e é
     * reutilizada entre aberturas, então a origem é reinformada a cada [onScreenShown]
     * em vez de ser fixada na construção.
     */
    private var currentSource: PaywallSource = PaywallSource.SETTINGS

    /** Evita contar duas vezes o mesmo par exibição/fechamento. */
    private var shownReported = false

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
    fun onScreenShown(source: PaywallSource) {
        currentSource = source
        _purchaseSuccess.value = false
        _errorMessage.value = null
        if (!shownReported) {
            shownReported = true
            analyticsManager.logPaywallShown(source)
        }
        if (!hasPlans && !offeringsInFlight) {
            loadOfferings()
        }
    }

    /**
     * Fechar sem comprar é o dado que falta para saber qual gate só irrita. Chamado
     * tanto no botão de fechar quanto no voltar do sistema.
     */
    fun onScreenDismissed() {
        if (!shownReported) return
        shownReported = false
        analyticsManager.logPaywallDismissed(currentSource, purchased = _purchaseSuccess.value)
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
        val plan = planTypeOf(pkg)
        analyticsManager.logPurchaseStarted(plan)
        billingManager.purchase(
            activity = activity,
            packageToPurchase = pkg,
            onSuccess = { _, _ ->
                _isLoading.value = false
                _purchaseSuccess.value = true
                analyticsManager.logPurchaseCompleted(plan)
            },
            onError = { error, userCancelled ->
                _isLoading.value = false
                // Fechar a folha do Google Play não é erro; mostrar mensagem vermelha
                // para quem só desistiu é ruído.
                if (!userCancelled) {
                    _errorMessage.value = error.message
                    analyticsManager.logPurchaseFailed(
                        plan,
                        BillingManager.failureReasonOf(error)
                    )
                    crashReporter.recordException(
                        IllegalStateException("Purchase failed: ${error.code}"),
                        "purchase ${plan.id} falhou"
                    )
                }
            }
        )
    }

    /**
     * O identifier do pacote é configurável no dashboard, então a classificação sai do
     * [PackageType], que o RevenueCat normaliza.
     */
    private fun planTypeOf(pkg: Package): PlanType = when (pkg.packageType) {
        PackageType.MONTHLY -> PlanType.MONTHLY
        PackageType.ANNUAL -> PlanType.ANNUAL
        else -> PlanType.UNKNOWN
    }

    fun restorePurchases() {
        _isLoading.value = true
        _errorMessage.value = null
        billingManager.restorePurchases(
            onSuccess = { _ ->
                _isLoading.value = false
                val found = billingManager.isPremium.value
                analyticsManager.logPurchaseRestored(found)
                if (found) {
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
