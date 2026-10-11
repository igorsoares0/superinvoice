package com.example.superinvoice.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.Payment
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.PaymentMethod
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvConfirmDialog
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvFieldLabel
import com.example.superinvoice.ui.components.InvListRow
import com.example.superinvoice.ui.components.InvMenuAction
import com.example.superinvoice.ui.components.InvRowMenu
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvStatusLabel
import com.example.superinvoice.ui.components.InvUnderlineTab
import com.example.superinvoice.ui.components.paymentStatusLabel
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.util.InvoiceDates
import com.example.superinvoice.util.formatDecimalInput
import com.example.superinvoice.util.labelRes
import com.example.superinvoice.util.parseDecimalInput
import online.isdevapps.superinvoice.R

/*
 * Pagamentos na pré-visualização da fatura (Onda 1, seção 5.2): o resumo com status e saldo,
 * a folha de registrar pagamento e a folha com a lista, de onde sai o recibo.
 */

private fun money(currencySymbol: String, value: Double) =
    "$currencySymbol${String.format("%.2f", value)}"

/** Status, quanto já foi pago e as ações de pagamento, logo abaixo da fatura. */
@Composable
fun InvoicePaymentSummary(
    invoice: Invoice,
    currencySymbol: String,
    paymentCount: Int,
    onRecordPayment: () -> Unit,
    onShowPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor) = paymentStatusLabel(invoice.status)
    val balance = InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)

    Column(modifier = modifier.fillMaxWidth()) {
        InvSectionRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            InvStatusLabel(label = statusLabel, color = statusColor, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.paid_of_total,
                    money(currencySymbol, invoice.amountPaid),
                    money(currencySymbol, invoice.totalAmount)
                ),
                style = InvType.support,
                color = Neutral,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.padding(top = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            if (balance > 0.0) {
                TextAction(stringResource(R.string.record_payment), onRecordPayment)
            }
            if (paymentCount > 0) {
                TextAction(stringResource(R.string.payments_count, paymentCount), onShowPayments)
            }
        }
    }
}

/** Ação em texto laranja, para as linhas de ação abaixo da pré-visualização. */
@Composable
internal fun TextAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = InvType.action,
        color = Orange,
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm)
    )
}

/**
 * Valor (já com o saldo), data (hoje), método e observação. Valor acima do saldo é aceito, só
 * com um aviso: o cliente pode ter pago a mais ou arredondado.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentSheet(
    balance: Double,
    currencySymbol: String,
    dateFormat: String,
    onConfirm: (amount: Double, paidAt: Long, method: PaymentMethod, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(formatDecimalInput(balance)) }
    var paidAt by remember { mutableLongStateOf(InvoiceDates.today()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var note by remember { mutableStateOf("") }
    val amount = parseDecimalInput(amountText)?.takeIf { it > 0.0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.xl)
        ) {
            Text(
                text = stringResource(R.string.record_payment),
                style = InvType.sectionTitle,
                color = Ink,
                modifier = Modifier.padding(bottom = Space.lg)
            )
            InvField(
                label = stringResource(R.string.payment_amount),
                value = amountText,
                onValueChange = { amountText = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                opensSection = true,
                trailing = { Text(text = currencySymbol, style = InvType.label, color = Neutral) }
            )
            if (amount != null && InvoiceCalculator.balanceDue(amount, balance) > 0.0) {
                Text(
                    text = stringResource(R.string.payment_above_balance, money(currencySymbol, balance)),
                    style = InvType.support,
                    color = Orange,
                    modifier = Modifier.padding(top = Space.sm)
                )
            }
            DatePickerField(
                label = stringResource(R.string.payment_date),
                value = paidAt,
                onValueChange = { paidAt = it },
                dateFormatPattern = dateFormat
            )
            Column(modifier = Modifier.padding(vertical = Space.lg)) {
                InvFieldLabel(stringResource(R.string.payment_method))
                Spacer(modifier = Modifier.height(Space.md))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    PaymentMethod.entries.forEach { option ->
                        InvUnderlineTab(
                            text = stringResource(option.labelRes),
                            isSelected = option == method,
                            onClick = { method = option }
                        )
                    }
                }
            }
            InvField(
                label = stringResource(R.string.payment_note),
                value = note,
                onValueChange = { note = it },
                opensSection = true
            )
            Spacer(modifier = Modifier.height(Space.xl))
            InvButton(
                text = stringResource(R.string.record_payment),
                enabled = amount != null,
                onClick = { amount?.let { onConfirm(it, paidAt, method, note) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Histórico de pagamentos da fatura; cada linha gera o recibo ou apaga o pagamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsSheet(
    payments: List<Payment>,
    currencySymbol: String,
    dateFormat: String,
    onShareReceipt: (Payment) -> Unit,
    onDelete: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    paymentToDelete?.let { payment ->
        InvConfirmDialog(
            title = stringResource(R.string.delete_payment_title),
            message = stringResource(R.string.delete_payment_message),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                onDelete(payment)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.xl)
        ) {
            Text(
                text = stringResource(R.string.payments_title),
                style = InvType.sectionTitle,
                color = Ink,
                modifier = Modifier.padding(bottom = Space.lg)
            )
            InvSectionRule()
            val menuDescription = stringResource(R.string.menu)
            val receiptLabel = stringResource(R.string.generate_receipt)
            val deleteLabel = stringResource(R.string.delete)
            payments.forEach { payment ->
                val note = payment.note.takeIf { it.isNotBlank() }
                InvListRow(
                    title = "${InvoiceDates.format(payment.paidAt, dateFormat)} · ${stringResource(payment.method.labelRes)}",
                    meta = listOfNotNull(payment.receiptNumber, note).joinToString(" · "),
                    amount = money(currencySymbol, payment.amount),
                    trailing = {
                        InvRowMenu(
                            actions = listOf(
                                InvMenuAction(receiptLabel) { onShareReceipt(payment) },
                                InvMenuAction(deleteLabel, destructive = true) { paymentToDelete = payment }
                            ),
                            contentDescription = menuDescription
                        )
                    }
                )
            }
        }
    }
}
