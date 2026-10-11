package com.example.superinvoice.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.superinvoice.data.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    /** Clientes arquivados não aparecem na lista nem na seleção de cliente da fatura. */
    @Query("SELECT * FROM clients WHERE archived = 0 ORDER BY name ASC")
    fun getActive(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE archived = 1 ORDER BY name ASC")
    fun getArchived(): Flow<List<Client>>

    @Query("UPDATE clients SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Int, archived: Boolean)

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getCount(): Int
}
