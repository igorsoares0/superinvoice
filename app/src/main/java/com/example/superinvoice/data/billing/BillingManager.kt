package com.example.superinvoice.data.billing

import android.app.Activity
import android.app.Application
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor() : UpdatedCustomerInfoListener {

    companion object {
        private const val REVENUECAT_API_KEY = "test_HgGjQIBBJJDnKypkRLPfTsBoFGO"
        private const val ENTITLEMENT_ID = "premium"
        const val FREE_INVOICE_LIMIT = 5
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val invoiceLimit: Int
        get() = if (_isPremium.value) Int.MAX_VALUE else FREE_INVOICE_LIMIT

    fun initialize(application: Application) {
        Purchases.configure(
            PurchasesConfiguration.Builder(application, REVENUECAT_API_KEY).build()
        )
        Purchases.sharedInstance.updatedCustomerInfoListener = this
        refreshPremiumStatus()
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        _isPremium.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
    }

    private fun refreshPremiumStatus() {
        Purchases.sharedInstance.getCustomerInfo(
            callback = object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPremium.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) { }
            }
        )
    }

    fun fetchOfferings(
        onSuccess: (monthly: Package?, annual: Package?) -> Unit,
        onError: (String) -> Unit
    ) {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error -> onError(error.message) },
            onSuccess = { offerings ->
                val current = offerings.current
                val monthly = current?.monthly
                val annual = current?.annual
                onSuccess(monthly, annual)
            }
        )
    }

    fun purchase(
        activity: Activity,
        packageToPurchase: Package,
        onSuccess: (StoreTransaction?, CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()
        Purchases.sharedInstance.purchaseWith(
            purchaseParams = purchaseParams,
            onError = { error, _ -> onError(error.message) },
            onSuccess = { transaction, customerInfo ->
                _isPremium.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                onSuccess(transaction, customerInfo)
            }
        )
    }

    fun restorePurchases(
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error -> onError(error.message) },
            onSuccess = { customerInfo ->
                _isPremium.value = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                onSuccess(customerInfo)
            }
        )
    }
}
