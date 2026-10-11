package com.example.superinvoice.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.superinvoice.data.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paidAt ASC, id ASC")
    fun getByInvoiceId(invoiceId: Int): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paidAt ASC, id ASC")
    suspend fun getByInvoiceIdSync(invoiceId: Int): List<Payment>

    @Insert
    suspend fun insert(payment: Payment): Long

    @Delete
    suspend fun delete(payment: Payment)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE invoiceId = :invoiceId")
    suspend fun getTotalPaid(invoiceId: Int): Double

    @Query("SELECT MAX(paidAt) FROM payments WHERE invoiceId = :invoiceId")
    suspend fun getLastPaidAt(invoiceId: Int): Long?

    @Query("SELECT receiptNumber FROM payments ORDER BY id DESC LIMIT 1")
    suspend fun getLastReceiptNumber(): String?
}
