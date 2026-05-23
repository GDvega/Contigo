package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.util.todayRangeMillis
import java.time.LocalDate
import java.time.ZoneId

class MedicationLogRepository(
    private val medicationLogDao: MedicationLogDao,
) {
    fun observeLogsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ) = medicationLogDao.observeLogsForRange(patientId, startAt, endAt)

    suspend fun getLogsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ) = medicationLogDao.getLogsForRange(patientId, startAt, endAt)

    suspend fun getTodayMedicationLogs(patientId: String): List<MedicationLogEntity> {
        val (startOfDay, endOfDay) = todayRangeMillis()
        return medicationLogDao.getLogsForDay(patientId, startOfDay, endOfDay)
    }

    suspend fun getMedicationLogsForDate(
        patientId: String,
        date: LocalDate,
    ): List<MedicationLogEntity> {
        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return medicationLogDao.getLogsForDay(patientId, start, end)
    }
}
