package com.example.superinvoice.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdDate DESC")
    fun getAll(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: Int): Invoice?

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY createdDate DESC")
    fun getByStatus(status: InvoiceStatus): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY createdDate DESC")
    fun getByClientId(clientId: Int): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE clientId = :clientId")
    suspend fun getCountByClientId(clientId: Int): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE type = 'INVOICE' AND status = 'PAID'")
    suspend fun getPaidCount(): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE type = 'INVOICE' AND status != 'PAID'")
    suspend fun getUnpaidCount(): Int

    /** Só os campos que os pagamentos controlam; o resto da fatura fica como está. */
    @Query(
        "UPDATE invoices SET amountPaid = :amountPaid, status = :status, paymentDate = :paymentDate " +
            "WHERE id = :id"
    )
    suspend fun updatePaymentState(id: Int, amountPaid: Double, status: InvoiceStatus, paymentDate: Long?)

    /** Faturas e orçamentos têm sequências separadas (INV-001, EST-001). */
    @Query("SELECT number FROM invoices WHERE type = 'INVOICE' ORDER BY id DESC LIMIT 1")
    suspend fun getLastInvoiceNumber(): String?

    /** Faturas e orçamentos têm sequências separadas (INV-, EST-). */
    @Query("SELECT number FROM invoices WHERE type = :type ORDER BY id DESC LIMIT 1")
    suspend fun getLastNumber(type: DocumentType): String?

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: InvoiceStatus)

    /** A fatura que nasceu de um orçamento, se ainda existir. */
    @Query("SELECT * FROM invoices WHERE convertedFromId = :estimateId ORDER BY id DESC LIMIT 1")
    suspend fun getConvertedFrom(estimateId: Int): Invoice?
}
