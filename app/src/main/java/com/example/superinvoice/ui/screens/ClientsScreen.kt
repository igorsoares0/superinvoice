package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.ui.components.ClientCard
import com.example.superinvoice.ui.components.ClientSearchBar
import com.example.superinvoice.ui.components.InvEmptyState
import com.example.superinvoice.ui.components.InvFab
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.ClientsViewModel
import online.isdevapps.superinvoice.R

@Composable
fun ClientsScreen(
    onClose: () -> Unit = {},
    onNavigateToAddClient: () -> Unit = {},
    onNavigateToEditClient: ((Int) -> Unit)? = null,
    onClientSelected: ((Client) -> Unit)? = null,
    viewModel: ClientsViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    val filteredClients = clients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
    }

    val isSelectionMode = onClientSelected != null

    InvScaffold(
        floatingActionButton = {
            InvFab(
                text = stringResource(R.string.add_client),
                icon = InvIcons.Plus,
                onClick = onNavigateToAddClient
            )
        }
    ) {
        InvScreenHeader(
            title = stringResource(R.string.title_clients),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )

        Column(modifier = Modifier.padding(horizontal = Space.screen)) {
            ClientSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(Space.sm))

            if (filteredClients.isEmpty()) {
                InvEmptyState(
                    title = if (searchQuery.isEmpty()) {
                        stringResource(R.string.no_clients_yet)
                    } else {
                        stringResource(R.string.no_clients_found)
                    },
                    message = if (searchQuery.isEmpty()) {
                        stringResource(R.string.no_clients_message)
                    } else {
                        stringResource(R.string.no_clients_search_message)
                    },
                    icon = InvIcons.Person
                )
            } else {
                InvSectionRule()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = Space.xxl)
                ) {
                    items(filteredClients) { client ->
                        ClientCard(
                            client = client,
                            onClick = if (isSelectionMode) {
                                { onClientSelected?.invoke(client) }
                            } else {
                                null
                            },
                            onEdit = if (!isSelectionMode) {
                                { onNavigateToEditClient?.invoke(client.id) }
                            } else {
                                null
                            },
                            onDelete = if (!isSelectionMode) {
                                { viewModel.deleteClient(client) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
