package com.example.superinvoice.ui.viewmodel

import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.calc.Adjustment
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.calc.Totals
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.util.formatDecimalInput
import com.example.superinvoice.util.parseDecimalInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Item na tela de criação/edição. [unit] começa com a unidade do produto e pode ser trocada só
 * nesta fatura.
 */
data class LineItem(
    val productService: ProductService,
    val quantity: Double,
    val unit: String,
    val lineTotal: Double
)

/**
 * Item já gravado volta para a tela a partir da própria cópia (nome, descrição, preço), sem
 * consultar o cadastro: o produto pode ter mudado de preço ou ter sido apagado (`SET NULL`), e a
 * fatura não pode mudar nem perder a linha por isso. Produto apagado fica com id 0.
 */
fun InvoiceItem.toLineItem() = LineItem(
    productService = ProductService(
        id = productServiceId ?: 0,
        name = productServiceName,
        description = productServiceDescription,
        pricePerUnit = pricePerUnit
    ),
    quantity = quantity,
    unit = unit,
    lineTotal = lineTotal
)

fun LineItem.toInvoiceItem(invoiceId: Int) = InvoiceItem(
    invoiceId = invoiceId,
    productServiceId = productService.id.takeIf { it != 0 },
    productServiceName = productService.name,
    productServiceDescription = productService.description,
    pricePerUnit = productService.pricePerUnit,
    quantity = quantity,
    unit = unit.trim(),
    lineTotal = lineTotal
)

/**
 * Desconto ou imposto como está no campo: o texto digitado (com vírgula ou ponto) e se ele é
 * percentual ou valor na moeda.
 */
data class AdjustmentInput(
    val type: AdjustmentType = AdjustmentType.FIXED,
    val text: String = ""
) {
    val value: Double get() = parseDecimalInput(text) ?: 0.0

    fun toAdjustment() = Adjustment(type, value)
}

/**
 * Itens, desconto, imposto e frete da fatura em edição. A criação e a edição tinham cada uma a
 * sua cópia dessa lógica; agora as duas telas usam esta classe, e todo total sai do
 * [InvoiceCalculator].
 */
class InvoiceForm {

    private val _lineItems = MutableStateFlow<List<LineItem>>(emptyList())
    val lineItems: StateFlow<List<LineItem>> = _lineItems.asStateFlow()

    private val _discount = MutableStateFlow(AdjustmentInput())
    val discount: StateFlow<AdjustmentInput> = _discount.asStateFlow()

    private val _tax = MutableStateFlow(AdjustmentInput())
    val tax: StateFlow<AdjustmentInput> = _tax.asStateFlow()

    private val _shipping = MutableStateFlow("")
    val shipping: StateFlow<String> = _shipping.asStateFlow()

    private val _totals = MutableStateFlow(calculate())
    val totals: StateFlow<Totals> = _totals.asStateFlow()

    val hasDecimalQuantity: Boolean
        get() = _lineItems.value.any { it.quantity % 1.0 != 0.0 }

    fun setDiscount(input: AdjustmentInput) {
        _discount.value = input
        recalculate()
    }

    fun setTax(input: AdjustmentInput) {
        _tax.value = input
        recalculate()
    }

    fun setShipping(text: String) {
        _shipping.value = text
        recalculate()
    }

    fun addLineItem(productService: ProductService) {
        _lineItems.update { it + newLine(productService, 1.0, productService.unit) }
        recalculate()
    }

    /** O passo de +1/−1 da tela. A quantidade nunca chega a zero: para isso existe remover. */
    fun stepQuantity(index: Int, delta: Int) {
        val item = _lineItems.value.getOrNull(index) ?: return
        val quantity = InvoiceCalculator.quantity(item.quantity + delta)
        if (quantity > 0.0) updateLineItem(index, quantity, item.unit)
    }

    fun updateLineItem(index: Int, quantity: Double, unit: String) {
        val rounded = InvoiceCalculator.quantity(quantity)
        if (rounded <= 0.0) return
        _lineItems.update { items ->
            items.mapIndexed { i, item ->
                if (i == index) newLine(item.productService, rounded, unit.trim()) else item
            }
        }
        recalculate()
    }

    fun removeLineItem(index: Int) {
        _lineItems.update { items -> items.filterIndexed { i, _ -> i != index } }
        recalculate()
    }

    /** Preenche o formulário com uma fatura gravada. Os campos mostram o que o usuário digitou. */
    fun load(invoice: Invoice, items: List<InvoiceItem>) {
        _lineItems.value = items.map { it.toLineItem() }
        _discount.value = AdjustmentInput(invoice.discountType, formatDecimalInput(invoice.discountValue))
        _tax.value = AdjustmentInput(invoice.taxType, formatDecimalInput(invoice.taxValue))
        _shipping.value = formatDecimalInput(invoice.shipping)
        recalculate()
    }

    fun reset() {
        _lineItems.value = emptyList()
        _discount.value = AdjustmentInput()
        _tax.value = AdjustmentInput()
        _shipping.value = ""
        recalculate()
    }

    /** Copia para a fatura os ajustes como digitados e os montantes já calculados. */
    fun applyTo(invoice: Invoice): Invoice {
        val totals = _totals.value
        return invoice.copy(
            subtotal = totals.subtotal,
            discountType = _discount.value.type,
            discountValue = _discount.value.value,
            discountAmount = totals.discountAmount,
            taxType = _tax.value.type,
            taxValue = _tax.value.value,
            taxAmount = totals.taxAmount,
            shipping = totals.shipping,
            totalAmount = totals.total
        )
    }

    fun toInvoiceItems(invoiceId: Int): List<InvoiceItem> =
        _lineItems.value.map { it.toInvoiceItem(invoiceId) }

    private fun newLine(productService: ProductService, quantity: Double, unit: String) = LineItem(
        productService = productService,
        quantity = quantity,
        unit = unit,
        lineTotal = InvoiceCalculator.lineTotal(productService.pricePerUnit, quantity)
    )

    private fun recalculate() {
        _totals.value = calculate()
    }

    private fun calculate(): Totals = InvoiceCalculator.calculate(
        lines = _lineItems.value.map { it.productService.pricePerUnit to it.quantity },
        discount = _discount.value.toAdjustment(),
        tax = _tax.value.toAdjustment(),
        shipping = parseDecimalInput(_shipping.value) ?: 0.0
    )
}
