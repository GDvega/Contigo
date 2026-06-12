package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationScheduleCalculatorTest {
    @Test
    fun alwaysAppearsTodayAndTomorrow() {
        val medication = medication(
            scheduleType = ScheduleType.ALWAYS,
            startDate = "2026-05-16",
        )

        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-17")))
        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-18")))
    }

    @Test
    fun dateRangeAppearsInsideRangeAndNotAfter() {
        val medication = medication(
            scheduleType = ScheduleType.DATE_RANGE,
            startDate = "2026-05-16",
            endDate = "2026-05-20",
        )

        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-18")))
        assertFalse(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-21")))
    }

    @Test
    fun weeklyDaysAppearsOnlyMondayWednesdayFriday() {
        val medication = medication(
            scheduleType = ScheduleType.WEEKLY_DAYS,
            startDate = "2026-05-01",
            endDate = "2026-05-31",
            daysOfWeek = listOf(1, 3, 5),
        )

        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-18")))
        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-20")))
        assertFalse(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-19")))
    }

    @Test
    fun specificDatesAppearsOnlyOnChosenDates() {
        val medication = medication(
            scheduleType = ScheduleType.SPECIFIC_DATES,
            startDate = "2026-05-16",
            endDate = "2026-05-20",
            specificDates = listOf(LocalDate.parse("2026-05-16"), LocalDate.parse("2026-05-20")),
        )

        assertTrue(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-16")))
        assertFalse(MedicationScheduleCalculator.isMedicationDueOnDate(medication, LocalDate.parse("2026-05-17")))
    }

    @Test
    fun expiredMedicationHasNoNextOccurrence() {
        val medication = medication(
            scheduleType = ScheduleType.DATE_RANGE,
            startDate = "2026-05-01",
            endDate = "2026-05-05",
        )

        val next = MedicationScheduleCalculator.getNextMedicationOccurrence(
            medication,
            LocalDateTime.parse("2026-05-06T08:00:00"),
        )

        assertNull(next)
    }

    @Test
    fun medicationThatDoesNotMatchTodayIsNotMarkedPending() {
        val medication = medication(
            id = "losartan",
            scheduleType = ScheduleType.WEEKLY_DAYS,
            startDate = "2026-05-01",
            endDate = "2026-05-31",
            daysOfWeek = listOf(1, 3, 5),
        )

        val pending = MedicationScheduleCalculator.getTodayPendingMedications(
            medications = listOf(medication),
            logs = emptyList(),
            today = LocalDate.parse("2026-05-19"),
        )

        assertTrue(pending.isEmpty())
    }

    @Test
    fun getNextMedicationOccurrenceReturnsCorrectNextDate() {
        val medication = medication(
            scheduleType = ScheduleType.WEEKLY_DAYS,
            startDate = "2026-05-01",
            endDate = "2026-05-31",
            daysOfWeek = listOf(1, 3, 5),
            scheduleTime = "20:00",
        )

        val next = MedicationScheduleCalculator.getNextMedicationOccurrence(
            medication,
            LocalDateTime.parse("2026-05-19T09:00:00"),
        )

        assertEquals(LocalDateTime.parse("2026-05-20T20:00:00"), next)
    }

    private fun medication(
        id: String = "med-1",
        scheduleType: ScheduleType,
        startDate: String,
        endDate: String? = null,
        daysOfWeek: List<Int> = MedicationScheduleDefaults.allDaysOfWeek.toList(),
        specificDates: List<LocalDate> = emptyList(),
        scheduleTime: String = "08:00",
    ): MedicationEntity {
        return MedicationEntity(
            id = id,
            patientId = "patient-1",
            name = "Medicamento",
            dose = "1 tableta",
            color = null,
            shape = null,
            instructions = null,
            scheduleTime = scheduleTime,
            imageUri = null,
            isActive = true,
            scheduleType = scheduleType.name,
            startDate = startDate,
            endDate = endDate,
            daysOfWeek = daysOfWeek,
            specificDates = specificDates,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
