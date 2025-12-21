package com.example.superinvoice.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.superinvoice.data.ProductService
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductServiceDao {
    @Query("SELECT * FROM products_services ORDER BY name ASC")
    fun getAll(): Flow<List<ProductService>>

    @Query("SELECT * FROM products_services WHERE id = :id")
    suspend fun getById(id: Int): ProductService?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(productService: ProductService): Long

    @Update
    suspend fun update(productService: ProductService)

    @Delete
    suspend fun delete(productService: ProductService)

    @Query("SELECT COUNT(*) FROM products_services")
    suspend fun getCount(): Int
}
