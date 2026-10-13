package com.example.data.model

import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity

object AccountingEngine {

    /**
     * Determines whether a given transaction adds to or reduces the party's dues.
     * Returns a Pair(debit, credit) where:
     * - debit > 0 means party dues increase (we are owed more or we owe supplier more)
     * - credit > 0 means party dues decrease (payment received/made, return, or settlement)
     */
    fun calculateDebitCredit(
        transaction: TransactionEntity,
        allPartyTransactions: Map<Long, TransactionEntity> = emptyMap()
    ): Pair<Double, Double> {
        val type = TransactionType.fromString(transaction.transactionType)
        val amount = transaction.amount

        if (type == TransactionType.REVERSAL) {
            val original = transaction.reversalOfTransactionId?.let { allPartyTransactions[it] }
            if (original != null) {
                val (origDebit, origCredit) = calculateDebitCredit(original, emptyMap())
                // Reversal flips the effect of the original transaction
                return Pair(origCredit, origDebit)
            }
            // Fallback if original is not in the map
            return Pair(0.0, amount)
        }

        return when (type) {
            // Customer dues increasing
            TransactionType.CREDIT_SALE,
            TransactionType.SERVICE_ON_CREDIT,
            TransactionType.CUSTOMER_ADJUST_ADD -> Pair(amount, 0.0)

            // Customer dues decreasing
            TransactionType.PAYMENT_RECEIVED,
            TransactionType.CUSTOMER_RETURN,
            TransactionType.CUSTOMER_ADJUST_SUB -> Pair(0.0, amount)

            // Supplier dues increasing (we owe supplier more)
            TransactionType.CREDIT_PURCHASE,
            TransactionType.EXPENSE_ON_CREDIT,
            TransactionType.SUPPLIER_ADJUST_ADD -> Pair(amount, 0.0)

            // Supplier dues decreasing (we paid supplier or returned goods)
            TransactionType.PAYMENT_MADE,
            TransactionType.SUPPLIER_RETURN,
            TransactionType.SUPPLIER_ADJUST_SUB -> Pair(0.0, amount)

            TransactionType.REVERSAL -> Pair(0.0, amount)
        }
    }

    /**
     * Builds a chronological ledger with running balance for a party.
     */
    fun buildLedger(
        party: PartyEntity,
        transactions: List<TransactionEntity>
    ): List<LedgerItem> {
        val txMap = transactions.associateBy { it.id }
        val sortedTransactions = transactions.sortedWith(
            compareBy<TransactionEntity> { it.date }.thenBy { it.id }
        )

        var currentBalance = party.openingBalance
        val ledgerItems = ArrayList<LedgerItem>(sortedTransactions.size)

        for (tx in sortedTransactions) {
            val (debit, credit) = calculateDebitCredit(tx, txMap)
            currentBalance += (debit - credit)

            val originalType = tx.reversalOfTransactionId?.let { txMap[it]?.transactionType }

            ledgerItems.add(
                LedgerItem(
                    transaction = tx,
                    debitAmount = debit,
                    creditAmount = credit,
                    runningBalance = currentBalance,
                    reversalOfOriginalType = originalType
                )
            )
        }

        return ledgerItems
    }

    /**
     * Calculates the party's current balance and statistics from all transactions.
     */
    fun computePartyWithBalance(
        party: PartyEntity,
        transactions: List<TransactionEntity>
    ): PartyWithBalance {
        val ledger = buildLedger(party, transactions)
        val finalBalance = if (ledger.isNotEmpty()) ledger.last().runningBalance else party.openingBalance

        var totalCredit = if (party.openingBalance > 0) party.openingBalance else 0.0
        var totalPaid = if (party.openingBalance < 0) -party.openingBalance else 0.0
        var totalReturns = 0.0

        for (item in ledger) {
            totalCredit += item.debitAmount
            totalPaid += item.creditAmount

            val type = TransactionType.fromString(item.transaction.transactionType)
            if (type == TransactionType.CUSTOMER_RETURN || type == TransactionType.SUPPLIER_RETURN) {
                totalReturns += item.transaction.amount
            }
        }

        val lastTxDate = transactions.maxOfOrNull { it.date }

        return PartyWithBalance(
            party = party,
            netBalance = finalBalance,
            totalCredit = totalCredit,
            totalPaid = totalPaid,
            totalReturns = totalReturns,
            lastTransactionDate = lastTxDate,
            transactionCount = transactions.size
        )
    }
}
