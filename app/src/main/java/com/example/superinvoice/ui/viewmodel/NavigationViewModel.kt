package com.example.superinvoice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository,
    private val analyticsManager: AnalyticsManager,
    private val application: Application
) : ViewModel() {

    init {
        // A faixa de faturas criadas é a dimensão pela qual quase todo relatório vai ser
        // fatiado (quem está perto do limite se comporta diferente de quem acabou de
        // instalar), então acompanha o contador em vez de ser lida uma vez.
        viewModelScope.launch {
            settingsRepository.totalInvoicesCreated.collect { count ->
                analyticsManager.setInvoiceCountBucket(count)
            }
        }
    }

    fun onScreenShown(screenName: String) {
        analyticsManager.logScreenView(screenName)
    }

    fun onInvoiceCreationStarted() {
        analyticsManager.logInvoiceStarted()
    }

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    val invoiceCount: StateFlow<Int> = settingsRepository.totalInvoicesCreated
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Suspende até saber se o usuário é premium antes de decidir.
     *
     * Lendo `isPremium.value` direto, um assinante que abrisse o app e tocasse em "nova
     * fatura" nos primeiros segundos era mandado para o paywall, porque o status ainda
     * não tinha voltado do RevenueCat.
     */
    suspend fun canCreateInvoice(): Boolean {
        if (billingManager.awaitPremiumStatus()) return true
        return invoiceCount.value < BillingManager.FREE_INVOICE_LIMIT
    }

    fun restorePurchases(onSuccess: () -> Unit, onError: (String) -> Unit) {
        billingManager.restorePurchases(
            onSuccess = { _ ->
                if (billingManager.isPremium.value) {
                    onSuccess()
                } else {
                    onError(application.getString(R.string.no_active_subscription_found))
                }
            },
            onError = { message -> onError(message) }
        )
    }
}
