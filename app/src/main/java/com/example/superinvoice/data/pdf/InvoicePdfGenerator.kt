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
        isPremium: Boolean = true
    ): File? {
        return try {
            android.util.Log.d("InvoicePDF", "Generating PDF with template: $template")
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    android.util.Log.d("InvoicePDF", "Drawing CLASSIC template")
                    drawClassicTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.MODERN -> {
                    android.util.Log.d("InvoicePDF", "Drawing MODERN template")
                    drawModernTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    android.util.Log.d("InvoicePDF", "Drawing PROFESSIONAL template")
                    drawProfessionalTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
            }

            if (!isPremium) {
                drawWatermark(canvas)
            }

            pdfDocument.finishPage(page)

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
            e.printStackTrace()
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
        isPremium: Boolean = true
    ): Bitmap? {
        return try {
            // Create PDF document in memory
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    drawClassicTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.MODERN -> {
                    drawModernTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    drawProfessionalTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath)
                }
            }

            if (!isPremium) {
                drawWatermark(canvas)
            }

            pdfDocument.finishPage(page)

            // Save to temporary file
            val tempFile = File.createTempFile("invoice_preview_", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Render PDF to Bitmap with configurable resolution
            val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page0 = pdfRenderer.openPage(0)

            // Create bitmap with the specified scale
            val bitmap = Bitmap.createBitmap(
                pageWidth * scale,
                pageHeight * scale,
                Bitmap.Config.ARGB_8888
            )

            // Fill with white background
            val bitmapCanvas = Canvas(bitmap)
            bitmapCanvas.drawColor(Color.WHITE)

            // Render PDF page to bitmap at high resolution
            page0.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page0.close()
            pdfRenderer.close()
            fileDescriptor.close()

            // Clean up temp file
            tempFile.delete()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawWatermark(canvas: Canvas) {
        val watermarkPaint = Paint().apply {
            color = Color.GRAY
            alpha = 38 // ~15% opacity (255 * 0.15)
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.save()
        canvas.rotate(-45f, pageWidth / 2f, pageHeight / 2f)
        canvas.drawText("SUPERINVOICE", pageWidth / 2f, pageHeight / 2f, watermarkPaint)
        canvas.restore()
    }

    private fun drawClassicTemplate(
        canvas: Canvas,
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
                        val logoHeight = maxLogoHeight
                        val logoWidth = logoHeight * aspectRatio

                        // Draw logo on the left
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Title with line
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.15f
        }

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }

        // Draw line
        canvas.drawLine(margin, yPos + 15, pageWidth - 200f, yPos + 15, linePaint)

        // Draw INVOICE text
        canvas.drawText("INVOICE", pageWidth - 180f, yPos + 20, titlePaint)
        yPos += 60f

        // Section: ISSUED TO and Invoice Info
        val sectionTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Left column - FROM (Business Information)
        val leftYStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            canvas.drawText("FROM:", margin, leftYPos, sectionTitlePaint)
            leftYPos += 18f
            canvas.drawText(businessInfo.businessName, margin, leftYPos, bodyPaint)
            leftYPos += 15f
            if (businessInfo.ownerName.isNotEmpty()) {
                canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.email.isNotEmpty()) {
                canvas.drawText(businessInfo.email, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.phone.isNotEmpty()) {
                canvas.drawText(businessInfo.phone, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.website.isNotEmpty()) {
                canvas.drawText(businessInfo.website, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.address.isNotEmpty()) {
                canvas.drawText(businessInfo.address, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            val cityStateZip = buildString {
                if (businessInfo.city.isNotEmpty()) append(businessInfo.city)
                if (businessInfo.state.isNotEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(businessInfo.state)
                }
                if (businessInfo.zipCode.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(businessInfo.zipCode)
                }
            }
            if (cityStateZip.isNotEmpty()) {
                canvas.drawText(cityStateZip, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.taxId.isNotEmpty()) {
                canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
        }

        // Right column - INVOICE INFO
        var rightYPos = yPos
        val rightX = pageWidth - margin - 150f

        canvas.drawText("INVOICE NO:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(invoice.number, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        // Format createdDate (Long timestamp) for DATE
        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))

        // Reformat dueDate (String) for DUE DATE
        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)

        canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(formattedCreatedDate, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(formattedDueDate, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        // Advance to the max height of both columns
        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Second row: ISSUED TO (left) and PAY TO (right)
        leftYPos = yPos
        rightYPos = yPos

        // ISSUED TO (left)
        canvas.drawText("ISSUED TO:", margin, leftYPos, sectionTitlePaint)
        leftYPos += 18f
        canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += 15f
        if (client.email.isNotEmpty()) {
            canvas.drawText(client.email, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }
        if (client.phone.isNotEmpty()) {
            canvas.drawText(client.phone, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }
        if (client.address.isNotEmpty()) {
            canvas.drawText(client.address, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }
        val clientCityStateZip = buildString {
            if (client.city.isNotEmpty()) append(client.city)
            if (client.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(client.state)
            }
            if (client.zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(client.zipCode)
            }
        }
        if (clientCityStateZip.isNotEmpty()) {
            canvas.drawText(clientCityStateZip, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }
        if (client.notes.isNotEmpty()) {
            canvas.drawText("Notes: ${client.notes}", margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }

        // PAY TO (right)
        val payRightX = pageWidth / 2f + 20f
        if (paymentInfo.bankName.isNotEmpty()) {
            canvas.drawText("PAY TO:", payRightX, rightYPos, sectionTitlePaint)
            rightYPos += 18f
            canvas.drawText(paymentInfo.bankName, payRightX, rightYPos, bodyPaint)
            rightYPos += 15f
            if (paymentInfo.bankAddress.isNotEmpty()) {
                canvas.drawText(paymentInfo.bankAddress, payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                canvas.drawText("Acc Name: ${paymentInfo.accountHolderName}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                canvas.drawText("Acc Number: ${paymentInfo.accountNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                canvas.drawText("Routing: ${paymentInfo.routingNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.iban.isNotEmpty()) {
                canvas.drawText("IBAN: ${paymentInfo.iban}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                canvas.drawText("Terms: ${paymentInfo.paymentTerms}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                canvas.drawText("Notes: ${paymentInfo.additionalInstructions}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
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
                            canvas.drawText("Scan to Pay:", payRightX, rightYPos, sectionTitlePaint)
                            rightYPos += 12f
                            canvas.drawBitmap(scaledQr, payRightX, rightYPos, null)
                            rightYPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Advance to max height
        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Divider line before table
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // Table Header
        canvas.drawText("DESCRIPTION", margin, yPos, sectionTitlePaint)
        canvas.drawText("UNIT PRICE", pageWidth - margin - 280f, yPos, sectionTitlePaint)
        canvas.drawText("QTY", pageWidth - margin - 150f, yPos, sectionTitlePaint)
        canvas.drawText("TOTAL", pageWidth - margin - 80f, yPos, sectionTitlePaint)

        yPos += 20f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 15f

        // Table Items
        val currencySymbol = getCurrencySymbol(currency)
        val descriptionPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        items.forEach { item ->
            canvas.drawText(item.productServiceName, margin, yPos, bodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}", pageWidth - margin - 280f, yPos, bodyPaint)
            canvas.drawText("${item.quantity}", pageWidth - margin - 150f, yPos, bodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}", pageWidth - margin - 80f, yPos, bodyPaint)
            yPos += 14f
            if (item.productServiceDescription.isNotEmpty()) {
                canvas.drawText(item.productServiceDescription, margin, yPos, descriptionPaint)
                yPos += 14f
            } else {
                yPos += 6f
            }
        }

        yPos += 20f

        // Totals
        val totalX = pageWidth - margin - 180f
        val totalValueX = pageWidth - margin - 80f

        canvas.drawText("SUBTOTAL", totalX, yPos, sectionTitlePaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.subtotal)}", totalValueX, yPos, bodyPaint)
        yPos += 15f

        if (invoice.tax > 0) {
            canvas.drawText("TAX", totalX, yPos, sectionTitlePaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", invoice.tax)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        if (invoice.discount > 0) {
            canvas.drawText("DISCOUNT", totalX, yPos, sectionTitlePaint)
            canvas.drawText("-$currencySymbol${String.format("%.2f", invoice.discount)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        val totalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("TOTAL", totalX, yPos, totalPaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.totalAmount)}", totalValueX, yPos, totalPaint)
        yPos += 40f

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
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to text signature if image fails
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                }
            }
        } ?: run {
            // No signature image, use text signature if owner name exists
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
            }
        }
    }

    private fun drawModernTemplate(
        canvas: Canvas,
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

        val sectionTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bodySmallPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val logoHeight = maxLogoHeight
                        val logoWidth = logoHeight * aspectRatio

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Business Information (left) and Invoice # + Dates (right)
        val topSectionStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            val businessNamePaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(businessInfo.businessName, margin, leftYPos, businessNamePaint)
            leftYPos += 13f

            if (businessInfo.ownerName.isNotEmpty()) {
                canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodySmallPaint)
                leftYPos += 11f
            }
            if (businessInfo.address.isNotEmpty()) {
                canvas.drawText(businessInfo.address, margin, leftYPos, bodySmallPaint)
                leftYPos += 11f
            }
            val cityStateZip = buildString {
                if (businessInfo.city.isNotEmpty()) append(businessInfo.city)
                if (businessInfo.state.isNotEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(businessInfo.state)
                }
                if (businessInfo.zipCode.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(businessInfo.zipCode)
                }
            }
            if (cityStateZip.isNotEmpty()) {
                canvas.drawText(cityStateZip, margin, leftYPos, bodySmallPaint)
                leftYPos += 11f
            }
            if (businessInfo.taxId.isNotEmpty()) {
                canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, leftYPos, bodySmallPaint)
                leftYPos += 11f
            }
        }

        // Right side - Invoice number and dates
        var rightYPos = topSectionStart
        val rightX = pageWidth - margin - 120f

        val invoiceTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("INVOICE #${invoice.number}", rightX, rightYPos, invoiceTitlePaint)
        rightYPos += 15f

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)

        canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        rightYPos += 12f
        canvas.drawText(formattedCreatedDate, rightX, rightYPos, bodyPaint)
        rightYPos += 15f

        canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        rightYPos += 12f
        canvas.drawText(formattedDueDate, rightX, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Draw horizontal line separator
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // BILLED TO section
        leftYPos = yPos
        canvas.drawText("{ BILLED TO }", margin, leftYPos, bodySmallPaint)
        leftYPos += 15f
        canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += 12f
        if (client.phone.isNotEmpty()) {
            canvas.drawText(client.phone, margin, leftYPos, bodySmallPaint)
            leftYPos += 12f
        }
        if (client.email.isNotEmpty()) {
            canvas.drawText(client.email, margin, leftYPos, bodySmallPaint)
            leftYPos += 12f
        }
        if (client.address.isNotEmpty()) {
            canvas.drawText(client.address, margin, leftYPos, bodySmallPaint)
            leftYPos += 12f
        }
        val clientCityStateZip = buildString {
            if (client.city.isNotEmpty()) append(client.city)
            if (client.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(client.state)
            }
            if (client.zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(client.zipCode)
            }
        }
        if (clientCityStateZip.isNotEmpty()) {
            canvas.drawText(clientCityStateZip, margin, leftYPos, bodySmallPaint)
            leftYPos += 12f
        }
        if (client.notes.isNotEmpty()) {
            canvas.drawText("Notes: ${client.notes}", margin, leftYPos, bodySmallPaint)
            leftYPos += 12f
        }

        yPos = leftYPos + 20f

        // Draw horizontal line separator
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        // Table Header
        val tableTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }

        canvas.drawText("DESCRIPTION", margin, yPos, tableTitlePaint)
        canvas.drawText("RATE", pageWidth - margin - 280f, yPos, tableTitlePaint)
        canvas.drawText("QTY", pageWidth - margin - 200f, yPos, tableTitlePaint)

        val pricePaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.1f
        }
        canvas.drawText("PRICE", pageWidth - margin, yPos, pricePaint)
        yPos += 20f

        // Table rows
        val tableBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val priceBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val descriptionPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        items.forEach { item ->
            canvas.drawText(item.productServiceName, margin, yPos, tableBodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 280f, yPos, tableBodyPaint)
            canvas.drawText("${item.quantity}", pageWidth - margin - 200f, yPos, tableBodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin, yPos, priceBodyPaint)
            yPos += 12f
            if (item.productServiceDescription.isNotEmpty()) {
                canvas.drawText(item.productServiceDescription, margin, yPos, descriptionPaint)
                yPos += 12f
            } else {
                yPos += 6f
            }
        }

        yPos += 30f

        // Draw horizontal line before totals
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        // Totals section (right aligned)
        val totalLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val totalValuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val subtotal = invoice.subtotal
        val taxAmount = invoice.tax
        val discountAmount = invoice.discount
        val total = invoice.totalAmount

        val totalsX = pageWidth - margin - 180f

        canvas.drawText("TOTAL AMOUNT", totalsX, yPos, totalLabelPaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", subtotal)}",
            pageWidth - margin, yPos, totalValuePaint)
        yPos += 15f

        if (taxAmount > 0) {
            val taxPercentage = if (subtotal > 0) (taxAmount / subtotal) * 100.0 else 0.0
            canvas.drawText("VAT (${String.format("%.0f", taxPercentage)}%)",
                totalsX, yPos, totalLabelPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", taxAmount)}",
                pageWidth - margin, yPos, totalValuePaint)
            yPos += 15f
        }

        if (discountAmount > 0) {
            canvas.drawText("DISCOUNT", totalsX, yPos, totalLabelPaint)
            canvas.drawText("-$currencySymbol${String.format("%.2f", discountAmount)}",
                pageWidth - margin, yPos, totalValuePaint)
            yPos += 15f
        }

        val amountDuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val amountDueValuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("AMOUNT DUE", totalsX, yPos, amountDuePaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", total)}",
            pageWidth - margin, yPos, amountDueValuePaint)
        yPos += 30f

        // Payment Information section
        if (paymentInfo.bankName.isNotEmpty()) {
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += 20f

            canvas.drawText("{ PAYMENT INFORMATION }", margin, yPos, bodySmallPaint)
            yPos += 15f

            if (paymentInfo.bankName.isNotEmpty()) {
                canvas.drawText("Bank: ${paymentInfo.bankName}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                canvas.drawText("Account Holder: ${paymentInfo.accountHolderName}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                canvas.drawText("Account Number: ${paymentInfo.accountNumber}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                canvas.drawText("Routing Number: ${paymentInfo.routingNumber}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.iban.isNotEmpty()) {
                canvas.drawText("IBAN: ${paymentInfo.iban}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", margin, yPos, bodyPaint)
                yPos += 12f
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                canvas.drawText("Bank Address: ${paymentInfo.bankAddress}", margin, yPos, bodySmallPaint)
                yPos += 12f
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                canvas.drawText("Payment Terms: ${paymentInfo.paymentTerms}", margin, yPos, bodySmallPaint)
                yPos += 12f
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                canvas.drawText("Additional Instructions: ${paymentInfo.additionalInstructions}", margin, yPos, bodySmallPaint)
                yPos += 12f
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
                            canvas.drawText("Scan to Pay:", margin, yPos, bodySmallPaint)
                            yPos += 12f
                            canvas.drawBitmap(scaledQr, margin, yPos, null)
                            yPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 15f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                    yPos += 20f
                }
            }
        } ?: run {
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                yPos += 20f
            }
        }

        // Move to bottom section
        yPos = pageHeight - margin - 100f

        // Large "INVOICE" text at bottom left
        val invoiceBottomPaint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("INVOICE", margin, yPos, invoiceBottomPaint)
        yPos += 30f

        // Thank you message
        val thankYouPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.1f
        }
        canvas.drawText("THANK YOU FOR YOUR BUSINESS!", margin, yPos, thankYouPaint)
        yPos += 15f

        // Contact information at bottom
        val contactPaint = Paint().apply {
            color = Color.BLACK
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        if (businessInfo.phone.isNotEmpty()) {
            canvas.drawText("HELPDESK: ${businessInfo.phone}", margin, yPos, contactPaint)
            yPos += 10f
        }
        if (businessInfo.email.isNotEmpty()) {
            canvas.drawText("E-MAIL: ${businessInfo.email}", margin, yPos, contactPaint)
            yPos += 10f
        }
        if (businessInfo.website.isNotEmpty()) {
            canvas.drawText("WEB: ${businessInfo.website}", margin, yPos, contactPaint)
        }
    }

    private fun drawProfessionalTemplate(
        canvas: Canvas,
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

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val bodySmallPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val logoHeight = maxLogoHeight
                        val logoWidth = logoHeight * aspectRatio

                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Business info at top left
        if (businessInfo.businessName.isNotEmpty()) {
            canvas.drawText(businessInfo.businessName, margin, yPos, bodyPaint)
            yPos += 12f
        }
        if (businessInfo.ownerName.isNotEmpty()) {
            canvas.drawText(businessInfo.ownerName, margin, yPos, bodySmallPaint)
            yPos += 10f
        }
        if (businessInfo.email.isNotEmpty()) {
            canvas.drawText(businessInfo.email, margin, yPos, bodySmallPaint)
            yPos += 10f
        }
        if (businessInfo.phone.isNotEmpty()) {
            canvas.drawText(businessInfo.phone, margin, yPos, bodySmallPaint)
            yPos += 10f
        }
        if (businessInfo.address.isNotEmpty()) {
            canvas.drawText(businessInfo.address, margin, yPos, bodySmallPaint)
            yPos += 10f
        }
        val cityStateZip = buildString {
            if (businessInfo.city.isNotEmpty()) append(businessInfo.city)
            if (businessInfo.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(businessInfo.state)
            }
            if (businessInfo.zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(businessInfo.zipCode)
            }
        }
        if (cityStateZip.isNotEmpty()) {
            canvas.drawText(cityStateZip, margin, yPos, bodySmallPaint)
            yPos += 10f
        }
        if (businessInfo.taxId.isNotEmpty()) {
            canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, yPos, bodySmallPaint)
            yPos += 10f
        }

        yPos += 20f

        // Large "INVOICE" title at center
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.3f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("INVOICE", pageWidth / 2f, yPos, titlePaint)
        yPos += 50f

        // Two columns: Issued to (left) and Invoice info (right)
        val leftX = margin
        val rightX = pageWidth - margin - 150f
        var leftYPos = yPos
        var rightYPos = yPos

        // Left column - Issued to
        canvas.drawText("Issued to:", leftX, leftYPos, labelPaint)
        leftYPos += 12f
        canvas.drawText(client.name, leftX, leftYPos, bodyPaint)
        leftYPos += 11f
        if (client.email.isNotEmpty()) {
            canvas.drawText(client.email, leftX, leftYPos, bodySmallPaint)
            leftYPos += 11f
        }
        if (client.phone.isNotEmpty()) {
            canvas.drawText(client.phone, leftX, leftYPos, bodySmallPaint)
            leftYPos += 11f
        }
        if (client.address.isNotEmpty()) {
            canvas.drawText(client.address, leftX, leftYPos, bodySmallPaint)
            leftYPos += 11f
        }
        val clientCityStateZip = buildString {
            if (client.city.isNotEmpty()) append(client.city)
            if (client.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(client.state)
            }
            if (client.zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(client.zipCode)
            }
        }
        if (clientCityStateZip.isNotEmpty()) {
            canvas.drawText(clientCityStateZip, leftX, leftYPos, bodySmallPaint)
            leftYPos += 11f
        }
        if (client.notes.isNotEmpty()) {
            canvas.drawText("Notes: ${client.notes}", leftX, leftYPos, bodySmallPaint)
            leftYPos += 11f
        }

        // Right column - Invoice No and Dates
        canvas.drawText("Invoice No:", rightX, rightYPos, labelPaint)
        canvas.drawText("#${invoice.number}", rightX + 80f, rightYPos, bodyPaint)
        rightYPos += 12f

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        canvas.drawText("Date Issued:", rightX, rightYPos, labelPaint)
        canvas.drawText(formattedCreatedDate, rightX + 80f, rightYPos, bodyPaint)
        rightYPos += 12f

        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)
        canvas.drawText("Due Date:", rightX, rightYPos, labelPaint)
        canvas.drawText(formattedDueDate, rightX + 80f, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Table with black header
        val tableHeaderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tableBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Draw black header background
        val headerHeight = 25f
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + headerHeight, tableHeaderPaint)

        // Draw header text
        val headerY = yPos + 16f
        canvas.drawText("Description", margin + 10f, headerY, tableHeaderTextPaint)
        canvas.drawText("Quantity", pageWidth - margin - 280f, headerY, tableHeaderTextPaint)
        canvas.drawText("Unit Price", pageWidth - margin - 180f, headerY, tableHeaderTextPaint)

        val totalHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Total", pageWidth - margin - 10f, headerY, totalHeaderPaint)

        yPos += headerHeight + 15f

        // Table rows
        val descriptionPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        items.forEach { item ->
            canvas.drawText(item.productServiceName, margin + 10f, yPos, tableBodyPaint)
            canvas.drawText("${item.quantity}", pageWidth - margin - 280f, yPos, tableBodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 180f, yPos, tableBodyPaint)

            val totalPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin - 10f, yPos, totalPaint)
            yPos += 12f
            if (item.productServiceDescription.isNotEmpty()) {
                canvas.drawText(item.productServiceDescription, margin + 10f, yPos, descriptionPaint)
                yPos += 14f
            } else {
                yPos += 8f
            }
        }

        yPos += 20f

        // Horizontal line separator
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 30f

        // Bottom section: Payment info (left) and Totals (right)
        leftYPos = yPos
        rightYPos = yPos

        // Left - Payment Info
        if (paymentInfo.bankName.isNotEmpty()) {
            canvas.drawText("PAYMENT INFO", leftX, leftYPos, labelPaint)
            leftYPos += 12f
            if (paymentInfo.bankName.isNotEmpty()) {
                canvas.drawText(paymentInfo.bankName, leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                canvas.drawText(paymentInfo.bankAddress, leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                canvas.drawText("Account Name: ${paymentInfo.accountHolderName}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                canvas.drawText("Account No: ${paymentInfo.accountNumber}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                canvas.drawText("Routing: ${paymentInfo.routingNumber}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.iban.isNotEmpty()) {
                canvas.drawText("IBAN: ${paymentInfo.iban}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                canvas.drawText("Terms: ${paymentInfo.paymentTerms}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                canvas.drawText("Notes: ${paymentInfo.additionalInstructions}", leftX, leftYPos, bodySmallPaint)
                leftYPos += 10f
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
                            canvas.drawText("Scan to Pay:", leftX, leftYPos, labelPaint)
                            leftYPos += 12f
                            canvas.drawBitmap(scaledQr, leftX, leftYPos, null)
                            leftYPos += qrSize + 10f
                            scaledQr.recycle()
                            it.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Right - Totals
        val totalsLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val totalsValuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val totalsX = pageWidth - margin - 150f

        canvas.drawText("Subtotal:", totalsX, rightYPos, totalsLabelPaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.subtotal)}",
            pageWidth - margin - 10f, rightYPos, totalsValuePaint)
        rightYPos += 12f

        if (invoice.tax > 0) {
            val taxPercentage = if (invoice.subtotal > 0) (invoice.tax / invoice.subtotal) * 100.0 else 0.0
            canvas.drawText("Tax (${String.format("%.0f", taxPercentage)}%):", totalsX, rightYPos, totalsLabelPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", invoice.tax)}",
                pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += 12f
        }

        if (invoice.discount > 0) {
            canvas.drawText("Discount:", totalsX, rightYPos, totalsLabelPaint)
            canvas.drawText("-$currencySymbol${String.format("%.2f", invoice.discount)}",
                pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += 12f
        }

        val totalBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val totalBoldValuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL:", totalsX, rightYPos, totalBoldPaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.totalAmount)}",
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
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 15f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                    yPos += 20f
                }
            }
        } ?: run {
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                yPos += 20f
            }
        }

        // Website at bottom center
        yPos = pageHeight - margin - 20f
        val websitePaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        if (businessInfo.website.isNotEmpty()) {
            canvas.drawText(businessInfo.website, pageWidth / 2f, yPos, websitePaint)
        }
    }
}
