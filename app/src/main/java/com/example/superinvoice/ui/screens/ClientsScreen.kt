package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.ui.components.ClientCard
import com.example.superinvoice.ui.components.ClientSearchBar
import com.example.superinvoice.ui.components.InvConfirmDialog
import com.example.superinvoice.ui.components.InvEmptyState
import com.example.superinvoice.ui.components.InvFab
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvUnderlineTab
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
    val clientWithDocuments by viewModel.clientWithDocuments.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filteredClients = clients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
    }

    val isSelectionMode = onClientSelected != null

    // Escolher cliente para a fatura nunca mostra arquivados; o ViewModel é da Activity e pode
    // ter ficado no filtro "Arquivados" da última visita.
    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode) viewModel.setShowArchived(false)
    }

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

            if (!isSelectionMode) {
                Row(
                    modifier = Modifier.padding(top = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.xl)
                ) {
                    InvUnderlineTab(
                        text = stringResource(R.string.clients_active),
                        isSelected = !showArchived,
                        onClick = { viewModel.setShowArchived(false) }
                    )
                    InvUnderlineTab(
                        text = stringResource(R.string.clients_archived),
                        isSelected = showArchived,
                        onClick = { viewModel.setShowArchived(true) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space.sm))

            if (filteredClients.isEmpty()) {
                InvEmptyState(
                    title = when {
                        searchQuery.isNotEmpty() -> stringResource(R.string.no_clients_found)
                        showArchived -> stringResource(R.string.no_archived_clients)
                        else -> stringResource(R.string.no_clients_yet)
                    },
                    message = when {
                        searchQuery.isNotEmpty() -> stringResource(R.string.no_clients_search_message)
                        showArchived -> stringResource(R.string.no_archived_clients_message)
                        else -> stringResource(R.string.no_clients_message)
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
                                { viewModel.requestDeleteClient(client) }
                            } else {
                                null
                            },
                            onArchive = if (!isSelectionMode && !client.archived) {
                                { viewModel.setArchived(client, true) }
                            } else {
                                null
                            },
                            onUnarchive = if (!isSelectionMode && client.archived) {
                                { viewModel.setArchived(client, false) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    clientWithDocuments?.let { pending ->
        if (pending.client.archived) {
            InvConfirmDialog(
                title = stringResource(R.string.cannot_delete_client_title),
                message = context.resources.getQuantityString(
                    R.plurals.cannot_delete_client_message,
                    pending.documentCount,
                    pending.client.name,
                    pending.documentCount
                ),
                confirmText = stringResource(R.string.ok),
                onConfirm = viewModel::dismissClientWithDocuments,
                onDismiss = viewModel::dismissClientWithDocuments
            )
        } else {
            InvConfirmDialog(
                title = stringResource(R.string.archive_client_title),
                message = context.resources.getQuantityString(
                    R.plurals.archive_client_message,
                    pending.documentCount,
                    pending.client.name,
                    pending.documentCount
                ),
                confirmText = stringResource(R.string.archive),
                dismissText = stringResource(R.string.cancel),
                onConfirm = viewModel::confirmArchiveClient,
                onDismiss = viewModel::dismissClientWithDocuments
            )
        }
    }
}
