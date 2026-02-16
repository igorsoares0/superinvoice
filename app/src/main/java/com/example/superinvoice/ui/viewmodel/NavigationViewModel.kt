package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    val invoiceCount: StateFlow<Int> = settingsRepository.totalInvoicesCreated
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun canCreateInvoice(): Boolean {
        return billingManager.isPremium.value || invoiceCount.value < BillingManager.FREE_INVOICE_LIMIT
    }

    fun restorePurchases(onSuccess: () -> Unit, onError: (String) -> Unit) {
        billingManager.restorePurchases(
            onSuccess = { _ ->
                if (billingManager.isPremium.value) {
                    onSuccess()
                } else {
                    onError("No active subscription found")
                }
            },
            onError = { message -> onError(message) }
        )
    }
}
