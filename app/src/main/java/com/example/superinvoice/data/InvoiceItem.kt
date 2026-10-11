package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Nome, descrição e preço são cópia do produto no momento da fatura: a fatura não depende de o
 * produto continuar existindo nem de o preço dele mudar depois. Por isso apagar o produto só
 * desliga o vínculo (`SET NULL`); até a v4 era `RESTRICT` e a exclusão derrubava o app.
 */
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
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("invoiceId"), Index("productServiceId")]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val invoiceId: Int,
    val productServiceId: Int?,
    val productServiceName: String,
    val productServiceDescription: String = "",
    val pricePerUnit: Double,
    /** Até 3 casas decimais (horas, metros). Era `Int` até a v4. */
    val quantity: Double,
    /** Texto livre curto: "h", "m²", "un". */
    val unit: String = "",
    val lineTotal: Double
)
