package com.example.superinvoice.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exclusão segura (spec, seção 5.3): cliente com documentos é arquivado e nunca leva as faturas
 * junto; produto sempre sai, e o item da fatura fica com a cópia de nome e preço.
 */
@RunWith(AndroidJUnit4::class)
class ClientArchiveTest {

    private lateinit var db: AppDatabase
    private lateinit var clients: ClientRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        clients = ClientRepository(db.clientDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun clienteArquivadoSaiDaListaDeAtivosEVoltaAoDesarquivar() = runBlocking {
        val id = db.clientDao().insert(Client(name = "Ana", email = "", phone = "")).toInt()
        val client = clients.getClientById(id)!!

        clients.setArchived(client, true)
        assertEquals(emptyList<Client>(), clients.getActiveClients().first())
        assertEquals(listOf(id), clients.getArchivedClients().first().map { it.id })

        clients.setArchived(client, false)
        assertEquals(listOf(id), clients.getActiveClients().first().map { it.id })
    }

    @Test
    fun clienteComDocumentoNaoEhApagadoEADocumentoFica() = runBlocking {
        val id = db.clientDao().insert(Client(name = "Bia", email = "", phone = "")).toInt()
        db.invoiceDao().insert(Invoice(number = "INV-001", clientId = id, dueDate = 0L))

        try {
            clients.deleteClient(clients.getClientById(id)!!)
            fail("cliente com documento não pode ser apagado (RESTRICT)")
        } catch (expected: SQLiteConstraintException) {
        }
        assertEquals(1, db.invoiceDao().getCount())
    }

    @Test
    fun produtoEmUsoEhApagadoEOItemMantemACopia() = runBlocking {
        val products = ProductServiceRepository(db.productServiceDao())
        val clientId = db.clientDao().insert(Client(name = "Caio", email = "", phone = "")).toInt()
        val invoiceId = db.invoiceDao().insert(Invoice(number = "INV-001", clientId = clientId, dueDate = 0L)).toInt()
        val productId = db.productServiceDao().insert(ProductService(name = "Revisão", pricePerUnit = 90.0)).toInt()
        db.invoiceItemDao().insertAll(
            listOf(
                InvoiceItem(
                    invoiceId = invoiceId, productServiceId = productId, productServiceName = "Revisão",
                    pricePerUnit = 90.0, quantity = 1.0, lineTotal = 90.0
                )
            )
        )

        products.deleteProductService(db.productServiceDao().getById(productId)!!)

        val item = db.invoiceItemDao().getByInvoiceIdSync(invoiceId).single()
        assertNull(item.productServiceId)
        assertEquals("Revisão", item.productServiceName)
        assertEquals(90.0, item.pricePerUnit, 0.0)
    }
}
