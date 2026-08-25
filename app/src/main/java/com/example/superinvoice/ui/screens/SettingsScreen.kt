package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.ui.components.BottomNavigationBar
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.PremiumCard
import com.example.superinvoice.ui.components.SettingsOption
import com.example.superinvoice.ui.components.SettingsToggleOption
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun SettingsScreen(
    selectedBottomNavItem: Int,
    onBottomNavItemSelected: (Int) -> Unit,
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToBusinessInfo: () -> Unit = {},
    onNavigateToPaymentInstructions: () -> Unit = {},
    onNavigateToLogo: () -> Unit = {},
    onNavigateToSignature: () -> Unit = {},
    onNavigateToInvoiceStyle: () -> Unit = {},
    onNavigateToPaymentQrCode: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToDateFormat: () -> Unit = {},
    onNavigateToManageClients: () -> Unit = {},
    onNavigateToManageProducts: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onRestorePurchases: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _ -> },
    isPremium: Boolean = false,
    analyticsEnabled: Boolean = true,
    onAnalyticsEnabledChange: (Boolean) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isRestoringPurchases by remember { mutableStateOf(false) }
    val context = LocalContext.current

    InvScaffold(
        snackbarHostState = snackbarHostState,
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = onBottomNavItemSelected
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InvScreenHeader(
                title = stringResource(R.string.title_settings),
                titleStyle = InvType.screenTitle
            )

            Column(modifier = Modifier.padding(horizontal = Space.screen)) {
                if (!isPremium) {
                    PremiumCard(
                        onUnlockClick = onNavigateToPaywall,
                        modifier = Modifier.padding(bottom = Space.xxl)
                    )
                }

                SettingsSectionLabel(stringResource(R.string.section_business))
                InvSectionRule()
                SettingsOption(
                    text = stringResource(R.string.settings_logo),
                    onClick = onNavigateToLogo
                )
                SettingsOption(
                    text = stringResource(R.string.settings_business_info),
                    onClick = onNavigateToBusinessInfo
                )
                SettingsOption(
                    text = stringResource(R.string.settings_signature),
                    onClick = onNavigateToSignature
                )
                SettingsOption(
                    text = stringResource(R.string.settings_invoice_style),
                    onClick = onNavigateToInvoiceStyle
                )
                SettingsOption(
                    text = stringResource(R.string.settings_payment_instructions),
                    onClick = onNavigateToPaymentInstructions
                )
                SettingsOption(
                    text = stringResource(R.string.settings_payment_qr_code),
                    onClick = onNavigateToPaymentQrCode
                )

                Spacer(modifier = Modifier.height(Space.xxl))

                SettingsSectionLabel(stringResource(R.string.section_general))
                InvSectionRule()
                SettingsOption(
                    text = stringResource(R.string.settings_manage_clients),
                    onClick = onNavigateToManageClients
                )
                SettingsOption(
                    text = stringResource(R.string.settings_manage_products),
                    onClick = onNavigateToManageProducts
                )
                SettingsOption(
                    text = stringResource(R.string.settings_currency),
                    onClick = onNavigateToCurrency
                )
                SettingsOption(
                    text = stringResource(R.string.settings_date_format),
                    onClick = onNavigateToDateFormat
                )
                SettingsOption(
                    text = stringResource(R.string.settings_templates),
                    onClick = onNavigateToTemplates
                )
                SettingsOption(
                    text = if (isRestoringPurchases) {
                        stringResource(R.string.settings_restoring)
                    } else {
                        stringResource(R.string.settings_restore_purchases)
                    },
                    showArrow = !isRestoringPurchases,
                    onClick = {
                        if (!isRestoringPurchases) {
                            isRestoringPurchases = true
                            onRestorePurchases(
                                {
                                    isRestoringPurchases = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.purchases_restored_successfully
                                            )
                                        )
                                    }
                                },
                                { error ->
                                    isRestoringPurchases = false
                                    scope.launch { snackbarHostState.showSnackbar(error) }
                                }
                            )
                        }
                    }
                )
                SettingsOption(
                    text = stringResource(R.string.settings_terms),
                    onClick = onNavigateToTerms
                )
                SettingsOption(
                    text = stringResource(R.string.settings_policy),
                    onClick = onNavigateToPolicy
                )
                SettingsOption(
                    text = stringResource(R.string.settings_support),
                    onClick = onNavigateToSupport
                )
                // No fim da lista: é o único controle com interruptor no meio de linhas
                // com seta, então destoa menos aqui do que entre elas.
                SettingsToggleOption(
                    text = stringResource(R.string.settings_share_analytics),
                    description = stringResource(R.string.settings_share_analytics_description),
                    checked = analyticsEnabled,
                    onCheckedChange = onAnalyticsEnabledChange
                )

                Spacer(modifier = Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = InvType.label,
        color = Neutral,
        modifier = Modifier.padding(bottom = Space.md)
    )
}
