package com.example.superinvoice.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.analytics.ConsentRegion
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod
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
 * Pagamento, `amountPaid` e status são gravados juntos numa transação (spec, seção 5.2). O banco é
 * em memória; o teste não chama nada que escreva no DataStore real do app.
 */
@RunWith(AndroidJUnit4::class)
class PaymentRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: InvoiceRepository
    private var invoiceId = 0

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
        invoiceId = db.invoiceDao().insert(
            Invoice(
                number = "INV-001", clientId = clientId, dueDate = 0L,
                subtotal = 100.0, totalAmount = 100.0, status = InvoiceStatus.SENT
            )
        ).toInt()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun pagamentosParciaisAteQuitarEApagarVoltaOStatus() = runBlocking {
        val first = repository.addPayment(invoiceId, 40.0, paidAt = 1_000L, PaymentMethod.PIX, "sinal")
        var invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals("REC-001", first.receiptNumber)
        assertEquals(40.0, invoice.amountPaid, 0.0)
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.status)
        assertNull(invoice.paymentDate)

        val second = repository.addPayment(invoiceId, 60.0, paidAt = 2_000L, PaymentMethod.CASH, "")
        invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals("REC-002", second.receiptNumber)
        assertEquals(100.0, invoice.amountPaid, 0.0)
        assertEquals(InvoiceStatus.PAID, invoice.status)
        assertEquals(2_000L, invoice.paymentDate)

        repository.deletePayment(second)
        invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.status)
        assertNull(invoice.paymentDate)

        repository.deletePayment(first)
        invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals(0.0, invoice.amountPaid, 0.0)
        assertEquals(InvoiceStatus.SENT, invoice.status)
    }

    /** Salvar a edição com uma cópia velha da fatura não pode desfazer o que os pagamentos gravaram. */
    @Test
    fun editarItensRecalculaOStatusComOTotalNovo() = runBlocking {
        val loadedByEditScreen = repository.getInvoiceById(invoiceId)!!
        repository.addPayment(invoiceId, 100.0, paidAt = 1_000L, PaymentMethod.CARD, "")

        val item = InvoiceItem(
            invoiceId = invoiceId, productServiceId = null, productServiceName = "Extra",
            pricePerUnit = 150.0, quantity = 1.0, lineTotal = 150.0
        )
        repository.updateInvoiceWithItems(
            loadedByEditScreen.copy(subtotal = 150.0, totalAmount = 150.0),
            listOf(item)
        )

        val invoice = repository.getInvoiceById(invoiceId)!!
        assertEquals(100.0, invoice.amountPaid, 0.0)
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.status)
    }
}
