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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    init {
        loadOfferings()
    }

    fun resetState() {
        _purchaseSuccess.value = false
        _errorMessage.value = null
    }

    private fun loadOfferings() {
        _isLoading.value = true
        billingManager.fetchOfferings(
            onSuccess = { monthly, annual ->
                _monthlyPackage.value = monthly
                _annualPackage.value = annual
                _isLoading.value = false
            },
            onError = { message ->
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
            onError = { message ->
                _errorMessage.value = message
                _isLoading.value = false
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
