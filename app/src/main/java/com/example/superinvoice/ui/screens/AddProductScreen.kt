package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.ProductsServicesViewModel
import online.isdevapps.superinvoice.R

@Composable
fun AddProductScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: ProductsServicesViewModel = hiltViewModel()
) {
    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    InvSettingsSubScreen(
        title = stringResource(R.string.title_add_product),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        saveEnabled = productName.isNotBlank() && price.toDoubleOrNull() != null,
        onSave = {
            val priceValue = price.toDoubleOrNull()
            if (productName.isNotBlank() && priceValue != null && priceValue > 0) {
                viewModel.addProductService(
                    name = productName,
                    pricePerUnit = priceValue,
                    description = description
                )
                onSave()
            }
        }
    ) {
        InvField(
            label = stringResource(R.string.product_name_required),
            value = productName,
            onValueChange = { productName = it },
            opensSection = true
        )
        InvField(
            label = stringResource(R.string.description),
            value = description,
            onValueChange = { description = it }
        )
        InvField(
            label = stringResource(R.string.price_required),
            value = price,
            onValueChange = { price = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailing = {
                Text(
                    text = stringResource(R.string.unit_abbreviation).uppercase(),
                    style = InvType.label,
                    color = Neutral
                )
            }
        )
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
