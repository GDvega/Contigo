package com.cuidavoz.mobile.domain.report

import com.cuidavoz.mobile.data.model.BloodPressureEntity

data class PressureReportSummary(
    val totalPressureReadings: Int,
    val latestPressure: BloodPressureEntity?,
    val averageSystolic: Int?,
    val averageDiastolic: Int?,
    val averagePulse: Int?,
    val outOfRangeCount: Int,
    val highOrCriticalCount: Int,
)
