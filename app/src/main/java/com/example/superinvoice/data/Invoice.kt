package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus

/**
 * Fatura ou orçamento ([type]).
 *
 * O cliente é `RESTRICT`: até a v4 era `CASCADE`, e apagar um cliente levava as faturas dele
 * em silêncio. Quem quiser remover o cliente apaga (ou, a partir da Onda 1, arquiva) antes.
 */
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("clientId")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: DocumentType = DocumentType.INVOICE,
    val number: String,
    val clientId: Int,
    /**
     * Meia-noite UTC do dia escolhido (ver [com.example.superinvoice.util.InvoiceDates]).
     * Em orçamento é o "válido até". Até a v4 era texto já formatado no padrão do usuário.
     */
    val dueDate: Long,
    val createdDate: Long = System.currentTimeMillis(),
    /** Data do pagamento que quitou a fatura. */
    val paymentDate: Long? = null,
    val notes: String = "",
    /** Soma dos `lineTotal` dos itens. */
    val subtotal: Double = 0.0,
    val discountType: AdjustmentType = AdjustmentType.FIXED,
    /** O que o usuário digitou: 10 pode ser 10% ou 10 na moeda, conforme [discountType]. */
    val discountValue: Double = 0.0,
    /** Desconto calculado em moeda pelo `InvoiceCalculator`. */
    val discountAmount: Double = 0.0,
    val taxType: AdjustmentType = AdjustmentType.FIXED,
    val taxValue: Double = 0.0,
    val taxAmount: Double = 0.0,
    val shipping: Double = 0.0,
    /** `subtotal − discountAmount + taxAmount + shipping`. */
    val totalAmount: Double = 0.0,
    /**
     * Soma de `payments.amount`, desnormalizada de propósito: a Home mostra saldo em todas as
     * linhas, e somar pagamentos por fatura em cada lista custa mais do que manter o campo
     * atualizado na mesma transação que grava o pagamento.
     */
    val amountPaid: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val currency: String = "USD",
    /** Na fatura criada a partir de um orçamento: o id do orçamento. */
    val convertedFromId: Int? = null
)
