package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.ui.components.InvAdjustmentDialog
import com.example.superinvoice.ui.components.InvAmountDialog
import com.example.superinvoice.ui.components.InvChip
import com.example.superinvoice.ui.components.InvLineItemDialog
import com.example.superinvoice.ui.components.InvLineItemRow
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvTotalSummary
import com.example.superinvoice.ui.components.InvTotalsBreakdown
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.AdjustmentInput
import com.example.superinvoice.ui.viewmodel.InvoiceForm
import com.example.superinvoice.util.formatQuantity
import com.example.superinvoice.util.parseDecimalInput
import online.isdevapps.superinvoice.R

/*
 * Partes do formulário de fatura que a criação e a edição mostram iguais: itens, chips de
 * desconto/imposto/frete e o bloco de totais. Todas leem e escrevem no mesmo [InvoiceForm].
 */

private fun money(currencySymbol: String, value: Double) =
    "$currencySymbol${String.format("%.2f", value)}"

/** "10%" ou "$10.00", conforme o tipo escolhido no diálogo. */
private fun adjustmentText(input: AdjustmentInput, currencySymbol: String) =
    if (input.type == AdjustmentType.PERCENT) "${formatQuantity(input.value)}%"
    else money(currencySymbol, input.value)

private enum class AdjustmentDialog { TAX, DISCOUNT, SHIPPING }

@Composable
fun InvoiceLineItemsSection(form: InvoiceForm, currencySymbol: String) {
    val lineItems by form.lineItems.collectAsStateWithLifecycle()
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    editingIndex?.let { index ->
        lineItems.getOrNull(index)?.let { item ->
            InvLineItemDialog(
                title = item.productService.name,
                initialQuantity = formatQuantity(item.quantity),
                initialUnit = item.unit,
                quantityLabel = stringResource(R.string.quantity),
                unitLabel = stringResource(R.string.unit_optional),
                unitPlaceholder = stringResource(R.string.unit_hint),
                onConfirm = { quantity, unit ->
                    form.updateLineItem(index, quantity, unit)
                    editingIndex = null
                },
                onDismiss = { editingIndex = null },
                confirmText = stringResource(R.string.ok),
                dismissText = stringResource(R.string.cancel)
            )
        }
    }

    if (lineItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Space.section))
        Text(
            text = stringResource(R.string.items).uppercase(),
            style = InvType.label,
            color = Neutral,
            modifier = Modifier.padding(bottom = Space.md)
        )
        InvSectionRule()
        lineItems.forEachIndexed { index, item ->
            val price = money(currencySymbol, item.productService.pricePerUnit)
            InvLineItemRow(
                name = item.productService.name,
                unitPrice = if (item.unit.isBlank()) {
                    stringResource(R.string.price_per_unit, price)
                } else {
                    stringResource(R.string.price_per_named_unit, price, item.unit)
                },
                quantity = formatQuantity(item.quantity),
                canDecrease = item.quantity > 1.0,
                lineTotal = money(currencySymbol, item.lineTotal),
                onDecrease = { form.stepQuantity(index, -1) },
                onIncrease = { form.stepQuantity(index, 1) },
                onQuantityClick = { editingIndex = index },
                quantityClickLabel = stringResource(R.string.edit_quantity),
                onRemove = { form.removeLineItem(index) },
                removeContentDescription = stringResource(R.string.remove)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvoiceAdjustmentChips(form: InvoiceForm, currencySymbol: String) {
    val tax by form.tax.collectAsStateWithLifecycle()
    val discount by form.discount.collectAsStateWithLifecycle()
    val shipping by form.shipping.collectAsStateWithLifecycle()

    var openDialog by remember { mutableStateOf<AdjustmentDialog?>(null) }
    var shippingInput by remember { mutableStateOf("") }

    when (openDialog) {
        AdjustmentDialog.TAX -> InvAdjustmentDialog(
            title = stringResource(R.string.tax),
            initialType = tax.type,
            initialValue = tax.text,
            percentTabText = stringResource(R.string.adjustment_percent),
            fixedTabText = stringResource(R.string.adjustment_fixed),
            percentLabel = stringResource(R.string.enter_tax_rate),
            fixedLabel = stringResource(R.string.enter_tax_amount),
            currencySymbol = currencySymbol,
            onConfirm = { type, text ->
                form.setTax(AdjustmentInput(type, text))
                openDialog = null
            },
            onDismiss = { openDialog = null },
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel)
        )
        AdjustmentDialog.DISCOUNT -> InvAdjustmentDialog(
            title = stringResource(R.string.discount),
            initialType = discount.type,
            initialValue = discount.text,
            percentTabText = stringResource(R.string.adjustment_percent),
            fixedTabText = stringResource(R.string.adjustment_fixed),
            percentLabel = stringResource(R.string.enter_discount_rate),
            fixedLabel = stringResource(R.string.enter_discount_amount),
            currencySymbol = currencySymbol,
            onConfirm = { type, text ->
                form.setDiscount(AdjustmentInput(type, text))
                openDialog = null
            },
            onDismiss = { openDialog = null },
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel)
        )
        AdjustmentDialog.SHIPPING -> InvAmountDialog(
            title = stringResource(R.string.shipping),
            label = stringResource(R.string.enter_shipping_amount),
            value = shippingInput,
            onValueChange = { shippingInput = it },
            onConfirm = {
                form.setShipping(shippingInput)
                openDialog = null
            },
            onDismiss = { openDialog = null },
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel)
        )
        null -> Unit
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        InvChip(
            text = if (tax.value > 0) {
                stringResource(R.string.tax_with_amount, adjustmentText(tax, currencySymbol))
            } else {
                stringResource(R.string.tax)
            },
            onClick = { openDialog = AdjustmentDialog.TAX }
        )
        InvChip(
            text = if (discount.value > 0) {
                stringResource(R.string.discount_with_amount, adjustmentText(discount, currencySymbol))
            } else {
                stringResource(R.string.discount)
            },
            onClick = { openDialog = AdjustmentDialog.DISCOUNT }
        )
        val shippingValue = parseDecimalInput(shipping) ?: 0.0
        InvChip(
            text = if (shippingValue > 0) {
                stringResource(R.string.shipping_with_amount, money(currencySymbol, shippingValue))
            } else {
                stringResource(R.string.shipping)
            },
            onClick = {
                shippingInput = shipping
                openDialog = AdjustmentDialog.SHIPPING
            }
        )
    }
}

