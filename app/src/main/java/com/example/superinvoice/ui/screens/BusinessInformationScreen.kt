package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.example.superinvoice.ui.viewmodel.BusinessInformationViewModel

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
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    text = stringResource(R.string.title_business_information),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
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
                    value = businessName,
                    onValueChange = { viewModel.setBusinessName(it) },
                    placeholder = stringResource(R.string.business_name_required),
                    icon = Icons.Default.Home
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = ownerName,
                    onValueChange = { viewModel.setOwnerName(it) },
                    placeholder = stringResource(R.string.owner_name_required),
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = email,
                    onValueChange = { viewModel.setEmail(it) },
                    placeholder = stringResource(R.string.email_required),
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = phone,
                    onValueChange = { viewModel.setPhone(it) },
                    placeholder = stringResource(R.string.phone_required),
                    icon = Icons.Default.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = website,
                    onValueChange = { viewModel.setWebsite(it) },
                    placeholder = stringResource(R.string.website),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = address,
                    onValueChange = { viewModel.setAddress(it) },
                    placeholder = stringResource(R.string.address_required),
                    iconRes = R.drawable.ic_address
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClientInputField(
                        value = city,
                        onValueChange = { viewModel.setCity(it) },
                        placeholder = stringResource(R.string.city_required),
                        iconRes = R.drawable.ic_address,
                        modifier = Modifier.weight(1f)
                    )

                    ClientInputField(
                        value = state,
                        onValueChange = { viewModel.setState(it) },
                        placeholder = stringResource(R.string.state_required),
                        iconRes = R.drawable.ic_address,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = zipCode,
                    onValueChange = { viewModel.setZipCode(it) },
                    placeholder = stringResource(R.string.zip_code_required),
                    iconRes = R.drawable.ic_zipcode
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = taxId,
                    onValueChange = { viewModel.setTaxId(it) },
                    placeholder = stringResource(R.string.tax_id_ein),
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.saveBusinessInformation {
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
