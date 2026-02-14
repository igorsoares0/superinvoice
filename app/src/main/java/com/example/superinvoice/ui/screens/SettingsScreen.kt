package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R
import com.example.superinvoice.ui.components.BottomNavigationBar
import com.example.superinvoice.ui.components.PremiumCard
import com.example.superinvoice.ui.components.SettingsOption

@Composable
fun SettingsScreen(
    selectedBottomNavItem: Int,
    onBottomNavItemSelected: (Int) -> Unit,
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToBusinessInfo: () -> Unit = {},
    onNavigateToPaymentInstructions: () -> Unit = {},
    onNavigateToLogo: () -> Unit = {},
    onNavigateToSignature: () -> Unit = {},
    onNavigateToPaymentQrCode: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToDateFormat: () -> Unit = {},
    onNavigateToManageClients: () -> Unit = {},
    onNavigateToManageProducts: () -> Unit = {},
    onNavigateToAddClient: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onRestorePurchases: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _ -> },
    isPremium: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isRestoringPurchases by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF9FAFB),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = onBottomNavItemSelected,
                onAddClient = onNavigateToAddClient,
                onAddProduct = onNavigateToAddProduct
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.title_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp)
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (!isPremium) {
                    PremiumCard(
                        onUnlockClick = onNavigateToPaywall,
                        modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.section_business),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    SettingsOption(text = stringResource(R.string.settings_logo), onClick = onNavigateToLogo)
                    SettingsOption(text = stringResource(R.string.settings_business_info), onClick = onNavigateToBusinessInfo)
                    SettingsOption(text = stringResource(R.string.settings_signature), onClick = onNavigateToSignature)
                    SettingsOption(text = stringResource(R.string.settings_payment_instructions), onClick = onNavigateToPaymentInstructions)
                    SettingsOption(text = stringResource(R.string.settings_payment_qr_code), onClick = onNavigateToPaymentQrCode)
                }

                Text(
                    text = stringResource(R.string.section_general),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    SettingsOption(text = stringResource(R.string.settings_manage_clients), onClick = onNavigateToManageClients)
                    SettingsOption(text = stringResource(R.string.settings_manage_products), onClick = onNavigateToManageProducts)
                    SettingsOption(text = stringResource(R.string.settings_currency), onClick = onNavigateToCurrency)
                    SettingsOption(text = stringResource(R.string.settings_date_format), onClick = onNavigateToDateFormat)
                    SettingsOption(text = stringResource(R.string.settings_language), onClick = { })
                    SettingsOption(text = stringResource(R.string.settings_templates), onClick = onNavigateToTemplates)
                    SettingsOption(
                        text = if (isRestoringPurchases) stringResource(R.string.settings_restoring) else stringResource(R.string.settings_restore_purchases),
                        onClick = {
                            if (!isRestoringPurchases) {
                                isRestoringPurchases = true
                                onRestorePurchases(
                                    {
                                        isRestoringPurchases = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.purchases_restored_successfully))
                                        }
                                    },
                                    { error ->
                                        isRestoringPurchases = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                )
                            }
                        }
                    )
                    SettingsOption(text = stringResource(R.string.settings_terms), onClick = onNavigateToTerms)
                    SettingsOption(text = stringResource(R.string.settings_policy), onClick = onNavigateToPolicy)
                    SettingsOption(text = stringResource(R.string.settings_support), onClick = onNavigateToSupport)
                }
            }
        }
    }
}
