package com.example.superinvoice.pdf

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.pdf.InvoiceAccent
import com.example.superinvoice.data.pdf.InvoiceFontChoice
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.pdf.InvoiceStyle
import com.example.superinvoice.data.pdf.InvoiceTemplate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Portão de segurança do refactor do gerador de PDF.
 *
 * Renderiza os três templates e imprime um SHA-256 dos pixels de cada um.
 * Rode antes de mexer no `InvoicePdfGenerator` e de novo depois: se os
 * hashes do conjunto `normal` baterem, o refactor não alterou um pixel.
 *
 * Os hashes saem no logcat sob a tag `PDF-RENDER`, o que funciona mesmo em
 * aparelhos onde a MIUI bloqueia o `adb` de ler `Android/data`:
 *
 *   adb logcat -d -s PDF-RENDER:I
 *
 * Os PNGs também vão para a pasta Downloads (`superinvoice-render/`) para
 * inspeção visual quando algum hash divergir.
 */
@RunWith(AndroidJUnit4::class)
class InvoicePdfRenderTest {

    @Test
    fun renderAllTemplates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val generator = InvoicePdfGenerator(context)

        var rendered = 0
        for ((datasetName, dataset) in datasets()) {
            for (template in InvoiceTemplate.entries) {
                val name = "$datasetName-${template.name.lowercase()}"
                val bitmap = generator.generateInvoicePreviewBitmap(
                    invoice = dataset.invoice,
                    client = dataset.client,
                    items = dataset.items,
                    businessInfo = dataset.businessInfo,
                    paymentInfo = dataset.paymentInfo,
                    currency = "USD",
                    dateFormat = "MM/dd/yyyy",
                    logoPath = null,
                    signaturePath = null,
                    paymentQrCodePath = null,
                    template = template,
                    scale = 2,
                    isPremium = true
                )
                requireNotNull(bitmap) { "$name devolveu null" }

                Log.i(TAG, "$RUN_LABEL  $name  ${bitmap.width}x${bitmap.height}  ${hash(bitmap)}")
                writeToDownloads(context, "$RUN_LABEL-$name.png", bitmap)
                bitmap.recycle()
                rendered++
            }
        }

        // 3 templates x 4 tipografias: a matriz que a Fase D precisa conferir.
        val typo = datasets().first { it.first == "stress" }.second
        for (choice in InvoiceFontChoice.entries) {
            for (template in InvoiceTemplate.entries) {
                val name = "font-${choice.id}-${template.name.lowercase()}"
                val bitmap = generator.generateInvoicePreviewBitmap(
                    invoice = typo.invoice,
                    client = typo.client,
                    items = typo.items,
                    businessInfo = typo.businessInfo,
                    paymentInfo = typo.paymentInfo,
                    currency = "USD",
                    dateFormat = "MM/dd/yyyy",
                    logoPath = null,
                    signaturePath = null,
                    paymentQrCodePath = null,
                    template = template,
                    scale = 2,
                    isPremium = true,
                    style = InvoiceStyle.of(fonts = choice.fonts(context))
                )
                requireNotNull(bitmap) { "$name devolveu null" }
                Log.i(TAG, "$RUN_LABEL  $name  ${bitmap.width}x${bitmap.height}  ${hash(bitmap)}")
                writeToDownloads(context, "$RUN_LABEL-$name.png", bitmap)
                bitmap.recycle()
                rendered++
            }
        }

        // Uma cor de destaque, para conferir onde ela pousa.
        val accented = datasets().first().second
        for (template in InvoiceTemplate.entries) {
            val name = "accent-${template.name.lowercase()}"
            val bitmap = generator.generateInvoicePreviewBitmap(
                invoice = accented.invoice,
                client = accented.client,
                items = accented.items,
                businessInfo = accented.businessInfo,
                paymentInfo = accented.paymentInfo,
                currency = "USD",
                dateFormat = "MM/dd/yyyy",
                logoPath = null,
                signaturePath = null,
                paymentQrCodePath = null,
                template = template,
                scale = 2,
                isPremium = true,
                style = InvoiceStyle.of(accent = InvoiceAccent.Orange)
            )
            requireNotNull(bitmap) { "$name devolveu null" }
            Log.i(TAG, "$RUN_LABEL  $name  ${bitmap.width}x${bitmap.height}  ${hash(bitmap)}")
            writeToDownloads(context, "$RUN_LABEL-$name.png", bitmap)
            bitmap.recycle()
            rendered++
        }

