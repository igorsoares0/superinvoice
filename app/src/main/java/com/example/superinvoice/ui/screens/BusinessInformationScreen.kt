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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superinvoice.R
import com.example.superinvoice.ui.components.ClientInputField

@Composable
fun BusinessInformationScreen(
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFFFFFFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
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
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Business Information",
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
                    onValueChange = { businessName = it },
                    placeholder = "Business Name*",
                    icon = Icons.Default.Home
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    placeholder = "Owner Name*",
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email*",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "Phone*",
                    icon = Icons.Default.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = website,
                    onValueChange = { website = it },
                    placeholder = "Website",
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "Address*",
                    iconRes = R.drawable.ic_address
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClientInputField(
                        value = city,
                        onValueChange = { city = it },
                        placeholder = "City*",
                        iconRes = R.drawable.ic_address,
                        modifier = Modifier.weight(1f)
                    )

                    ClientInputField(
                        value = state,
                        onValueChange = { state = it },
                        placeholder = "State*",
                        iconRes = R.drawable.ic_address,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = zipCode,
                    onValueChange = { zipCode = it },
                    placeholder = "ZIP Code*",
                    iconRes = R.drawable.ic_zipcode
                )

                Spacer(modifier = Modifier.height(16.dp))

                ClientInputField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    placeholder = "Tax ID / EIN",
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Save Button
            Button(
                onClick = onSave,
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
                    text = "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
