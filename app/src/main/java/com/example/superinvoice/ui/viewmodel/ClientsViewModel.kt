package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    val clients: StateFlow<List<Client>> = clientRepository.getAllClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addClient(name: String, email: String, phone: String) {
        viewModelScope.launch {
            val client = Client(
                name = name,
                email = email,
                phone = phone
            )
            clientRepository.insertClient(client)
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
