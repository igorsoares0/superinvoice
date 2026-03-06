package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import online.isdevapps.superinvoice.R

sealed class BottomNavItem(
    val titleResId: Int,
    val icon: @Composable () -> Unit
) {
    data object Invoices : BottomNavItem(
        titleResId = R.string.nav_invoices,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_invoice),
                contentDescription = stringResource(R.string.nav_invoices)
            )
        }
    )

    data object Settings : BottomNavItem(
        titleResId = R.string.nav_settings,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.nav_settings)
            )
        }
    )
}

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        BottomNavItem.Invoices,
        BottomNavItem.Settings
    )

    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
        NavigationBar(
            containerColor = Color(0xFFF9FAFB)
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedItem == index,
                    onClick = { onItemSelected(index) },
                    icon = item.icon,
                    label = { Text(stringResource(item.titleResId)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
