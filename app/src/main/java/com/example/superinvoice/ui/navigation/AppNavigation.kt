package com.example.superinvoice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.superinvoice.ui.screens.ClientsScreen
import com.example.superinvoice.ui.screens.CreateInvoiceScreen
import com.example.superinvoice.ui.screens.HomeScreen

enum class Screen {
    HOME,
    CREATE_INVOICE,
    CLIENTS
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onNavigateToCreateInvoice = { currentScreen = Screen.CREATE_INVOICE }
        )
        Screen.CREATE_INVOICE -> CreateInvoiceScreen(
            onClose = { currentScreen = Screen.HOME },
            onSave = { currentScreen = Screen.HOME },
            onNavigateToClients = { currentScreen = Screen.CLIENTS }
        )
        Screen.CLIENTS -> ClientsScreen(
            onClose = { currentScreen = Screen.CREATE_INVOICE }
        )
    }
}
