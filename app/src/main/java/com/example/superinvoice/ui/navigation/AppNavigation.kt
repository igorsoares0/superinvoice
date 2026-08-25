package com.example.superinvoice.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.superinvoice.data.analytics.PaywallSource
import com.example.superinvoice.ui.viewmodel.NavigationViewModel
import com.example.superinvoice.ui.screens.AddClientScreen
import com.example.superinvoice.ui.screens.AddProductScreen
import com.example.superinvoice.ui.screens.BusinessInformationScreen
import com.example.superinvoice.ui.screens.EditClientScreen
import com.example.superinvoice.ui.screens.EditProductScreen
import com.example.superinvoice.ui.screens.ClientsScreen
import com.example.superinvoice.ui.screens.CreateInvoiceScreen
import com.example.superinvoice.ui.screens.CurrencyScreen
import com.example.superinvoice.ui.screens.DateFormatScreen
import com.example.superinvoice.ui.screens.EditInvoiceScreen
import com.example.superinvoice.ui.screens.HomeScreen
import com.example.superinvoice.ui.screens.InvoicePreviewScreen
import com.example.superinvoice.ui.screens.InvoiceStyleScreen
import com.example.superinvoice.ui.screens.InvoiceTemplateScreen
import com.example.superinvoice.ui.screens.LogoScreen
import com.example.superinvoice.ui.screens.PaymentInstructionsScreen
import com.example.superinvoice.ui.screens.PaymentQrCodeScreen
import com.example.superinvoice.ui.screens.PaywallScreen
import com.example.superinvoice.ui.screens.PolicyScreen
import com.example.superinvoice.ui.screens.SupportScreen
import com.example.superinvoice.ui.screens.TermsScreen
import com.example.superinvoice.ui.screens.ProductsServicesScreen
import com.example.superinvoice.ui.screens.SettingsScreen
import com.example.superinvoice.ui.screens.SignatureScreen

enum class Screen {
    HOME,
    CREATE_INVOICE,
    CLIENTS,
    ADD_CLIENT,
    EDIT_CLIENT,
    PRODUCTS_SERVICES,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    SETTINGS,
    EDIT_INVOICE,
    INVOICE_TEMPLATE,
    INVOICE_PREVIEW,
    BUSINESS_INFO,
    PAYMENT_INSTRUCTIONS,
    LOGO,
    SIGNATURE,
    INVOICE_STYLE,
    PAYMENT_QR_CODE,
    CURRENCY,
    DATE_FORMAT,
    PAYWALL,
    POLICY,
    TERMS,
    SUPPORT
}

