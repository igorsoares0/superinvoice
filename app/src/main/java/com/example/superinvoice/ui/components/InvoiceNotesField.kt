package com.example.superinvoice.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Campo de texto longo — mesma gramática do [InvField], só que sem linha
 * única. O rótulo continua fixo em caixa alta acima do valor.
 */
@Composable
fun InvoiceNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    opensSection: Boolean = false
) {
    InvField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = false,
        minLines = 3,
        opensSection = opensSection
    )
}
