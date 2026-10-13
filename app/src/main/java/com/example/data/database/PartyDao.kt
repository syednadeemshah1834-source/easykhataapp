package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE businessId = :businessId AND isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllParties(businessId: Long): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE businessId = :businessId AND type = :type AND isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getPartiesByType(businessId: Long, type: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    fun getPartyById(id: Long): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyByIdSync(id: Long): PartyEntity?

    @Query("SELECT * FROM parties")
    suspend fun getAllPartiesSync(): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE businessId = :businessId")
    suspend fun getPartiesForBusinessSync(businessId: Long): List<PartyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(party: PartyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parties: List<PartyEntity>)

    @Update
    suspend fun update(party: PartyEntity)

    @Delete
    suspend fun delete(party: PartyEntity)

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM parties WHERE businessId = :businessId")
    suspend fun deleteByBusinessId(businessId: Long)

    @Query("DELETE FROM parties")
    suspend fun deleteAll()
}
