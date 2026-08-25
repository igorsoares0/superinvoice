package com.example.superinvoice.data.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import online.isdevapps.superinvoice.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada do Firebase Analytics.
 *
 * Duas regras moldam esta classe:
 *
 * 1. **Nenhum dado de fatura sai daqui.** Este é um app de faturamento: nome de cliente,
 *    e-mail, valor, IBAN e razão social são dados de terceiros que o usuário confiou ao
 *    app, não telemetria. Por isso [logEvent] é privado e a única forma de registrar algo
 *    é chamar um dos métodos tipados abaixo, cujos parâmetros são contadores, booleanos e
 *    enums. Não existe caminho que aceite uma String livre vinda da UI — se um evento novo
 *    precisar de uma, ela tem que virar enum aqui antes.
 *
 * 2. **Falta de Firebase não pode derrubar o app.** Enquanto não houver
 *    `google-services.json`, [FirebaseApp.getApps] volta vazio e todo método vira no-op
 *    silencioso, em vez de estourar `IllegalStateException` na primeira tela.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AnalyticsManager"

        // Nomes de evento: até 40 caracteres, sem os prefixos reservados
        // firebase_ / google_ / ga_.
        private const val EVENT_INVOICE_STARTED = "invoice_started"
        private const val EVENT_INVOICE_SAVED = "invoice_saved"
        private const val EVENT_FIRST_INVOICE_COMPLETED = "first_invoice_completed"
        private const val EVENT_INVOICE_PDF_SHARED = "invoice_pdf_shared"
        private const val EVENT_INVOICE_PDF_DOWNLOADED = "invoice_pdf_downloaded"
        private const val EVENT_INVOICE_PDF_FAILED = "invoice_pdf_failed"
        private const val EVENT_PAYWALL_SHOWN = "paywall_shown"
        private const val EVENT_PAYWALL_DISMISSED = "paywall_dismissed"
        private const val EVENT_PURCHASE_STARTED = "purchase_started"
        private const val EVENT_PURCHASE_COMPLETED = "purchase_completed"
        private const val EVENT_PURCHASE_FAILED = "purchase_failed"
        private const val EVENT_PURCHASE_RESTORED = "purchase_restored"
        private const val EVENT_BUSINESS_INFO_SAVED = "business_info_saved"
        private const val EVENT_PAYMENT_INFO_SAVED = "payment_info_saved"
        private const val EVENT_BRANDING_ASSET_ADDED = "branding_asset_added"
        private const val EVENT_TEMPLATE_CHANGED = "template_changed"
        private const val EVENT_CLIENT_CREATED = "client_created"
        private const val EVENT_PRODUCT_CREATED = "product_created"

        // User properties: nome até 24 caracteres.
        private const val PROP_INVOICE_COUNT_BUCKET = "invoice_count_bucket"
        private const val PROP_IS_PREMIUM = "is_premium"
        private const val PROP_HAS_BUSINESS_INFO = "has_business_info"
        private const val PROP_HAS_BRANDING = "has_branding"
    }

    /**
     * Resolvido preguiçosamente: o Hilt constrói este singleton cedo, e no primeiro
     * acesso o [FirebaseApp] já foi inicializado pelo ContentProvider do próprio Firebase.
     */
    private val analytics: FirebaseAnalytics? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase não configurado (google-services.json ausente) — analytics inerte")
            null
        } else {
            FirebaseAnalytics.getInstance(context)
        }
    }

    /**
     * Espelho do consentimento, usado **apenas** para o log de debug dizer a verdade.
     * Quem realmente descarta o evento é o SDK; duplicar esse gate aqui criaria duas
     * fontes de verdade que podem divergir.
     */
    @Volatile
    private var collectionEnabled = true

    /**
     * Liga ou desliga a coleta. O Firebase persiste esta escolha entre execuções, então
     * chamar uma vez basta — é o gancho para o consentimento (LGPD/GDPR).
     */
    fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
        analytics?.setAnalyticsCollectionEnabled(enabled)
    }

    /**
     * ID da instância do app, para casar as sessões daqui com as compras no RevenueCat.
     * Volta `null` quando o Firebase não está configurado ou a coleta está desligada.
     */
    fun fetchAppInstanceId(onResult: (String?) -> Unit) {
        val instance = analytics
        if (instance == null) {
            onResult(null)
            return
        }
        instance.appInstanceId
            .addOnSuccessListener { id -> onResult(id) }
            .addOnFailureListener { error ->
                Log.w(TAG, "Falha ao obter appInstanceId", error)
                onResult(null)
            }
    }

    // ---------------------------------------------------------------- user properties

    fun setInvoiceCountBucket(count: Int) {
        setProperty(PROP_INVOICE_COUNT_BUCKET, InvoiceCountBucket.of(count).id)
    }

    fun setPremium(isPremium: Boolean) {
        setProperty(PROP_IS_PREMIUM, isPremium.toString())
    }

    fun setHasBusinessInfo(hasBusinessInfo: Boolean) {
        setProperty(PROP_HAS_BUSINESS_INFO, hasBusinessInfo.toString())
    }

    fun setHasBranding(hasBranding: Boolean) {
        setProperty(PROP_HAS_BRANDING, hasBranding.toString())
    }

    // ----------------------------------------------------------------------- telas

    /**
     * O app não usa NavHost, então a tela é registrada à mão a partir do enum de
     * navegação. O nome vem do enum, nunca de texto digitado pelo usuário.
     */
    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    // ------------------------------------------------------------------- faturas

    fun logInvoiceStarted() = logEvent(EVENT_INVOICE_STARTED)

    /**
     * O par [EVENT_INVOICE_STARTED] / [EVENT_INVOICE_SAVED] é o funil mais acionável do
     * app: quem abre a tela de nova fatura e não salva. Note que nem valor nem nome de
     * cliente entram — só a forma da fatura.
     */
    fun logInvoiceSaved(itemCount: Int, hasTax: Boolean, hasDiscount: Boolean, currency: String) {
        logEvent(EVENT_INVOICE_SAVED) {
            putLong("item_count", itemCount.toLong())
            putBoolean("has_tax", hasTax)
            putBoolean("has_discount", hasDiscount)
            putString("currency", currency)
        }
    }

    fun logFirstInvoiceCompleted() = logEvent(EVENT_FIRST_INVOICE_COMPLETED)

    fun logInvoicePdfShared(template: String, hasLogo: Boolean, hasSignature: Boolean) {
        logEvent(EVENT_INVOICE_PDF_SHARED) {
            putString("template", template)
            putBoolean("has_logo", hasLogo)
            putBoolean("has_signature", hasSignature)
        }
    }

    fun logInvoicePdfDownloaded(template: String, hasLogo: Boolean, hasSignature: Boolean) {
        logEvent(EVENT_INVOICE_PDF_DOWNLOADED) {
            putString("template", template)
            putBoolean("has_logo", hasLogo)
            putBoolean("has_signature", hasSignature)
        }
    }

    /** Só o fato da falha; a exceção em si vai para o Crashlytics. */
    fun logInvoicePdfFailed(stage: String) {
        logEvent(EVENT_INVOICE_PDF_FAILED) {
            putString("stage", stage)
        }
    }

    // ------------------------------------------------------------------- paywall

    fun logPaywallShown(source: PaywallSource) {
        logEvent(EVENT_PAYWALL_SHOWN) {
            putString("source", source.id)
        }
    }

    fun logPaywallDismissed(source: PaywallSource, purchased: Boolean) {
        logEvent(EVENT_PAYWALL_DISMISSED) {
            putString("source", source.id)
            putBoolean("purchased", purchased)
        }
    }

    fun logPurchaseStarted(plan: PlanType) {
        logEvent(EVENT_PURCHASE_STARTED) {
            putString("plan_type", plan.id)
        }
    }

    /**
     * A receita continua sendo do RevenueCat — este evento existe só para fechar o funil
     * dentro do Firebase. Nenhum valor monetário é enviado, de propósito: duplicar receita
     * em duas ferramentas garante dois números que nunca batem.
     */
    fun logPurchaseCompleted(plan: PlanType) {
        logEvent(EVENT_PURCHASE_COMPLETED) {
            putString("plan_type", plan.id)
        }
    }

    fun logPurchaseFailed(plan: PlanType, reason: PurchaseFailureReason) {
        logEvent(EVENT_PURCHASE_FAILED) {
            putString("plan_type", plan.id)
            putString("reason", reason.id)
        }
    }

    fun logPurchaseRestored(foundSubscription: Boolean) {
        logEvent(EVENT_PURCHASE_RESTORED) {
            putBoolean("found_subscription", foundSubscription)
        }
    }

    // ------------------------------------------------------------------ ativação

    /**
     * [filledFieldCount] é quantos campos do formulário ficaram preenchidos, não o que
     * foi digitado neles.
     */
    fun logBusinessInfoSaved(filledFieldCount: Int, totalFieldCount: Int) {
        logEvent(EVENT_BUSINESS_INFO_SAVED) {
            putLong("filled_fields", filledFieldCount.toLong())
            putLong("total_fields", totalFieldCount.toLong())
        }
    }

    fun logPaymentInfoSaved(filledFieldCount: Int, totalFieldCount: Int) {
        logEvent(EVENT_PAYMENT_INFO_SAVED) {
            putLong("filled_fields", filledFieldCount.toLong())
            putLong("total_fields", totalFieldCount.toLong())
        }
    }

    /** [asset] é "logo", "signature" ou "payment_qr_code" — nunca o caminho do arquivo. */
    fun logBrandingAssetAdded(asset: String) {
        logEvent(EVENT_BRANDING_ASSET_ADDED) {
            putString("asset", asset)
        }
    }

    fun logTemplateChanged(template: String, isPremiumTemplate: Boolean) {
        logEvent(EVENT_TEMPLATE_CHANGED) {
            putString("template", template)
            putBoolean("is_premium_template", isPremiumTemplate)
        }
    }

    fun logClientCreated() = logEvent(EVENT_CLIENT_CREATED)

    fun logProductCreated() = logEvent(EVENT_PRODUCT_CREATED)

    // -------------------------------------------------------------------- interno

    private fun setProperty(name: String, value: String) {
        analytics?.setUserProperty(name, value)
        if (BuildConfig.DEBUG) Log.d(TAG, "${debugPrefix()}property $name = $value")
    }

    private fun logEvent(name: String, params: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(params)
        analytics?.logEvent(name, bundle)
        if (BuildConfig.DEBUG) Log.d(TAG, "${debugPrefix()}event $name $bundle")
    }

    /**
     * Sem isto o logcat fica idêntico com a coleta ligada ou desligada — o que torna
     * impossível verificar o interruptor de consentimento pelo log, e faz parecer que
     * o app continua enviando quando não está.
     */
    private fun debugPrefix(): String =
        if (collectionEnabled) "" else "[DESCARTADO — coleta desligada] "
}
