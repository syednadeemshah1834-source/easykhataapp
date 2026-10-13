package com.example.data.model

import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity

data class PartyWithBalance(
    val party: PartyEntity,
    val netBalance: Double, // For customer: >0 means customer owes us. For supplier: >0 means we owe supplier.
    val totalCredit: Double, // Total credit given or taken
    val totalPaid: Double,   // Total received or paid
    val totalReturns: Double,
    val lastTransactionDate: Long? = null,
    val transactionCount: Int = 0
)

data class LedgerItem(
    val transaction: TransactionEntity,
    val debitAmount: Double, // Increases what is owed (e.g., Credit Sale / Credit Purchase)
    val creditAmount: Double, // Decreases what is owed (e.g., Payment Received / Payment Made / Return)
    val runningBalance: Double,
    val reversalOfOriginalType: String? = null
)
