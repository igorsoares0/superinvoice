package com.example.superinvoice.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import online.isdevapps.superinvoice.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.viewmodel.PaywallViewModel
import com.revenuecat.purchases.Package

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val monthlyPackage by viewModel.monthlyPackage.collectAsStateWithLifecycle()
    val annualPackage by viewModel.annualPackage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val purchaseSuccess by viewModel.purchaseSuccess.collectAsStateWithLifecycle()

    var selectedPlan by remember { mutableStateOf("annual") }

    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess) onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.Black
                )
            }
            Text(
                text = stringResource(R.string.title_go_premium),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        Spacer(modifier = Modifier.height(24.dp))

        // Feature comparison
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.unlock_all_features),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.premium_subscription_description),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Feature list
            FeatureRow(text = stringResource(R.string.feature_unlimited_invoices), isFree = false, isPremium = true)
            FeatureRow(text = stringResource(R.string.feature_classic_template), isFree = true, isPremium = true)
            FeatureRow(text = stringResource(R.string.feature_premium_templates), isFree = false, isPremium = true)
            FeatureRow(text = stringResource(R.string.feature_remove_watermark), isFree = false, isPremium = true)
            FeatureRow(text = stringResource(R.string.feature_custom_logo), isFree = false, isPremium = true)
            FeatureRow(text = stringResource(R.string.feature_custom_signature), isFree = false, isPremium = true)

            Spacer(modifier = Modifier.height(32.dp))

            // Plan cards
            Text(
                text = stringResource(R.string.choose_your_plan),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Annual plan
            PlanCard(
                title = stringResource(R.string.plan_annual),
                price = annualPackage?.product?.price?.formatted ?: "$89.99/yr",
                subtitle = stringResource(R.string.plan_save_17),
                isSelected = selectedPlan == "annual",
                onClick = { selectedPlan = "annual" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Monthly plan
            PlanCard(
                title = stringResource(R.string.plan_monthly),
                price = monthlyPackage?.product?.price?.formatted ?: "$8.99/mo",
                subtitle = "",
                isSelected = selectedPlan == "monthly",
                onClick = { selectedPlan = "monthly" }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Subscribe button
            Button(
                onClick = {
                    val pkg = if (selectedPlan == "annual") annualPackage else monthlyPackage
                    if (pkg != null && activity != null) {
                        viewModel.purchase(activity, pkg)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D5016),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.subscribe),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Restore purchases
            TextButton(
                onClick = { if (!isLoading) viewModel.restorePurchases() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Gray,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.restore_purchases),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    text: String,
    isFree: Boolean,
    isPremium: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
            if (isFree) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.included_in_free),
                    tint = Color(0xFF9DEA6E),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.not_in_free),
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
            if (isPremium) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.included_in_premium),
                    tint = Color(0xFF9DEA6E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF9DEA6E) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFF9DEA6E).copy(alpha = 0.1f) else Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF2D5016),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = price,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}
