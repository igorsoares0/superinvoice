package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvSelectableRow
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.CurrencyViewModel
import online.isdevapps.superinvoice.R

data class Currency(
    val code: String,
    val name: String,
    val symbol: String
)

@Composable
fun CurrencyScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: CurrencyViewModel = hiltViewModel()
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    val currencies = listOf(
        Currency("USD", "United States Dollar", "$"),
        Currency("EUR", "Euro", "€"),
        Currency("GBP", "British Pound", "£"),
        Currency("BRL", "Brazilian Real", "R$"),
        Currency("JPY", "Japanese Yen", "¥"),
        Currency("CAD", "Canadian Dollar", "C$"),
        Currency("AUD", "Australian Dollar", "A$"),
        Currency("CHF", "Swiss Franc", "CHF"),
        Currency("CNY", "Chinese Yuan", "¥"),
        Currency("INR", "Indian Rupee", "₹"),
        Currency("MXN", "Mexican Peso", "MX$"),
        Currency("ARS", "Argentine Peso", "AR$")
    )

    InvSettingsSubScreen(
        title = stringResource(R.string.title_currency),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = { viewModel.saveCurrency { onSave() } }
    ) {
        InvSectionRule()
        currencies.forEach { currency ->
            InvSelectableRow(
                title = currency.code,
                subtitle = currency.name,
                trailingLabel = currency.symbol,
                isSelected = selectedCurrency == currency.code,
                onClick = { viewModel.setSelectedCurrency(currency.code) }
            )
        }
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
