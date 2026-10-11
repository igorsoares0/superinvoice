package com.example.superinvoice.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.analytics.ConsentRegion
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Conversão de orçamento em fatura (spec, seção 5.1): uma transação que copia tudo, numera na
 * sequência das faturas e marca o orçamento. Banco em memória; nada escreve no DataStore real.
 */
@RunWith(AndroidJUnit4::class)
class EstimateConversionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: InvoiceRepository
    private var estimateId = 0

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = InvoiceRepository(
            database = db,
            invoiceDao = db.invoiceDao(),
            invoiceItemDao = db.invoiceItemDao(),
            paymentDao = db.paymentDao(),
            settingsRepository = SettingsRepository(context, ConsentRegion(context))
        )
        val clientId = db.clientDao().insert(Client(name = "Cliente", email = "", phone = "")).toInt()
        db.invoiceDao().insert(Invoice(number = "INV-007", clientId = clientId, dueDate = 0L))
        estimateId = db.invoiceDao().insert(
            Invoice(
                type = DocumentType.ESTIMATE, number = "EST-001", clientId = clientId,
                dueDate = 0L, createdDate = 1_000L, notes = "Validade de 15 dias",
                subtotal = 200.0, discountType = AdjustmentType.PERCENT, discountValue = 10.0,
                discountAmount = 20.0, shipping = 5.0, totalAmount = 185.0,
                status = InvoiceStatus.ACCEPTED, currency = "BRL"
            )
        ).toInt()
        db.invoiceItemDao().insertAll(
            listOf(
                InvoiceItem(
                    invoiceId = estimateId, productServiceId = null, productServiceName = "Projeto",
                    pricePerUnit = 80.0, quantity = 2.5, unit = "h", lineTotal = 200.0
                )
            )
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun conversaoCopiaOrcamentoNumeraComoFaturaEMarcaConvertido() = runBlocking {
        val invoiceId = repository.convertEstimateToInvoice(estimateId)!!

        val invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals(DocumentType.INVOICE, invoice.type)
        assertEquals("INV-008", invoice.number)
        assertEquals(InvoiceStatus.DRAFT, invoice.status)
        assertEquals(estimateId, invoice.convertedFromId)
        assertEquals(AdjustmentType.PERCENT, invoice.discountType)
        assertEquals(185.0, invoice.totalAmount, 0.0)
        assertEquals(5.0, invoice.shipping, 0.0)
        assertEquals("Validade de 15 dias", invoice.notes)
        assertEquals("BRL", invoice.currency)

        val item = repository.getInvoiceItemsSync(invoiceId).single()
        assertEquals(2.5, item.quantity, 0.0)
        assertEquals("h", item.unit)
        assertEquals(1, repository.getInvoiceItemsSync(estimateId).size)

        assertEquals(InvoiceStatus.CONVERTED, repository.getInvoiceById(estimateId)!!.status)
        assertEquals("EST-002", repository.generateNextNumber(DocumentType.ESTIMATE))
    }

    @Test
    fun converterDuasVezesDevolveAMesmaFatura() = runBlocking {
        val first = repository.convertEstimateToInvoice(estimateId)
        val second = repository.convertEstimateToInvoice(estimateId)
        assertEquals(first, second)
        assertEquals(1, repository.getInvoiceItemsSync(first!!).size)
    }

    @Test
    fun faturaNaoEhConvertida() = runBlocking {
        val invoice = db.invoiceDao().getById(1)!!
        assertNull(repository.convertEstimateToInvoice(invoice.id))
    }
}
