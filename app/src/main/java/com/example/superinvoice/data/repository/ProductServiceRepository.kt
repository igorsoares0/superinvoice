package com.example.superinvoice.data.repository

import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.dao.ProductServiceDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductServiceRepository @Inject constructor(
    private val productServiceDao: ProductServiceDao
) {
    fun getAllProductsServices(): Flow<List<ProductService>> = productServiceDao.getAll()

    suspend fun getProductServiceById(id: Int): ProductService? = productServiceDao.getById(id)

    suspend fun insertProductService(productService: ProductService): Long =
        productServiceDao.insert(productService)

    suspend fun updateProductService(productService: ProductService) =
        productServiceDao.update(productService)

    suspend fun deleteProductService(productService: ProductService) =
        productServiceDao.delete(productService)

    suspend fun getProductServiceCount(): Int = productServiceDao.getCount()
}
