package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

enum class InvoiceFilter {
    ALL,
    PAID,
    UNPAID
}

/** Filtro é sublinhado, nunca cápsula preenchida. */
@Composable
fun InvoiceFilterTabs(
    selectedFilter: InvoiceFilter,
    onFilterSelected: (InvoiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.xl)
    ) {
        FilterTab(
            text = stringResource(R.string.filter_all),
            isSelected = selectedFilter == InvoiceFilter.ALL,
            onClick = { onFilterSelected(InvoiceFilter.ALL) }
        )
        FilterTab(
            text = stringResource(R.string.filter_paid),
            isSelected = selectedFilter == InvoiceFilter.PAID,
            onClick = { onFilterSelected(InvoiceFilter.PAID) }
        )
        FilterTab(
            text = stringResource(R.string.filter_unpaid),
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
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Text(
            text = text,
            style = InvType.filter,
            color = if (isSelected) Orange else Neutral,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Size.underline)
                .background(if (isSelected) Orange else Color.Transparent)
        )
    }
}

