package com.example

import com.example.data.database.BusinessEntity
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.AccountingEngine
import com.example.data.model.AppBackupData
import com.example.data.model.PartyType
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountingEngineTest {

    @Test
    fun testCustomerCreditAndPaymentBalances() {
        val customer = PartyEntity(
            id = 1,
            businessId = 1,
            type = PartyType.CUSTOMER.name,
            name = "John Doe",
            openingBalance = 50.0
        )

        val tx1 = TransactionEntity(
            id = 101,
            businessId = 1,
            partyId = 1,
            partyType = PartyType.CUSTOMER.name,
            transactionType = TransactionType.CREDIT_SALE.name,
            amount = 100.0,
            date = 1000L
        )

        val tx2 = TransactionEntity(
            id = 102,
            businessId = 1,
            partyId = 1,
            partyType = PartyType.CUSTOMER.name,
            transactionType = TransactionType.PAYMENT_RECEIVED.name,
            amount = 30.0,
            date = 2000L
        )

        val partyWithBal = AccountingEngine.computePartyWithBalance(customer, listOf(tx1, tx2))
        // 50 (opening) + 100 (credit) - 30 (paid) = 120.0
        assertEquals(120.0, partyWithBal.netBalance, 0.001)
    }

    @Test
    fun testTransactionReversalNegation() {
        val customer = PartyEntity(
            id = 2,
            businessId = 1,
            type = PartyType.CUSTOMER.name,
            name = "Jane Smith",
            openingBalance = 0.0
        )

        // Original credit sale of 200
        val originalTx = TransactionEntity(
            id = 201,
            businessId = 1,
            partyId = 2,
            partyType = PartyType.CUSTOMER.name,
            transactionType = TransactionType.CREDIT_SALE.name,
            amount = 200.0,
            date = 1000L,
            isReversed = true,
            reversedByTransactionId = 202
        )

        // Reversal transaction created to negate original
        val reversalTx = TransactionEntity(
            id = 202,
            businessId = 1,
            partyId = 2,
            partyType = PartyType.CUSTOMER.name,
            transactionType = TransactionType.REVERSAL.name,
            amount = 200.0,
            date = 2000L,
            isReversal = true,
            reversalOfTransactionId = 201,
            reversalReason = "Incorrect invoice amount"
        )

        val transactions = listOf(originalTx, reversalTx)
        val ledger = AccountingEngine.buildLedger(customer, transactions)

        // Step 1: After original sale of 200, balance is 200
        assertEquals(200.0, ledger[0].runningBalance, 0.001)

        // Step 2: After reversal of original sale, balance returns to 0
        assertEquals(0.0, ledger[1].runningBalance, 0.001)

        val partyWithBal = AccountingEngine.computePartyWithBalance(customer, transactions)
        assertEquals(0.0, partyWithBal.netBalance, 0.001)
    }

    @Test
    fun testBackupRoundtripSerialization() {
        val biz = BusinessEntity(id = 1, name = "My Shop", currencySymbol = "Rs")
        val party = PartyEntity(id = 10, businessId = 1, type = "CUSTOMER", name = "Test Customer")
        val tx = TransactionEntity(
            id = 100,
            businessId = 1,
            partyId = 10,
            partyType = "CUSTOMER",
            transactionType = "CREDIT_SALE",
            amount = 50.0,
            date = 123456L,
            isReversal = true,
            reversalOfTransactionId = 99,
            reversalReason = "Test reversal reason"
        )

        val backup = AppBackupData(
            version = 1,
            exportedAt = 99999L,
            appName = "DuesLedger",
            businesses = listOf(biz),
            parties = listOf(party),
            transactions = listOf(tx)
        )

        val json = backup.toJsonString()
        val parsed = AppBackupData.fromJsonString(json)

        assertEquals(1, parsed.businesses.size)
        assertEquals("My Shop", parsed.businesses[0].name)
        assertEquals(1, parsed.parties.size)
        assertEquals("Test Customer", parsed.parties[0].name)
        assertEquals(1, parsed.transactions.size)
        assertEquals(50.0, parsed.transactions[0].amount, 0.001)
        assertTrue(parsed.transactions[0].isReversal)
        assertEquals(99L, parsed.transactions[0].reversalOfTransactionId)
        assertEquals("Test reversal reason", parsed.transactions[0].reversalReason)
    }
}
