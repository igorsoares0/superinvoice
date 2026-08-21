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
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.PaymentInstructionsViewModel
import online.isdevapps.superinvoice.R

@Composable
fun PaymentInstructionsScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: PaymentInstructionsViewModel = hiltViewModel()
) {
    val bankName by viewModel.bankName.collectAsStateWithLifecycle()
    val accountHolderName by viewModel.accountHolderName.collectAsStateWithLifecycle()
    val accountNumber by viewModel.accountNumber.collectAsStateWithLifecycle()
    val routingNumber by viewModel.routingNumber.collectAsStateWithLifecycle()
    val iban by viewModel.iban.collectAsStateWithLifecycle()
    val swiftCode by viewModel.swiftCode.collectAsStateWithLifecycle()
    val bankAddress by viewModel.bankAddress.collectAsStateWithLifecycle()
    val paymentTerms by viewModel.paymentTerms.collectAsStateWithLifecycle()
    val additionalInstructions by viewModel.additionalInstructions
        .collectAsStateWithLifecycle()

    InvSettingsSubScreen(
        title = stringResource(R.string.title_payment_instructions),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = { viewModel.savePaymentInstructions { onSave() } }
    ) {
        InvField(
            value = bankName,
            onValueChange = { viewModel.setBankName(it) },
            label = stringResource(R.string.bank_name_required),
            opensSection = true
        )
        InvField(
            value = accountHolderName,
            onValueChange = { viewModel.setAccountHolderName(it) },
            label = stringResource(R.string.account_holder_required)
        )
        InvField(
            value = accountNumber,
            onValueChange = { viewModel.setAccountNumber(it) },
            label = stringResource(R.string.account_number_required)
        )
        InvField(
            value = routingNumber,
            onValueChange = { viewModel.setRoutingNumber(it) },
            label = stringResource(R.string.routing_number)
        )
        InvField(
            value = iban,
            onValueChange = { viewModel.setIban(it) },
            label = stringResource(R.string.iban)
        )
        InvField(
            value = swiftCode,
            onValueChange = { viewModel.setSwiftCode(it) },
            label = stringResource(R.string.swift_bic_code)
        )
        InvField(
            value = bankAddress,
            onValueChange = { viewModel.setBankAddress(it) },
            label = stringResource(R.string.bank_address)
        )
        InvField(
            value = paymentTerms,
            onValueChange = { viewModel.setPaymentTerms(it) },
            label = stringResource(R.string.payment_terms_placeholder)
        )
        InvoiceNotesField(
            value = additionalInstructions,
            onValueChange = { viewModel.setAdditionalInstructions(it) },
            label = stringResource(R.string.additional_instructions)
        )
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
