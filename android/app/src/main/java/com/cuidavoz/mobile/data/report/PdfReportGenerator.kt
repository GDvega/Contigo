package com.cuidavoz.mobile.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.cuidavoz.mobile.util.ContigoLog
import androidx.core.content.FileProvider
import com.cuidavoz.mobile.domain.report.MedicalReportBuilder
import com.cuidavoz.mobile.domain.report.MedicalReportData
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.util.formatDate
import com.cuidavoz.mobile.util.formatDateTime
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.formatTimeForDisplay
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

fun formatDateLocal(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

class PdfReportGenerator(
    private val context: Context,
) {
    suspend fun generateToCache(reportData: MedicalReportData): GeneratedPdfFile = withContext(Dispatchers.IO) {
        ContigoLog.d(PDF_TAG, "Generando PDF temporal")
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
        ContigoLog.d(PDF_TAG, "Guardando PDF en destino SAF")
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

            // --- ENCABEZADO ---
            composer.drawTitle("Reporte médico")
            composer.drawSubtitle("Resumen de presión arterial, medicamentos y seguimiento")

            var currentX = PAGE_MARGIN
            currentX += composer.drawChip(MedicalReportBuilder.periodLabel(reportData.period), currentX, composer.getCursorY()) + 8f
            composer.drawChip("Fecha de generación: ${formatDateTime(reportData.generatedAt)}", currentX, composer.getCursorY())
            composer.drawSpacer(32f)

            // --- SECCIÓN 1: DATOS DEL PACIENTE ---
            composer.drawSectionHeader("1) Datos del paciente")
            composer.drawCard(height = 60f) { canvas, rect ->
                val colWidth = rect.width() / 4
                val yLabel = rect.top + 20f
                val yVal = rect.top + 45f

                // Nombre
                canvas.drawText("Nombre", rect.left + 15f, yLabel, composer.getMutedPaint())
                canvas.drawText(reportData.patient?.fullName ?: "-", rect.left + 15f, yVal, composer.getBodyPaint().apply { typeface = Typeface.DEFAULT_BOLD })

                // Edad
                canvas.drawText("Edad", rect.left + colWidth + 15f, yLabel, composer.getMutedPaint())
                canvas.drawText("${reportData.patient?.age ?: "-"} años", rect.left + colWidth + 15f, yVal, composer.getBodyPaint())

                // Contacto
                canvas.drawText("Contacto familiar", rect.left + colWidth * 2 + 15f, yLabel, composer.getMutedPaint())
                canvas.drawText(reportData.familyContact?.fullName ?: "-", rect.left + colWidth * 2 + 15f, yVal, composer.getBodyPaint())

                // Teléfono
                canvas.drawText("Teléfono", rect.left + colWidth * 3 + 15f, yLabel, composer.getMutedPaint())
                canvas.drawText(reportData.familyContact?.phone ?: "-", rect.left + colWidth * 3 + 15f, yVal, composer.getBodyPaint())
            }

            // --- SECCIÓN 2: RESUMEN DE PRESIÓN ARTERIAL ---
            composer.drawSectionHeader("2) Resumen de presión arterial")

            // Tarjetas Estadísticas
            composer.drawCard(height = 65f) { canvas, rect ->
                val colWidth = rect.width() / 6
                val stats = listOf(
                    "Total" to "${reportData.pressureSummary.totalPressureReadings}",
                    "Promedio" to "${reportData.pressureSummary.averageSystolic ?: "-"}/${reportData.pressureSummary.averageDiastolic ?: "-"}",
                    "Último" to (reportData.pressureSummary.latestPressure?.let { "${it.systolic}/${it.diastolic}" } ?: "-"),
                    "Pulso" to "${reportData.pressureSummary.averagePulse ?: "-"}",
                    "Fuera rango" to "${reportData.pressureSummary.outOfRangeCount}",
                    "Estado" to MedicalReportBuilder.generalStatusLabel(reportData.pressureSummary)
                )

                stats.forEachIndexed { i, stat ->
                    val x = rect.left + (i * colWidth) + 10f
                    canvas.drawText(stat.first, x, rect.top + 20f, composer.getMutedPaint())
                    canvas.drawText(stat.second, x, rect.top + 45f, composer.getBodyPaint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 13f })
                }
            }

            // Gráfica
            composer.drawPressureChart(reportData.pressureReadings)

            // Tabla de Registros
            val pressureRows = reportData.pressureReadings.map { reading ->
                listOf(
                    formatDateLocal(reading.measuredAt),
                    formatTimeForDisplay(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reading.measuredAt))),
                    "${reading.systolic}/${reading.diastolic}",
                    reading.pulse?.toString() ?: "-",
                    MedicalReportBuilder.pressureStatusLabel(reading.status)
                )
            }
            composer.drawTable(
                headers = listOf("Fecha", "Hora", "Presión (mmHg)", "Pulso (lpm)", "Estado"),
                rows = pressureRows,
                columnWeights = floatArrayOf(1.2f, 1f, 1.2f, 1f, 1f),
                emptyMessage = "Sin registros de presión.",
                statusColumnIndex = 4
            )

            // --- SECCIÓN 3 Y 4: MEDICAMENTOS Y RANGOS ---
            composer.drawSpacer(16f)

            // 3) Medicamentos (Lado Izquierdo)
            composer.drawSectionHeader("3) Medicamentos y adherencia")

            val medicationsToShow = reportData.activeMedications
            val medicationCardHeight = if (medicationsToShow.isEmpty()) 80f else 60f + (medicationsToShow.size * 25f)

            composer.drawCard(height = medicationCardHeight.coerceAtMost(300f)) { canvas, rect ->
                // Renderizar pequeña tabla interna o lista
                canvas.drawText("Adherencia del periodo", rect.left + 80f, rect.bottom - 25f, composer.getMutedPaint())
                composer.drawCircularAdherence(rect.left + 40f, rect.bottom - 30f, reportData.medicationSummary.adherencePercentage)

                // Lista de medicamentos
                medicationsToShow.forEachIndexed { i, med ->
                    val y = rect.top + 25f + (i * 25f)
                    if (y < rect.bottom - 40f) { // Evitar sobrelapar con el círculo de adherencia si hay demasiados
                         canvas.drawText("${med.name} (${med.dose})", rect.left + 15f, y, composer.getBodyPaint())
                         canvas.drawText(formatScheduleTime(med.scheduleTime), rect.left + 180f, y, composer.getMutedPaint())
                    }
                }
            }

            composer.drawSectionHeader("4) Rangos médicos")
            composer.drawCard(height = 100f) { canvas, rect ->
                val settings = reportData.healthSettings
                val ranges = listOf(
                    "Sistólica normal" to "${settings?.systolicMinNormal ?: 100}-${settings?.systolicMaxNormal ?: 130} mmHg",
                    "Diastólica normal" to "${settings?.diastolicMinNormal ?: 60}-${settings?.diastolicMaxNormal ?: 85} mmHg",
                    "Pulso normal" to "${settings?.pulseMinNormal ?: 60}-${settings?.pulseMaxNormal ?: 100} lpm"
                )
                ranges.forEachIndexed { i, range ->
                    val y = rect.top + 25f + (i * 20f)
                    canvas.drawText(range.first, rect.left + 15f, y, composer.getBodyPaint())
                    canvas.drawText(range.second, rect.right - 100f, y, composer.getBodyPaint().apply { typeface = Typeface.DEFAULT_BOLD })
                }
            }

            // --- SECCIÓN 5: OBSERVACIONES ---
            composer.drawSectionHeader("5) Observaciones")
            val recommendation = reportData.doctorRecommendation ?: "Se recomienda continuar el seguimiento diario."
            composer.drawRecommendation(recommendation)

            composer.finish(outputStream)
        } finally {
            document.close()
        }
    }

    private class PdfComposer(
        private val document: PdfDocument,
    ) {
        // --- Paleta de Colores Premium ---
        private val COLOR_PRIMARY = Color.rgb(0, 132, 119) // Verde Médico
        private val COLOR_SECONDARY = Color.rgb(26, 35, 126) // Azul Profundo
        private val COLOR_BACKGROUND = Color.rgb(245, 247, 248) // Gris Fondo
        private val COLOR_TEXT_PRIMARY = Color.rgb(33, 37, 41)
        private val COLOR_TEXT_MUTED = Color.rgb(108, 117, 125)
        private val COLOR_WHITE = Color.WHITE
        private val COLOR_BORDER = Color.rgb(218, 224, 229)

        // --- Pinceles (Paints) ---
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 11f
        }
        private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
        }
        private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        private val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 10f
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            style = Paint.Style.FILL
        }
        private val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 242, 241) // Verde muy claro
            style = Paint.Style.FILL
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9f
        }

        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var cursorY = PAGE_MARGIN

        fun getCursorY() = cursorY
        fun getMutedPaint() = mutedPaint
        fun getBodyPaint() = bodyPaint

        fun drawTitle(text: String) {
            ensurePage()
            drawWrappedText(text, titlePaint, extraSpacing = 4f)
        }

        fun drawSubtitle(text: String) {
            ensurePage()
            drawWrappedText(text, mutedPaint.apply { textSize = 12f }, extraSpacing = 12f)
        }

        fun drawChip(text: String, x: Float, y: Float): Float {
            val padding = 8f
            val textWidth = mutedPaint.measureText(text)
            val rect = android.graphics.RectF(x, y, x + textWidth + padding * 2, y + 24f)
            canvas?.drawRoundRect(rect, 12f, 12f, chipBgPaint)
            canvas?.drawText(text, x + padding, y + 16f, mutedPaint)
            return rect.width()
        }

        fun drawCard(
            height: Float,
            marginTop: Float = 8f,
            marginBottom: Float = 12f,
            block: (Canvas, android.graphics.RectF) -> Unit
        ) {
            ensureSpace(height + marginTop + marginBottom)
            cursorY += marginTop
            val rect = android.graphics.RectF(
                PAGE_MARGIN,
                cursorY,
                PAGE_WIDTH - PAGE_MARGIN,
                cursorY + height
            )
            canvas?.drawRoundRect(rect, 12f, 12f, cardBgPaint)
            canvas?.drawRoundRect(rect, 12f, 12f, borderPaint)
            canvas?.let { block(it, rect) }
            cursorY += height + marginBottom
        }

        fun drawSectionHeader(text: String, icon: String? = null) {
            ensureSpace(sectionPaint.lineHeight() + 24f)
            cursorY += 8f
            val currentCanvas = canvas ?: return

            // Icon Placeholder (Small circle for now)
            currentCanvas.drawCircle(PAGE_MARGIN + 10f, cursorY + 8f, 10f, chipBgPaint)

            currentCanvas.drawText(text, PAGE_MARGIN + 30f, cursorY + 14f, sectionPaint)
            cursorY += 24f
        }

        fun drawCircularAdherence(x: Float, y: Float, percentage: Int) {
            val radius = 25f
            val rect = android.graphics.RectF(x - radius, y - radius, x + radius, y + radius)
            val currentCanvas = canvas ?: return

            val trackPaint = Paint(borderPaint).apply { strokeWidth = 5f }
            val progressPaint = Paint(trackPaint).apply {
                color = if (percentage >= 80) COLOR_PRIMARY else Color.rgb(255, 152, 0)
                strokeCap = Paint.Cap.ROUND
            }

            currentCanvas.drawCircle(x, y, radius, trackPaint)
            currentCanvas.drawArc(rect, -90f, (percentage * 3.6f), false, progressPaint)

            val text = "$percentage%"
            val textPaint = Paint(tableHeaderPaint).apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            currentCanvas.drawText(text, x, y + 5f, textPaint)
        }

        fun drawPressureChart(
            readings: List<com.cuidavoz.mobile.data.model.BloodPressureEntity>,
            height: Float = 220f
        ) {
            if (readings.isEmpty()) return

            ensureSpace(height + 60f)
            val currentCanvas = canvas ?: return
            val chartLeft = PAGE_MARGIN + 35f
            val chartRight = PAGE_WIDTH - PAGE_MARGIN - 15f
            val chartTop = cursorY + 25f
            val chartBottom = cursorY + height - 35f
            val width = chartRight - chartLeft
            val h = chartBottom - chartTop

            // Grid y Ejes
            val gridPaint = Paint(borderPaint).apply { color = Color.rgb(240, 240, 240); strokeWidth = 0.5f }
            val axisLabelPaint = Paint(mutedPaint).apply { textSize = 8f; textAlign = Paint.Align.RIGHT }

            // Eje Y con parámetros
            val ySteps = 8
            val maxVal = 200f
            val minVal = 40f
            for (i in 0..ySteps) {
                val valY = minVal + (i * (maxVal - minVal) / ySteps)
                val ratio = i.toFloat() / ySteps
                val yPos = chartBottom - (ratio * h)
                currentCanvas.drawLine(chartLeft, yPos, chartRight, yPos, gridPaint)
                currentCanvas.drawText(valY.toInt().toString(), chartLeft - 5f, yPos + 3f, axisLabelPaint)
            }

            currentCanvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, borderPaint)
            currentCanvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, borderPaint)

            val data = readings.reversed().take(10) // Mostrar últimos 10
            if (data.size < 1) return

            val stepX = if (data.size > 1) width / (data.size - 1) else width
            val sysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY; strokeWidth = 2.5f; style = Paint.Style.STROKE }
            val diaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SECONDARY; strokeWidth = 2.5f; style = Paint.Style.STROKE }
            val pulseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; strokeWidth = 1.5f; style = Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f) }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 9f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            fun getYPos(value: Int): Float {
                val ratio = (value - minVal) / (maxVal - minVal)
                return chartBottom - (ratio * h)
            }

            val sysPath = android.graphics.Path()
            val diaPath = android.graphics.Path()
            val pulsePath = android.graphics.Path()

            data.forEachIndexed { index, reading ->
                val x = chartLeft + (index * stepX)
                val ySys = getYPos(reading.systolic)
                val yDia = getYPos(reading.diastolic)
                val yPulse = reading.pulse?.let { getYPos(it) }

                if (index == 0) {
                    sysPath.moveTo(x, ySys)
                    diaPath.moveTo(x, yDia)
                    yPulse?.let { pulsePath.moveTo(x, it) }
                } else {
                    sysPath.lineTo(x, ySys)
                    diaPath.lineTo(x, yDia)
                    yPulse?.let { pulsePath.lineTo(x, it) }
                }

                // Puntos
                currentCanvas.drawCircle(x, ySys, 3.5f, Paint(sysPaint).apply { style = Paint.Style.FILL })
                currentCanvas.drawCircle(x, yDia, 3.5f, Paint(diaPaint).apply { style = Paint.Style.FILL })
                yPulse?.let { currentCanvas.drawCircle(x, it, 2.5f, Paint().apply { color = Color.GRAY; style = Paint.Style.FILL }) }

                // Etiquetas de Datos
                currentCanvas.drawText(reading.systolic.toString(), x, ySys - 8f, labelPaint)
                currentCanvas.drawText(reading.diastolic.toString(), x, yDia + 15f, labelPaint)
                yPulse?.let { currentCanvas.drawText(reading.pulse.toString(), x, it - 8f, Paint(labelPaint).apply { color = Color.GRAY; textSize = 8f }) }

                // Eje X con Fechas Completas
                val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(reading.measuredAt))
                currentCanvas.drawText(dateStr, x, chartBottom + 15f, mutedPaint.apply { textAlign = Paint.Align.CENTER })
            }

            currentCanvas.drawPath(sysPath, sysPaint)
            currentCanvas.drawPath(diaPath, diaPaint)
            currentCanvas.drawPath(pulsePath, pulseLinePaint)

            // Leyenda Triple
            val legendY = chartBottom + 35f
            currentCanvas.drawCircle(chartLeft + 10f, legendY - 3f, 4f, Paint().apply { color = COLOR_PRIMARY })
            currentCanvas.drawText("Sistólica", chartLeft + 20f, legendY, mutedPaint.apply { textAlign = Paint.Align.LEFT })

            currentCanvas.drawCircle(chartLeft + 80f, legendY - 3f, 4f, Paint().apply { color = COLOR_SECONDARY })
            currentCanvas.drawText("Diastólica", chartLeft + 90f, legendY, mutedPaint)

            currentCanvas.drawCircle(chartLeft + 160f, legendY - 3f, 4f, Paint().apply { color = Color.GRAY })
            currentCanvas.drawText("Pulso", chartLeft + 170f, legendY, mutedPaint)

            cursorY += height + 40f
        }

        fun drawSpacer(height: Float) {
            ensurePage()
            ensureSpace(height)
            cursorY += height
        }

        fun drawParagraph(text: String) {
            ensurePage()
            drawWrappedText(text, bodyPaint, extraSpacing = 6f)
        }

        fun drawTable(
            headers: List<String>,
            rows: List<List<String>>,
            columnWeights: FloatArray,
            emptyMessage: String,
            statusColumnIndex: Int = -1, // Nueva opción para dibujar puntos de color
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
                    currentCanvas.drawRect(x, cursorY, x + width, cursorY + rowHeight, tableHeaderBgPaint)
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
                    val availableWidth = if (index == statusColumnIndex) {
                        columnWidths[index] - (CELL_HORIZONTAL_PADDING * 2) - 15f
                    } else {
                        columnWidths[index] - (CELL_HORIZONTAL_PADDING * 2)
                    }
                    wrapText(value, tableCellPaint, availableWidth)
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

                    if (index == statusColumnIndex) {
                        val statusText = row[index]
                        val dotColor = when {
                            statusText.contains("Normal", true) -> Color.rgb(76, 175, 80)
                            statusText.contains("Atención", true) || statusText.contains("Elevado", true) -> Color.rgb(255, 152, 0)
                            statusText.contains("Bajo", true) -> Color.rgb(33, 150, 243)
                            else -> Color.rgb(244, 67, 54)
                        }
                        val dotPaint = Paint().apply { color = dotColor; style = Paint.Style.FILL }
                        currentCanvas.drawCircle(x + CELL_HORIZONTAL_PADDING + 5f, cursorY + (rowHeight / 2), 4f, dotPaint)

                        drawLinesInCell(
                            lines = cellLines[index],
                            x = x + 12f,
                            y = cursorY,
                            width = width - 12f,
                            rowHeight = rowHeight,
                            paint = tableCellPaint,
                        )
                    } else {
                        drawLinesInCell(
                            lines = cellLines[index],
                            x = x,
                            y = cursorY,
                            width = width,
                            rowHeight = rowHeight,
                            paint = tableCellPaint,
                        )
                    }
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

        fun drawRecommendation(recommendation: String) {
            val wrapped = wrapText(recommendation, bodyPaint, PAGE_WIDTH - (PAGE_MARGIN * 2) - 30f)
            val h = (wrapped.size * bodyPaint.lineHeight()) + 30f
            drawCard(height = h) { canvas, rect ->
                var y = rect.top + 25f
                wrapped.forEach { line ->
                    canvas.drawText(line, rect.left + 15f, y, bodyPaint)
                    y += bodyPaint.lineHeight()
                }
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
                "Generado por Contigo",
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
        private const val PDF_TAG = "[Contigo][PDF]"
    }
}
