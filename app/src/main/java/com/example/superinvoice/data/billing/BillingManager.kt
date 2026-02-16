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
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import online.isdevapps.superinvoice.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor() : UpdatedCustomerInfoListener {

    companion object {
        private val REVENUECAT_API_KEY = BuildConfig.REVENUECAT_API_KEY
        private const val ENTITLEMENT_ID = "premium"
        const val FREE_INVOICE_LIMIT = 5
        private const val TAG = "BillingManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 2000L
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

    private fun checkPremium(customerInfo: CustomerInfo): Boolean {
        return customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
            || customerInfo.activeSubscriptions.isNotEmpty()
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        _isPremium.value = checkPremium(customerInfo)
    }

    private fun refreshPremiumStatus(attempt: Int = 0) {
        Purchases.sharedInstance.getCustomerInfo(
            callback = object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPremium.value = checkPremium(customerInfo)
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    Log.w(TAG, "Failed to refresh premium status (attempt ${attempt + 1}): ${error.message}")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1L shl attempt)
                        Handler(Looper.getMainLooper()).postDelayed({
                            refreshPremiumStatus(attempt + 1)
                        }, delayMs)
                    } else {
                        Log.e(TAG, "Exhausted retries for premium status refresh")
                    }
                }
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
                _isPremium.value = checkPremium(customerInfo)
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
                _isPremium.value = checkPremium(customerInfo)
                onSuccess(customerInfo)
            }
        )
    }
}