@Composable
fun AppNavigation(
    navigationViewModel: NavigationViewModel = hiltViewModel()
) {
    val isPremium by navigationViewModel.isPremium.collectAsStateWithLifecycle()
    val invoiceCount by navigationViewModel.invoiceCount.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    // O gate de criação virou assíncrono (espera o status premium resolver), então
    // precisa de trava: dois toques rápidos empilhariam duas navegações.
    var isCheckingInvoiceGate by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedBottomNavItem by remember { mutableIntStateOf(0) }
    var navigationStack by remember { mutableStateOf(listOf<Screen>()) }
    var selectedInvoiceId by remember { mutableIntStateOf(0) }
    var selectedClientId by remember { mutableIntStateOf(0) }
    var selectedProductId by remember { mutableIntStateOf(0) }
    var isSelectingForInvoice by remember { mutableStateOf(false) }
    var pendingClientSelection by remember { mutableStateOf<com.example.superinvoice.data.Client?>(null) }
    var pendingProductSelection by remember { mutableStateOf<com.example.superinvoice.data.ProductService?>(null) }
    var productSelectionVersion by remember { mutableIntStateOf(0) }
    var clientSelectionVersion by remember { mutableIntStateOf(0) }
    var shouldResetCreateInvoice by remember { mutableStateOf(false) }
    var previewVersion by remember { mutableIntStateOf(0) }

    // Qual gate mandou o usuário para o paywall. Sem NavHost não há argumento de rota,
    // então a origem viaja neste estado, gravado no mesmo lugar que decide navegar.
    var paywallSource by remember { mutableStateOf(PaywallSource.SETTINGS) }

    // Navigate to a screen and add to history
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            navigationStack = navigationStack + currentScreen
            currentScreen = screen
        }
    }

    fun navigateToPaywall(source: PaywallSource) {
        paywallSource = source
        navigateTo(Screen.PAYWALL)
    }

    // Um screen_view por tela, derivado do enum — nunca de texto do usuário.
    LaunchedEffect(currentScreen) {
        navigationViewModel.onScreenShown(currentScreen.name)
    }

    // Navigate to edit invoice with ID
    fun navigateToEditInvoice(invoiceId: Int) {
        selectedInvoiceId = invoiceId
        navigateTo(Screen.EDIT_INVOICE)
    }

    // Navigate to edit client with ID
    fun navigateToEditClient(clientId: Int) {
        selectedClientId = clientId
        navigateTo(Screen.EDIT_CLIENT)
    }

    // Navigate to edit product with ID
    fun navigateToEditProduct(productId: Int) {
        selectedProductId = productId
        navigateTo(Screen.EDIT_PRODUCT)
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
            Screen.SETTINGS -> selectedBottomNavItem = 1
            else -> {}
        }
    }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onNavigateToCreateInvoice = {
                if (!isCheckingInvoiceGate) {
                    isCheckingInvoiceGate = true
                    coroutineScope.launch {
                        try {
                            if (navigationViewModel.canCreateInvoice()) {
                                navigationViewModel.onInvoiceCreationStarted()
                                shouldResetCreateInvoice = true
                                navigateTo(Screen.CREATE_INVOICE)
                            } else {
                                navigateToPaywall(PaywallSource.INVOICE_LIMIT)
                            }
                        } finally {
                            isCheckingInvoiceGate = false
                        }
                    }
                }
            },
            isPremium = isPremium,
            invoiceCount = invoiceCount,
            onNavigateToEditInvoice = { invoiceId -> navigateToEditInvoice(invoiceId) },
            onNavigateToPreview = { invoiceId ->
                selectedInvoiceId = invoiceId
                previewVersion++
                navigateTo(Screen.INVOICE_PREVIEW)
            },
            selectedBottomNavItem = selectedBottomNavItem,
            onBottomNavItemSelected = { index ->
                selectedBottomNavItem = index
                when (index) {
                    0 -> {
                        navigationStack = emptyList()
                        currentScreen = Screen.HOME
                    }
                    1 -> navigateTo(Screen.SETTINGS)
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
            onNavigateToEditClient = { clientId -> navigateToEditClient(clientId) },
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
        Screen.EDIT_CLIENT -> EditClientScreen(
            clientId = selectedClientId,
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.PRODUCTS_SERVICES -> ProductsServicesScreen(
            onClose = {
                isSelectingForInvoice = false
                navigateBack()
            },
            onNavigateToAddProductService = { navigateTo(Screen.ADD_PRODUCT) },
            onNavigateToEditProduct = { productId -> navigateToEditProduct(productId) },
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
        Screen.EDIT_PRODUCT -> EditProductScreen(
            productId = selectedProductId,
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
                    1 -> {
                        navigationStack = emptyList()
                        currentScreen = Screen.SETTINGS
                    }
                }
            },
            onNavigateToTemplates = { navigateTo(Screen.INVOICE_TEMPLATE) },
            onNavigateToBusinessInfo = { navigateTo(Screen.BUSINESS_INFO) },
            onNavigateToPaymentInstructions = { navigateTo(Screen.PAYMENT_INSTRUCTIONS) },
            onNavigateToLogo = {
                if (isPremium) navigateTo(Screen.LOGO) else navigateToPaywall(PaywallSource.LOGO)
            },
            onNavigateToSignature = {
                if (isPremium) navigateTo(Screen.SIGNATURE) else navigateToPaywall(PaywallSource.SIGNATURE)
            },
            onNavigateToInvoiceStyle = {
                if (isPremium) navigateTo(Screen.INVOICE_STYLE) else navigateToPaywall(PaywallSource.INVOICE_STYLE)
            },
            onNavigateToPaymentQrCode = { navigateTo(Screen.PAYMENT_QR_CODE) },
            onNavigateToCurrency = { navigateTo(Screen.CURRENCY) },
            onNavigateToDateFormat = { navigateTo(Screen.DATE_FORMAT) },
            onNavigateToManageClients = {
                isSelectingForInvoice = false
                navigateTo(Screen.CLIENTS)
            },
            onNavigateToManageProducts = {
                isSelectingForInvoice = false
                navigateTo(Screen.PRODUCTS_SERVICES)
            },
            onNavigateToPaywall = { navigateToPaywall(PaywallSource.SETTINGS) },
            onNavigateToPolicy = { navigateTo(Screen.POLICY) },
            onNavigateToTerms = { navigateTo(Screen.TERMS) },
            onNavigateToSupport = { navigateTo(Screen.SUPPORT) },
            onRestorePurchases = { onSuccess, onError ->
                navigationViewModel.restorePurchases(onSuccess, onError)
            },
            isPremium = isPremium
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
            onClose = { navigateBack() },
            isPremium = isPremium,
            onNavigateToPaywall = { navigateToPaywall(PaywallSource.TEMPLATE) }
        )
        Screen.INVOICE_PREVIEW -> InvoicePreviewScreen(
            invoiceId = selectedInvoiceId,
            previewVersion = previewVersion,
            onClose = { navigateBack() }
        )
        Screen.INVOICE_STYLE -> InvoiceStyleScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
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
            onSave = { navigateBack() }
        )
        Screen.SIGNATURE -> SignatureScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.PAYMENT_QR_CODE -> PaymentQrCodeScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.CURRENCY -> CurrencyScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.DATE_FORMAT -> DateFormatScreen(
            onClose = { navigateBack() },
            onSave = { navigateBack() }
        )
        Screen.PAYWALL -> PaywallScreen(
            onClose = { navigateBack() },
            source = paywallSource
        )
        Screen.POLICY -> PolicyScreen(
            onClose = { navigateBack() }
        )
        Screen.TERMS -> TermsScreen(
            onClose = { navigateBack() }
        )
        Screen.SUPPORT -> SupportScreen(
            onClose = { navigateBack() }
        )
    }
}
