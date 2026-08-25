package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

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

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            clientRepository.deleteClient(client)
        }
    }
}
