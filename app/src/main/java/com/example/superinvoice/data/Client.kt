package com.example.superinvoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
