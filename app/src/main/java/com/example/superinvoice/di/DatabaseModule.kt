package com.example.superinvoice.di

import android.content.Context
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.dao.ClientDao
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.dao.ProductServiceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideClientDao(database: AppDatabase): ClientDao {
        return database.clientDao()
    }

    @Provides
    fun provideProductServiceDao(database: AppDatabase): ProductServiceDao {
        return database.productServiceDao()
    }

    @Provides
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao {
        return database.invoiceDao()
    }

    @Provides
    fun provideInvoiceItemDao(database: AppDatabase): InvoiceItemDao {
        return database.invoiceItemDao()
    }
}
