package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import online.isdevapps.superinvoice.R
import com.example.superinvoice.ui.components.ClientInputField
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.viewmodel.PaymentInstructionsViewModel

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
    val additionalInstructions by viewModel.additionalInstructions.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color(0xFFF9FAFB)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.Black
                    )
                }
                Text(
                    text = stringResource(R.string.title_payment_instructions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(0.1f))
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                ClientInputField(
                    value = bankName,
                    onValueChange = { viewModel.setBankName(it) },
                    placeholder = stringResource(R.string.bank_name_required),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = accountHolderName,
                    onValueChange = { viewModel.setAccountHolderName(it) },
                    placeholder = stringResource(R.string.account_holder_required),
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = accountNumber,
                    onValueChange = { viewModel.setAccountNumber(it) },
                    placeholder = stringResource(R.string.account_number_required),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = routingNumber,
                    onValueChange = { viewModel.setRoutingNumber(it) },
                    placeholder = stringResource(R.string.routing_number),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = iban,
                    onValueChange = { viewModel.setIban(it) },
                    placeholder = stringResource(R.string.iban),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = swiftCode,
                    onValueChange = { viewModel.setSwiftCode(it) },
                    placeholder = stringResource(R.string.swift_bic_code),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = bankAddress,
                    onValueChange = { viewModel.setBankAddress(it) },
                    placeholder = stringResource(R.string.bank_address),
                    iconRes = R.drawable.ic_address
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = paymentTerms,
                    onValueChange = { viewModel.setPaymentTerms(it) },
                    placeholder = stringResource(R.string.payment_terms_placeholder),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                InvoiceNotesField(
                    value = additionalInstructions,
                    onValueChange = { viewModel.setAdditionalInstructions(it) },
                    placeholder = stringResource(R.string.additional_instructions)
                )

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.savePaymentInstructions {
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9DEA6E),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
