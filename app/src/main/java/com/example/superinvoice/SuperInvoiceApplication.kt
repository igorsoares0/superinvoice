package com.example.superinvoice

import android.app.Application
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SuperInvoiceApplication : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Vive enquanto o processo viver; não há o que cancelar. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        billingManager.initialize(this)

        // O consentimento é observado, não lido uma vez: desligar na tela de ajustes
        // precisa valer na hora, sem reabrir o app.
        applicationScope.launch {
            settingsRepository.analyticsEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    analyticsManager.setCollectionEnabled(enabled)
                    crashReporter.setCollectionEnabled(enabled)
                }
        }

        // Manda o App Instance ID para o RevenueCat. É o que permite perguntar "quem
        // assinou tinha travado em qual passo?" — sem isso, o funil do Firebase e a
        // receita do RevenueCat ficam sendo dois relatórios que não se cruzam.
        analyticsManager.fetchAppInstanceId { id ->
            if (id != null) billingManager.setFirebaseAppInstanceId(id)
        }
    }
}
