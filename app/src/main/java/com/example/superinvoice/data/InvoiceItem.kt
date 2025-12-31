package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductService::class,
            parentColumns = ["id"],
            childColumns = ["productServiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("invoiceId"), Index("productServiceId")]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val invoiceId: Int,
    val productServiceId: Int,
    val productServiceName: String,
    val productServiceDescription: String = "",
    val pricePerUnit: Double,
    val quantity: Int,
    val lineTotal: Double
)
