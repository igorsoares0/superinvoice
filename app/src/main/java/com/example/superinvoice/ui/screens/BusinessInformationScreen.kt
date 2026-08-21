package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.BusinessInformationViewModel
import online.isdevapps.superinvoice.R

@Composable
fun BusinessInformationScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: BusinessInformationViewModel = hiltViewModel()
) {
    val businessName by viewModel.businessName.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val website by viewModel.website.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    val city by viewModel.city.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zipCode by viewModel.zipCode.collectAsStateWithLifecycle()
    val taxId by viewModel.taxId.collectAsStateWithLifecycle()

    InvSettingsSubScreen(
        title = stringResource(R.string.title_business_information),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = { viewModel.saveBusinessInformation { onSave() } }
    ) {
        InvField(
            value = businessName,
            onValueChange = { viewModel.setBusinessName(it) },
            label = stringResource(R.string.business_name_required),
            opensSection = true
        )
        InvField(
            value = ownerName,
            onValueChange = { viewModel.setOwnerName(it) },
            label = stringResource(R.string.owner_name_required)
        )
        InvField(
            value = email,
            onValueChange = { viewModel.setEmail(it) },
            label = stringResource(R.string.email_required)
        )
        InvField(
            value = phone,
            onValueChange = { viewModel.setPhone(it) },
            label = stringResource(R.string.phone_required)
        )
        InvField(
            value = website,
            onValueChange = { viewModel.setWebsite(it) },
            label = stringResource(R.string.website)
        )
        InvField(
            value = address,
            onValueChange = { viewModel.setAddress(it) },
            label = stringResource(R.string.address_required)
        )
        InvField(
            value = city,
            onValueChange = { viewModel.setCity(it) },
            label = stringResource(R.string.city_required)
        )
        InvField(
            value = state,
            onValueChange = { viewModel.setState(it) },
            label = stringResource(R.string.state_required)
        )
        InvField(
            value = zipCode,
            onValueChange = { viewModel.setZipCode(it) },
            label = stringResource(R.string.zip_code_required)
        )
        InvField(
            value = taxId,
            onValueChange = { viewModel.setTaxId(it) },
            label = stringResource(R.string.tax_id_ein)
        )
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
