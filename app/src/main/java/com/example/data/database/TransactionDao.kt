package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY date ASC, id ASC")
    fun getTransactionsForParty(partyId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId ORDER BY date DESC, id DESC")
    fun getTransactionsForBusiness(businessId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getTransactionsBetweenDates(businessId: Long, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId")
    suspend fun getTransactionsForBusinessSync(businessId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE partyId = :partyId")
    suspend fun deleteByPartyId(partyId: Long)

    @Query("DELETE FROM transactions WHERE businessId = :businessId")
    suspend fun deleteByBusinessId(businessId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    /**
     * Atomically creates a reversal transaction and marks the original transaction as reversed.
     * Preserves financial audit trail:
     * - The original transaction remains in history with isReversed = true, reversedByTransactionId set.
     * - A new transaction is created referencing original ID with the reason for reversal.
     */
    @Transaction
    suspend fun reverseTransaction(originalId: Long, reason: String, reversalDate: Long): Long {
        val original = getTransactionById(originalId)
            ?: throw IllegalArgumentException("Transaction with ID $originalId not found.")

        if (original.isReversed) {
            throw IllegalStateException("Transaction is already marked as reversed.")
        }

        val refCode = if (original.referenceNo.isNotBlank()) "REV-${original.referenceNo}" else "REV-#${original.id}"
        val reversalDescription = "Reversal: " + original.description.ifBlank { original.transactionType }

        val reversalEntity = TransactionEntity(
            businessId = original.businessId,
            partyId = original.partyId,
            partyType = original.partyType,
            transactionType = TransactionType.REVERSAL.name,
            amount = original.amount,
            date = reversalDate,
            referenceNo = refCode,
            description = reversalDescription,
            notes = "Correction reason: $reason",
            isReversed = false,
            reversedByTransactionId = null,
            isReversal = true,
            reversalOfTransactionId = original.id,
            reversalReason = reason,
            createdAt = System.currentTimeMillis()
        )

        val reversalId = insert(reversalEntity)

        // Mark the original transaction as reversed
        update(original.copy(isReversed = true, reversedByTransactionId = reversalId))

        return reversalId
    }
}
