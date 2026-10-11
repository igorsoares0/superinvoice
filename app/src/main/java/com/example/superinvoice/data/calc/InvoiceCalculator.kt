package com.example.superinvoice.data.calc

import com.example.superinvoice.data.database.entities.AdjustmentType
import java.math.BigDecimal
import java.math.RoundingMode

/** Desconto ou imposto como o usuário digitou: 10 pode ser 10% ou 10 na moeda. */
data class Adjustment(val type: AdjustmentType, val value: Double) {
    companion object {
        val NONE = Adjustment(AdjustmentType.FIXED, 0.0)
    }
}

/** Totais já arredondados para 2 casas, prontos para gravar e exibir. */
data class Totals(
    val lineTotals: List<Double>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val shipping: Double,
    val total: Double,
)

/**
 * Único lugar onde o total de uma fatura é calculado. Criação, edição e (na Onda 1) a conversão
 * de orçamento passam por aqui, para que o número na tela, no banco e no PDF seja sempre o mesmo.
 *
 * O dinheiro continua `Double` no banco (decisão D4 da spec), mas toda conta é feita em
 * [BigDecimal] e arredondada com HALF_UP em cada etapa, como numa nota feita à mão.
 */
object InvoiceCalculator {

    private const val SCALE = 2
    private const val QUANTITY_SCALE = 3

    fun lineTotal(pricePerUnit: Double, quantity: Double): Double =
        lineTotalDecimal(pricePerUnit, quantity).toDouble()

    /**
     * O desconto é aplicado antes do imposto (decisão D3): o imposto incide sobre
     * `subtotal − desconto`, como é o padrão de mercado.
     *
     * @param lines pares (preço unitário, quantidade)
     */
    fun calculate(
        lines: List<Pair<Double, Double>>,
        discount: Adjustment,
        tax: Adjustment,
        shipping: Double,
    ): Totals {
        val lineTotals = lines.map { (price, quantity) -> lineTotalDecimal(price, quantity) }
        // Soma das linhas já arredondadas: o subtotal precisa bater com o que o PDF lista.
        val subtotal = lineTotals.fold(BigDecimal.ZERO, BigDecimal::add)

        // Desconto acima do subtotal deixaria o total negativo.
        val discountAmount = amountOf(discount, subtotal).coerceIn(BigDecimal.ZERO, subtotal)
        val taxAmount = amountOf(tax, subtotal - discountAmount).max(BigDecimal.ZERO)
        val shippingAmount = money(shipping).max(BigDecimal.ZERO)

        val total = subtotal - discountAmount + taxAmount + shippingAmount

        return Totals(
            lineTotals = lineTotals.map { it.toDouble() },
            subtotal = subtotal.toDouble(),
            discountAmount = discountAmount.toDouble(),
            taxAmount = taxAmount.toDouble(),
            shipping = shippingAmount.toDouble(),
            total = total.setScale(SCALE, RoundingMode.HALF_UP).toDouble(),
        )
    }

    /**
     * Quantidade com no máximo 3 casas (spec, seção 4). Também desfaz o ruído do `Double` no
     * passo de +1/−1: 0.1 + 1 dá 1.1000000000000001.
     */
    fun quantity(value: Double): Double =
        decimal(value).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP).toDouble()

    /** Quanto falta pagar. Pagamento acima do total não gera saldo negativo. */
    fun balanceDue(total: Double, amountPaid: Double): Double =
        (money(total) - money(amountPaid)).max(BigDecimal.ZERO).toDouble()

    private fun lineTotalDecimal(pricePerUnit: Double, quantity: Double): BigDecimal =
        (decimal(pricePerUnit) * decimal(quantity)).setScale(SCALE, RoundingMode.HALF_UP)

    private fun amountOf(adjustment: Adjustment, base: BigDecimal): BigDecimal {
        val value = decimal(adjustment.value)
        val amount = when (adjustment.type) {
            // movePointLeft divide por 100 sem perder casas; o `/` do Kotlin manteria a escala
            // do dividendo e arredondaria duas vezes.
            AdjustmentType.PERCENT -> (base * value).movePointLeft(2)
            AdjustmentType.FIXED -> value
        }
        return amount.setScale(SCALE, RoundingMode.HALF_UP)
    }

    private fun money(value: Double): BigDecimal = decimal(value).setScale(SCALE, RoundingMode.HALF_UP)

    // valueOf parte da representação decimal do Double ("1.005"), não do binário exato
    // (1.00499999…). Com BigDecimal(double), 1.005 arredondaria para 1.00 em vez de 1.01.
    private fun decimal(value: Double): BigDecimal = BigDecimal.valueOf(value)
}
