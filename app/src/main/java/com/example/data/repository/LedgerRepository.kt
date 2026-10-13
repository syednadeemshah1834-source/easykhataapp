package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.BusinessEntity
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.AccountingEngine
import com.example.data.model.AppBackupData
import com.example.data.model.DashboardStats
import com.example.data.model.LedgerItem
import com.example.data.model.PartyType
import com.example.data.model.PartyWithBalance
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithParty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar

class LedgerRepository(private val database: AppDatabase) {

    private val businessDao = database.businessDao()
    private val partyDao = database.partyDao()
    private val transactionDao = database.transactionDao()

    val allBusinesses: Flow<List<BusinessEntity>> = businessDao.getAllBusinesses()

    suspend fun ensureDefaultBusinessExists(): Long {
        val count = businessDao.getBusinessCount()
        if (count == 0) {
            val defaultBiz = BusinessEntity(
                name = "General Store",
                businessType = "Retail & Wholesale",
                ownerName = "Shop Owner",
                phone = "",
                address = "Main Market",
                currencySymbol = "Rs",
                isDefault = true
            )
            return businessDao.insert(defaultBiz)
        }
        val default = businessDao.getDefaultBusiness()
        if (default != null) {
            if (default.currencySymbol == "$") {
                businessDao.update(default.copy(currencySymbol = "Rs"))
            }
            return default.id
        }
        val firstBiz = businessDao.getAllBusinesses().first().first()
        if (firstBiz.currencySymbol == "$") {
            businessDao.update(firstBiz.copy(currencySymbol = "Rs"))
        }
        return firstBiz.id
    }

    fun getBusiness(id: Long): Flow<BusinessEntity?> = businessDao.getBusinessById(id)

    suspend fun createBusiness(
        name: String,
        businessType: String,
        ownerName: String,
        phone: String,
        address: String,
        currencySymbol: String
    ): Long {
        return businessDao.insert(
            BusinessEntity(
                name = name,
                businessType = businessType,
                ownerName = ownerName,
                phone = phone,
                address = address,
                currencySymbol = currencySymbol.ifBlank { "Rs" },
                isDefault = false
            )
        )
    }

    suspend fun updateBusiness(business: BusinessEntity) = businessDao.update(business)

    suspend fun deleteBusiness(id: Long) {
        transactionDao.deleteByBusinessId(id)
        partyDao.deleteByBusinessId(id)
        businessDao.deleteById(id)
    }

    // Party Operations
    fun getPartiesWithBalances(businessId: Long, type: PartyType): Flow<List<PartyWithBalance>> {
        return combine(
            partyDao.getPartiesByType(businessId, type.name),
            transactionDao.getTransactionsForBusiness(businessId)
        ) { parties, transactions ->
            val txByParty = transactions.groupBy { it.partyId }
            parties.map { party ->
                val pTx = txByParty[party.id] ?: emptyList()
                AccountingEngine.computePartyWithBalance(party, pTx)
            }
        }
    }

    fun getAllPartiesWithBalances(businessId: Long): Flow<List<PartyWithBalance>> {
        return combine(
            partyDao.getAllParties(businessId),
            transactionDao.getTransactionsForBusiness(businessId)
        ) { parties, transactions ->
            val txByParty = transactions.groupBy { it.partyId }
            parties.map { party ->
                val pTx = txByParty[party.id] ?: emptyList()
                AccountingEngine.computePartyWithBalance(party, pTx)
            }
        }
    }

    fun getParty(id: Long): Flow<PartyEntity?> = partyDao.getPartyById(id)

    fun getPartyWithBalance(partyId: Long): Flow<PartyWithBalance?> {
        return combine(
            partyDao.getPartyById(partyId),
            transactionDao.getTransactionsForParty(partyId)
        ) { party, transactions ->
            if (party == null) null
            else AccountingEngine.computePartyWithBalance(party, transactions)
        }
    }

    suspend fun createParty(party: PartyEntity): Long = partyDao.insert(party)

    suspend fun updateParty(party: PartyEntity) = partyDao.update(party)

    suspend fun deleteParty(partyId: Long) {
        transactionDao.deleteByPartyId(partyId)
        partyDao.deleteById(partyId)
    }

