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
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAll(): Flow<List<Client>>

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
