package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.superinvoice.data.database.entities.PaymentMethod

/**
 * Pagamento parcial ou total de uma fatura. Cada gravação ou exclusão atualiza
 * `Invoice.amountPaid` e o status na mesma transação.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val invoiceId: Int,
    val amount: Double,
    val paidAt: Long,
    val method: PaymentMethod,
    val note: String = "",
    /** Sequência própria: REC-001, REC-002… */
    val receiptNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
