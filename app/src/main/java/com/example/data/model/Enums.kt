package com.example.data.model

enum class PartyType {
    CUSTOMER,
    SUPPLIER
}

enum class TransactionType(
    val title: String,
    val appliesTo: PartyType,
    val isCreditGivingOrBuying: Boolean // true if dues increase, false if payment/return
) {
    // Customer Transactions
    CREDIT_SALE("Credit Sale", PartyType.CUSTOMER, true),
    SERVICE_ON_CREDIT("Service on Credit", PartyType.CUSTOMER, true),
    PAYMENT_RECEIVED("Payment Received", PartyType.CUSTOMER, false),
    CUSTOMER_RETURN("Customer Return / Refund", PartyType.CUSTOMER, false),
    CUSTOMER_ADJUST_ADD("Balance Increase (+)", PartyType.CUSTOMER, true),
    CUSTOMER_ADJUST_SUB("Balance Decrease (-)", PartyType.CUSTOMER, false),

    // Supplier Transactions
    CREDIT_PURCHASE("Credit Purchase", PartyType.SUPPLIER, true),
    EXPENSE_ON_CREDIT("Expense on Credit", PartyType.SUPPLIER, true),
    PAYMENT_MADE("Payment Made", PartyType.SUPPLIER, false),
    SUPPLIER_RETURN("Purchase Return", PartyType.SUPPLIER, false),
    SUPPLIER_ADJUST_ADD("Payable Increase (+)", PartyType.SUPPLIER, true),
    SUPPLIER_ADJUST_SUB("Payable Decrease (-)", PartyType.SUPPLIER, false),

    // Universal Reversal
    REVERSAL("Correction Reversal", PartyType.CUSTOMER, false);

    companion object {
        fun fromString(name: String?): TransactionType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: CREDIT_SALE
        }
    }
}
