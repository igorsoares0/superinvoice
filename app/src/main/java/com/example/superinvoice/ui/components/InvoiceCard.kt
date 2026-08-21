package com.example.superinvoice.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.ui.theme.Green
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.util.getCurrencySymbol
import online.isdevapps.superinvoice.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceCard(
    invoice: Invoice,
    dateFormatPattern: String = "MM/dd/yyyy",
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onPreview: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDownloadPdf: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat(dateFormatPattern, Locale.getDefault())
    val formattedDate = dateFormat.format(Date(invoice.createdDate))
    val currencySymbol = getCurrencySymbol(invoice.currency)

    val isPaid = invoice.status == InvoiceStatus.PAID
    val statusColor = if (isPaid) Green else Orange
    val statusLabel = stringResource(
        if (isPaid) R.string.status_paid else R.string.status_unpaid
    )

    val actions = buildList {
        onPreview?.let { add(InvMenuAction(stringResource(R.string.preview), onClick = it)) }
        onEdit?.let { add(InvMenuAction(stringResource(R.string.edit), onClick = it)) }
        onShare?.let { add(InvMenuAction(stringResource(R.string.share), onClick = it)) }
        onDownloadPdf?.let {
            add(InvMenuAction(stringResource(R.string.download_pdf), onClick = it))
        }
        onDelete?.let {
            add(InvMenuAction(stringResource(R.string.delete), destructive = true, onClick = it))
        }
    }
    val menuDescription = stringResource(R.string.menu)

    InvListRow(
        modifier = modifier,
        title = "#${invoice.number}",
        meta = formattedDate,
        amount = "$currencySymbol${String.format("%.2f", invoice.totalAmount)}",
        statusLabel = statusLabel,
        statusColor = statusColor,
        showStatusDot = true,
        onClick = onClick,
        trailing = { InvRowMenu(actions = actions, contentDescription = menuDescription) }
    )
}
