package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.ProductsServicesViewModel
import com.example.superinvoice.util.formatDecimalInput
import com.example.superinvoice.util.parseDecimalInput
import online.isdevapps.superinvoice.R

@Composable
fun EditProductScreen(
    productId: Int,
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: ProductsServicesViewModel = hiltViewModel()
) {
    val productsServices by viewModel.productsServices.collectAsStateWithLifecycle()
    val product = productsServices.find { it.id == productId }

    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    LaunchedEffect(product) {
        product?.let {
            productName = it.name
            description = it.description
            price = formatDecimalInput(it.pricePerUnit)
            unit = it.unit
        }
    }

    InvSettingsSubScreen(
        title = stringResource(R.string.title_edit_product),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        saveEnabled = productName.isNotBlank() && parseDecimalInput(price) != null,
        onSave = {
            product?.let {
                val priceValue = parseDecimalInput(price)
                if (productName.isNotBlank() && priceValue != null && priceValue > 0) {
                    viewModel.updateProductService(
                        it.copy(
                            name = productName,
                            description = description,
                            pricePerUnit = priceValue,
                            unit = unit.trim()
                        )
                    )
                    onSave()
                }
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
                    text = unit.trim().ifEmpty { stringResource(R.string.unit_abbreviation) }.uppercase(),
                    style = InvType.label,
                    color = Neutral
                )
            }
        )
        InvField(
            label = stringResource(R.string.unit_optional),
            value = unit,
            onValueChange = { unit = it },
            placeholder = stringResource(R.string.unit_hint)
        )
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
