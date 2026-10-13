package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val businessType: String = "Retail Shop",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val currencySymbol: String = "Rs",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