/**
 * Total da fatura. Com desconto, imposto ou frete, mostra antes o subtotal e cada ajuste já em
 * moeda: um "10%" no chip não diz quanto ele vale.
 */
@Composable
fun InvoiceTotalsSection(form: InvoiceForm, currencySymbol: String, currency: String) {
    val totals by form.totals.collectAsStateWithLifecycle()
    val tax by form.tax.collectAsStateWithLifecycle()
    val discount by form.discount.collectAsStateWithLifecycle()

    val discountLabel = stringResource(R.string.discount)
    val taxLabel = stringResource(R.string.tax)
    val rows = buildList {
        if (totals.discountAmount > 0 || totals.taxAmount > 0 || totals.shipping > 0) {
            add(stringResource(R.string.subtotal) to money(currencySymbol, totals.subtotal))
        }
        if (totals.discountAmount > 0) {
            add(withRate(discountLabel, discount) to "-${money(currencySymbol, totals.discountAmount)}")
        }
        if (totals.taxAmount > 0) {
            add(withRate(taxLabel, tax) to money(currencySymbol, totals.taxAmount))
        }
        if (totals.shipping > 0) {
            add(stringResource(R.string.shipping) to money(currencySymbol, totals.shipping))
        }
    }

    if (rows.isNotEmpty()) InvTotalsBreakdown(rows)
    InvTotalSummary(
        label = stringResource(R.string.total),
        amount = money(currencySymbol, totals.total),
        meta = currency
    )
}

private fun withRate(label: String, input: AdjustmentInput) =
    if (input.type == AdjustmentType.PERCENT) "$label (${formatQuantity(input.value)}%)" else label
