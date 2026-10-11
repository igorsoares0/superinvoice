package com.example.superinvoice.data.database

import androidx.room.TypeConverter
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod

/**
 * Enums são gravados pelo nome. A leitura tolera valor desconhecido (banco restaurado de um
 * backup de versão mais nova, por exemplo) caindo num padrão em vez de derrubar a consulta.
 */
class Converters {
    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus): String {
        return status.name
    }

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus {
        return try {
            InvoiceStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            InvoiceStatus.DRAFT
        }
    }

    @TypeConverter
    fun fromDocumentType(type: DocumentType): String = type.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType =
        enumValueOrNull<DocumentType>(value) ?: DocumentType.INVOICE

    @TypeConverter
    fun fromAdjustmentType(type: AdjustmentType): String = type.name

    @TypeConverter
    fun toAdjustmentType(value: String): AdjustmentType =
        enumValueOrNull<AdjustmentType>(value) ?: AdjustmentType.FIXED

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        enumValueOrNull<PaymentMethod>(value) ?: PaymentMethod.OTHER

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }
}
