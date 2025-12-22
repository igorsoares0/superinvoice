package com.example.superinvoice.ui.navigation

import androidx.activity.compose.BackHandler
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
    var navigationStack by remember { mutableStateOf(listOf<Screen>()) }
    var selectedInvoiceId by remember { mutableIntStateOf(0) }
    var isSelectingForInvoice by remember { mutableStateOf(false) }
    var pendingClientSelection by remember { mutableStateOf<com.example.superinvoice.data.Client?>(null) }
    var pendingProductSelection by remember { mutableStateOf<com.example.superinvoice.data.ProductService?>(null) }
    var productSelectionVersion by remember { mutableIntStateOf(0) }
    var clientSelectionVersion by remember { mutableIntStateOf(0) }
    var shouldResetCreateInvoice by remember { mutableStateOf(false) }
    var previewVersion by remember { mutableIntStateOf(0) }

    // Navigate to a screen and add to history
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            navigationStack = navigationStack + currentScreen
            currentScreen = screen
        }
    }

    // Navigate to edit invoice with ID
    fun navigateToEditInvoice(invoiceId: Int) {
        selectedInvoiceId = invoiceId
        navigateTo(Screen.EDIT_INVOICE)
    }

    // Navigate back to previous screen
    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            currentScreen = navigationStack.last()
            navigationStack = navigationStack.dropLast(1)
        }
    }

    // Handle back button - go to previous screen in stack
    BackHandler(enabled = navigationStack.isNotEmpty()) {
        navigateBack()
        // Update bottom nav if returning to HOME or SETTINGS
        when (currentScreen) {
            Screen.HOME -> selectedBottomNavItem = 0
            Screen.SETTINGS -> selectedBottomNavItem = 2
            else -> {}
        }
    }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onNavigateToCreateInvoice = {
                shouldResetCreateInvoice = true
                navigateTo(Screen.CREATE_INVOICE)
            },
            onNavigateToEditInvoice = { invoiceId -> navigateToEditInvoice(invoiceId) },
            onNavigateToAddClient = { navigateTo(Screen.ADD_CLIENT) },
            onNavigateToAddProduct = { navigateTo(Screen.ADD_PRODUCT) },
            selectedBottomNavItem = selectedBottomNavItem,
            onBottomNavItemSelected = { index ->
                selectedBottomNavItem = index
                when (index) {
                    0 -> {
                        navigationStack = emptyList()
                        currentScreen = Screen.HOME
                    }
                    2 -> navigateTo(Screen.SETTINGS)
                }
            }
        )
        Screen.CREATE_INVOICE -> CreateInvoiceScreen(
            onClose = {
                shouldResetCreateInvoice = false
                navigateBack()
            },
            onSave = {
                shouldResetCreateInvoice = false
                navigationStack = emptyList()
                currentScreen = Screen.HOME
            },
            onNavigateToSelectClient = {
                shouldResetCreateInvoice = false
                isSelectingForInvoice = true
                navigateTo(Screen.CLIENTS)
            },
            onNavigateToSelectProduct = {
                shouldResetCreateInvoice = false
                isSelectingForInvoice = true
                navigateTo(Screen.PRODUCTS_SERVICES)
            },
            pendingClientSelection = pendingClientSelection,
            pendingProductSelection = pendingProductSelection,
            clientSelectionVersion = clientSelectionVersion,
            productSelectionVersion = productSelectionVersion,
            onClientSelectionProcessed = { pendingClientSelection = null },
            onProductSelectionProcessed = { pendingProductSelection = null },
            shouldReset = shouldResetCreateInvoice
        )
        Screen.CLIENTS -> ClientsScreen(
            onClose = {
                isSelectingForInvoice = false
                navigateBack()
            },
            onNavigateToAddClient = { navigateTo(Screen.ADD_CLIENT) },
            onClientSelected = if (isSelectingForInvoice) {
                { client: com.example.superinvoice.data.Client ->
                    pendingClientSelection = client
                    clientSelectionVersion++
                    isSelectingForInvoice = false
                    navigateBack()
                }
            } else null
        )
        Screen.ADD_CLIENT -> AddClientScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.PRODUCTS_SERVICES -> ProductsServicesScreen(
            onClose = {
                isSelectingForInvoice = false
                navigateBack()
            },
            onNavigateToAddProductService = { navigateTo(Screen.ADD_PRODUCT) },
            onProductSelected = if (isSelectingForInvoice) {
                { product: com.example.superinvoice.data.ProductService ->
                    pendingProductSelection = product
                    productSelectionVersion++
                    isSelectingForInvoice = false
                    navigateBack()
                }
            } else null
        )
        Screen.ADD_PRODUCT -> AddProductScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.SETTINGS -> SettingsScreen(
            selectedBottomNavItem = selectedBottomNavItem,
            onBottomNavItemSelected = { index ->
                selectedBottomNavItem = index
                when (index) {
                    0 -> {
                        navigationStack = emptyList()
                        currentScreen = Screen.HOME
                    }
                    2 -> {
                        navigationStack = emptyList()
                        currentScreen = Screen.SETTINGS
                    }
                }
            },
            onNavigateToTemplates = { navigateTo(Screen.INVOICE_TEMPLATE) },
            onNavigateToBusinessInfo = { navigateTo(Screen.BUSINESS_INFO) },
            onNavigateToPaymentInstructions = { navigateTo(Screen.PAYMENT_INSTRUCTIONS) },
            onNavigateToLogo = { navigateTo(Screen.LOGO) },
            onNavigateToSignature = { navigateTo(Screen.SIGNATURE) },
            onNavigateToCurrency = { navigateTo(Screen.CURRENCY) },
            onNavigateToDateFormat = { navigateTo(Screen.DATE_FORMAT) },
            onNavigateToAddClient = { navigateTo(Screen.ADD_CLIENT) },
            onNavigateToAddProduct = { navigateTo(Screen.ADD_PRODUCT) }
        )
        Screen.EDIT_INVOICE -> EditInvoiceScreen(
            invoiceId = selectedInvoiceId,
            onClose = { navigateBack() },
            onSave = {
                navigationStack = emptyList()
                currentScreen = Screen.HOME
            },
            onNavigateToSelectClient = {
                isSelectingForInvoice = true
                navigateTo(Screen.CLIENTS)
            },
            onNavigateToSelectProduct = {
                isSelectingForInvoice = true
                navigateTo(Screen.PRODUCTS_SERVICES)
            },
            onNavigateToPreview = {
                previewVersion++
                navigateTo(Screen.INVOICE_PREVIEW)
            },
            pendingClientSelection = pendingClientSelection,
            pendingProductSelection = pendingProductSelection,
            clientSelectionVersion = clientSelectionVersion,
            productSelectionVersion = productSelectionVersion,
            onClientSelectionProcessed = { pendingClientSelection = null },
            onProductSelectionProcessed = { pendingProductSelection = null }
        )
        Screen.INVOICE_TEMPLATE -> InvoiceTemplateScreen(
            onClose = { navigateBack() }
        )
        Screen.INVOICE_PREVIEW -> InvoicePreviewScreen(
            invoiceId = selectedInvoiceId,
            previewVersion = previewVersion,
            onClose = { navigateBack() },
            onShare = { },
            onSaveAsPdf = { }
        )
        Screen.BUSINESS_INFO -> BusinessInformationScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.PAYMENT_INSTRUCTIONS -> PaymentInstructionsScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.LOGO -> LogoScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() },
            onUploadLogo = { }
        )
        Screen.SIGNATURE -> SignatureScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() },
            onUploadSignature = { }
        )
        Screen.CURRENCY -> CurrencyScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.DATE_FORMAT -> DateFormatScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
    }
}
