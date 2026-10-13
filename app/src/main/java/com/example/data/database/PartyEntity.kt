package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [
        Index("businessId"),
        Index(value = ["businessId", "type"])
    ]
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val type: String, // "CUSTOMER" or "SUPPLIER"
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val openingBalance: Double = 0.0, // > 0: customer owes us / we owe supplier; < 0: advance
    val openingBalanceDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
