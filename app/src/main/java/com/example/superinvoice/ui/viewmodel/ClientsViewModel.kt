package com.example.superinvoice.ui.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    /**
     * Cliente com faturas ou orçamentos que o usuário tentou excluir. A FK `invoices.clientId`
     * é `RESTRICT`: cliente com documentos não é apagado, é arquivado (decisão D2 da spec). Se
     * já está arquivado, o diálogo só explica por que não dá para excluir.
     */
    data class ClientWithDocuments(val client: Client, val documentCount: Int)

    private val _clientWithDocuments = MutableStateFlow<ClientWithDocuments?>(null)
    val clientWithDocuments: StateFlow<ClientWithDocuments?> = _clientWithDocuments.asStateFlow()

    /** Filtro "Arquivados" da tela de clientes; a seleção para fatura sempre usa os ativos. */
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val clients: StateFlow<List<Client>> = _showArchived
        .flatMapLatest { archived ->
            if (archived) clientRepository.getArchivedClients() else clientRepository.getActiveClients()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setShowArchived(archived: Boolean) {
        _showArchived.value = archived
    }

    fun addClient(
        name: String,
        email: String,
        phone: String,
        address: String = "",
        city: String = "",
        state: String = "",
        zipCode: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val client = Client(
                name = name,
                email = email,
                phone = phone,
                address = address,
                city = city,
                state = state,
                zipCode = zipCode,
                notes = notes
            )
            clientRepository.insertClient(client)
            // Evento sem parâmetro nenhum: o que interessa é a contagem, e qualquer
            // atributo do cliente aqui seria dado de terceiro.
            analyticsManager.logClientCreated()
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            clientRepository.updateClient(client)
        }
    }

    /** Exclui na hora se o cliente não tem documentos; se tem, oferece arquivar. */
    fun requestDeleteClient(client: Client) {
        viewModelScope.launch {
            val documentCount = invoiceRepository.getInvoiceCountForClient(client.id)
            if (documentCount > 0) {
                _clientWithDocuments.value = ClientWithDocuments(client, documentCount)
                return@launch
            }
            try {
                clientRepository.deleteClient(client)
            } catch (e: SQLiteConstraintException) {
                // Corrida rara: um documento usou o cliente entre a contagem e o delete.
                _clientWithDocuments.value = ClientWithDocuments(
                    client,
                    invoiceRepository.getInvoiceCountForClient(client.id).coerceAtLeast(1)
                )
            }
        }
    }

    fun setArchived(client: Client, archived: Boolean) {
        viewModelScope.launch {
            clientRepository.setArchived(client, archived)
        }
    }

    fun confirmArchiveClient() {
        val pending = _clientWithDocuments.value ?: return
        _clientWithDocuments.value = null
        setArchived(pending.client, true)
    }

    fun dismissClientWithDocuments() {
        _clientWithDocuments.value = null
    }
}
