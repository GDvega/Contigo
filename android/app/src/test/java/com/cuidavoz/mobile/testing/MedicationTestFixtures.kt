package com.cuidavoz.mobile.testing

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.domain.ScheduleType
import java.time.LocalDate

object MedicationTestFixtures {
    fun medication(
        id: String = "med-1",
        name: String = "Losartan",
        scheduleTime: String = "08:00",
        scheduleType: ScheduleType = ScheduleType.ALWAYS,
        startDate: String = "2026-01-01",
        endDate: String? = null,
        daysOfWeek: List<Int> = MedicationScheduleDefaults.allDaysOfWeek.toList(),
        specificDates: List<LocalDate> = emptyList(),
        isActive: Boolean = true,
    ): MedicationEntity = MedicationEntity(
        id = id,
        patientId = "patient-1",
        name = name,
        dose = "1 tableta",
        color = null,
        shape = null,
        instructions = null,
        scheduleTime = scheduleTime,
        imageUri = null,
        isActive = isActive,
        scheduleType = scheduleType.name,
        startDate = startDate,
        endDate = endDate,
        daysOfWeek = daysOfWeek,
        specificDates = specificDates,
        createdAt = 0L,
        updatedAt = 0L,
    )

    fun log(
        id: String,
        medicationId: String,
        status: String,
        scheduledFor: Long = 1_000L,
    ): MedicationLogEntity = MedicationLogEntity(
        id = id,
        medicationId = medicationId,
        patientId = "patient-1",
        scheduledFor = scheduledFor,
        takenAt = if (status == "TAKEN") scheduledFor + 100L else null,
        status = status,
        createdAt = scheduledFor,
    )

    fun pressureReading(
        status: PressureStatus = PressureStatus.NORMAL,
        measuredAt: Long = 1_000L,
    ): BloodPressureEntity = BloodPressureEntity(
        id = "pressure-1",
        patientId = "patient-1",
        systolic = 120,
        diastolic = 80,
        pulse = 70,
        status = status.name,
        notes = null,
        measuredAt = measuredAt,
        createdAt = measuredAt,
    )
}
