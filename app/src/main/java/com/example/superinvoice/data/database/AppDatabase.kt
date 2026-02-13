package com.example.superinvoice.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.dao.ClientDao
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.dao.ProductServiceDao

@Database(
    entities = [
        Client::class,
        ProductService::class,
        Invoice::class,
        InvoiceItem::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun productServiceDao(): ProductServiceDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrations for future schema changes go here.
        // Version 4 is the first production release — no prior migrations needed.
        // Example for future use:
        // val MIGRATION_4_5 = object : Migration(4, 5) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE invoices ADD COLUMN newColumn TEXT NOT NULL DEFAULT ''")
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "superinvoice_database"
                )
                    // Add migrations here as needed, e.g.: .addMigrations(MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