    // Ledger & Transactions
    fun getLedgerForParty(partyId: Long): Flow<List<LedgerItem>> {
        return combine(
            partyDao.getPartyById(partyId),
            transactionDao.getTransactionsForParty(partyId)
        ) { party, transactions ->
            if (party == null) emptyList()
            else AccountingEngine.buildLedger(party, transactions)
        }
    }

    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }

    /**
     * Reverses an existing transaction:
     * - Flags the original transaction as reversed with reversedByTransactionId
     * - Creates a new REVERSAL transaction pointing back with reason
     * - Financial calculations update reactively
     */
    suspend fun reverseTransaction(originalId: Long, reason: String, reversalDate: Long = System.currentTimeMillis()): Long {
        return transactionDao.reverseTransaction(originalId, reason, reversalDate)
    }

    fun getTransactionsForBusiness(businessId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForBusiness(businessId)
    }

    // Dashboard Statistics Calculation
    fun getDashboardStats(businessId: Long): Flow<DashboardStats> {
        return combine(
            partyDao.getAllParties(businessId),
            transactionDao.getTransactionsForBusiness(businessId)
        ) { parties, transactions ->
            val partyMap = parties.associateBy { it.id }
            val txByParty = transactions.groupBy { it.partyId }

            var totalCustomerReceivables = 0.0
            var totalSupplierPayables = 0.0
            var activeCustCount = 0
            var activeSuppCount = 0
            var overdueCount = 0

            for (party in parties) {
                val pTx = txByParty[party.id] ?: emptyList()
                val pBalance = AccountingEngine.computePartyWithBalance(party, pTx)
                if (party.type == PartyType.CUSTOMER.name) {
                    activeCustCount++
                    if (pBalance.netBalance > 0.0) {
                        totalCustomerReceivables += pBalance.netBalance
                        overdueCount++
                    }
                } else {
                    activeSuppCount++
                    if (pBalance.netBalance > 0.0) {
                        totalSupplierPayables += pBalance.netBalance
                    }
                }
            }

            // Today's range in millis
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfToday = cal.timeInMillis

            var todayReceived = 0.0
            var todayPaid = 0.0
            var todayCreditSales = 0.0
            var todayCreditPurchases = 0.0

            val txMap = transactions.associateBy { it.id }

            for (tx in transactions) {
                if (tx.date in startOfToday until endOfToday) {
                    val (debit, credit) = AccountingEngine.calculateDebitCredit(tx, txMap)
                    val type = TransactionType.fromString(tx.transactionType)
                    when (type) {
                        TransactionType.CREDIT_SALE, TransactionType.SERVICE_ON_CREDIT -> todayCreditSales += tx.amount
                        TransactionType.CREDIT_PURCHASE, TransactionType.EXPENSE_ON_CREDIT -> todayCreditPurchases += tx.amount
                        TransactionType.PAYMENT_RECEIVED -> todayReceived += tx.amount
                        TransactionType.PAYMENT_MADE -> todayPaid += tx.amount
                        TransactionType.REVERSAL -> {
                            // Reversal reflects in adjustments
                        }
                        else -> {}
                    }
                }
            }

            val recent = transactions.take(15).map { tx ->
                val p = partyMap[tx.partyId]
                TransactionWithParty(
                    transaction = tx,
                    partyName = p?.name ?: "Unknown Party",
                    partyPhone = p?.phone ?: ""
                )
            }

            DashboardStats(
                totalCustomerReceivables = totalCustomerReceivables,
                totalSupplierPayables = totalSupplierPayables,
                netBalance = totalCustomerReceivables - totalSupplierPayables,
                todayReceived = todayReceived,
                todayPaid = todayPaid,
                todayCreditSales = todayCreditSales,
                todayCreditPurchases = todayCreditPurchases,
                totalActiveCustomers = activeCustCount,
                totalActiveSuppliers = activeSuppCount,
                totalOverdueCount = overdueCount,
                recentTransactions = recent
            )
        }
    }

    // Full Local Backup & Restore
    suspend fun exportBackupData(): AppBackupData {
        val businesses = businessDao.getAllBusinesses().first()
        val parties = partyDao.getAllPartiesSync()
        val transactions = transactionDao.getAllTransactionsSync()
        return AppBackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            businesses = businesses,
            parties = parties,
            transactions = transactions
        )
    }

    suspend fun restoreBackup(backup: AppBackupData, overwriteExisting: Boolean) {
        if (overwriteExisting) {
            transactionDao.deleteAll()
            partyDao.deleteAll()
            businessDao.deleteAll()
        }
        businessDao.insertAll(backup.businesses)
        partyDao.insertAll(backup.parties)
        transactionDao.insertAll(backup.transactions)
    }

    // Realistic Demo Data Seed
    suspend fun seedDemoData(businessId: Long) {
        val biz = businessDao.getBusinessByIdSync(businessId) ?: return
        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L

        // Customers
        val cust1Id = partyDao.insert(
            PartyEntity(
                businessId = biz.id,
                type = PartyType.CUSTOMER.name,
                name = "Ahmed General Store",
                phone = "+1 555-0192",
                address = "12 Market Street",
                notes = "Wholesale buyer - pays on weekly terms",
                openingBalance = 250.0,
                openingBalanceDate = now - (15 * oneDay)
            )
        )

        val cust2Id = partyDao.insert(
            PartyEntity(
                businessId = biz.id,
                type = PartyType.CUSTOMER.name,
                name = "Sarah Miller",
                phone = "+1 555-0144",
                address = "Oak Ridge Apts #4",
                notes = "Regular customer - monthly groceries",
                openingBalance = 0.0,
                openingBalanceDate = now - (10 * oneDay)
            )
        )

        val cust3Id = partyDao.insert(
            PartyEntity(
                businessId = biz.id,
                type = PartyType.CUSTOMER.name,
                name = "Apex Auto Workshop",
                phone = "+1 555-0188",
                address = "Industrial Zone Plot 5",
                notes = "Spare parts credit account",
                openingBalance = 500.0,
                openingBalanceDate = now - (20 * oneDay)
            )
        )

        // Suppliers
        val supp1Id = partyDao.insert(
            PartyEntity(
                businessId = biz.id,
                type = PartyType.SUPPLIER.name,
                name = "Global FMCG Distributors",
                phone = "+1 555-0111",
                address = "Logistics Hub Warehouse 3",
                notes = "Primary beverage and grocery supplier",
                openingBalance = 1200.0,
                openingBalanceDate = now - (30 * oneDay)
            )
        )

        val supp2Id = partyDao.insert(
            PartyEntity(
                businessId = biz.id,
                type = PartyType.SUPPLIER.name,
                name = "National Packaging Ltd",
                phone = "+1 555-0133",
                address = "Station Road",
                notes = "Boxes and bags supplier",
                openingBalance = 180.0,
                openingBalanceDate = now - (14 * oneDay)
            )
        )

        // Transactions for Cust 1
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = cust1Id,
                partyType = PartyType.CUSTOMER.name,
                transactionType = TransactionType.CREDIT_SALE.name,
                amount = 450.0,
                date = now - (12 * oneDay),
                referenceNo = "INV-1001",
                description = "Bulk cooking oil & flour shipment"
            )
        )
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = cust1Id,
                partyType = PartyType.CUSTOMER.name,
                transactionType = TransactionType.PAYMENT_RECEIVED.name,
                amount = 300.0,
                date = now - (7 * oneDay),
                referenceNo = "REC-2041",
                description = "Cash installment payment"
            )
        )
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = cust1Id,
                partyType = PartyType.CUSTOMER.name,
                transactionType = TransactionType.CUSTOMER_RETURN.name,
                amount = 50.0,
                date = now - (3 * oneDay),
                referenceNo = "RET-004",
                description = "Damaged cartons returned"
            )
        )

        // Transactions for Cust 2
        val wrongTxId = transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = cust2Id,
                partyType = PartyType.CUSTOMER.name,
                transactionType = TransactionType.CREDIT_SALE.name,
                amount = 120.0,
                date = now - (5 * oneDay),
                referenceNo = "INV-1002",
                description = "Organic milk and cereals (Wrong entry)"
            )
        )
        // Reverse this erroneous transaction to demonstrate audited reversal!
        transactionDao.reverseTransaction(
            originalId = wrongTxId,
            reason = "Billed to wrong customer account by cashier",
            reversalDate = now - (4 * oneDay)
        )
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = cust2Id,
                partyType = PartyType.CUSTOMER.name,
                transactionType = TransactionType.CREDIT_SALE.name,
                amount = 85.0,
                date = now - (2 * oneDay),
                referenceNo = "INV-1005",
                description = "Weekly essentials"
            )
        )

        // Transactions for Supplier 1
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = supp1Id,
                partyType = PartyType.SUPPLIER.name,
                transactionType = TransactionType.CREDIT_PURCHASE.name,
                amount = 800.0,
                date = now - (10 * oneDay),
                referenceNo = "BILL-8891",
                description = "Monthly stock replenishment"
            )
        )
        transactionDao.insert(
            TransactionEntity(
                businessId = biz.id,
                partyId = supp1Id,
                partyType = PartyType.SUPPLIER.name,
                transactionType = TransactionType.PAYMENT_MADE.name,
                amount = 1000.0,
                date = now - (1 * oneDay),
                referenceNo = "PAY-501",
                description = "Bank wire transfer payment"
            )
        )
    }
}
