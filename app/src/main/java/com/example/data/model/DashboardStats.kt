package com.example.data.model

import com.example.data.database.TransactionEntity

data class DashboardStats(
    val totalCustomerReceivables: Double = 0.0, // Money owed to business by customers
    val totalSupplierPayables: Double = 0.0,    // Money business owes to suppliers
    val netBalance: Double = 0.0,               // Receivables - Payables
    val todayReceived: Double = 0.0,            // Cash received today
    val todayPaid: Double = 0.0,                // Cash paid out today
    val todayCreditSales: Double = 0.0,         // Credit sales today
    val todayCreditPurchases: Double = 0.0,     // Credit purchases today
    val totalActiveCustomers: Int = 0,
    val totalActiveSuppliers: Int = 0,
    val totalOverdueCount: Int = 0,             // Customers with non-zero dues
    val recentTransactions: List<TransactionWithParty> = emptyList()
)

data class TransactionWithParty(
    val transaction: TransactionEntity,
    val partyName: String,
    val partyPhone: String
)

enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time"),
    CUSTOM("Custom")
}

data class DateRangeFilter(
    val preset: DateRangePreset = DateRangePreset.THIS_MONTH,
    val startMillis: Long,
    val endMillis: Long
)
