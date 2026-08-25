package com.example.superinvoice.data.analytics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import online.isdevapps.superinvoice.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada do Crashlytics.
 *
 * Além dos crashes que o SDK captura sozinho, serve para as exceções que o app hoje
 * engole: a geração de PDF desenha em Canvas e pagina à mão, e os `catch` em
 * `InvoicePreviewViewModel` transformam qualquer falha em "não deu certo" na tela, sem
 * deixar rastro. [recordException] é o que torna essas falhas investigáveis.
 *
 * Vale a mesma regra do [AnalyticsManager]: nada de dado de fatura. Chaves são
 * identificadores estruturais (id do template, contagem de itens), nunca conteúdo. E
 * [log] recebe texto fixo do código, não texto do usuário.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CrashReporter"
    }

    private val crashlytics: FirebaseCrashlytics? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase não configurado — Crashlytics inerte")
            null
        } else {
            FirebaseCrashlytics.getInstance()
        }
    }

    /**
     * Único ponto que liga ou desliga a coleta.
     *
     * `setCrashlyticsCollectionEnabled` **persiste entre execuções** (documentado). Havia
     * aqui um `initialize()` que forçava `true` em todo `onCreate` de release, antes de o
     * consentimento ser lido do DataStore: quem tivesse optado por sair tinha a coleta
     * reativada numa janela no começo de cada sessão. Por isso existe um escritor só, e
     * ele é o fluxo de consentimento.
     *
     * O `&& !BuildConfig.DEBUG` precisa continuar aqui mesmo com o manifesto de debug
     * desligando a coleta: o valor persistido vence o manifesto, então uma execução de
     * release na mesma instalação deixaria a flag ligada para o debug seguinte.
     */
    fun setCollectionEnabled(enabled: Boolean) {
        crashlytics?.setCrashlyticsCollectionEnabled(enabled && !BuildConfig.DEBUG)
    }

    /**
     * Registra uma exceção que o app tratou. Continua logando no Logcat porque, sem o
     * `google-services.json`, esta é a única saída.
     */
    fun recordException(throwable: Throwable, context: String) {
        Log.e(TAG, context, throwable)
        crashlytics?.apply {
            log(context)
            recordException(throwable)
        }
    }

    /** Migalha de contexto que acompanha o próximo crash. Texto fixo do código. */
    fun log(message: String) {
        crashlytics?.log(message)
    }

    fun setCustomKey(key: String, value: String) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        crashlytics?.setCustomKey(key, value)
    }
}
