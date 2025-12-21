package com.example.superinvoice.data.repository

import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.entities.InvoiceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao
) {
    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao.getAll()

    suspend fun getInvoiceById(id: Int): Invoice? = invoiceDao.getById(id)

    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>> =
        invoiceDao.getByStatus(status)

    fun getInvoicesByClientId(clientId: Int): Flow<List<Invoice>> =
        invoiceDao.getByClientId(clientId)

    fun getInvoiceItems(invoiceId: Int): Flow<List<InvoiceItem>> =
        invoiceItemDao.getByInvoiceId(invoiceId)

    suspend fun getInvoiceItemsSync(invoiceId: Int): List<InvoiceItem> =
        invoiceItemDao.getByInvoiceIdSync(invoiceId)

    suspend fun insertInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = invoiceDao.insert(invoice)
        val itemsWithInvoiceId = items.map { it.copy(invoiceId = invoiceId.toInt()) }
        invoiceItemDao.insertAll(itemsWithInvoiceId)
        return invoiceId
    }

    suspend fun updateInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        invoiceDao.update(invoice)
        invoiceItemDao.deleteByInvoiceId(invoice.id)
        invoiceItemDao.insertAll(items)
    }

    suspend fun insertInvoice(invoice: Invoice): Long = invoiceDao.insert(invoice)

    suspend fun updateInvoice(invoice: Invoice) = invoiceDao.update(invoice)

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.delete(invoice)
    }

    suspend fun getInvoiceCount(): Int = invoiceDao.getCount()

    suspend fun getPaidInvoiceCount(): Int = invoiceDao.getPaidCount()

    suspend fun getUnpaidInvoiceCount(): Int = invoiceDao.getUnpaidCount()

    suspend fun generateNextInvoiceNumber(): String {
        val lastNumber = invoiceDao.getLastInvoiceNumber()
        return if (lastNumber == null) {
            "INV-001"
        } else {
            // Extract number from "INV-XXX" format
            val numberPart = lastNumber.substringAfterLast("-").toIntOrNull() ?: 0
            val nextNumber = numberPart + 1
            "INV-${nextNumber.toString().padStart(3, '0')}"
        }
    }
}
