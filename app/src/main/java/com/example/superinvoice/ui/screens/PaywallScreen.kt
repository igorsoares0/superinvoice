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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvDivider
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Ghost
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.OrangeWash
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.PaywallViewModel
import online.isdevapps.superinvoice.R

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

    InvScaffold {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InvScreenHeader(
                title = stringResource(R.string.title_go_premium),
                onClose = onClose,
                closeContentDescription = stringResource(R.string.close)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screen)
            ) {
                Text(
                    text = stringResource(R.string.unlock_all_features),
                    style = InvType.sectionTitle,
                    color = Ink
                )
                Text(
                    text = stringResource(R.string.premium_subscription_description),
                    style = InvType.body,
                    color = Neutral,
                    modifier = Modifier.padding(top = Space.sm, bottom = Space.xl)
                )

                InvSectionRule()
                FeatureRow(
                    text = stringResource(R.string.feature_unlimited_invoices),
                    isFree = false,
                    isPremium = true
                )
                FeatureRow(
                    text = stringResource(R.string.feature_classic_template),
                    isFree = true,
                    isPremium = true
                )
                FeatureRow(
                    text = stringResource(R.string.feature_premium_templates),
                    isFree = false,
                    isPremium = true
                )
                FeatureRow(
                    text = stringResource(R.string.feature_remove_watermark),
                    isFree = false,
                    isPremium = true
                )
                FeatureRow(
                    text = stringResource(R.string.feature_custom_logo),
                    isFree = false,
                    isPremium = true
                )
                FeatureRow(
                    text = stringResource(R.string.feature_custom_signature),
                    isFree = false,
                    isPremium = true
                )

                Spacer(modifier = Modifier.height(Space.xxl))

                Text(
                    text = stringResource(R.string.choose_your_plan).uppercase(),
                    style = InvType.label,
                    color = Neutral,
                    modifier = Modifier.padding(bottom = Space.lg)
                )

                val savingsPercent = if (monthlyPackage != null && annualPackage != null) {
                    val yearlyIfMonthly = monthlyPackage!!.product.price.amountMicros * 12
                    val annualPrice = annualPackage!!.product.price.amountMicros
                    if (yearlyIfMonthly > 0) {
                        ((yearlyIfMonthly - annualPrice) * 100 / yearlyIfMonthly).toInt()
                    } else {
                        0
                    }
                } else {
                    0
                }

                PlanCard(
                    title = stringResource(R.string.plan_annual),
                    price = annualPackage?.product?.price?.formatted ?: "$79.99/yr",
                    subtitle = if (savingsPercent > 0) {
                        stringResource(R.string.plan_save_percent, savingsPercent)
                    } else {
                        ""
                    },
                    isSelected = selectedPlan == "annual",
                    onClick = { selectedPlan = "annual" }
                )

                Spacer(modifier = Modifier.height(Space.md))

                PlanCard(
                    title = stringResource(R.string.plan_monthly),
                    price = monthlyPackage?.product?.price?.formatted ?: "$8.99/mo",
                    subtitle = "",
                    isSelected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" }
                )

                Spacer(modifier = Modifier.height(Space.xl))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = InvType.body,
                        color = Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Space.md)
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Size.button),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(IconSize.action),
                            color = Orange
                        )
                    }
                } else {
                    InvButton(
                        text = stringResource(R.string.subscribe),
                        onClick = {
                            val pkg = if (selectedPlan == "annual") {
                                annualPackage
                            } else {
                                monthlyPackage
                            }
                            if (pkg != null && activity != null) {
                                viewModel.purchase(activity, pkg)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(Space.lg))

                TextButton(
                    onClick = { if (!isLoading) viewModel.restorePurchases() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.restore_purchases),
                        style = InvType.body,
                        color = Neutral
                    )
                }

                Spacer(modifier = Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun FeatureRow(
    text: String,
    isFree: Boolean,
    isPremium: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = InvType.body,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                if (isFree) {
                    Icon(
                        imageVector = InvIcons.Check,
                        contentDescription = stringResource(R.string.included_in_free),
                        tint = Ink,
                        modifier = Modifier.size(IconSize.lg)
                    )
                } else {
                    Icon(
                        imageVector = InvIcons.Close,
                        contentDescription = stringResource(R.string.not_in_free),
                        tint = Ghost,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }
            }
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                if (isPremium) {
                    Icon(
                        imageVector = InvIcons.Check,
                        contentDescription = stringResource(R.string.included_in_premium),
                        tint = Orange,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }
            }
        }
        InvDivider()
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
            .clip(InvShape.card)
            .background(if (isSelected) OrangeWash else Color.Transparent)
            .border(
                width = if (isSelected) Size.underline else Size.hairline,
                color = if (isSelected) Orange else Hairline,
                shape = InvShape.card
            )
            .clickable(onClick = onClick)
            .padding(Space.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text(
                text = title,
                style = InvType.name,
                color = Ink,
                maxLines = 1
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle.uppercase(),
                    style = InvType.status,
                    color = Orange,
                    maxLines = 1
                )
            }
        }
        Text(
            text = price,
            style = InvType.amountRow,
            color = Ink,
            maxLines = 1,
            softWrap = false
        )
    }
}
