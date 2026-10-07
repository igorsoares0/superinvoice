package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Cliente que tem faturas e está esperando confirmação para ser excluído.
     *
     * A FK `invoices.clientId` é `ON DELETE CASCADE`: excluir o cliente leva junto todas as
     * faturas dele. Antes isso acontecia direto pelo menu, sem aviso nenhum. Até a Onda 1
     * trocar o CASCADE por "arquivar" (docs/spec-onda-1.md), o mínimo é dizer quantas
     * faturas vão junto e pedir confirmação.
     */
    data class PendingClientDeletion(val client: Client, val invoiceCount: Int)

    private val _pendingDeletion = MutableStateFlow<PendingClientDeletion?>(null)
    val pendingDeletion: StateFlow<PendingClientDeletion?> = _pendingDeletion.asStateFlow()

    val clients: StateFlow<List<Client>> = clientRepository.getAllClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    /** Exclui na hora se o cliente não tem faturas; se tem, pede confirmação. */
    fun requestDeleteClient(client: Client) {
        viewModelScope.launch {
            val invoiceCount = invoiceRepository.getInvoiceCountForClient(client.id)
            if (invoiceCount == 0) {
                clientRepository.deleteClient(client)
            } else {
                _pendingDeletion.value = PendingClientDeletion(client, invoiceCount)
            }
        }
    }

    fun confirmDeleteClient() {
        val pending = _pendingDeletion.value ?: return
        _pendingDeletion.value = null
        viewModelScope.launch {
            clientRepository.deleteClient(pending.client)
        }
    }

    fun cancelDeleteClient() {
        _pendingDeletion.value = null
    }
}
