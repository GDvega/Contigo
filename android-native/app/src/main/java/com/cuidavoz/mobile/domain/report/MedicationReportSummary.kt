package com.cuidavoz.mobile.domain.report

data class MedicationReportSummary(
    val activeMedicationCount: Int,
    val totalMedicationLogs: Int,
    val takenCount: Int,
    val pendingOrSkippedCount: Int,
    val adherencePercentage: Int,
)
