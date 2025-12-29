package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.ui.components.ClientCard
import com.example.superinvoice.ui.components.ClientSearchBar
import com.example.superinvoice.ui.components.EmptyState
import com.example.superinvoice.ui.viewmodel.ClientsViewModel

@Composable
fun ClientsScreen(
    onClose: () -> Unit,
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
                    text = "Clients",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(24.dp))
            }

            ClientSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (filteredClients.isEmpty()) {
                EmptyState(
                    title = if (searchQuery.isEmpty()) "No clients yet" else "No clients found",
                    message = if (searchQuery.isEmpty())
                        "Tap the + button to add your first client and start creating invoices."
                    else
                        "No clients match your search. Try a different keyword."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredClients) { client ->
                        ClientCard(
                            client = client,
                            onClick = if (isSelectionMode) {
                                { onClientSelected?.invoke(client) }
                            } else null,
                            onEdit = if (!isSelectionMode) {
                                { onNavigateToEditClient?.invoke(client.id) }
                            } else null,
                            onDelete = if (!isSelectionMode) {
                                { viewModel.deleteClient(client) }
                            } else null
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToAddClient,
            containerColor = Color(0xFF9DEA6E),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Client"
            )
        }
    }
}
