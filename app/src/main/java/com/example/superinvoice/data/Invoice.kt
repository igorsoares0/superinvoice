package com.example.superinvoice.data

data class Invoice(
    val id: Int,
    val number: String,
    val amount: Double,
    val date: String,
    val isPaid: Boolean
)
