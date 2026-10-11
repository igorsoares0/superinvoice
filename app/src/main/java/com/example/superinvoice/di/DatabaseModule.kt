package com.example.superinvoice.di

import android.content.Context
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.dao.ClientDao
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.dao.PaymentDao
import com.example.superinvoice.data.database.dao.ProductServiceDao
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        crashReporter: CrashReporter
    ): AppDatabase {
        return AppDatabase.getDatabase(
            context,
            // Só é chamado se a migração 4 → 5 rodar, e ela roda na thread do Room na
            // primeira consulta, nunca na main: o runBlocking não trava a UI.
            dateFormatProvider = { runBlocking { settingsRepository.dateFormat.first() } },
            crashReporter = crashReporter
        )
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

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }
}
