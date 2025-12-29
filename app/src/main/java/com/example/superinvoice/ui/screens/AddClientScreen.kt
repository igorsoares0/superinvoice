package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.superinvoice.R
import com.example.superinvoice.ui.components.ClientInputField
import com.example.superinvoice.ui.viewmodel.ClientsViewModel

@Composable
fun AddClientScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: ClientsViewModel = hiltViewModel()
) {
    var clientName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 72.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }

                Text(
                    text = "Add Client",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(24.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ClientInputField(
                    placeholder = "Client Name *",
                    value = clientName,
                    onValueChange = { clientName = it },
                    icon = Icons.Default.Person
                )

                ClientInputField(
                    placeholder = "Email",
                    value = email,
                    onValueChange = { email = it },
                    icon = Icons.Default.Email
                )

                ClientInputField(
                    placeholder = "Phone",
                    value = phone,
                    onValueChange = { phone = it },
                    icon = Icons.Default.Phone
                )

                ClientInputField(
                    placeholder = "Address",
                    value = address,
                    onValueChange = { address = it },
                    iconRes = R.drawable.ic_address
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClientInputField(
                        placeholder = "City",
                        value = city,
                        onValueChange = { city = it },
                        modifier = Modifier.weight(1f)
                    )
                    ClientInputField(
                        placeholder = "State",
                        value = state,
                        onValueChange = { state = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                ClientInputField(
                    placeholder = "ZIP Code",
                    value = zipCode,
                    onValueChange = { zipCode = it },
                    iconRes = R.drawable.ic_zipcode
                )

                ClientInputField(
                    placeholder = "Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    iconRes = R.drawable.ic_notes
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF9FAFB),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = {
                        if (clientName.isNotBlank()) {
                            viewModel.addClient(
                                name = clientName,
                                email = email,
                                phone = phone
                            )
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9DEA6E),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp),
                    enabled = clientName.isNotBlank()
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
