package com.example.superinvoice

import android.app.Application
import com.example.superinvoice.data.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SuperInvoiceApplication : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        billingManager.initialize(this)
    }
}
