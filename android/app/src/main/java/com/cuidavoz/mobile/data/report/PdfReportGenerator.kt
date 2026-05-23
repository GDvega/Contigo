package com.cuidavoz.mobile.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.cuidavoz.mobile.domain.report.MedicalReportBuilder
import com.cuidavoz.mobile.domain.report.MedicalReportData
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.util.formatDateTime
import com.cuidavoz.mobile.util.formatScheduleTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedPdfFile(
    val file: File,
    val contentUri: Uri,
    val fileName: String,
)

class PdfReportGenerator(
    private val context: Context,
) {
    suspend fun generateToCache(reportData: MedicalReportData): GeneratedPdfFile = withContext(Dispatchers.IO) {
        Log.d(PDF_TAG, "Generando PDF temporal")
        val reportsDirectory = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsDirectory, suggestedFileName(reportData.generatedAt))
        FileOutputStream(outputFile).use { outputStream ->
            writePdf(reportData, outputStream)
        }
        GeneratedPdfFile(
            file = outputFile,
            contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile,
            ),
            fileName = outputFile.name,
        )
    }

    suspend fun saveToUri(
        reportData: MedicalReportData,
        destinationUri: Uri,
    ) = withContext(Dispatchers.IO) {
        Log.d(PDF_TAG, "Guardando PDF en destino SAF")
        context.contentResolver.openOutputStream(destinationUri, "w")?.use { outputStream ->
            writePdf(reportData, outputStream)
        } ?: error("No pudimos abrir el destino del PDF.")
    }

    fun suggestedFileName(generatedAt: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return "cuidavoz-reporte-medico-${formatter.format(Date(generatedAt))}.pdf"
    }

    private fun writePdf(
        reportData: MedicalReportData,
        outputStream: OutputStream,
    ) {
        val document = PdfDocument()
        try {
            val composer = PdfComposer(document)
            val pressureRows = reportData.pressureReadings.take(MAX_TABLE_ROWS).map { reading ->
                listOf(
                    formatDateTime(reading.measuredAt),
                    "${reading.systolic}/${reading.diastolic}",
                    reading.pulse?.toString() ?: "-",
                    MedicalReportBuilder.pressureStatusLabel(reading.status),
                    reading.notes.orEmpty().ifBlank { "-" },
                )
            }
            val medicationRows = reportData.activeMedications.take(MAX_TABLE_ROWS).map { medication ->
                listOf(
                    medication.name,
                    medication.dose,
                    formatScheduleTime(medication.scheduleTime),
                    medication.treatmentSummary(),
                    medication.instructions.orEmpty().ifBlank { "-" },
                    if (medication.isExpired()) "Vencido" else "Activo",
                )
            }
            val medicationEntryRows = reportData.medicationEntries.take(MAX_TABLE_ROWS).map { entry ->
                listOf(
                    formatDateTime(entry.scheduledFor),
                    entry.medicationName,
                    formatScheduleTime(entry.scheduleTime),
                    entry.treatmentDuration,
                    MedicalReportBuilder.medicationStatusLabel(entry.status),
                    entry.takenAt?.let(::formatDateTime) ?: "-",
                )
            }

            composer.drawTitle("CuidaVoz - Reporte para el médico")
            composer.drawParagraph("Fecha de generación: ${formatDateTime(reportData.generatedAt)}")
            composer.drawParagraph(
                "Periodo del reporte: ${MedicalReportBuilder.periodLabel(reportData.period)} " +
                    "(${MedicalReportBuilder.periodRangeLabel(reportData)})",
            )
            composer.drawParagraph("Generado por CuidaVoz. Este reporte es informativo y no reemplaza una evaluación médica.")
            composer.drawSpacer(8f)
            if (reportData.pressureReadings.isEmpty() && reportData.medicationEntries.isEmpty()) {
                composer.drawParagraph("No hay registros suficientes para este periodo.")
                composer.drawSpacer(8f)
            }

            composer.drawSectionTitle("Datos del paciente")
            composer.drawParagraph("Paciente: ${reportData.patient?.fullName ?: "Sin datos"}")
            composer.drawParagraph("Edad: ${reportData.patient?.age?.toString() ?: "-"}")
            composer.drawParagraph(
                "Contacto familiar: ${reportData.familyContact?.fullName ?: "Sin datos"}" +
                    if (reportData.familyContact?.phone.isNullOrBlank()) "" else " - ${reportData.familyContact?.phone}"
            )

            composer.drawSectionTitle("Resumen de presión arterial")
            composer.drawParagraph("Total de registros: ${reportData.pressureSummary.totalPressureReadings}")
            composer.drawParagraph(
                "Última presión: ${MedicalReportBuilder.latestPressureLabel(reportData.pressureSummary.latestPressure)}",
            )
            composer.drawParagraph(
                "Fecha de última lectura: ${MedicalReportBuilder.latestPressureDateLabel(reportData.pressureSummary.latestPressure)}",
            )
            composer.drawParagraph(
                "Promedio del periodo: " +
                    "${reportData.pressureSummary.averageSystolic ?: "-"}" +
                    "/${reportData.pressureSummary.averageDiastolic ?: "-"}" +
                    if (reportData.pressureSummary.averagePulse != null) {
                        " - Pulso ${reportData.pressureSummary.averagePulse}"
                    } else {
                        ""
                    },
            )
            composer.drawParagraph("Registros fuera del rango indicado: ${reportData.pressureSummary.outOfRangeCount}")
            composer.drawParagraph("Registros altos o muy altos: ${reportData.pressureSummary.highOrCriticalCount}")
            if (reportData.pressureReadings.size > MAX_TABLE_ROWS) {
                composer.drawParagraph("Se muestran los últimos 20 registros del periodo.")
            }
            composer.drawTable(
                headers = listOf("Fecha", "Presión", "Pulso", "Estado", "Nota"),
                rows = pressureRows,
                columnWeights = floatArrayOf(1.6f, 1f, 0.7f, 1.5f, 1.6f),
                emptyMessage = "Sin registros en este periodo.",
            )

            composer.drawSectionTitle("Medicamentos")
            composer.drawParagraph("Medicamentos activos: ${reportData.medicationSummary.activeMedicationCount}")
            composer.drawParagraph("Adherencia del periodo: ${reportData.medicationSummary.adherencePercentage}%")
            composer.drawParagraph("Tomas registradas: ${reportData.medicationSummary.totalMedicationLogs}")
            composer.drawParagraph("Tomas pendientes u omitidas: ${reportData.medicationSummary.pendingOrSkippedCount}")
            composer.drawTable(
                headers = listOf("Medicamento", "Dosis", "Hora", "Duración", "Instrucciones", "Estado"),
                rows = medicationRows,
                columnWeights = floatArrayOf(1.2f, 0.8f, 0.7f, 1.5f, 1.4f, 0.8f),
                emptyMessage = "Sin medicamentos activos.",
            )

            composer.drawSectionTitle("Tomas recientes")
            if (reportData.medicationEntries.size > MAX_TABLE_ROWS) {
                composer.drawParagraph("Se muestran las últimas 20 tomas del periodo.")
            }
            composer.drawTable(
                headers = listOf("Fecha", "Medicamento", "Hora", "Duración", "Estado", "Hora de toma"),
                rows = medicationEntryRows,
                columnWeights = floatArrayOf(1.3f, 1.1f, 0.9f, 1.5f, 1f, 1f),
                emptyMessage = "Sin registros en este periodo.",
            )

            composer.drawSectionTitle("Rangos y observaciones")
            val settings = reportData.healthSettings
            if (settings == null) {
                composer.drawParagraph("Sin rangos configurados.")
            } else {
                composer.drawParagraph("Sistólica: ${settings.systolicMinNormal} - ${settings.systolicMaxNormal}")
                composer.drawParagraph("Diastólica: ${settings.diastolicMinNormal} - ${settings.diastolicMaxNormal}")
                composer.drawParagraph("Pulso: ${settings.pulseMinNormal} - ${settings.pulseMaxNormal}")
            }
            composer.drawParagraph(
                "Recomendación del médico: ${reportData.doctorRecommendation ?: "Sin observaciones registradas."}",
            )
            composer.drawParagraph("Ante síntomas o valores preocupantes, consulta con un profesional de salud.")

            composer.finish(outputStream)
        } finally {
            document.close()
        }
    }

    private class PdfComposer(
        private val document: PdfDocument,
    ) {
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
        }
        private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        private val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(236, 240, 243)
            style = Paint.Style.FILL
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
        }

        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var cursorY = PAGE_MARGIN

        fun drawTitle(text: String) {
            ensurePage()
            drawWrappedText(text, titlePaint, extraSpacing = 10f)
        }

        fun drawSectionTitle(text: String) {
            ensurePage()
            ensureSpace(sectionPaint.lineHeight() + 16f)
            drawWrappedText(text, sectionPaint, extraSpacing = 8f)
        }

        fun drawParagraph(text: String) {
            ensurePage()
            drawWrappedText(text, bodyPaint, extraSpacing = 6f)
        }

        fun drawSpacer(height: Float) {
            ensurePage()
            ensureSpace(height)
            cursorY += height
        }

        fun drawTable(
            headers: List<String>,
            rows: List<List<String>>,
            columnWeights: FloatArray,
            emptyMessage: String,
        ) {
            if (rows.isEmpty()) {
                drawParagraph(emptyMessage)
                return
            }

            val totalWeight = columnWeights.sum()
            val contentWidth = PAGE_WIDTH - (PAGE_MARGIN * 2)
            val columnWidths = columnWeights.map { weight -> contentWidth * (weight / totalWeight) }

            fun drawHeader() {
                val headerLines = headers.mapIndexed { index, value ->
                    wrapText(value, tableHeaderPaint, columnWidths[index] - (CELL_HORIZONTAL_PADDING * 2))
                }
                val rowHeight = headerLines.maxOf { it.size }.coerceAtLeast(1) *
                    tableHeaderPaint.lineHeight() + (CELL_VERTICAL_PADDING * 2)
                ensureSpace(rowHeight + 8f)

                var x = PAGE_MARGIN
                val currentCanvas = canvas ?: return
                headers.indices.forEach { index ->
                    val width = columnWidths[index]
                    currentCanvas.drawRect(x, cursorY, x + width, cursorY + rowHeight, headerFillPaint)
                    currentCanvas.drawRect(x, cursorY, x + width, cursorY + rowHeight, borderPaint)
                    drawLinesInCell(
                        lines = headerLines[index],
                        x = x,
                        y = cursorY,
                        width = width,
                        rowHeight = rowHeight,
                        paint = tableHeaderPaint,
                    )
                    x += width
                }
                cursorY += rowHeight
            }

            drawHeader()
            rows.forEach { row ->
                val cellLines = row.mapIndexed { index, value ->
                    wrapText(value, tableCellPaint, columnWidths[index] - (CELL_HORIZONTAL_PADDING * 2))
                }
                val rowHeight = cellLines.maxOf { it.size }.coerceAtLeast(1) *
                    tableCellPaint.lineHeight() + (CELL_VERTICAL_PADDING * 2)
                if (!hasSpace(rowHeight + FOOTER_SPACE)) {
                    newPage()
                    drawHeader()
                }

                var x = PAGE_MARGIN
                val currentCanvas = canvas ?: return@forEach
                row.indices.forEach { index ->
                    val width = columnWidths[index]
                    currentCanvas.drawRect(x, cursorY, x + width, cursorY + rowHeight, borderPaint)
                    drawLinesInCell(
                        lines = cellLines[index],
                        x = x,
                        y = cursorY,
                        width = width,
                        rowHeight = rowHeight,
                        paint = tableCellPaint,
                    )
                    x += width
                }
                cursorY += rowHeight
            }

            drawSpacer(8f)
        }

        fun finish(outputStream: OutputStream) {
            finishPage()
            document.writeTo(outputStream)
        }

        private fun drawWrappedText(
            text: String,
            paint: Paint,
            extraSpacing: Float,
        ) {
            val lines = wrapText(text, paint, PAGE_WIDTH - (PAGE_MARGIN * 2))
            ensureSpace(lines.size.coerceAtLeast(1) * paint.lineHeight() + extraSpacing)
            val currentCanvas = canvas ?: return
            lines.forEach { line ->
                cursorY += paint.lineHeight()
                currentCanvas.drawText(line, PAGE_MARGIN, cursorY, paint)
            }
            cursorY += extraSpacing
        }

        private fun drawLinesInCell(
            lines: List<String>,
            x: Float,
            y: Float,
            width: Float,
            rowHeight: Float,
            paint: Paint,
        ) {
            val currentCanvas = canvas ?: return
            val lineHeight = paint.lineHeight()
            var textY = y + CELL_VERTICAL_PADDING + lineHeight
            lines.forEach { line ->
                currentCanvas.drawText(line, x + CELL_HORIZONTAL_PADDING, textY, paint)
                textY += lineHeight
            }
        }

        private fun wrapText(
            text: String,
            paint: Paint,
            maxWidth: Float,
        ): List<String> {
            if (text.isBlank()) {
                return listOf("-")
            }

            val result = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                val words = paragraph.split(' ')
                var currentLine = ""
                words.forEach { word ->
                    val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotBlank()) {
                            result += currentLine
                        }
                        currentLine = word
                    }
                }
                if (currentLine.isNotBlank()) {
                    result += currentLine
                }
            }
            return result.ifEmpty { listOf("-") }
        }

        private fun ensurePage() {
            if (page == null || canvas == null) {
                newPage()
            }
        }

        private fun ensureSpace(requiredHeight: Float) {
            ensurePage()
            if (!hasSpace(requiredHeight)) {
                newPage()
            }
        }

        private fun hasSpace(requiredHeight: Float): Boolean {
            return cursorY + requiredHeight <= PAGE_HEIGHT - FOOTER_SPACE
        }

        private fun newPage() {
            finishPage()
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page?.canvas
            cursorY = PAGE_MARGIN
        }

        private fun finishPage() {
            val currentPage = page ?: return
            val currentCanvas = canvas ?: return
            currentCanvas.drawLine(
                PAGE_MARGIN,
                PAGE_HEIGHT - FOOTER_SPACE + 6f,
                PAGE_WIDTH - PAGE_MARGIN,
                PAGE_HEIGHT - FOOTER_SPACE + 6f,
                borderPaint,
            )
            currentCanvas.drawText(
                "Generado por CuidaVoz",
                PAGE_MARGIN,
                PAGE_HEIGHT - 12f,
                footerPaint,
            )
            currentCanvas.drawText(
                "Pagina $pageNumber",
                PAGE_WIDTH - PAGE_MARGIN - 48f,
                PAGE_HEIGHT - 12f,
                footerPaint,
            )
            document.finishPage(currentPage)
            page = null
            canvas = null
        }

        private fun Paint.lineHeight(): Float {
            return fontMetrics.descent - fontMetrics.ascent
        }
    }

    companion object {
        private const val PAGE_WIDTH = 595f
        private const val PAGE_HEIGHT = 842f
        private const val PAGE_MARGIN = 40f
        private const val FOOTER_SPACE = 40f
        private const val CELL_HORIZONTAL_PADDING = 4f
        private const val CELL_VERTICAL_PADDING = 4f
        private const val MAX_TABLE_ROWS = 20
        private const val PDF_TAG = "[CuidaVoz][PDF]"
    }
}
