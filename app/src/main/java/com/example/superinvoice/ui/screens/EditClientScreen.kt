package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.ClientsViewModel
import online.isdevapps.superinvoice.R

@Composable
fun EditClientScreen(
    clientId: Int,
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: ClientsViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val client = clients.find { it.id == clientId }

    var clientName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(client) {
        client?.let {
            clientName = it.name
            email = it.email
            phone = it.phone
            address = it.address
            city = it.city
            state = it.state
            zipCode = it.zipCode
            notes = it.notes
        }
    }

    InvSettingsSubScreen(
        title = stringResource(R.string.title_edit_client),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        saveEnabled = clientName.isNotBlank(),
        onSave = {
            if (clientName.isNotBlank()) {
                client?.let {
                    viewModel.updateClient(
                        it.copy(
                            name = clientName,
                            email = email,
                            phone = phone,
                            address = address,
                            city = city,
                            state = state,
                            zipCode = zipCode,
                            notes = notes
                        )
                    )
                    onSave()
                }
            }
        }
    ) {
        InvField(
            label = stringResource(R.string.client_name_required),
            value = clientName,
            onValueChange = { clientName = it },
            opensSection = true
        )
        InvField(
            label = stringResource(R.string.email),
            value = email,
            onValueChange = { email = it }
        )
        InvField(
            label = stringResource(R.string.phone),
            value = phone,
            onValueChange = { phone = it }
        )
        InvField(
            label = stringResource(R.string.address),
            value = address,
            onValueChange = { address = it }
        )
        InvField(
            label = stringResource(R.string.city),
            value = city,
            onValueChange = { city = it }
        )
        InvField(
            label = stringResource(R.string.state),
            value = state,
            onValueChange = { state = it }
        )
        InvField(
            label = stringResource(R.string.zip_code),
            value = zipCode,
            onValueChange = { zipCode = it }
        )
        InvField(
            label = stringResource(R.string.notes),
            value = notes,
            onValueChange = { notes = it }
        )
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
