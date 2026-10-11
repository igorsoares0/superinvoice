package com.example.superinvoice.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.Payment
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.database.dao.ClientDao
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.dao.PaymentDao
import com.example.superinvoice.data.database.dao.ProductServiceDao

/**
 * Versão 4 foi a primeira em produção. Toda mudança de entidade exige uma `Migration` escrita
 * à mão e registrada em [getDatabase]; nunca `fallbackToDestructiveMigration`, que apagaria as
 * faturas dos usuários.
 */
@Database(
    entities = [
        Client::class,
        ProductService::class,
        Invoice::class,
        InvoiceItem::class,
        Payment::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun productServiceDao(): ProductServiceDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * @param dateFormatProvider formato de data do usuário, lido do DataStore só se a
         *   migração 4 → 5 precisar converter `dueDate`
         */
        fun getDatabase(
            context: Context,
            dateFormatProvider: () -> String?,
            crashReporter: CrashReporter
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "superinvoice_database"
                )
                    .addMigrations(
                        Migration4To5(
                            dateFormatProvider = dateFormatProvider,
                            onUnparsedDueDates = { count ->
                                // Só a contagem: o texto da data é dado do usuário.
                                crashReporter.setCustomKey("migration_4_5_unparsed_due_dates", count)
                                crashReporter.log("Migração 4→5: dueDate ilegível em algumas faturas")
                            }
                        )
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