        assertTrue("nada foi renderizado", rendered > 0)
    }

    /** SHA-256 dos pixels crus — a comparação antes/depois. */
    private fun hash(bitmap: Bitmap): String {
        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        val digest = MessageDigest.getInstance("SHA-256").digest(buffer.array())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun writeToDownloads(
        context: android.content.Context,
        fileName: String,
        bitmap: Bitmap
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/superinvoice-render"
                    )
                }
                val uri = context.contentResolver
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                context.contentResolver.openOutputStream(uri)?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    "superinvoice-render"
                ).apply { mkdirs() }
                java.io.FileOutputStream(java.io.File(dir, fileName)).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        } catch (e: Exception) {
            // O hash no logcat é o que importa; o PNG é conveniência.
            Log.w(TAG, "não consegui escrever $fileName: ${e.message}")
        }
    }

    // ---- Dados de teste --------------------------------------------------

    private class Dataset(
        val invoice: Invoice,
        val client: Client,
        val items: List<InvoiceItem>,
        val businessInfo: InvoicePdfGenerator.BusinessInfo,
        val paymentInfo: InvoicePdfGenerator.PaymentInfo
    )

    private fun datasets(): List<Pair<String, Dataset>> = listOf(
        // Caso típico — é este que precisa sair idêntico depois do refactor.
        "normal" to Dataset(
            invoice = invoice(subtotal = 1000.0, tax = 100.0, total = 1100.0),
            client = Client(
                id = 1,
                name = "Studio Vinte",
                email = "contato@studiovinte.com",
                phone = "+55 11 98765-4321",
                address = "Rua das Palmeiras, 240",
                city = "São Paulo",
                state = "SP",
                zipCode = "01415-000"
            ),
            items = listOf(
                item(1, "Consultoria de marca", 400.0, 1),
                item(2, "Identidade visual", 300.0, 2)
            ),
            businessInfo = businessInfo(),
            paymentInfo = paymentInfo()
        ),
        // Casos de borda: nome longo, endereço longo, muitos itens, valor alto.
        "stress" to Dataset(
            invoice = invoice(subtotal = 98450.0, tax = 9845.0, total = 108295.0),
            client = Client(
                id = 2,
                name = "Consultoria e Participações Aurora Boreal do Brasil Ltda ME",
                email = "financeiro.contas.a.pagar@auroraborealconsultoria.com.br",
                phone = "+55 11 3000-0000",
                address = "Avenida Engenheiro Luís Carlos Berrini, 1500, Conjunto 142, Bloco B",
                city = "São Paulo",
                state = "SP",
                zipCode = "04571-000"
            ),
            items = (1..15).map {
                item(
                    id = it,
                    name = "Serviço de consultoria estratégica continuada — etapa $it",
                    price = 6563.33,
                    quantity = if (it % 3 == 0) 2 else 1
                )
            },
            businessInfo = businessInfo(),
            paymentInfo = paymentInfo()
        )
    )

    private fun invoice(subtotal: Double, tax: Double, total: Double) = Invoice(
        id = 1,
        number = "042",
        clientId = 1,
        createdDate = FIXED_DATE,
        dueDate = "12/31/2024",
        notes = "Pagamento em até 15 dias corridos após o recebimento.",
        subtotal = subtotal,
        tax = tax,
        discount = 0.0,
        totalAmount = total,
        status = InvoiceStatus.DRAFT,
        currency = "USD"
    )

    private fun item(id: Int, name: String, price: Double, quantity: Int) = InvoiceItem(
        id = id,
        invoiceId = 1,
        productServiceId = id,
        productServiceName = name,
        productServiceDescription = "Descrição do item $id",
        pricePerUnit = price,
        quantity = quantity,
        lineTotal = price * quantity
    )

    private fun businessInfo() = InvoicePdfGenerator.BusinessInfo(
        businessName = "Oficina Tipográfica",
        ownerName = "Ana Prado",
        email = "ana@oficinatipografica.com",
        phone = "+55 11 91234-5678",
        website = "oficinatipografica.com",
        address = "Rua Aurora, 88",
        city = "São Paulo",
        state = "SP",
        zipCode = "01209-000",
        taxId = "12.345.678/0001-90"
    )

    private fun paymentInfo() = InvoicePdfGenerator.PaymentInfo(
        bankName = "Banco do Brasil",
        accountHolderName = "Oficina Tipográfica ME",
        accountNumber = "12345-6",
        routingNumber = "001",
        iban = "BR15 0000 0000 0000 1234 5678 901P 1",
        swiftCode = "BRASBRRJ",
        bankAddress = "Av. Paulista, 1000 — São Paulo, SP",
        paymentTerms = "Net 15",
        additionalInstructions = "Enviar comprovante para financeiro@oficinatipografica.com"
    )

    companion object {
        private const val TAG = "PDF-RENDER"

        /** Troque a cada rodada: "antes", "fase-a", "fase-b"… */
        private const val RUN_LABEL = "fase-d"

        /** Data fixa para o render não mudar só porque o relógio andou. */
        private const val FIXED_DATE = 1735689600000L // 2025-01-01 00:00 UTC
    }
}
