package com.example.superinvoice.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_services")
data class ProductService(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val pricePerUnit: Double,
    /** Unidade padrão ("h", "m²"), copiada para o item da fatura. */
    @ColumnInfo(defaultValue = "")
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
