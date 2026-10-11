package com.example.superinvoice.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.util.getCurrencySymbol
import online.isdevapps.superinvoice.R

@Composable
fun ProductServiceCard(
    productService: ProductService,
    currency: String = "USD",
    onMenuClick: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val currencySymbol = getCurrencySymbol(currency)
    val price = "$currencySymbol${String.format("%.2f", productService.pricePerUnit)}"
    val actions = buildList {
        onEdit?.let { add(InvMenuAction(stringResource(R.string.edit), onClick = it)) }
        onDelete?.let {
            add(InvMenuAction(stringResource(R.string.delete), destructive = true, onClick = it))
        }
    }
    val menuDescription = stringResource(R.string.menu)

    InvListRow(
        modifier = modifier,
        title = productService.name,
        meta = productService.description.takeIf { it.isNotBlank() },
        amount = if (productService.unit.isBlank()) {
            stringResource(R.string.price_per_un, price)
        } else {
            stringResource(R.string.price_per_named_unit, price, productService.unit)
        },
        leading = { InvAvatar(productService.name) },
        onClick = onClick,
        trailing = { InvRowMenu(actions = actions, contentDescription = menuDescription) }
    )
}
