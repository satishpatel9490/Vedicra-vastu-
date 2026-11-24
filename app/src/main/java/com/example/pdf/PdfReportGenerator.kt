package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.example.R
import com.example.data.local.AnalysisRowEntity
import com.example.data.local.ExportedPdfEntity
import com.example.data.local.FullReportBundle
import com.example.data.local.JsonCodec
import com.example.data.local.MediaFileManager
import com.example.data.local.SiteMediaEntity
import com.example.model.AppSettings
import com.example.model.MediaKind
import com.example.model.PdfThemeOption
import com.example.model.VastuConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface PdfPageHandle {
    val canvas: Canvas
}

interface PdfDocumentAdapter {
    fun startPage(width: Int, height: Int, pageNumber: Int): PdfPageHandle
    fun finishPage(page: PdfPageHandle)
    fun writeTo(outputStream: OutputStream)
    fun close()
}

private class AndroidPdfDocumentAdapter : PdfDocumentAdapter {
    private val doc = PdfDocument()

    private class AndroidPageHandle(val page: PdfDocument.Page) : PdfPageHandle {
        override val canvas: Canvas get() = page.canvas
    }

    override fun startPage(width: Int, height: Int, pageNumber: Int): PdfPageHandle {
        val info = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        return AndroidPageHandle(doc.startPage(info))
    }

    override fun finishPage(page: PdfPageHandle) {
        doc.finishPage((page as AndroidPageHandle).page)
    }

    override fun writeTo(outputStream: OutputStream) {
        doc.writeTo(outputStream)
    }

    override fun close() {
        doc.close()
    }
}

