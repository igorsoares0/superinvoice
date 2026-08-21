package com.example.superinvoice.data.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.util.getCurrencySymbol
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class InvoiceTemplate {
    CLASSIC,
    MODERN,
    PROFESSIONAL
}

@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pageWidth = 595  // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 60f

    /** Vinco entre páginas no preview empilhado, em pontos. */
    private val previewPageGap = 12

    data class BusinessInfo(
        val businessName: String = "",
        val ownerName: String = "",
        val email: String = "",
        val phone: String = "",
        val website: String = "",
        val address: String = "",
        val city: String = "",
        val state: String = "",
        val zipCode: String = "",
        val taxId: String = ""
    )

    data class PaymentInfo(
        val bankName: String = "",
        val accountHolderName: String = "",
        val accountNumber: String = "",
        val routingNumber: String = "",
        val iban: String = "",
        val swiftCode: String = "",
        val bankAddress: String = "",
        val paymentTerms: String = "",
        val additionalInstructions: String = ""
    )

    /**
     * "Cidade, UF CEP" — a mesma composição aparecia seis vezes, três para
     * o negócio e três para o cliente.
     */
    private fun cityStateZip(city: String, state: String, zipCode: String): String =
        buildString {
            if (city.isNotEmpty()) append(city)
            if (state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(state)
            }
            if (zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(zipCode)
            }
        }

    private fun reformatDateIfNeeded(dateString: String, targetFormat: String): String {
        if (dateString.isEmpty()) return dateString

        // Lista de formatos conhecidos para tentar parsear
        val knownFormats = listOf(
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd-MM-yyyy",
            "MMMM dd, yyyy",
            "dd MMMM yyyy",
            "MMM dd, yyyy",
            "MMM, dd"  // Formato antigo
        )

        // Tentar parsear com cada formato conhecido
        for (format in knownFormats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                parser.isLenient = false
                val date = parser.parse(dateString)

                // Se conseguiu parsear, reformatar com o formato alvo
                if (date != null) {
                    val formatter = SimpleDateFormat(targetFormat, Locale.getDefault())
                    return formatter.format(date)
                }
            } catch (e: Exception) {
                // Continuar tentando outros formatos
                continue
            }
        }

        // Se não conseguiu parsear com nenhum formato, retornar a string original
        return dateString
    }

    fun generateInvoicePdf(
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String = "USD",
        dateFormat: String = "MM/dd/yyyy",
        logoPath: String? = null,
        signaturePath: String? = null,
        paymentQrCodePath: String? = null,
        template: InvoiceTemplate = InvoiceTemplate.CLASSIC,
        isPremium: Boolean = true,
        style: InvoiceStyle = InvoiceStyle.Default
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val paints = InvoicePaints(style)
            val pager = InvoicePager(
                document = pdfDocument,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                margin = margin,
                // Em cada página, por cima do conteúdo.
                onBeforeFinishPage = if (!isPremium) ({ c -> drawWatermark(c) }) else null
            )

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    drawClassicTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.MODERN -> {
                    drawModernTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    drawProfessionalTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
            }

            pager.finish()

            // Save to file using modern MediaStore API (Android 10+) or legacy method
            val fileName = "Invoice_${invoice.number}.pdf"
            val file: File?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri: Uri? = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }

                    // Get the file path for return value
                    val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                    val filePath = cursor?.use {
                        if (it.moveToFirst()) {
                            val columnIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (columnIndex >= 0) it.getString(columnIndex) else null
                        } else null
                    }

                    file = if (filePath != null) File(filePath) else {
                        // Fallback: create a temp file reference
                        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    }
                } else {
                    file = null
                }
            } else {
                // Android 9 and below: Use legacy method
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }

            pdfDocument.close()
            file
        } catch (e: Exception) {
            // Error handled silently
            null
        }
    }

    fun generateInvoicePreviewBitmap(
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String = "USD",
        dateFormat: String = "MM/dd/yyyy",
        logoPath: String? = null,
        signaturePath: String? = null,
        paymentQrCodePath: String? = null,
        template: InvoiceTemplate = InvoiceTemplate.CLASSIC,
        scale: Int = 5,
        isPremium: Boolean = true,
        style: InvoiceStyle = InvoiceStyle.Default
    ): Bitmap? {
        return try {
            // Create PDF document in memory
            val pdfDocument = PdfDocument()
            val paints = InvoicePaints(style)
            val pager = InvoicePager(
                document = pdfDocument,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                margin = margin,
                // Em cada página, por cima do conteúdo.
                onBeforeFinishPage = if (!isPremium) ({ c -> drawWatermark(c) }) else null
            )

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    drawClassicTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.MODERN -> {
                    drawModernTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    drawProfessionalTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
            }

            pager.finish()

            // Save to temporary file
            val tempFile = File.createTempFile("invoice_preview_", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Render PDF to Bitmap with configurable resolution
            val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            // Todas as páginas empilhadas numa imagem só, com um vinco
            // entre elas — assim o preview mostra a fatura inteira sem
            // precisar de controle de página na tela.
            val pageGap = if (pdfRenderer.pageCount > 1) previewPageGap * scale else 0
            val bitmap = Bitmap.createBitmap(
                pageWidth * scale,
                pageHeight * scale * pdfRenderer.pageCount +
                    pageGap * (pdfRenderer.pageCount - 1),
                Bitmap.Config.ARGB_8888
            )

            val bitmapCanvas = Canvas(bitmap)
            bitmapCanvas.drawColor(if (pdfRenderer.pageCount > 1) Color.LTGRAY else Color.WHITE)

            for (index in 0 until pdfRenderer.pageCount) {
                val pdfPage = pdfRenderer.openPage(index)
                val top = index * (pageHeight * scale + pageGap)
                val pageBitmap = Bitmap.createBitmap(
                    pageWidth * scale,
                    pageHeight * scale,
                    Bitmap.Config.ARGB_8888
                )
                Canvas(pageBitmap).drawColor(Color.WHITE)
                pdfPage.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                bitmapCanvas.drawBitmap(pageBitmap, 0f, top.toFloat(), null)
                pageBitmap.recycle()
                pdfPage.close()
            }
            pdfRenderer.close()
            fileDescriptor.close()

            // Clean up temp file
            tempFile.delete()

            bitmap
        } catch (e: Exception) {
            // Error handled silently
            null
        }
    }

    private fun drawWatermark(canvas: Canvas) {
        val watermarkPaint = Paint().apply {
            color = Color.GRAY
            alpha = 38 // ~15% opacity (255 * 0.15)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("SUPERINVOICE", pageWidth / 2f, pageHeight * 0.75f, watermarkPaint)
    }

    private fun drawClassicTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?
    ) {
        var yPos = margin

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        // Calculate logo dimensions (max height 50, maintain aspect ratio)
                        val maxLogoHeight = 50f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        // Draw logo on the left
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Title with line
        val titlePaint = paints.text(24f, family = InvoicePaints.Family.Display, color = paints.style.accent, tracking = 0.15f)

        val linePaint = paints.stroke(color = paints.style.ink, width = 1f)

        // Draw line
        pager.canvas.drawLine(margin, yPos + 15, pageWidth - 200f, yPos + 15, linePaint)

        // Draw INVOICE text
        pager.canvas.drawText("INVOICE", pageWidth - 180f, yPos + 20, titlePaint)
        yPos += paints.advance(titlePaint, 60f)

        // Section: ISSUED TO and Invoice Info
        val sectionTitlePaint = paints.text(9f, weight = InvoicePaints.Weight.Bold, tracking = 0.1f)

        val bodyPaint = paints.text(11f)

        // Left column - FROM (Business Information)
        val leftYStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            pager.canvas.drawText("FROM:", margin, leftYPos, sectionTitlePaint)
            leftYPos += paints.advance(sectionTitlePaint, 18f)
            pager.canvas.drawText(businessInfo.businessName, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
            if (businessInfo.ownerName.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.email.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.email, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.phone.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.phone, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.website.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.website, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.address.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.address, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
            if (cityStateZip.isNotEmpty()) {
                pager.canvas.drawText(cityStateZip, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.taxId.isNotEmpty()) {
                pager.canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
        }

        // Right column - INVOICE INFO
        var rightYPos = yPos
        val rightX = pageWidth - margin - 150f
        // Espaço para o rótulo: o projetado, ou o que a fonte realmente ocupa.
        val labelGap = paints.labelGap(
            sectionTitlePaint,
            listOf("INVOICE NO:", "DATE:", "DUE DATE:"),
            100f
        )

        pager.canvas.drawText("INVOICE NO:", rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(invoice.number, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        // Format createdDate (Long timestamp) for DATE
        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))

        // Reformat dueDate (String) for DUE DATE
        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)

        pager.canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(formattedCreatedDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        pager.canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(formattedDueDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        // Advance to the max height of both columns
        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Second row: ISSUED TO (left) and PAY TO (right)
        leftYPos = yPos
        rightYPos = yPos

        // ISSUED TO (left)
        pager.canvas.drawText("ISSUED TO:", margin, leftYPos, sectionTitlePaint)
        leftYPos += paints.advance(sectionTitlePaint, 18f)
        pager.canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 15f)
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText("Notes: ${client.notes}", margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }

        // PAY TO (right)
        val payRightX = pageWidth / 2f + 20f
        if (paymentInfo.bankName.isNotEmpty()) {
            pager.canvas.drawText("PAY TO:", payRightX, rightYPos, sectionTitlePaint)
            rightYPos += paints.advance(sectionTitlePaint, 18f)
            pager.canvas.drawText(paymentInfo.bankName, payRightX, rightYPos, bodyPaint)
            rightYPos += paints.advance(bodyPaint, 15f)
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankAddress, payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText("Acc Name: ${paymentInfo.accountHolderName}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText("Acc Number: ${paymentInfo.accountNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText("Routing: ${paymentInfo.routingNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText("IBAN: ${paymentInfo.iban}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText("Terms: ${paymentInfo.paymentTerms}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText("Notes: ${paymentInfo.additionalInstructions}", payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }

            // QR Code for payment
            paymentQrCodePath?.let { qrPath ->
                try {
                    val qrFile = File(qrPath)
                    if (qrFile.exists()) {
                        val qrBitmap = BitmapFactory.decodeFile(qrPath)
                        qrBitmap?.let {
                            val qrSize = 80f
                            val scaledQr = Bitmap.createScaledBitmap(
                                it,
                                qrSize.toInt(),
                                qrSize.toInt(),
                                true
                            )
                            pager.canvas.drawText("Scan to Pay:", payRightX, rightYPos, sectionTitlePaint)
                            rightYPos += paints.advance(sectionTitlePaint, 12f)
                            pager.canvas.drawBitmap(scaledQr, payRightX, rightYPos, null)
                            rightYPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    // Error handled silently
                }
            }
        }

        // Advance to max height
        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Divider line before table
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // Table Header — repetido no topo de cada página de continuação.
        val drawTableHeader: (Float) -> Float = { start ->
            var y = start
            pager.canvas.drawText("DESCRIPTION", margin, y, sectionTitlePaint)
            pager.canvas.drawText("UNIT PRICE", pageWidth - margin - 280f, y, sectionTitlePaint)
            pager.canvas.drawText("QTY", pageWidth - margin - 150f, y, sectionTitlePaint)
            pager.canvas.drawText("TOTAL", pageWidth - margin - 80f, y, sectionTitlePaint)
            y += paints.advance(sectionTitlePaint, 20f)
            pager.canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y + paints.advance(sectionTitlePaint, 15f)
        }
        yPos = drawTableHeader(yPos)

        // Table Items
        val currencySymbol = getCurrencySymbol(currency)
        val descriptionPaint = paints.text(9f, color = paints.style.muted)
        val classicRowHeight = paints.advance(bodyPaint, 14f) + paints.advance(descriptionPaint, 14f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, classicRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - margin - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, bodyPaint, descWidth),
                margin, yPos, bodyPaint
            )
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}", pageWidth - margin - 280f, yPos, bodyPaint)
            pager.canvas.drawText("${item.quantity}", pageWidth - margin - 150f, yPos, bodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}", pageWidth - margin - 80f, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 14f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 14f)
            } else {
                yPos += paints.advance(descriptionPaint, 6f)
            }
        }

        yPos += 20f

        // Totais e assinatura seguem juntos; se não couberem, vão inteiros
        // para a próxima página em vez de serem cortados.
        yPos = pager.flowTo(yPos, 150f)

        // Totals
        val totalX = pageWidth - margin - 180f
        val totalValueX = pageWidth - margin - 80f

        pager.canvas.drawText("SUBTOTAL", totalX, yPos, sectionTitlePaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.subtotal)}", totalValueX, yPos, bodyPaint)
        yPos += paints.advance(bodyPaint, 15f)

        if (invoice.tax > 0) {
            pager.canvas.drawText("TAX", totalX, yPos, sectionTitlePaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.tax)}", totalValueX, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 15f)
        }

        if (invoice.discount > 0) {
            pager.canvas.drawText("DISCOUNT", totalX, yPos, sectionTitlePaint)
            pager.canvas.drawText("-$currencySymbol${String.format("%.2f", invoice.discount)}", totalValueX, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 15f)
        }

        val totalPaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)

        pager.canvas.drawText("TOTAL", totalX, yPos, totalPaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.totalAmount)}", totalValueX, yPos, totalPaint)
        yPos += paints.advance(totalPaint, 40f)

        // Business signature
        signaturePath?.let { path ->
            try {
                val signatureFile = File(path)
                if (signatureFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        // Calculate signature dimensions (max height 40, maintain aspect ratio)
                        val maxSignatureHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val signatureHeight = maxSignatureHeight
                        val signatureWidth = signatureHeight * aspectRatio

                        // Draw signature on the left
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            signatureWidth.toInt(),
                            signatureHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
                // Fallback to text signature if image fails
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = paints.text(18f)
                    pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                }
            }
        } ?: run {
            // No signature image, use text signature if owner name exists
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = paints.text(18f)
                pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
            }
        }
    }

    private fun drawModernTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?
    ) {
        val currencySymbol = getCurrencySymbol(currency)
        var yPos = margin

        val sectionTitlePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, tracking = 0.05f)

        val bodyPaint = paints.text(9f)

        val bodySmallPaint = paints.text(8f, color = paints.style.muted)

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Business Information (left) and Invoice # + Dates (right)
        val topSectionStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            val businessNamePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold)
            pager.canvas.drawText(businessInfo.businessName, margin, leftYPos, businessNamePaint)
            leftYPos += paints.advance(businessNamePaint, 13f)

            if (businessInfo.ownerName.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            if (businessInfo.address.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.address, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
            if (cityStateZip.isNotEmpty()) {
                pager.canvas.drawText(cityStateZip, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            if (businessInfo.taxId.isNotEmpty()) {
                pager.canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
        }

        // Right side - Invoice number and dates
        var rightYPos = topSectionStart
        val rightX = pageWidth - margin - 120f

        val invoiceTitlePaint = paints.text(12f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        pager.canvas.drawText("INVOICE #${invoice.number}", rightX, rightYPos, invoiceTitlePaint)
        rightYPos += paints.advance(invoiceTitlePaint, 15f)

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)

        pager.canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        rightYPos += paints.advance(sectionTitlePaint, 12f)
        pager.canvas.drawText(formattedCreatedDate, rightX, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        pager.canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        rightYPos += paints.advance(sectionTitlePaint, 12f)
        pager.canvas.drawText(formattedDueDate, rightX, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Draw horizontal line separator
        val linePaint = paints.stroke(width = 0.5f)
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // BILLED TO section
        leftYPos = yPos
        pager.canvas.drawText("{ BILLED TO }", margin, leftYPos, bodySmallPaint)
        leftYPos += paints.advance(bodySmallPaint, 15f)
        pager.canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 12f)
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText("Notes: ${client.notes}", margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }

        yPos = leftYPos + 20f

        // Draw horizontal line separator
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        // Table Header
        val tableTitlePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, tracking = 0.1f)

        val pricePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, align = Paint.Align.RIGHT, tracking = 0.1f)
        val drawTableHeader: (Float) -> Float = { start ->
            pager.canvas.drawText("DESCRIPTION", margin, start, tableTitlePaint)
            pager.canvas.drawText("RATE", pageWidth - margin - 280f, start, tableTitlePaint)
            pager.canvas.drawText("QTY", pageWidth - margin - 200f, start, tableTitlePaint)
            pager.canvas.drawText("PRICE", pageWidth - margin, start, pricePaint)
            start + paints.advance(pricePaint, 20f)
        }
        yPos = drawTableHeader(yPos)

        // Table rows
        val tableBodyPaint = paints.text(9f)

        val priceBodyPaint = paints.text(9f, align = Paint.Align.RIGHT)

        val descriptionPaint = paints.text(8f, color = paints.style.muted)

        val modernRowHeight = paints.advance(tableBodyPaint, 12f) + paints.advance(descriptionPaint, 12f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, modernRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - margin - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, tableBodyPaint, descWidth),
                margin, yPos, tableBodyPaint
            )
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 280f, yPos, tableBodyPaint)
            pager.canvas.drawText("${item.quantity}", pageWidth - margin - 200f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin, yPos, priceBodyPaint)
            yPos += paints.advance(tableBodyPaint, 12f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 12f)
            } else {
                yPos += paints.advance(descriptionPaint, 6f)
            }
        }

        yPos += 30f

        // Draw horizontal line before totals
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        yPos = pager.flowTo(yPos, 150f)

        // Totals section (right aligned)
        val totalLabelPaint = paints.text(9f)

        val totalValuePaint = paints.text(9f, align = Paint.Align.RIGHT)

        val subtotal = invoice.subtotal
        val taxAmount = invoice.tax
        val discountAmount = invoice.discount
        val total = invoice.totalAmount

        val totalsX = pageWidth - margin - 180f

        pager.canvas.drawText("TOTAL AMOUNT", totalsX, yPos, totalLabelPaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", subtotal)}",
            pageWidth - margin, yPos, totalValuePaint)
        yPos += paints.advance(totalLabelPaint, 15f)

        if (taxAmount > 0) {
            val taxPercentage = if (subtotal > 0) (taxAmount / subtotal) * 100.0 else 0.0
            pager.canvas.drawText("VAT (${String.format("%.0f", taxPercentage)}%)",
                totalsX, yPos, totalLabelPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", taxAmount)}",
                pageWidth - margin, yPos, totalValuePaint)
            yPos += 15f
        }

        if (discountAmount > 0) {
            pager.canvas.drawText("DISCOUNT", totalsX, yPos, totalLabelPaint)
            pager.canvas.drawText("-$currencySymbol${String.format("%.2f", discountAmount)}",
                pageWidth - margin, yPos, totalValuePaint)
            yPos += paints.advance(totalLabelPaint, 15f)
        }

        val amountDuePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)

        val amountDueValuePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.RIGHT)

        pager.canvas.drawText("AMOUNT DUE", totalsX, yPos, amountDuePaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", total)}",
            pageWidth - margin, yPos, amountDueValuePaint)
        yPos += paints.advance(amountDuePaint, 30f)

        // Payment Information section
        if (paymentInfo.bankName.isNotEmpty()) {
            pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += 20f

            pager.canvas.drawText("{ PAYMENT INFORMATION }", margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 15f)

            if (paymentInfo.bankName.isNotEmpty()) {
                pager.canvas.drawText("Bank: ${paymentInfo.bankName}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText("Account Holder: ${paymentInfo.accountHolderName}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText("Account Number: ${paymentInfo.accountNumber}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText("Routing Number: ${paymentInfo.routingNumber}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText("IBAN: ${paymentInfo.iban}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText("Bank Address: ${paymentInfo.bankAddress}", margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText("Payment Terms: ${paymentInfo.paymentTerms}", margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText("Additional Instructions: ${paymentInfo.additionalInstructions}", margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }

            // QR Code for payment
            paymentQrCodePath?.let { qrPath ->
                try {
                    val qrFile = File(qrPath)
                    if (qrFile.exists()) {
                        val qrBitmap = BitmapFactory.decodeFile(qrPath)
                        qrBitmap?.let {
                            val qrSize = 80f
                            val scaledQr = Bitmap.createScaledBitmap(
                                it,
                                qrSize.toInt(),
                                qrSize.toInt(),
                                true
                            )
                            pager.canvas.drawText("Scan to Pay:", margin, yPos, bodySmallPaint)
                            yPos += paints.advance(bodySmallPaint, 12f)
                            pager.canvas.drawBitmap(scaledQr, margin, yPos, null)
                            yPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    // Error handled silently
                }
            }
            yPos += 15f
        }

        // Business signature
        signaturePath?.let { path ->
            try {
                val signatureFile = File(path)
                if (signatureFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxSignatureHeight = 35f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val signatureHeight = maxSignatureHeight
                        val signatureWidth = signatureHeight * aspectRatio

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            signatureWidth.toInt(),
                            signatureHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 15f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = paints.text(14f)
                    pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                    yPos += paints.advance(signaturePaint, 20f)
                }
            }
        } ?: run {
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = paints.text(14f)
                pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                yPos += paints.advance(signaturePaint, 20f)
            }
        }

        // Move to bottom section
        // Se o conteúdo já chegou aqui, abre página nova em vez de sobrescrever.
        yPos = pager.reserveFooter(yPos, 100f)

        // Large "INVOICE" text at bottom left
        val invoiceBottomPaint = paints.text(48f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        pager.canvas.drawText("INVOICE", margin, yPos, invoiceBottomPaint)
        yPos += paints.advance(invoiceBottomPaint, 30f)

        // Thank you message
        val thankYouPaint = paints.text(8f, color = paints.style.muted, tracking = 0.1f)
        pager.canvas.drawText("THANK YOU FOR YOUR BUSINESS!", margin, yPos, thankYouPaint)
        yPos += paints.advance(thankYouPaint, 15f)

        // Contact information at bottom
        val contactPaint = paints.text(7f)

        if (businessInfo.phone.isNotEmpty()) {
            pager.canvas.drawText("HELPDESK: ${businessInfo.phone}", margin, yPos, contactPaint)
            yPos += paints.advance(contactPaint, 10f)
        }
        if (businessInfo.email.isNotEmpty()) {
            pager.canvas.drawText("E-MAIL: ${businessInfo.email}", margin, yPos, contactPaint)
            yPos += paints.advance(contactPaint, 10f)
        }
        if (businessInfo.website.isNotEmpty()) {
            pager.canvas.drawText("WEB: ${businessInfo.website}", margin, yPos, contactPaint)
        }
    }

    private fun drawProfessionalTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?
    ) {
        val currencySymbol = getCurrencySymbol(currency)
        var yPos = margin

        val bodyPaint = paints.text(9f)

        // TODO Fase C: o Professional usa DKGRAY aqui e GRAY na descrição do item;
        // normalizar muda pixel, então fica para quando a cor virar configurável.
        val bodySmallPaint = paints.text(8f, color = Color.DKGRAY)

        val labelPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold)

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Business info at top left
        if (businessInfo.businessName.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.businessName, margin, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 12f)
        }
        if (businessInfo.ownerName.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.ownerName, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.email.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.email, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.phone.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.phone, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.address.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.address, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
        if (cityStateZip.isNotEmpty()) {
            pager.canvas.drawText(cityStateZip, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.taxId.isNotEmpty()) {
            pager.canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }

        yPos += paints.advance(bodySmallPaint, 20f)

        // Large "INVOICE" title at center
        val titlePaint = paints.text(32f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.CENTER, tracking = 0.3f)
        pager.canvas.drawText("INVOICE", pageWidth / 2f, yPos, titlePaint)
        yPos += paints.advance(titlePaint, 50f)

        // Two columns: Issued to (left) and Invoice info (right)
        val leftX = margin
        val rightX = pageWidth - margin - 150f
        var leftYPos = yPos
        var rightYPos = yPos

        // Left column - Issued to
        pager.canvas.drawText("Issued to:", leftX, leftYPos, labelPaint)
        leftYPos += paints.advance(labelPaint, 12f)
        pager.canvas.drawText(client.name, leftX, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 11f)
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText("Notes: ${client.notes}", leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }

        // Right column - Invoice No and Dates
        val labelGap = paints.labelGap(
            labelPaint,
            listOf("Invoice No:", "Date Issued:", "Due Date:"),
            80f
        )
        pager.canvas.drawText("Invoice No:", rightX, rightYPos, labelPaint)
        pager.canvas.drawText("#${invoice.number}", rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 12f)

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        pager.canvas.drawText("Date Issued:", rightX, rightYPos, labelPaint)
        pager.canvas.drawText(formattedCreatedDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 12f)

        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)
        pager.canvas.drawText("Due Date:", rightX, rightYPos, labelPaint)
        pager.canvas.drawText(formattedDueDate, rightX + labelGap, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Table with black header
        val tableHeaderPaint = paints.fill(paints.style.accent)

        val tableHeaderTextPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, color = paints.style.onAccent)

        val tableBodyPaint = paints.text(9f)

        val headerHeight = maxOf(25f, paints.lineHeight(tableHeaderTextPaint) + 12f)
        val totalHeaderPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, color = paints.style.onAccent, align = Paint.Align.RIGHT)
        val drawTableHeader: (Float) -> Float = { start ->
            pager.canvas.drawRect(margin, start, pageWidth - margin, start + headerHeight, tableHeaderPaint)
            val headerY = start + 16f
            pager.canvas.drawText("Description", margin + 10f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText("Quantity", pageWidth - margin - 280f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText("Unit Price", pageWidth - margin - 180f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText("Total", pageWidth - margin - 10f, headerY, totalHeaderPaint)
            start + headerHeight + 15f
        }
        yPos = drawTableHeader(yPos)

        // Table rows
        val descriptionPaint = paints.text(8f, color = paints.style.muted)

        val totalPaint = paints.text(9f, align = Paint.Align.RIGHT)
        val professionalRowHeight = paints.advance(tableBodyPaint, 14f) + paints.advance(descriptionPaint, 12f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, professionalRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - (margin + 10f) - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, tableBodyPaint, descWidth),
                margin + 10f, yPos, tableBodyPaint
            )
            pager.canvas.drawText("${item.quantity}", pageWidth - margin - 280f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 180f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin - 10f, yPos, totalPaint)
            yPos += paints.advance(tableBodyPaint, 12f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin + 10f, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 14f)
            } else {
                yPos += paints.advance(descriptionPaint, 8f)
            }
        }

        yPos += 20f

        // Horizontal line separator
        val linePaint = paints.stroke(width = 1f)
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 30f

        // Bottom section: Payment info (left) and Totals (right).
        // O bloco inteiro migra de página se não couber.
        yPos = pager.flowTo(yPos, 200f)
        leftYPos = yPos
        rightYPos = yPos

        // Left - Payment Info
        if (paymentInfo.bankName.isNotEmpty()) {
            pager.canvas.drawText("PAYMENT INFO", leftX, leftYPos, labelPaint)
            leftYPos += paints.advance(labelPaint, 12f)
            if (paymentInfo.bankName.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankName, leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankAddress, leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText("Account Name: ${paymentInfo.accountHolderName}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText("Account No: ${paymentInfo.accountNumber}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText("Routing: ${paymentInfo.routingNumber}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText("IBAN: ${paymentInfo.iban}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText("Terms: ${paymentInfo.paymentTerms}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText("Notes: ${paymentInfo.additionalInstructions}", leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }

            // QR Code for payment
            paymentQrCodePath?.let { qrPath ->
                try {
                    val qrFile = File(qrPath)
                    if (qrFile.exists()) {
                        val qrBitmap = BitmapFactory.decodeFile(qrPath)
                        qrBitmap?.let {
                            val qrSize = 80f
                            val scaledQr = Bitmap.createScaledBitmap(
                                it,
                                qrSize.toInt(),
                                qrSize.toInt(),
                                true
                            )
                            leftYPos += 5f
                            pager.canvas.drawText("Scan to Pay:", leftX, leftYPos, labelPaint)
                            leftYPos += paints.advance(labelPaint, 12f)
                            pager.canvas.drawBitmap(scaledQr, leftX, leftYPos, null)
                            leftYPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    // Error handled silently
                }
            }
        }

        // Right - Totals
        val totalsLabelPaint = paints.text(9f)

        val totalsValuePaint = paints.text(9f, align = Paint.Align.RIGHT)

        val totalsX = pageWidth - margin - 150f

        pager.canvas.drawText("Subtotal:", totalsX, rightYPos, totalsLabelPaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.subtotal)}",
            pageWidth - margin - 10f, rightYPos, totalsValuePaint)
        rightYPos += paints.advance(totalsLabelPaint, 12f)

        if (invoice.tax > 0) {
            val taxPercentage = if (invoice.subtotal > 0) (invoice.tax / invoice.subtotal) * 100.0 else 0.0
            pager.canvas.drawText("Tax (${String.format("%.0f", taxPercentage)}%):", totalsX, rightYPos, totalsLabelPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.tax)}",
                pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += paints.advance(totalsLabelPaint, 12f)
        }

        if (invoice.discount > 0) {
            pager.canvas.drawText("Discount:", totalsX, rightYPos, totalsLabelPaint)
            pager.canvas.drawText("-$currencySymbol${String.format("%.2f", invoice.discount)}",
                pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += paints.advance(totalsLabelPaint, 12f)
        }

        val totalBoldPaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)

        val totalBoldValuePaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.RIGHT)

        pager.canvas.drawText("TOTAL:", totalsX, rightYPos, totalBoldPaint)
        pager.canvas.drawText("$currencySymbol${String.format("%.2f", invoice.totalAmount)}",
            pageWidth - margin - 10f, rightYPos, totalBoldValuePaint)

        // Signature
        yPos = maxOf(leftYPos, rightYPos) + 30f
        signaturePath?.let { path ->
            try {
                val signatureFile = File(path)
                if (signatureFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxSignatureHeight = 35f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val signatureHeight = maxSignatureHeight
                        val signatureWidth = signatureHeight * aspectRatio

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            signatureWidth.toInt(),
                            signatureHeight.toInt(),
                            true
                        )
                        pager.canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 15f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = paints.text(14f)
                    pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                    yPos += paints.advance(signaturePaint, 20f)
                }
            }
        } ?: run {
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = paints.text(14f)
                pager.canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                yPos += paints.advance(signaturePaint, 20f)
            }
        }

        // Website at bottom center
        yPos = pager.reserveFooter(yPos, 20f)
        val websitePaint = paints.text(8f, align = Paint.Align.CENTER)
        if (businessInfo.website.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.website, pageWidth / 2f, yPos, websitePaint)
        }
    }
}
