package com.example.superinvoice.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.Migration4To5
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * A v4 é a primeira versão em produção: um erro nesta migração apaga ou corrompe faturas reais,
 * ou impede o app de abrir. O banco v4 é montado a partir de `app/schemas/.../4.json`, e o
 * `runMigrationsAndValidate` confere o resultado contra o `5.json`.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private var unparsedDueDates = 0

    private val migration = Migration4To5(
        dateFormatProvider = { "MM/dd/yyyy" },
        onUnparsedDueDates = { unparsedDueDates = it },
        locale = Locale.US
    )

    @Test
    fun totaisStatusEDatasSobrevivemAMigracao() {
        createV4Database()
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, migration)

        // Totais iguais aos da v4; desconto e imposto viram FIXED com os mesmos valores.
        db.query("SELECT type, subtotal, discountType, discountValue, discountAmount, taxType, taxValue, taxAmount, shipping, totalAmount FROM invoices WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("INVOICE", it.getString(0))
            assertEquals(100.0, it.getDouble(1), 0.0)
            assertEquals("FIXED", it.getString(2))
            assertEquals(5.0, it.getDouble(3), 0.0)
            assertEquals(5.0, it.getDouble(4), 0.0)
            assertEquals("FIXED", it.getString(5))
            assertEquals(10.0, it.getDouble(6), 0.0)
            assertEquals(10.0, it.getDouble(7), 0.0)
            assertEquals(0.0, it.getDouble(8), 0.0)
            assertEquals(105.0, it.getDouble(9), 0.0)
        }

        // Cada formato da lista vira a mesma meia-noite UTC; o ilegível fica no dia da criação.
        val expectedDue = mapOf(
            1 to DEC_31, 2 to DEC_31, 3 to DEC_31, 4 to DEC_31, 5 to DEC_31,
            6 to DEC_31, 7 to DEC_31, 8 to DEC_31, 9 to DEC_31, 10 to JUN_1
        )
        db.query("SELECT id, dueDate FROM invoices ORDER BY id").use {
            while (it.moveToNext()) {
                assertEquals("fatura ${it.getInt(0)}", expectedDue.getValue(it.getInt(0)), it.getLong(1))
            }
        }
        assertEquals(1, unparsedDueDates)

        // OVERDUE deixa de ser gravado.
        assertEquals("SENT", stringOf(db, "SELECT status FROM invoices WHERE id = 4"))

        // Fatura paga ganha um pagamento do total e amountPaid; a não paga, não.
        db.query("SELECT invoiceId, amount, paidAt, method, receiptNumber FROM payments ORDER BY invoiceId").use {
            assertEquals(2, it.count)
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(105.0, it.getDouble(1), 0.0)
            assertEquals(PAID_AT, it.getLong(2))
            assertEquals("OTHER", it.getString(3))
            assertEquals("REC-001", it.getString(4))
            it.moveToNext()
            assertEquals(2, it.getInt(0))
            assertEquals(CREATED, it.getLong(2)) // sem paymentDate: usa a criação
            assertEquals("REC-002", it.getString(4))
        }
        assertEquals(105.0, doubleOf(db, "SELECT amountPaid FROM invoices WHERE id = 1"), 0.0)
        assertEquals(CREATED, longOf(db, "SELECT paymentDate FROM invoices WHERE id = 2"))
        assertEquals(0.0, doubleOf(db, "SELECT amountPaid FROM invoices WHERE id = 3"), 0.0)

        // Item: quantidade vira REAL, unidade vazia, cópia de nome e preço intacta.
        db.query("SELECT quantity, unit, productServiceName, pricePerUnit, lineTotal FROM invoice_items WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(3.0, it.getDouble(0), 0.0)
            assertEquals("", it.getString(1))
            assertEquals("Consultoria", it.getString(2))
            assertEquals(33.0, it.getDouble(3), 0.0)
            assertEquals(99.0, it.getDouble(4), 0.0)
        }

        assertEquals(0L, longOf(db, "SELECT archived FROM clients WHERE id = 1"))
        assertEquals("", stringOf(db, "SELECT unit FROM products_services WHERE id = 1"))
    }

    @Test
    fun fksNovasProdutoSoltaOItemEClienteComFaturasEhRecusado() {
        createV4Database()
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, migration)
        // O Room liga as FKs no onOpen; o banco devolvido pelo helper não passa por ele.
        db.execSQL("PRAGMA foreign_keys = ON")

        db.execSQL("DELETE FROM products_services WHERE id = 1")
        assertNull(nullableLongOf(db, "SELECT productServiceId FROM invoice_items WHERE id = 1"))
        assertEquals(1L, longOf(db, "SELECT COUNT(*) FROM invoice_items WHERE id = 1"))

        try {
            db.execSQL("DELETE FROM clients WHERE id = 1")
            fail("cliente com faturas não pode ser apagado (RESTRICT)")
        } catch (expected: SQLiteConstraintException) {
        }
        assertEquals(10L, longOf(db, "SELECT COUNT(*) FROM invoices"))

        // Apagar a fatura leva itens e pagamentos junto (CASCADE).
        db.execSQL("DELETE FROM invoices WHERE id = 1")
        assertEquals(0L, longOf(db, "SELECT COUNT(*) FROM invoice_items WHERE invoiceId = 1"))
        assertEquals(0L, longOf(db, "SELECT COUNT(*) FROM payments WHERE invoiceId = 1"))
    }

    /** Abre o banco migrado pelo Room de verdade: entidades, converters e DAOs leem a v5. */
    @Test
    fun roomLeOBancoMigrado() = runBlocking {
        createV4Database()
        helper.runMigrationsAndValidate(TEST_DB, 5, true, migration).close()

        val room = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(migration).build()
        try {
            val invoice = room.invoiceDao().getById(1)!!
            assertEquals(DocumentType.INVOICE, invoice.type)
            assertEquals(InvoiceStatus.PAID, invoice.status)
            assertEquals(AdjustmentType.FIXED, invoice.taxType)
            assertEquals(DEC_31, invoice.dueDate)
            assertEquals(105.0, invoice.amountPaid, 0.0)

            val payments = room.paymentDao().getByInvoiceId(1).first()
            assertEquals(PaymentMethod.OTHER, payments.single().method)

            val item = room.invoiceItemDao().getByInvoiceIdSync(1).single()
            assertEquals(1, item.productServiceId)
            assertEquals(3.0, item.quantity, 0.0)

            assertTrue(room.clientDao().getById(1)!!.archived.not())
            assertEquals("INV-010", room.invoiceDao().getLastInvoiceNumber())
        } finally {
            room.close()
        }
    }

    private fun createV4Database() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                "INSERT INTO clients (id, name, email, phone, address, city, state, zipCode, notes, createdAt) " +
                    "VALUES (1, 'Cliente com faturas', 'a@a.com', '', '', '', '', '', '', $CREATED), " +
                    "(2, 'Cliente sem faturas', 'b@b.com', '', '', '', '', '', '', $CREATED)"
            )
            db.execSQL(
                "INSERT INTO products_services (id, name, description, pricePerUnit, createdAt) " +
                    "VALUES (1, 'Consultoria', '', 33.0, $CREATED), (2, 'Sem uso', '', 10.0, $CREATED)"
            )
            val dueDates = listOf(
                "12/31/2024", "31/12/2024", "2024-12-31", "31.12.2024", "31-12-2024",
                "December 31, 2024", "31 December 2024", "Dec 31, 2024", "Dec, 31", "data ilegível"
            )
            dueDates.forEachIndexed { index, dueDate ->
                val id = index + 1
                val status = when (id) {
                    1, 2 -> "PAID"
                    4 -> "OVERDUE"
                    5 -> "SENT"
                    else -> "DRAFT"
                }
                val paymentDate = if (id == 1) PAID_AT.toString() else "NULL"
                db.execSQL(
                    "INSERT INTO invoices (id, number, clientId, dueDate, createdDate, paymentDate, notes, " +
                        "tax, discount, subtotal, totalAmount, status, currency, isPaid) VALUES " +
                        "(?, ?, 1, ?, $CREATED, $paymentDate, '', 10.0, 5.0, 100.0, 105.0, ?, 'USD', 0)",
                    arrayOf<Any>(id, "INV-" + id.toString().padStart(3, '0'), dueDate, status)
                )
            }
            db.execSQL(
                "INSERT INTO invoice_items (id, invoiceId, productServiceId, productServiceName, " +
                    "productServiceDescription, pricePerUnit, quantity, lineTotal) " +
                    "VALUES (1, 1, 1, 'Consultoria', '', 33.0, 3, 99.0)"
            )
        }
    }

    private fun stringOf(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { it.moveToFirst(); it.getString(0) }

    private fun longOf(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { it.moveToFirst(); it.getLong(0) }

    private fun nullableLongOf(db: SupportSQLiteDatabase, sql: String): Long? =
        db.query(sql).use { it.moveToFirst(); if (it.isNull(0)) null else it.getLong(0) }

    private fun doubleOf(db: SupportSQLiteDatabase, sql: String): Double =
        db.query(sql).use { it.moveToFirst(); it.getDouble(0) }

    private companion object {
        const val TEST_DB = "migration-test"
        const val DEC_31 = 1735603200000L // 2024-12-31 00:00 UTC
        const val JUN_1 = 1717200000000L // 2024-06-01 00:00 UTC

        // Meio-dia UTC: o "dia da criação" é 1º de junho em qualquer fuso do aparelho de teste.
        const val CREATED = 1717243200000L // 2024-06-01 12:00 UTC
        const val PAID_AT = 1718000000000L
    }
}
