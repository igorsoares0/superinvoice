package com.example.superinvoice.data.analytics

/**
 * De onde o paywall foi aberto.
 *
 * O app não tem NavHost — a navegação é um `when` sobre estado — então o paywall é
 * alcançado de seis lugares diferentes e sem isso todos viram um número só. É esta
 * dimensão que responde "o limite de 5 faturas converte mais ou menos que o gate do
 * logo?".
 */
enum class PaywallSource(val id: String) {
    INVOICE_LIMIT("invoice_limit"),
    LOGO("logo"),
    SIGNATURE("signature"),
    INVOICE_STYLE("invoice_style"),
    TEMPLATE("template"),
    SETTINGS("settings")
}

enum class PlanType(val id: String) {
    MONTHLY("monthly"),
    ANNUAL("annual"),
    UNKNOWN("unknown")
}

/**
 * Motivo agrupado de uma compra que não completou.
 *
 * Deliberadamente um enum e não a mensagem de erro: o texto que o RevenueCat devolve
 * pode carregar identificador de usuário ou do produto, e evento de analytics não é
 * lugar para isso. O detalhe do erro vai para o Crashlytics, que é o canal certo.
 */
enum class PurchaseFailureReason(val id: String) {
    NETWORK("network"),
    STORE_UNAVAILABLE("store_unavailable"),
    PRODUCT_UNAVAILABLE("product_unavailable"),
    ALREADY_OWNED("already_owned"),
    NOT_ALLOWED("not_allowed"),
    OTHER("other")
}

/**
 * Faixa de faturas criadas, usada como user property.
 *
 * Faixa em vez do número exato porque user property é dimensão de segmentação, não
 * métrica — e o número exato de faturas de uma pessoa, cruzado com o resto, chega
 * perto demais de identificá-la.
 */
enum class InvoiceCountBucket(val id: String) {
    NONE("0"),
    LOW("1_4"),
    AT_LIMIT("5_9"),
    HIGH("10_plus");

    companion object {
        fun of(count: Int): InvoiceCountBucket = when {
            count <= 0 -> NONE
            count < 5 -> LOW
            count < 10 -> AT_LIMIT
            else -> HIGH
        }
    }
}
