package com.example.superinvoice.data.database

import androidx.room.TypeConverter
import com.example.superinvoice.data.database.entities.InvoiceStatus

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
}
