package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import online.isdevapps.superinvoice.R

enum class InvoiceFilter {
    ALL,
    PAID,
    UNPAID
}

@Composable
fun InvoiceFilterTabs(
    selectedFilter: InvoiceFilter,
    onFilterSelected: (InvoiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterTab(
            text = stringResource(R.string.filter_all),
            isSelected = selectedFilter == InvoiceFilter.ALL,
            onClick = { onFilterSelected(InvoiceFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        FilterTab(
            text = stringResource(R.string.filter_paid),
            isSelected = selectedFilter == InvoiceFilter.PAID,
            onClick = { onFilterSelected(InvoiceFilter.PAID) },
            modifier = Modifier.weight(1f)
        )
        FilterTab(
            text = stringResource(R.string.filter_unpaid),
            isSelected = selectedFilter == InvoiceFilter.UNPAID,
            onClick = { onFilterSelected(InvoiceFilter.UNPAID) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF9DEA6E) else Color.Transparent
    val textColor = if (isSelected) Color.Black else Color.Black

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
