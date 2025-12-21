package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.superinvoice.data.database.entities.InvoiceStatus

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val number: String,
    val clientId: Int,
    val dueDate: String,
    val createdDate: Long = System.currentTimeMillis(),
    val paymentDate: Long? = null,
    val notes: String = "",
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val subtotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val currency: String = "USD",
    @Deprecated("Use status instead")
    val isPaid: Boolean = false
)
