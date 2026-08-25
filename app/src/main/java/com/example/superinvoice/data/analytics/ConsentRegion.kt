package com.example.superinvoice.data.analytics

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import online.isdevapps.superinvoice.BuildConfig
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decide se o aparelho está numa jurisdição que exige consentimento prévio (opt-in)
 * para telemetria.
 *
 * O GDPR e a ePrivacy exigem ato afirmativo antes de coletar; o resto do mundo, incluindo
 * a LGPD, admite legítimo interesse com transparência. Como a base de usuários é global,
 * um padrão único obrigaria a escolher entre conformidade e ter dados — por isso o padrão
 * é regional, e só o padrão: assim que a pessoa mexe no interruptor, a escolha dela vence
 * em qualquer lugar.
 */
@Singleton
class ConsentRegion @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ConsentRegion"

        /**
         * EEE (UE 27 + Islândia, Liechtenstein, Noruega), Reino Unido e Gibraltar
         * (UK GDPR), mais a Suíça, cuja LPD revisada segue o mesmo desenho. A lista é
         * deliberadamente mais larga que o estritamente exigido: incluir um país a mais
         * custa alguns eventos, incluir um a menos custa uma infração.
         */
        private val OPT_IN_COUNTRIES = setOf(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE",
            "IS", "LI", "NO",
            "GB", "GI",
            "CH"
        )

        /**
         * A decisão em si, separada da detecção, porque o país da rede não pode ser
         * forjado no aparelho — esta é a parte que dá para testar de verdade.
         */
        fun requiresOptIn(country: String?): Boolean =
            country.isNullOrBlank() || country.uppercase(Locale.ROOT) in OPT_IN_COUNTRIES
    }

    /**
     * Ordem: país da rede, país do SIM, locale.
     *
     * A rede vem primeiro porque o que importa é onde a pessoa **está**, não de onde ela
     * é — um brasileiro em Paris está sob o GDPR enquanto estiver lá. O locale fica por
     * último porque é preferência de idioma, não localização: muita gente usa o aparelho
     * em inglês sem nunca ter saído do país.
     *
     * Sem nenhum sinal (tablet sem SIM e sem país no locale), assume que exige opt-in.
     * Errar para o lado de coletar menos é recuperável; o contrário não é.
     */
    fun requiresOptIn(): Boolean {
        val country = detectCountry()
        val requires = requiresOptIn(country)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "país=${country ?: "indeterminado"} exigeOptIn=$requires")
        }
        return requires
    }

    private fun detectCountry(): String? {
        val telephony = runCatching {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        }.getOrNull()

        val candidates = listOf(
            runCatching { telephony?.networkCountryIso }.getOrNull(),
            runCatching { telephony?.simCountryIso }.getOrNull(),
            runCatching { Locale.getDefault().country }.getOrNull()
        )

        return candidates
            .firstOrNull { !it.isNullOrBlank() }
            ?.uppercase(Locale.ROOT)
    }
}
