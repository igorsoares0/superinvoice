package com.example.superinvoice.data.billing

import android.app.Activity
import android.app.Application
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.LogLevel
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.analytics.PurchaseFailureReason
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import online.isdevapps.superinvoice.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val analyticsManager: AnalyticsManager,
    private val crashReporter: CrashReporter
) : UpdatedCustomerInfoListener {

    companion object {
        private val REVENUECAT_API_KEY = BuildConfig.REVENUECAT_API_KEY
        /**
         * Tem de bater exatamente com o identifier do entitlement no dashboard do
         * RevenueCat. O nome é esquisito porque veio do catálogo de teste criado em
         * jan/2026 e o RevenueCat não deixa renomear identifier depois de criado —
         * só o Display Name. Para trocar, seria preciso criar um entitlement novo e
         * reanexar os produtos.
         */
        private const val ENTITLEMENT_ID = "is dev Pro"
        const val FREE_INVOICE_LIMIT = 5
        private const val TAG = "BillingManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 2000L

        /**
         * Teto de espera do [awaitPremiumStatus]. Na prática nunca é atingido: o
         * RevenueCat cacheia o CustomerInfo e, se todas as tentativas falharem, o
         * status resolve para [PremiumStatus.Free] em ~14s. É só uma rede de segurança
         * para nenhuma tela ficar presa para sempre.
         */
        private const val AWAIT_TIMEOUT_MS = 15_000L

        /**
         * Agrupa o erro do RevenueCat numa das poucas categorias que mudam decisão.
         * Só o grupo vai para o Analytics; a mensagem completa fica no Crashlytics.
         */
        fun failureReasonOf(error: PurchasesError): PurchaseFailureReason =
            when (error.code) {
                PurchasesErrorCode.NetworkError ->
                    PurchaseFailureReason.NETWORK
                PurchasesErrorCode.StoreProblemError,
                PurchasesErrorCode.OperationAlreadyInProgressError ->
                    PurchaseFailureReason.STORE_UNAVAILABLE
                PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                PurchasesErrorCode.ConfigurationError ->
                    PurchaseFailureReason.PRODUCT_UNAVAILABLE
                PurchasesErrorCode.ProductAlreadyPurchasedError,
                PurchasesErrorCode.ReceiptAlreadyInUseError ->
                    PurchaseFailureReason.ALREADY_OWNED
                PurchasesErrorCode.PurchaseNotAllowedError,
                PurchasesErrorCode.InsufficientPermissionsError ->
                    PurchaseFailureReason.NOT_ALLOWED
                else -> PurchaseFailureReason.OTHER
            }
    }

    private val _premiumStatus = MutableStateFlow(PremiumStatus.Unknown)
    val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    private val _isPremium = MutableStateFlow(false)

    /**
     * Atalho para UI que só mostra ou esconde algo — [PremiumStatus.Unknown] conta
     * como não-premium. Para decisão irreversível use [awaitPremiumStatus].
     */
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val invoiceLimit: Int
        get() = if (_isPremium.value) Int.MAX_VALUE else FREE_INVOICE_LIMIT

    fun initialize(application: Application) {
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(application, REVENUECAT_API_KEY).build()
        )
        Purchases.sharedInstance.updatedCustomerInfoListener = this
        refreshPremiumStatus()
        if (BuildConfig.DEBUG) logOfferings()
    }

    /**
     * Diagnóstico: imprime o preço que o paywall vai mostrar, direto da loja. Serve para
     * conferir moeda e valor sem precisar chegar na tela — que fica inacessível para
     * quem já é assinante.
     */
    private fun logOfferings() {
        fetchOfferings(
            onSuccess = { monthly, annual ->
                Log.i(TAG, "--- offerings ---")
                listOfNotNull(
                    monthly?.let { "monthly" to it },
                    annual?.let { "annual" to it }
                ).forEach { (label, pkg) ->
                    val price = pkg.product.price
                    Log.i(TAG, "$label: id=${pkg.identifier} type=${pkg.packageType} " +
                        "product=${pkg.product.id} formatted='${price.formatted}' " +
                        "currency=${price.currencyCode} micros=${price.amountMicros}")
                }
                if (monthly == null) Log.w(TAG, "monthly package AUSENTE")
                if (annual == null) Log.w(TAG, "annual package AUSENTE")
                Log.i(TAG, "-----------------")
            },
            onError = { message -> Log.e(TAG, "offerings falhou: $message") }
        )
    }

    /**
     * Anexa o App Instance ID do Firebase ao usuário do RevenueCat, como subscriber
     * attribute. É o que liga uma assinatura à sessão que a antecedeu no Analytics.
     */
    fun setFirebaseAppInstanceId(id: String) {
        runCatching { Purchases.sharedInstance.setFirebaseAppInstanceID(id) }
            .onFailure { crashReporter.recordException(it, "setFirebaseAppInstanceId") }
    }

    /**
     * Suspende até o status sair de [PremiumStatus.Unknown] e devolve se o usuário é
     * premium. Use antes de renderizar PDF ou liberar cota — nunca `isPremium.value`.
     */
    suspend fun awaitPremiumStatus(): Boolean {
        val resolved = withTimeoutOrNull(AWAIT_TIMEOUT_MS) {
            _premiumStatus.first { it != PremiumStatus.Unknown }
        }
        if (resolved == null) {
            Log.w(TAG, "Premium status unresolved after ${AWAIT_TIMEOUT_MS}ms; treating as free")
        }
        return resolved?.isPremium == true
    }

    /**
     * O acesso sai só do entitlement. Antes havia um `|| activeSubscriptions.isNotEmpty()`
     * que mascarava erro de mapeamento no dashboard e liberaria premium para qualquer
     * assinatura futura (um plano "lite", por exemplo).
     */
    private fun statusOf(customerInfo: CustomerInfo): PremiumStatus {
        logCustomerInfo(customerInfo)
        return if (customerInfo.entitlements.active.containsKey(ENTITLEMENT_ID)) {
            PremiumStatus.Premium
        } else {
            PremiumStatus.Free
        }
    }

    /**
     * Diagnóstico do descasamento "o Play diz que já assinei mas o app não libera":
     * mostra lado a lado o que a loja registrou (`activeSubscriptions`) e o que o
     * RevenueCat derivou disso (`entitlements`). Se houver assinatura ativa e o
     * entitlement esperado não aparecer em `active`, o mapeamento no dashboard está
     * errado — ou o identificador não é exatamente [ENTITLEMENT_ID].
     */
    private fun logCustomerInfo(customerInfo: CustomerInfo) {
        if (!BuildConfig.DEBUG) return
        Log.i(TAG, "--- RevenueCat customer ---")
        Log.i(TAG, "appUserID            = ${Purchases.sharedInstance.appUserID}")
        Log.i(TAG, "activeSubscriptions  = ${customerInfo.activeSubscriptions}")
        Log.i(TAG, "entitlements.all     = ${customerInfo.entitlements.all.keys}")
        Log.i(TAG, "entitlements.active  = ${customerInfo.entitlements.active.keys}")
        Log.i(TAG, "looking for          = '$ENTITLEMENT_ID'")
        customerInfo.entitlements.all.forEach { (id, ent) ->
            Log.i(TAG, "  entitlement '$id': isActive=${ent.isActive}, product=${ent.productIdentifier}")
        }
        Log.i(TAG, "---------------------------")
    }

    private fun setStatus(status: PremiumStatus) {
        _premiumStatus.value = status
        _isPremium.value = status.isPremium
        analyticsManager.setPremium(status.isPremium)
        // Custom key para poder filtrar crash de assinante — o gate de premium muda o
        // caminho de várias telas, então saber disso encurta a investigação.
        crashReporter.setCustomKey("is_premium", status.isPremium)
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        setStatus(statusOf(customerInfo))
    }

    private fun refreshPremiumStatus(attempt: Int = 0) {
        Purchases.sharedInstance.getCustomerInfo(
            callback = object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    setStatus(statusOf(customerInfo))
                }
                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "Failed to refresh premium status (attempt ${attempt + 1}): ${error.message}")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1L shl attempt)
                        Handler(Looper.getMainLooper()).postDelayed({
                            refreshPremiumStatus(attempt + 1)
                        }, delayMs)
                    } else {
                        // Esgotadas as tentativas, resolve para Free em vez de ficar
                        // Unknown para sempre — só cai aqui quem nunca carregou o
                        // CustomerInfo neste dispositivo, e portanto não pode ter
                        // comprado nada. O listener do SDK corrige quando a rede voltar.
                        Log.e(TAG, "Exhausted retries for premium status refresh; assuming free")
                        // Assinante rebaixado a free por falha de rede é exatamente o
                        // bug que gera reembolso, e hoje ele só aparecia no Logcat.
                        crashReporter.recordException(
                            IllegalStateException("Premium status unresolved: ${error.code}"),
                            "refreshPremiumStatus esgotou $MAX_RETRY_ATTEMPTS tentativas"
                        )
                        setStatus(PremiumStatus.Free)
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
                // `.monthly`/`.annual` só resolvem para os identificadores padrão
                // ($rc_monthly / $rc_annual); o scan por packageType cobre offerings
                // montadas com identificadores próprios.
                val monthly = current?.monthly
                    ?: current?.availablePackages?.firstOrNull { it.packageType == PackageType.MONTHLY }
                val annual = current?.annual
                    ?: current?.availablePackages?.firstOrNull { it.packageType == PackageType.ANNUAL }
                onSuccess(monthly, annual)
            }
        )
    }

    fun purchase(
        activity: Activity,
        packageToPurchase: Package,
        onSuccess: (StoreTransaction?, CustomerInfo) -> Unit,
        onError: (error: PurchasesError, userCancelled: Boolean) -> Unit
    ) {
        val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()
        Purchases.sharedInstance.purchaseWith(
            purchaseParams = purchaseParams,
            onError = { error, userCancelled -> onError(error, userCancelled) },
            onSuccess = { transaction, customerInfo ->
                setStatus(statusOf(customerInfo))
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
                setStatus(statusOf(customerInfo))
                onSuccess(customerInfo)
            }
        )
    }
}
