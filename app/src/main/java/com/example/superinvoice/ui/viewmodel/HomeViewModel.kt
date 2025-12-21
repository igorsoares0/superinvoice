package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.ui.components.InvoiceFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(InvoiceFilter.PAID)
    val selectedFilter: StateFlow<InvoiceFilter> = _selectedFilter.asStateFlow()

    private val allInvoices = invoiceRepository.getAllInvoices()

    val filteredInvoices: StateFlow<List<Invoice>> = combine(
        allInvoices,
        selectedFilter
    ) { invoices, filter ->
        when (filter) {
            InvoiceFilter.PAID -> invoices.filter { it.status == InvoiceStatus.PAID }
            InvoiceFilter.UNPAID -> invoices.filter { it.status != InvoiceStatus.PAID }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: InvoiceFilter) {
        _selectedFilter.value = filter
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoice)
        }
    }
}
