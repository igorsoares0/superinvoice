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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToDateFormat: () -> Unit = {},
    onNavigateToManageClients: () -> Unit = {},
    onNavigateToAddClient: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFFFFFFFF),
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
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Settings",
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
                PremiumCard(
                    onUnlockClick = { },
                    modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
                )

                Text(
                    text = "Business",
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
                    SettingsOption(text = "Logo", onClick = onNavigateToLogo)
                    SettingsOption(text = "Business Information", onClick = onNavigateToBusinessInfo)
                    SettingsOption(text = "Signature", onClick = onNavigateToSignature)
                    SettingsOption(text = "Payment Instructions", onClick = onNavigateToPaymentInstructions)
                }

                Text(
                    text = "General",
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
                    SettingsOption(text = "Manage Clients", onClick = onNavigateToManageClients)
                    SettingsOption(text = "Currency", onClick = onNavigateToCurrency)
                    SettingsOption(text = "Date format", onClick = onNavigateToDateFormat)
                    SettingsOption(text = "Language", onClick = { })
                    SettingsOption(text = "Templates", onClick = onNavigateToTemplates)
                    SettingsOption(text = "Terms", onClick = { })
                    SettingsOption(text = "Policy", onClick = { })
                    SettingsOption(text = "Support", onClick = { })
                }
            }
        }
    }
}
