package com.example.superinvoice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.superinvoice.ui.screens.AddClientScreen
import com.example.superinvoice.ui.screens.AddProductScreen
import com.example.superinvoice.ui.screens.BusinessInformationScreen
import com.example.superinvoice.ui.screens.ClientsScreen
import com.example.superinvoice.ui.screens.CreateInvoiceScreen
import com.example.superinvoice.ui.screens.CurrencyScreen
import com.example.superinvoice.ui.screens.DateFormatScreen
import com.example.superinvoice.ui.screens.EditInvoiceScreen
import com.example.superinvoice.ui.screens.HomeScreen
import com.example.superinvoice.ui.screens.InvoicePreviewScreen
import com.example.superinvoice.ui.screens.InvoiceTemplateScreen
import com.example.superinvoice.ui.screens.LogoScreen
import com.example.superinvoice.ui.screens.PaymentInstructionsScreen
import com.example.superinvoice.ui.screens.ProductsServicesScreen
import com.example.superinvoice.ui.screens.SettingsScreen
import com.example.superinvoice.ui.screens.SignatureScreen

enum class Screen {
    HOME,
    CREATE_INVOICE,
    CLIENTS,
    ADD_CLIENT,
    PRODUCTS_SERVICES,
    ADD_PRODUCT,
    SETTINGS,
    EDIT_INVOICE,
    INVOICE_TEMPLATE,
    INVOICE_PREVIEW,
    BUSINESS_INFO,
    PAYMENT_INSTRUCTIONS,
    LOGO,
    SIGNATURE,
    CURRENCY,
    DATE_FORMAT
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedBottomNavItem by remember { mutableIntStateOf(0) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onNavigateToCreateInvoice = { currentScreen = Screen.CREATE_INVOICE },
            onNavigateToEditInvoice = { currentScreen = Screen.EDIT_INVOICE },
            selectedBottomNavItem = selectedBottomNavItem,
            onBottomNavItemSelected = { index ->
                selectedBottomNavItem = index
                when (index) {
                    0 -> currentScreen = Screen.HOME
                    2 -> currentScreen = Screen.SETTINGS
                }
            }
        )
        Screen.CREATE_INVOICE -> CreateInvoiceScreen(
            onClose = { currentScreen = Screen.HOME },
            onSave = { currentScreen = Screen.HOME },
            onNavigateToClients = { currentScreen = Screen.CLIENTS },
            onNavigateToProductsServices = { currentScreen = Screen.PRODUCTS_SERVICES }
        )
        Screen.CLIENTS -> ClientsScreen(
            onClose = { currentScreen = Screen.CREATE_INVOICE },
            onNavigateToAddClient = { currentScreen = Screen.ADD_CLIENT }
        )
        Screen.ADD_CLIENT -> AddClientScreen(
            onClose = { currentScreen = Screen.CLIENTS },
            onSave = { currentScreen = Screen.CLIENTS }
        )
        Screen.PRODUCTS_SERVICES -> ProductsServicesScreen(
            onClose = { currentScreen = Screen.CREATE_INVOICE },
            onNavigateToAddProductService = { currentScreen = Screen.ADD_PRODUCT }
        )
        Screen.ADD_PRODUCT -> AddProductScreen(
            onClose = { currentScreen = Screen.PRODUCTS_SERVICES },
            onSave = { currentScreen = Screen.PRODUCTS_SERVICES }
        )
        Screen.SETTINGS -> SettingsScreen(
            selectedBottomNavItem = selectedBottomNavItem,
            onBottomNavItemSelected = { index ->
                selectedBottomNavItem = index
                when (index) {
                    0 -> currentScreen = Screen.HOME
                    2 -> currentScreen = Screen.SETTINGS
                }
            },
            onNavigateToTemplates = { currentScreen = Screen.INVOICE_TEMPLATE },
            onNavigateToBusinessInfo = { currentScreen = Screen.BUSINESS_INFO },
            onNavigateToPaymentInstructions = { currentScreen = Screen.PAYMENT_INSTRUCTIONS },
            onNavigateToLogo = { currentScreen = Screen.LOGO },
            onNavigateToSignature = { currentScreen = Screen.SIGNATURE },
            onNavigateToCurrency = { currentScreen = Screen.CURRENCY },
            onNavigateToDateFormat = { currentScreen = Screen.DATE_FORMAT }
        )
        Screen.EDIT_INVOICE -> EditInvoiceScreen(
            onClose = { currentScreen = Screen.HOME },
            onSaveChanges = { currentScreen = Screen.HOME },
            onPreview = { currentScreen = Screen.INVOICE_PREVIEW },
            onDelete = { currentScreen = Screen.HOME }
        )
        Screen.INVOICE_TEMPLATE -> InvoiceTemplateScreen(
            onClose = { currentScreen = Screen.SETTINGS }
        )
        Screen.INVOICE_PREVIEW -> InvoicePreviewScreen(
            onClose = { currentScreen = Screen.EDIT_INVOICE },
            onShare = { },
            onSaveAsPdf = { }
        )
        Screen.BUSINESS_INFO -> BusinessInformationScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS }
        )
        Screen.PAYMENT_INSTRUCTIONS -> PaymentInstructionsScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS }
        )
        Screen.LOGO -> LogoScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS },
            onUploadLogo = { }
        )
        Screen.SIGNATURE -> SignatureScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS },
            onUploadSignature = { }
        )
        Screen.CURRENCY -> CurrencyScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS }
        )
        Screen.DATE_FORMAT -> DateFormatScreen(
            onClose = { currentScreen = Screen.SETTINGS },
            onSave = { currentScreen = Screen.SETTINGS }
        )
    }
}
