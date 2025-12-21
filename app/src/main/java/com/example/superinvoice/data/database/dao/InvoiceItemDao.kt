package com.example.superinvoice.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.superinvoice.data.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getByInvoiceId(invoiceId: Int): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getByInvoiceIdSync(invoiceId: Int): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE id = :id")
    suspend fun getById(id: Int): InvoiceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoiceItem: InvoiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvoiceItem>)

    @Update
    suspend fun update(invoiceItem: InvoiceItem)

    @Delete
    suspend fun delete(invoiceItem: InvoiceItem)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: Int)
}
