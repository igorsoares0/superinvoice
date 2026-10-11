package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvStatusLabel
import com.example.superinvoice.ui.components.documentStatusLabel
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

/**
 * Abaixo da pré-visualização de um orçamento: a resposta do cliente e a conversão em fatura
 * (spec, seção 5.1). Orçamento já convertido não converte de novo; mostra o link da fatura.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EstimateSummary(
    estimate: Invoice,
    currencySymbol: String,
    convertedInvoice: Invoice?,
    onSetStatus: (InvoiceStatus) -> Unit,
    onConvert: () -> Unit,
    onOpenInvoice: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor) = documentStatusLabel(estimate.type, estimate.status)

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
                text = "$currencySymbol${String.format("%.2f", estimate.totalAmount)}",
                style = InvType.support,
                color = Neutral,
                maxLines = 1
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            if (convertedInvoice != null) {
                TextAction(stringResource(R.string.view_converted_invoice, convertedInvoice.number)) {
                    onOpenInvoice(convertedInvoice.id)
                }
            } else {
                TextAction(stringResource(R.string.convert_to_invoice), onConvert)
                if (estimate.status != InvoiceStatus.ACCEPTED) {
                    TextAction(stringResource(R.string.mark_as_accepted)) { onSetStatus(InvoiceStatus.ACCEPTED) }
                }
                if (estimate.status != InvoiceStatus.DECLINED) {
                    TextAction(stringResource(R.string.mark_as_declined)) { onSetStatus(InvoiceStatus.DECLINED) }
                }
            }
        }
    }
}
