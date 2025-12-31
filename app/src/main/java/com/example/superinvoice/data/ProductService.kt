package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_services")
data class ProductService(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val pricePerUnit: Double,
    val createdAt: Long = System.currentTimeMillis()
)
