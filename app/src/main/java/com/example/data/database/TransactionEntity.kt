package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("businessId"),
        Index("partyId"),
        Index("date"),
        Index("reversalOfTransactionId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val partyId: Long,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val transactionType: String, // TransactionType name, e.g. CREDIT_SALE, PAYMENT_RECEIVED, REVERSAL
    val amount: Double,
    val date: Long,
    val referenceNo: String = "",
    val description: String = "",
    val notes: String = "",

    // Reversal & Audit Trail
    val isReversed: Boolean = false,
    val reversedByTransactionId: Long? = null,
    val isReversal: Boolean = false,
    val reversalOfTransactionId: Long? = null,
    val reversalReason: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)
