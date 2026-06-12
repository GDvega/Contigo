package com.cuidavoz.mobile.domain.report

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.util.formatDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MedicalReportBuilder {
    fun periodLabel(period: MedicalReportPeriod): String {
        return when (period) {
            MedicalReportPeriod.LAST_7_DAYS -> "Últimos 7 días"
            MedicalReportPeriod.LAST_30_DAYS -> "Últimos 30 días"
            MedicalReportPeriod.ALL -> "Todo"
        }
    }

    fun periodRangeLabel(reportData: MedicalReportData): String {
        return "${formatDate(reportData.periodStart)} - ${formatDate(reportData.periodEnd)}"
    }

    fun pressureStatusLabel(status: String): String {
        return when (runCatching { PressureStatus.valueOf(status) }.getOrNull()) {
            PressureStatus.NORMAL -> "Dentro del rango indicado"
            PressureStatus.ELEVATED -> "Valor elevado"
            PressureStatus.HIGH -> "Valor alto, revisar con profesional de salud"
            PressureStatus.CRITICAL -> "Valor muy alto, buscar orientación médica"
            PressureStatus.OUT_OF_RANGE -> "Fuera del rango indicado"
            null -> status
        }
    }

    fun medicationStatusLabel(status: String, skipReason: String? = null): String {
        return com.cuidavoz.mobile.domain.medicationStatusDetail(status, skipReason)
    }

    fun latestPressureLabel(reading: BloodPressureEntity?): String {
        return reading?.let {
            "${it.systolic}/${it.diastolic}" +
                if (it.pulse != null) " - Pulso ${it.pulse}" else ""
        } ?: "Sin registros"
    }

    fun latestPressureDateLabel(reading: BloodPressureEntity?): String {
        return reading?.let { formatDateTime(it.measuredAt) } ?: "-"
    }

    fun medicationActiveStatusLabel(medication: MedicationEntity): String {
        return if (medication.isExpired()) "Vencido" else "Activo"
    }

    fun medicationDurationLabel(medication: MedicationEntity): String {
        return medication.treatmentSummary()
    }

    fun generalStatusLabel(summary: PressureReportSummary): String {
        return when {
            summary.highOrCriticalCount > 5 -> "Crítico - Revisión inmediata"
            summary.highOrCriticalCount > 0 -> "Elevado - Seguimiento cercano"
            summary.outOfRangeCount > 3 -> "Inestable"
            summary.totalPressureReadings == 0 -> "Sin datos"
            else -> "Estable"
        }
    }

    fun hasEnoughData(reportData: MedicalReportData): Boolean {
        return reportData.pressureReadings.isNotEmpty() ||
            reportData.activeMedications.isNotEmpty() ||
            reportData.medicationLogs.isNotEmpty()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
