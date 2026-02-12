package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilterTab(
            text = "All",
            isSelected = selectedFilter == InvoiceFilter.ALL,
            onClick = { onFilterSelected(InvoiceFilter.ALL) }
        )
        FilterTab(
            text = "Paid",
            isSelected = selectedFilter == InvoiceFilter.PAID,
            onClick = { onFilterSelected(InvoiceFilter.PAID) }
        )
        FilterTab(
            text = "Unpaid",
            isSelected = selectedFilter == InvoiceFilter.UNPAID,
            onClick = { onFilterSelected(InvoiceFilter.UNPAID) }
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}
