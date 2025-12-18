package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.superinvoice.R

sealed class BottomNavItem(
    val title: String,
    val icon: @Composable () -> Unit
) {
    data object Invoices : BottomNavItem(
        title = "Invoices",
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_invoice),
                contentDescription = "Invoices"
            )
        }
    )

    data object Add : BottomNavItem(
        title = "Add",
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    )

    data object Settings : BottomNavItem(
        title = "Settings",
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
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
        BottomNavItem.Add,
        BottomNavItem.Settings
    )

    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
        NavigationBar(
            containerColor = Color(0xFFFFFFFF)
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedItem == index,
                    onClick = { onItemSelected(index) },
                    icon = item.icon,
                    label = { Text(item.title) },
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
