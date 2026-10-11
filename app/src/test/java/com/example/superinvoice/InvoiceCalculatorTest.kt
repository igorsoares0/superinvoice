package com.example.superinvoice

import com.example.superinvoice.data.calc.Adjustment
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.AdjustmentType.FIXED
import com.example.superinvoice.data.database.entities.AdjustmentType.PERCENT
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O total vai para o banco, para a tela e para o PDF; um centavo de diferença entre eles é o
 * tipo de erro que o cliente da fatura percebe. As regras estão em `docs/spec-onda-1.md`, seção 4.
 */
class InvoiceCalculatorTest {

    private fun fixed(value: Double) = Adjustment(FIXED, value)
    private fun percent(value: Double) = Adjustment(PERCENT, value)

    @Test
    fun `fatura antiga com valores fixos mantem o total da formula anterior`() {
        val lines = listOf(50.0 to 2.0, 19.9 to 3.0)
        val totals = InvoiceCalculator.calculate(lines, fixed(5.0), fixed(12.5), 0.0)

        assertEquals(159.7, totals.subtotal, 0.0)
        // Fórmula usada até a versão 1.0.8: subtotal + tax − discount.
        assertEquals(159.7 + 12.5 - 5.0, totals.total, 0.0)
        assertEquals(listOf(100.0, 59.7), totals.lineTotals)
    }

    @Test
    fun `imposto percentual incide sobre o subtotal menos o desconto`() {
        val totals = InvoiceCalculator.calculate(listOf(100.0 to 1.0), percent(10.0), percent(10.0), 0.0)

        assertEquals(10.0, totals.discountAmount, 0.0)
        assertEquals(9.0, totals.taxAmount, 0.0)
        assertEquals(99.0, totals.total, 0.0)
    }

    @Test
    fun `desconto fixo maior que o subtotal fica limitado e o total nao fica negativo`() {
        val totals = InvoiceCalculator.calculate(listOf(30.0 to 1.0), fixed(50.0), fixed(0.0), 0.0)

        assertEquals(30.0, totals.discountAmount, 0.0)
        assertEquals(0.0, totals.total, 0.0)
    }

    @Test
    fun `desconto acima de 100 por cento fica limitado ao subtotal`() {
        val totals = InvoiceCalculator.calculate(listOf(80.0 to 1.0), percent(150.0), fixed(5.0), 0.0)

        assertEquals(80.0, totals.discountAmount, 0.0)
        assertEquals(5.0, totals.total, 0.0)
    }

    @Test
    fun `valores negativos de desconto, imposto e frete viram zero`() {
        val totals = InvoiceCalculator.calculate(listOf(40.0 to 1.0), fixed(-5.0), fixed(-3.0), -10.0)

        assertEquals(0.0, totals.discountAmount, 0.0)
        assertEquals(0.0, totals.taxAmount, 0.0)
        assertEquals(0.0, totals.shipping, 0.0)
        assertEquals(40.0, totals.total, 0.0)
    }

    @Test
    fun `meio centavo arredonda para cima`() {
        // Com BigDecimal(1.005) o resultado seria 1.00: o binário de 1.005 é 1.00499999…
        assertEquals(1.01, InvoiceCalculator.lineTotal(1.005, 1.0), 0.0)
        assertEquals(0.13, InvoiceCalculator.lineTotal(0.125, 1.0), 0.0)
    }

    @Test
    fun `percentual com meio centavo arredonda para cima`() {
        // 10.05 × 5% = 0.5025 → 0.50; 10.10 × 5% = 0.505 → 0.51
        assertEquals(0.5, InvoiceCalculator.calculate(listOf(10.05 to 1.0), Adjustment.NONE, percent(5.0), 0.0).taxAmount, 0.0)
        assertEquals(0.51, InvoiceCalculator.calculate(listOf(10.1 to 1.0), Adjustment.NONE, percent(5.0), 0.0).taxAmount, 0.0)
    }

    @Test
    fun `subtotal soma as linhas ja arredondadas`() {
        val totals = InvoiceCalculator.calculate(
            listOf(0.333 to 1.0, 0.333 to 1.0, 0.333 to 1.0), Adjustment.NONE, Adjustment.NONE, 0.0
        )

        assertEquals(listOf(0.33, 0.33, 0.33), totals.lineTotals)
        assertEquals(0.99, totals.subtotal, 0.0)
    }

    @Test
    fun `quantidade decimal`() {
        assertEquals(25.0, InvoiceCalculator.lineTotal(10.0, 2.5), 0.0)
        assertEquals(1.0, InvoiceCalculator.lineTotal(3.0, 0.333), 0.0)
    }

    @Test
    fun `frete entra no total`() {
        val totals = InvoiceCalculator.calculate(listOf(100.0 to 1.0), fixed(10.0), percent(10.0), 15.0)

        assertEquals(15.0, totals.shipping, 0.0)
        assertEquals(100.0 - 10.0 + 9.0 + 15.0, totals.total, 0.0)
    }

    @Test
    fun `fatura sem itens da tudo zero`() {
        val totals = InvoiceCalculator.calculate(emptyList(), fixed(10.0), percent(10.0), 0.0)

        assertEquals(emptyList<Double>(), totals.lineTotals)
        assertEquals(0.0, totals.subtotal, 0.0)
        assertEquals(0.0, totals.discountAmount, 0.0)
        assertEquals(0.0, totals.taxAmount, 0.0)
        assertEquals(0.0, totals.total, 0.0)
    }

    @Test
    fun `quantidade fica com no maximo 3 casas`() {
        assertEquals(2.5, InvoiceCalculator.quantity(2.5), 0.0)
        assertEquals(0.334, InvoiceCalculator.quantity(0.3335), 0.0)
        assertEquals(1.1, InvoiceCalculator.quantity(0.1 + 1.0), 0.0)
    }

    @Test
    fun `saldo devedor nunca fica negativo`() {
        assertEquals(40.0, InvoiceCalculator.balanceDue(105.0, 65.0), 0.0)
        assertEquals(0.1, InvoiceCalculator.balanceDue(0.3, 0.2), 0.0)
        assertEquals(0.0, InvoiceCalculator.balanceDue(100.0, 120.0), 0.0)
    }
}