class PdfReportGenerator(
    private val context: Context,
    private val mediaManager: MediaFileManager,
    private val documentFactory: () -> PdfDocumentAdapter = { AndroidPdfDocumentAdapter() }
) {
    private data class PdfPalette(
        val primary: Int,
        val accent: Int,
        val headerBg: Int,
        val tableHeaderBg: Int,
        val tableAltRowBg: Int,
        val cardBg: Int,
        val border: Int,
        val textPrimary: Int,
        val textSecondary: Int
    )

    private fun paletteFor(theme: PdfThemeOption): PdfPalette = when (theme) {
        PdfThemeOption.CLASSIC_IVORY_GOLD -> PdfPalette(
            primary = Color.rgb(27, 77, 62),
            accent = Color.rgb(166, 123, 34),
            headerBg = Color.rgb(250, 246, 238),
            tableHeaderBg = Color.rgb(27, 77, 62),
            tableAltRowBg = Color.rgb(249, 246, 239),
            cardBg = Color.rgb(253, 251, 246),
            border = Color.rgb(206, 197, 180),
            textPrimary = Color.rgb(28, 25, 21),
            textSecondary = Color.rgb(88, 78, 64)
        )
        PdfThemeOption.SAGE_BOTANICAL -> PdfPalette(
            primary = Color.rgb(37, 83, 62),
            accent = Color.rgb(122, 149, 108),
            headerBg = Color.rgb(238, 243, 238),
            tableHeaderBg = Color.rgb(37, 83, 62),
            tableAltRowBg = Color.rgb(243, 247, 243),
            cardBg = Color.rgb(250, 252, 249),
            border = Color.rgb(190, 207, 194),
            textPrimary = Color.rgb(20, 33, 25),
            textSecondary = Color.rgb(72, 92, 79)
        )
        PdfThemeOption.MINIMALIST_SLATE -> PdfPalette(
            primary = Color.rgb(34, 48, 58),
            accent = Color.rgb(140, 114, 58),
            headerBg = Color.rgb(244, 246, 248),
            tableHeaderBg = Color.rgb(34, 48, 58),
            tableAltRowBg = Color.rgb(246, 248, 250),
            cardBg = Color.rgb(252, 253, 254),
            border = Color.rgb(205, 212, 218),
            textPrimary = Color.rgb(24, 29, 34),
            textSecondary = Color.rgb(88, 98, 108)
        )
        PdfThemeOption.HIGH_CONTRAST_PRINT -> PdfPalette(
            primary = Color.rgb(15, 15, 15),
            accent = Color.rgb(85, 85, 85),
            headerBg = Color.rgb(245, 245, 245),
            tableHeaderBg = Color.rgb(25, 25, 25),
            tableAltRowBg = Color.rgb(247, 247, 247),
            cardBg = Color.rgb(255, 255, 255),
            border = Color.rgb(180, 180, 180),
            textPrimary = Color.rgb(0, 0, 0),
            textSecondary = Color.rgb(65, 65, 65)
        )
    }

    suspend fun generateReportPdf(
        bundle: FullReportBundle,
        settings: AppSettings
    ): Result<ExportedPdfEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val report = bundle.report
            val palette = paletteFor(settings.pdfTheme)
            val fontScale = settings.defaultFontSize.pdfScale

            val pageWidth = 595
            val pageHeight = 842
            val marginX = 34f
            val contentWidth = pageWidth - marginX * 2f
            val topMargin = 34f
            val bottomFooterReserve = 42f
            val maxY = pageHeight - bottomFooterReserve

            val pdfDoc = documentFactory()
            var pageNumber = 0
            var currentPage: PdfPageHandle? = null
            var canvas: Canvas? = null
            var curY = topMargin

            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val fileDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primary
                textSize = 16f * fontScale
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val subTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accent
                textSize = 9.5f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.06f
            }
            val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10.5f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = 8.6f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = 9.2f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = 8f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val tableHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.4f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val tableCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = 7.5f * fontScale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.border
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            fun finishCurrentPage() {
                currentPage?.let { page ->
                    val c = page.canvas
                    // Draw bottom page line & page number
                    c.drawLine(marginX, pageHeight - 30f, pageWidth - marginX, pageHeight - 30f, linePaint)
                    val footerLeft = TextPaint(smallPaint).apply { textSize = 7.5f }
                    c.drawText(
                        "${settings.companyName}  •  Report ${report.reportNumber}",
                        marginX,
                        pageHeight - 17f,
                        footerLeft
                    )
                    val pageText = "Page $pageNumber"
                    val pw = footerLeft.measureText(pageText)
                    c.drawText(pageText, pageWidth - marginX - pw, pageHeight - 17f, footerLeft)
                    pdfDoc.finishPage(page)
                }
                currentPage = null
                canvas = null
            }

            fun startNewPage(isFirstPage: Boolean = false) {
                finishCurrentPage()
                pageNumber += 1
                val page = pdfDoc.startPage(pageWidth, pageHeight, pageNumber)
                currentPage = page
                val c = page.canvas
                canvas = c
                curY = topMargin

                if (isFirstPage) {
                    // Header Banner Card
                    val headerH = 74f
                    fillPaint.color = palette.headerBg
                    c.drawRoundRect(
                        RectF(marginX, curY, pageWidth - marginX, curY + headerH),
                        8f,
                        8f,
                        fillPaint
                    )
                    linePaint.color = palette.accent
                    linePaint.strokeWidth = 1.2f
                    c.drawRoundRect(
                        RectF(marginX, curY, pageWidth - marginX, curY + headerH),
                        8f,
                        8f,
                        linePaint
                    )
                    linePaint.color = palette.border
                    linePaint.strokeWidth = 0.8f

                    // Company Logo (clipped circularly with accent ring)
                    val logoBitmap = loadBrandLogo(settings)
                    var textStartX = marginX + 12f
                    if (logoBitmap != null) {
                        val logoSize = 52f
                        val logoTop = curY + (headerH - logoSize) / 2f
                        val logoRect = RectF(marginX + 12f, logoTop, marginX + 12f + logoSize, logoTop + logoSize)
                        val clipPath = Path().apply { addOval(logoRect, Path.Direction.CW) }
                        c.withClip(clipPath) {
                            drawBitmap(logoBitmap, null, logoRect, null)
                        }
                        linePaint.color = palette.accent
                        linePaint.strokeWidth = 1.2f
                        c.drawOval(logoRect, linePaint)
                        linePaint.color = palette.border
                        linePaint.strokeWidth = 0.8f
                        logoBitmap.recycle()
                        textStartX = marginX + 12f + logoSize + 12f
                    }

                    c.drawText(settings.companyName.uppercase(Locale.getDefault()), textStartX, curY + 24f, titlePaint)
                    c.drawText(
                        "PROFESSIONAL VASTU INSPECTION & CONSULTANCY REPORT",
                        textStartX,
                        curY + 40f,
                        subTitlePaint
                    )
                    val metaLine = "Inspection Date: ${dateFormatter.format(Date(report.inspectionDate))}   |   Report No: ${report.reportNumber}"
                    c.drawText(metaLine, textStartX, curY + 57f, smallPaint)
                    curY += headerH + 14f
                } else {
                    // Subtle running header on subsequent pages
                    val runPaint = TextPaint(smallPaint).apply {
                        color = palette.primary
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    val clientLabel = report.clientName.ifBlank { "Client Inspection" }
                    c.drawText("${settings.companyName} — $clientLabel", marginX, curY + 10f, runPaint)
                    val dateStr = dateFormatter.format(Date(report.inspectionDate))
                    c.drawText(dateStr, pageWidth - marginX - smallPaint.measureText(dateStr), curY + 10f, smallPaint)
                    c.drawLine(marginX, curY + 15f, pageWidth - marginX, curY + 15f, linePaint)
                    curY += 24f
                }
            }

            fun ensureSpace(neededHeight: Float) {
                if (canvas == null || curY + neededHeight > maxY) {
                    startNewPage(isFirstPage = false)
                }
            }

            fun buildLayout(text: String, paint: TextPaint, width: Float): StaticLayout {
                return StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length,
                    paint,
                    width.toInt().coerceAtLeast(10)
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.05f)
                    .setIncludePad(false)
                    .build()
            }

            fun drawSectionTitle(title: String) {
                ensureSpace(32f)
                val c = canvas!!
                val barH = 21f
                fillPaint.color = palette.primary
                c.drawRoundRect(
                    RectF(marginX, curY, pageWidth - marginX, curY + barH),
                    4f,
                    4f,
                    fillPaint
                )
                c.drawText(title, marginX + 10f, curY + 14.5f, sectionHeaderPaint)
                curY += barH + 8f
            }

            fun drawKeyValuePairs(pairs: List<Pair<String, String>>) {
                val colW = (contentWidth - 12f) / 2f
                val filtered = pairs.map { it.first to it.second.ifBlank { "—" } }
                var idx = 0
                while (idx < filtered.size) {
                    val leftPair = filtered[idx]
                    val rightPair = filtered.getOrNull(idx + 1)

                    val leftValLayout = buildLayout(leftPair.second, bodyPaint, colW - 8f)
                    val rightValLayout = rightPair?.let { buildLayout(it.second, bodyPaint, colW - 8f) }

                    val rowH = maxOf(
                        13f + leftValLayout.height,
                        rightValLayout?.let { 13f + it.height } ?: 0f
                    ) + 8f
                    ensureSpace(rowH + 4f)
                    val c = canvas!!

                    fillPaint.color = palette.cardBg
                    c.drawRoundRect(RectF(marginX, curY, marginX + colW, curY + rowH - 2f), 4f, 4f, fillPaint)
                    c.drawRoundRect(RectF(marginX, curY, marginX + colW, curY + rowH - 2f), 4f, 4f, linePaint)
                    c.drawText(leftPair.first, marginX + 6f, curY + 11f, labelPaint)
                    c.withTranslation(marginX + 6f, curY + 14f) {
                        leftValLayout.draw(this)
                    }

                    if (rightPair != null && rightValLayout != null) {
                        val rx = marginX + colW + 12f
                        c.drawRoundRect(RectF(rx, curY, rx + colW, curY + rowH - 2f), 4f, 4f, fillPaint)
                        c.drawRoundRect(RectF(rx, curY, rx + colW, curY + rowH - 2f), 4f, 4f, linePaint)
                        c.drawText(rightPair.first, rx + 6f, curY + 11f, labelPaint)
                        c.withTranslation(rx + 6f, curY + 14f) {
                            rightValLayout.draw(this)
                        }
                    }
                    curY += rowH + 2f
                    idx += 2
                }
                curY += 6f
            }

            fun drawLabeledBlock(label: String, value: String) {
                val safeVal = value.ifBlank { "—" }
                val valLayout = buildLayout(safeVal, bodyPaint, contentWidth - 16f)
                val blockH = 16f + valLayout.height + 10f
                ensureSpace(blockH + 4f)
                val c = canvas!!
                fillPaint.color = palette.cardBg
                c.drawRoundRect(RectF(marginX, curY, pageWidth - marginX, curY + blockH), 4f, 4f, fillPaint)
                c.drawRoundRect(RectF(marginX, curY, pageWidth - marginX, curY + blockH), 4f, 4f, linePaint)
                c.drawText(label, marginX + 8f, curY + 13f, labelPaint)
                c.withTranslation(marginX + 8f, curY + 17f) {
                    valLayout.draw(this)
                }
                curY += blockH + 6f
            }

            // START PAGE 1
            startNewPage(isFirstPage = true)

            // 1. CLIENT DETAILS
            drawSectionTitle("1. CLIENT DETAILS")
            drawKeyValuePairs(
                listOf(
                    "Client Name" to report.clientName,
                    "Mobile Number" to report.mobileNumber,
                    "Alternate Number" to report.alternateNumber,
                    "Area / Locality" to report.areaLocality,
                    "City" to report.city,
                    "State" to report.state,
                    "PIN Code" to report.pinCode,
                    "Full Address" to report.address
                )
            )

            // 2. BIRTH DETAILS
            drawSectionTitle("2. BIRTH DETAILS (OPTIONAL)")
            drawKeyValuePairs(
                listOf(
                    "Date of Birth" to report.birthDate,
                    "Birth Time" to report.birthTime,
                    "Birth Location" to report.birthLocation
                )
            )

            // 3, 4, 5. PROPERTY DETAILS, PLOT DEGREES & ENTRANCES
            drawSectionTitle("3–5. PROPERTY DETAILS, PLOT DEGREES & ENTRANCES")
            val propTypeText = if (report.propertyType == "Other" && report.propertyTypeOther.isNotBlank()) {
                "Other (${report.propertyTypeOther})"
            } else report.propertyType
            drawKeyValuePairs(
                listOf(
                    "Property Type" to propTypeText,
                    "Plot Degrees" to report.plotDegrees,
                    "Main Entrance Direction" to report.mainEntranceDirection,
                    "Plot Shape" to report.plotShape,
                    "Building Orientation" to report.buildingOrientation,
                    "Main Entrance Notes" to report.mainEntranceNotes
                )
            )
            val additionalEntrances = JsonCodec.decodeEntrances(report.additionalEntrancesJson)
            if (additionalEntrances.isNotEmpty()) {
                val entSummary = additionalEntrances.mapIndexed { idx, e ->
                    "${idx + 1}. ${e.label.ifBlank { "Entrance" }} — Direction: ${e.direction}" +
                        (if (e.degrees.isNotBlank()) ", Degrees: ${e.degrees}" else "") +
                        (if (e.notes.isNotBlank()) " (${e.notes})" else "")
                }.joinToString("\n")
                drawLabeledBlock("Additional Entrance Details", entSummary)
            }
            if (report.propertyObservations.isNotBlank()) {
                drawLabeledBlock("Additional Property Observations", report.propertyObservations)
            }

            // 6. SWAR CHECK
            drawSectionTitle("6. SWAR CHECK – WHILE ENTERING THE PREMISES")
            drawKeyValuePairs(
                listOf(
                    "Active Swar / Nadi" to report.swarNadi,
                    "Observation Time" to report.swarObservationTime
                )
            )
            if (report.swarNotes.isNotBlank()) {
                drawLabeledBlock("Swar Check Notes", report.swarNotes)
            }

            // 7. LAND ENERGY ANALYSIS
            drawSectionTitle("7. LAND ENERGY / GUNA ANALYSIS")
            val gunas = JsonCodec.decodeStringList(report.landEnergyGunasJson)
            drawLabeledBlock("Observed Land Energy Gunas", if (gunas.isEmpty()) "—" else gunas.joinToString(", "))
            if (report.landEnergyExplanation.isNotBlank()) {
                drawLabeledBlock("Land Energy Explanation", report.landEnergyExplanation)
            }

            // 8–10. UNIFIED CATEGORY-WISE PROBLEMS, PROBLEMATIC ZONES & REMEDIES TABLE
            drawSectionTitle("8–10. PROBLEMS, PROBLEMATIC ZONES & REMEDIES (CATEGORY-WISE)")
            drawLabeledBlock("Since when facing these problems?", report.problemSince)

            val rows = bundle.orderedAnalysisRows
            val problems = JsonCodec.decodeProblems(report.selectedProblemsJson)
            val unmappedProblems = problems.filter { p ->
                rows.none { it.clientProblem.equals(p.title, ignoreCase = true) }
            }
            if (unmappedProblems.isNotEmpty()) {
                val extraListText = unmappedProblems.mapIndexed { i, p ->
                    "${i + 1}. ${p.title} (${VastuConstants.categoryForProblem(p.title)})"
                }.joinToString("\n")
                drawLabeledBlock("Additional Reported Client Problems", extraListText)
            }

            if (rows.isEmpty()) {
                drawLabeledBlock("Problems, Zones & Remedies", "No rows added yet.")
            } else {
                // Sum = 24 + 90 + 80 + 128 + 140 + 65 = 527f == contentWidth
                val colWidths = floatArrayOf(24f, 90f, 80f, 128f, 140f, 65f)
                val colHeaders = listOf(
                    "S.No",
                    "Problem Identified",
                    "Expected Zones Responsible",
                    "Remarks & Observations",
                    "Remedies & Action Plan",
                    "Priority / Timeline"
                )

                fun drawTableHeaderRow() {
                    val headerLayouts = colHeaders.mapIndexed { i, h ->
                        buildLayout(h, tableHeaderTextPaint, colWidths[i] - 6f)
                    }
                    val headerH = (headerLayouts.maxOf { it.height } + 10f).coerceAtLeast(22f)
                    ensureSpace(headerH + 14f)
                    val c = canvas!!
                    fillPaint.color = palette.tableHeaderBg
                    c.drawRect(marginX, curY, marginX + contentWidth, curY + headerH, fillPaint)
                    var x = marginX
                    headerLayouts.forEachIndexed { i, layout ->
                        c.withTranslation(x + 3f, curY + 5f) {
                            layout.draw(this)
                        }
                        c.drawRect(x, curY, x + colWidths[i], curY + headerH, linePaint)
                        x += colWidths[i]
                    }
                    curY += headerH
                }

                fun drawCategoryBannerRow(category: String, count: Int) {
                    val catText = "CATEGORY: ${category.uppercase(Locale.getDefault())} ($count)"
                    val catPaint = TextPaint(labelPaint).apply {
                        color = palette.primary
                        textSize = 8.2f * fontScale
                    }
                    val catH = 19f
                    if (curY + catH + 22f > maxY) {
                        startNewPage(isFirstPage = false)
                        drawTableHeaderRow()
                    }
                    val c = canvas!!
                    fillPaint.color = palette.headerBg
                    c.drawRect(marginX, curY, marginX + contentWidth, curY + catH, fillPaint)
                    c.drawRect(marginX, curY, marginX + contentWidth, curY + catH, linePaint)
                    c.drawText(catText, marginX + 6f, curY + 13f, catPaint)
                    curY += catH
                }

                val normalizedRows = rows.map { r ->
                    val cat = r.problemCategory.ifBlank { VastuConstants.categoryForProblem(r.clientProblem) }
                    cat to r
                }
                val byCat = normalizedRows.groupBy({ it.first }, { it.second })
                val orderedCats = VastuConstants.problemCategories.filter { byCat.containsKey(it) } +
                    byCat.keys.filterNot { it in VastuConstants.problemCategories }

                drawTableHeaderRow()

                var globalSerial = 1
                orderedCats.forEach { category ->
                    val catRows = byCat[category] ?: emptyList()
                    drawCategoryBannerRow(category, catRows.size)

                    catRows.forEachIndexed { localIdx, row ->
                        val serial = globalSerial++
                        val attachedIds = JsonCodec.decodeStringList(row.attachedMediaIdsJson)
                        val obsText = buildString {
                            if (row.notes.isNotBlank()) append(row.notes)
                            if (row.problemObserved.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append(row.problemObserved)
                            }
                            if (row.antiActivity.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Anti-Activity: ${row.antiActivity}")
                            }
                            if (row.antiUtility.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Anti-Utility: ${row.antiUtility}")
                            }
                            if (row.antiObjects.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Anti-Objects: ${row.antiObjects}")
                            }
                        }
                        val remedyText = buildString {
                            if (row.suggestedRemedy.isNotBlank()) append(row.suggestedRemedy)
                            if (row.activitiesToStop.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Stop: ${row.activitiesToStop}")
                            }
                            if (row.objectsToRemove.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Remove: ${row.objectsToRemove}")
                            }
                            if (row.objectsToAdd.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Add: ${row.objectsToAdd}")
                            }
                            if (row.utilitiesToChange.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Utilities: ${row.utilitiesToChange}")
                            }
                            if (row.structuralCorrections.isNotBlank()) {
                                if (isNotBlank()) append("\n")
                                append("Structural: ${row.structuralCorrections}")
                            }
                        }
                        val metaNotesText = buildString {
                            append("Priority: ${row.remedyPriority.ifBlank { "Medium" }}")
                            if (row.waitingPeriod.isNotBlank()) append("\nWait: ${row.waitingPeriod}")
                            if (attachedIds.isNotEmpty()) append("\n[${attachedIds.size} photo(s) — Sec. 17]")
                        }

                        val cellTexts = listOf(
                            "$serial",
                            row.clientProblem.ifBlank { "—" },
                            row.responsibleDirectionZone.ifBlank { "—" },
                            obsText.ifBlank { "—" },
                            remedyText.ifBlank { "—" },
                            metaNotesText.ifBlank { "—" }
                        )
                        val cellLayouts = cellTexts.mapIndexed { i, text ->
                            buildLayout(text, tableCellPaint, colWidths[i] - 6f)
                        }
                        val rowH = (cellLayouts.maxOf { it.height } + 10f).coerceAtLeast(22f)

                        if (curY + rowH > maxY) {
                            startNewPage(isFirstPage = false)
                            drawTableHeaderRow()
                        }

                        val c = canvas!!
                        if (localIdx % 2 == 1) {
                            fillPaint.color = palette.tableAltRowBg
                            c.drawRect(marginX, curY, marginX + contentWidth, curY + rowH, fillPaint)
                        }
                        var x = marginX
                        cellLayouts.forEachIndexed { i, layout ->
                            c.withTranslation(x + 3f, curY + 5f) {
                                layout.draw(this)
                            }
                            c.drawRect(x, curY, x + colWidths[i], curY + rowH, linePaint)
                            x += colWidths[i]
                        }
                        curY += rowH
                    }
                }
                curY += 10f
            }

            // 11 & 12. OVERALL FOLLOW-UP, IMPLEMENTATION & EXPECTED RESULT PERIOD
            drawSectionTitle("11–12. OVERALL FOLLOW-UP, IMPLEMENTATION & RESULT TIMELINE")
            drawKeyValuePairs(
                listOf(
                    "Overall Remedy Priority" to report.remedyPriority,
                    "Expected Result Waiting Period" to if (report.expectedWaitingPeriod == "Custom period" && report.customWaitingPeriod.isNotBlank()) {
                        report.customWaitingPeriod
                    } else report.expectedWaitingPeriod,
                    "Expected Follow-Up Date" to report.expectedFollowUpDate,
                    "Follow-Up Notes" to report.remedyNotes
                )
            )
            if (report.recommendedRemedies.isNotBlank()) drawLabeledBlock("General Remedies Summary", report.recommendedRemedies)
            if (report.activitiesToStop.isNotBlank()) drawLabeledBlock("Activities to Stop", report.activitiesToStop)
            if (report.objectsToRemove.isNotBlank()) drawLabeledBlock("Objects to Remove", report.objectsToRemove)
            if (report.objectsToAdd.isNotBlank()) drawLabeledBlock("Objects to Add", report.objectsToAdd)
            if (report.utilitiesToChange.isNotBlank()) drawLabeledBlock("Utilities to Change", report.utilitiesToChange)
            if (report.structuralCorrections.isNotBlank()) drawLabeledBlock("Structural Corrections", report.structuralCorrections)
            if (report.implementationInstructions.isNotBlank()) drawLabeledBlock("Implementation Instructions", report.implementationInstructions)
            drawLabeledBlock(
                "Result Estimation Disclaimer",
                "Note: Expected result waiting periods are estimates based on site observations and recommended Vastu corrections. Results vary by individual compliance and circumstances and are not guaranteed."
            )

            // 13. CONSULTANT NOTES
            drawSectionTitle("13. CONSULTANT NOTES")
            val notesList = JsonCodec.decodeStringList(report.consultantNotesJson)
                .mapIndexedNotNull { idx, note ->
                    if (note.isBlank()) null else "${idx + 1}. $note"
                }
            if (notesList.isEmpty()) {
                drawLabeledBlock("Notes", "No additional consultant notes entered.")
            } else {
                notesList.forEach { formattedNote ->
                    val layout = buildLayout(formattedNote, bodyPaint, contentWidth - 16f)
                    val h = layout.height + 12f
                    ensureSpace(h + 4f)
                    val c = canvas!!
                    fillPaint.color = palette.cardBg
                    c.drawRoundRect(RectF(marginX, curY, pageWidth - marginX, curY + h), 4f, 4f, fillPaint)
                    c.drawRoundRect(RectF(marginX, curY, pageWidth - marginX, curY + h), 4f, 4f, linePaint)
                    c.withTranslation(marginX + 8f, curY + 6f) {
                        layout.draw(this)
                    }
                    curY += h + 4f
                }
                curY += 6f
            }

            val allIncludedMedia = bundle.orderedMediaItems.filter { it.includeInPdf }
            val mediaById = bundle.orderedMediaItems.associateBy { it.id }

            // 14. SITE PHOTOGRAPHS
            val sitePhotos = allIncludedMedia.filter { it.mediaKind == MediaKind.PHOTO.name }
            if (sitePhotos.isNotEmpty()) {
                drawSectionTitle("14. SITE PHOTOGRAPHS")
                drawMediaGridInPdf(
                    items = sitePhotos,
                    useAnnotatedIfAvailable = false,
                    marginX = marginX,
                    contentWidth = contentWidth,
                    maxY = maxY,
                    palette = palette,
                    labelPaint = labelPaint,
                    smallPaint = smallPaint,
                    linePaint = linePaint,
                    fillPaint = fillPaint,
                    buildLayout = ::buildLayout,
                    ensureSpace = ::ensureSpace,
                    getCanvas = { canvas!! },
                    getCurY = { curY },
                    setCurY = { curY = it },
                    dateTimeFormatter = dateTimeFormatter
                )
            }

            // 15. SITE DRAWINGS & FLOOR PLANS
            val siteDrawings = allIncludedMedia.filter { it.mediaKind == MediaKind.DRAWING.name }
            if (siteDrawings.isNotEmpty()) {
                drawSectionTitle("15. SITE DRAWINGS & FLOOR PLANS")
                siteDrawings.forEachIndexed { idx, drawing ->
                    drawLargeMediaCardInPdf(
                        index = idx + 1,
                        media = drawing,
                        useAnnotated = false,
                        marginX = marginX,
                        contentWidth = contentWidth,
                        maxY = maxY,
                        palette = palette,
                        labelPaint = labelPaint,
                        bodyPaint = bodyPaint,
                        smallPaint = smallPaint,
                        linePaint = linePaint,
                        fillPaint = fillPaint,
                        buildLayout = ::buildLayout,
                        ensureSpace = ::ensureSpace,
                        getCanvas = { canvas!! },
                        getCurY = { curY },
                        setCurY = { curY = it },
                        dateTimeFormatter = dateTimeFormatter
                    )
                }
            }

            // 16. ANNOTATED FLOOR PLANS
            val annotatedPlans = allIncludedMedia.filter { !it.annotatedFilePath.isNullOrBlank() && File(it.annotatedFilePath).exists() }
            if (annotatedPlans.isNotEmpty()) {
                drawSectionTitle("16. ANNOTATED FLOOR PLANS & DIRECTIONAL LAYOUTS")
                annotatedPlans.forEachIndexed { idx, plan ->
                    drawLargeMediaCardInPdf(
                        index = idx + 1,
                        media = plan,
                        useAnnotated = true,
                        marginX = marginX,
                        contentWidth = contentWidth,
                        maxY = maxY,
                        palette = palette,
                        labelPaint = labelPaint,
                        bodyPaint = bodyPaint,
                        smallPaint = smallPaint,
                        linePaint = linePaint,
                        fillPaint = fillPaint,
                        buildLayout = ::buildLayout,
                        ensureSpace = ::ensureSpace,
                        getCanvas = { canvas!! },
                        getCurY = { curY },
                        setCurY = { curY = it },
                        dateTimeFormatter = dateTimeFormatter
                    )
                }
            }

            // 17. RELEVANT PHOTOGRAPHS ATTACHED TO INDIVIDUAL OBSERVATIONS
            val rowsWithPhotos = rows.mapIndexedNotNull { idx, row ->
                val attached = JsonCodec.decodeStringList(row.attachedMediaIdsJson)
                    .mapNotNull { id -> mediaById[id] }
                    .filter { it.includeInPdf }
                if (attached.isEmpty()) null else Triple(idx + 1, row, attached)
            }
            if (rowsWithPhotos.isNotEmpty()) {
                drawSectionTitle("17. PHOTOGRAPHS ATTACHED TO VASTU OBSERVATIONS")
                rowsWithPhotos.forEach { (serial, row, attachedList) ->
                    val heading = "Row #$serial — ${row.clientProblem.ifBlank { "Observation" }}" +
                        (if (row.responsibleDirectionZone.isNotBlank()) " (${row.responsibleDirectionZone})" else "")
                    val sub = buildString {
                        if (row.problemObserved.isNotBlank()) append("Observed: ${row.problemObserved}  ")
                        if (row.suggestedRemedy.isNotBlank()) append("Remedy: ${row.suggestedRemedy}")
                    }
                    val headLayout = buildLayout(heading, labelPaint, contentWidth - 12f)
                    val subLayout = if (sub.isNotBlank()) buildLayout(sub, smallPaint, contentWidth - 12f) else null
                    ensureSpace(headLayout.height + (subLayout?.height ?: 0) + 24f)
                    val c = canvas!!
                    c.withTranslation(marginX + 4f, curY) {
                        headLayout.draw(this)
                    }
                    curY += headLayout.height + 3f
                    if (subLayout != null) {
                        c.withTranslation(marginX + 4f, curY) {
                            subLayout.draw(this)
                        }
                        curY += subLayout.height + 6f
                    }
                    drawMediaGridInPdf(
                        items = attachedList,
                        useAnnotatedIfAvailable = true,
                        marginX = marginX,
                        contentWidth = contentWidth,
                        maxY = maxY,
                        palette = palette,
                        labelPaint = labelPaint,
                        smallPaint = smallPaint,
                        linePaint = linePaint,
                        fillPaint = fillPaint,
                        buildLayout = ::buildLayout,
                        ensureSpace = ::ensureSpace,
                        getCanvas = { canvas!! },
                        getCurY = { curY },
                        setCurY = { curY = it },
                        dateTimeFormatter = dateTimeFormatter
                    )
                }
            }

            // FINAL PAGE FOOTER AT BOTTOM OF FINAL PAGE
            val footerBoxH = 74f
            if (curY + footerBoxH > maxY) {
                startNewPage(isFirstPage = false)
            }
            val finalCanvas = canvas!!
            val footerTop = maxY - footerBoxH
            fillPaint.color = palette.headerBg
            finalCanvas.drawRoundRect(
                RectF(marginX, footerTop, pageWidth - marginX, footerTop + footerBoxH),
                6f,
                6f,
                fillPaint
            )
            linePaint.color = palette.accent
            linePaint.strokeWidth = 1.1f
            finalCanvas.drawRoundRect(
                RectF(marginX, footerTop, pageWidth - marginX, footerTop + footerBoxH),
                6f,
                6f,
                linePaint
            )
            val centerPaintBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primary
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val centerPaintSub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accent
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.08f
            }
            val cx = pageWidth / 2f
            finalCanvas.drawText("CONSULTANCY DONE BY", cx, footerTop + 18f, centerPaintSub)
            finalCanvas.drawText(settings.consultantName.uppercase(Locale.getDefault()), cx, footerTop + 34f, centerPaintBold)
            finalCanvas.drawText(settings.companyName.uppercase(Locale.getDefault()), cx, footerTop + 49f, centerPaintBold)
            finalCanvas.drawText("CONTACT: ${settings.contactNumber}", cx, footerTop + 64f, centerPaintSub)

            finishCurrentPage()

            val sanitizedClient = report.clientName
                .trim()
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "Client" }
            val fileName = "Vedicra_Vastu_Report_${sanitizedClient}_$fileDate.pdf"
            val pdfFile = File(mediaManager.getPdfsDir(), fileName)
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            ExportedPdfEntity(
                id = UUID.randomUUID().toString(),
                reportId = report.id,
                clientName = report.clientName.ifBlank { "Unnamed Client" },
                fileName = fileName,
                filePath = pdfFile.absolutePath,
                createdAt = System.currentTimeMillis(),
                pageCount = pageNumber,
                fileSizeBytes = pdfFile.length()
            )
        }
    }

    private fun drawMediaGridInPdf(
        items: List<SiteMediaEntity>,
        useAnnotatedIfAvailable: Boolean,
        marginX: Float,
        contentWidth: Float,
        maxY: Float,
        palette: PdfPalette,
        labelPaint: TextPaint,
        smallPaint: TextPaint,
        linePaint: Paint,
        fillPaint: Paint,
        buildLayout: (String, TextPaint, Float) -> StaticLayout,
        ensureSpace: (Float) -> Unit,
        getCanvas: () -> Canvas,
        getCurY: () -> Float,
        setCurY: (Float) -> Unit,
        dateTimeFormatter: SimpleDateFormat
    ) {
        val gap = 10f
        val colW = (contentWidth - gap) / 2f
        val imgH = 152f
        var idx = 0
        while (idx < items.size) {
            val left = items[idx]
            val right = items.getOrNull(idx + 1)

            val leftTitle = buildPhotoCaption(left, dateTimeFormatter)
            val rightTitle = right?.let { buildPhotoCaption(it, dateTimeFormatter) }
            val leftLayout = buildLayout(leftTitle, smallPaint, colW - 12f)
            val rightLayout = rightTitle?.let { buildLayout(it, smallPaint, colW - 12f) }
            val cardH = imgH + 14f + maxOf(leftLayout.height, rightLayout?.height ?: 0) + 10f

            ensureSpace(cardH + 6f)
            val c = getCanvas()
            val y = getCurY()

            drawSinglePhotoCardInGrid(
                canvas = c,
                media = left,
                useAnnotatedIfAvailable = useAnnotatedIfAvailable,
                x = marginX,
                y = y,
                cardW = colW,
                imgH = imgH,
                cardH = cardH,
                captionLayout = leftLayout,
                palette = palette,
                labelPaint = labelPaint,
                linePaint = linePaint,
                fillPaint = fillPaint
            )

            if (right != null && rightLayout != null) {
                drawSinglePhotoCardInGrid(
                    canvas = c,
                    media = right,
                    useAnnotatedIfAvailable = useAnnotatedIfAvailable,
                    x = marginX + colW + gap,
                    y = y,
                    cardW = colW,
                    imgH = imgH,
                    cardH = cardH,
                    captionLayout = rightLayout,
                    palette = palette,
                    labelPaint = labelPaint,
                    linePaint = linePaint,
                    fillPaint = fillPaint
                )
            }

            setCurY(y + cardH + 8f)
            idx += 2
        }
    }

    private fun drawSinglePhotoCardInGrid(
        canvas: Canvas,
        media: SiteMediaEntity,
        useAnnotatedIfAvailable: Boolean,
        x: Float,
        y: Float,
        cardW: Float,
        imgH: Float,
        cardH: Float,
        captionLayout: StaticLayout,
        palette: PdfPalette,
        labelPaint: TextPaint,
        linePaint: Paint,
        fillPaint: Paint
    ) {
        fillPaint.color = palette.cardBg
        canvas.drawRoundRect(RectF(x, y, x + cardW, y + cardH), 5f, 5f, fillPaint)
        canvas.drawRoundRect(RectF(x, y, x + cardW, y + cardH), 5f, 5f, linePaint)

        val bitmap = loadMediaBitmapForPdf(media, useAnnotatedIfAvailable, 1200)
        val imgRect = RectF(x + 6f, y + 6f, x + cardW - 6f, y + 6f + imgH)
        if (bitmap != null) {
            drawBitmapFitCenter(canvas, bitmap, imgRect)
            bitmap.recycle()
        } else {
            canvas.drawText("Image unavailable", x + 14f, y + imgH / 2f, labelPaint)
        }
        canvas.withTranslation(x + 6f, y + imgH + 12f) {
            captionLayout.draw(this)
        }
    }

    private fun drawLargeMediaCardInPdf(
        index: Int,
        media: SiteMediaEntity,
        useAnnotated: Boolean,
        marginX: Float,
        contentWidth: Float,
        maxY: Float,
        palette: PdfPalette,
        labelPaint: TextPaint,
        bodyPaint: TextPaint,
        smallPaint: TextPaint,
        linePaint: Paint,
        fillPaint: Paint,
        buildLayout: (String, TextPaint, Float) -> StaticLayout,
        ensureSpace: (Float) -> Unit,
        getCanvas: () -> Canvas,
        getCurY: () -> Float,
        setCurY: (Float) -> Unit,
        dateTimeFormatter: SimpleDateFormat
    ) {
        val headerText = buildString {
            append("$index. ")
            append(media.drawingTitle.ifBlank { media.drawingCategory.ifBlank { media.caption.ifBlank { "Site Layout" } } })
            if (media.isMainSiteLayout) append("  [MAIN SITE LAYOUT]")
            if (useAnnotated) append("  [ANNOTATED VERSION]")
        }
        val metaText = buildString {
            val parts = mutableListOf<String>()
            if (media.drawingCategory.isNotBlank()) parts.add("Category: ${media.drawingCategory}")
            if (media.floorNumber.isNotBlank()) parts.add("Floor: ${media.floorNumber}")
            if (media.orientationDirection.isNotBlank()) parts.add("Orientation: ${media.orientationDirection}")
            parts.add("Captured: ${dateTimeFormatter.format(Date(media.capturedAt))}")
            append(parts.joinToString("  |  "))
            if (media.description.isNotBlank()) append("\nDescription: ${media.description}")
            if (media.caption.isNotBlank()) append("\nCaption: ${media.caption}")
            if (media.additionalNotes.isNotBlank()) append("\nNotes: ${media.additionalNotes}")
        }

        val headerLayout = buildLayout(headerText, labelPaint, contentWidth - 16f)
        val metaLayout = buildLayout(metaText, smallPaint, contentWidth - 16f)
        val imgH = 260f
        val totalH = headerLayout.height + metaLayout.height + imgH + 28f
        ensureSpace(totalH + 6f)

        val c = getCanvas()
        val y = getCurY()
        fillPaint.color = palette.cardBg
        c.drawRoundRect(RectF(marginX, y, marginX + contentWidth, y + totalH), 6f, 6f, fillPaint)
        c.drawRoundRect(RectF(marginX, y, marginX + contentWidth, y + totalH), 6f, 6f, linePaint)

        var innerY = y + 8f
        c.withTranslation(marginX + 8f, innerY) {
            headerLayout.draw(this)
        }
        innerY += headerLayout.height + 4f

        val bitmap = loadMediaBitmapForPdf(media, useAnnotated, 1600)
        val imgRect = RectF(marginX + 8f, innerY, marginX + contentWidth - 8f, innerY + imgH)
        if (bitmap != null) {
            drawBitmapFitCenter(c, bitmap, imgRect)
            bitmap.recycle()
        } else {
            c.drawText("Drawing image unavailable", marginX + 16f, innerY + imgH / 2f, bodyPaint)
        }
        innerY += imgH + 6f

        c.withTranslation(marginX + 8f, innerY) {
            metaLayout.draw(this)
        }

        setCurY(y + totalH + 10f)
    }

    private fun buildPhotoCaption(media: SiteMediaEntity, dateTimeFormatter: SimpleDateFormat): String = buildString {
        if (media.isMainSiteLayout) append("[MAIN SITE LAYOUT]\n")
        val title = media.caption.ifBlank { media.drawingTitle.ifBlank { "Site Photograph" } }
        append(title)
        if (media.description.isNotBlank()) append("\n${media.description}")
        append("\n${dateTimeFormatter.format(Date(media.capturedAt))}")
    }

    private fun loadMediaBitmapForPdf(
        media: SiteMediaEntity,
        useAnnotatedIfAvailable: Boolean,
        maxDim: Int
    ): Bitmap? {
        if (useAnnotatedIfAvailable && !media.annotatedFilePath.isNullOrBlank() && File(media.annotatedFilePath).exists()) {
            return mediaManager.loadRotatedBitmap(media.annotatedFilePath, 0, maxDim)
        }
        return mediaManager.loadRotatedBitmap(media.filePath, media.rotationDegrees, maxDim)
    }

    private fun drawBitmapFitCenter(canvas: Canvas, bitmap: Bitmap, target: RectF) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = minOf(target.width() / bw, target.height() / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val left = target.left + (target.width() - drawW) / 2f
        val top = target.top + (target.height() - drawH) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
    }

    private fun loadBrandLogo(settings: AppSettings): Bitmap? {
        if (settings.companyLogoPath.isNotBlank() && File(settings.companyLogoPath).exists()) {
            return mediaManager.loadRotatedBitmap(settings.companyLogoPath, 0, 256)
        }
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_vastu_brand_logo)
        }.getOrNull()
    }
}
