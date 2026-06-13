package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.testing.MedicationTestFixtures
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AdherenceCalculatorExtendedTest {
    @Test
    fun `today adherence excludes medications not due today`() {
        val mondayOnly = MedicationTestFixtures.medication(
            id = "weekly",
            scheduleType = ScheduleType.WEEKLY_DAYS,
            startDate = "2026-01-01",
            daysOfWeek = listOf(1),
        )
        val tuesday = LocalDate.parse("2026-06-09")
        require(!MedicationScheduleCalculator.isMedicationDueOnDate(mondayOnly, tuesday))

        val summary = AdherenceCalculator.calculateTodayAdherence(
            logs = emptyList(),
            activeMedications = listOf(mondayOnly),
            today = tuesday,
        )

        assertEquals(0, summary.totalScheduled)
        assertEquals(0, summary.totalPending)
        assertEquals(100, summary.adherencePercentage)
        assertEquals(true, summary.hasActiveMedications)
    }

    @Test
    fun `range adherence counts only due slots across days`() {
        val alwaysMed = MedicationTestFixtures.medication(id = "a")
        val mondayOnly = MedicationTestFixtures.medication(
            id = "b",
            scheduleType = ScheduleType.WEEKLY_DAYS,
            startDate = "2026-06-01",
            daysOfWeek = listOf(1),
        )
        val endDate = LocalDate.parse("2026-06-09") // martes; rango 7 días incluye un lunes

        val summary = AdherenceCalculator.calculateAdherenceForRange(
            logs = emptyList(),
            activeMedications = listOf(alwaysMed, mondayOnly),
            days = 7,
            endDate = endDate,
        )

        assertEquals(8, summary.totalScheduled)
        assertEquals(8, summary.totalPending)
    }

    @Test
    fun `skipped logs reduce pending count in adherence summary`() {
        val med = MedicationTestFixtures.medication(id = "m1")
        val today = LocalDate.parse("2026-06-11")
        val scheduledFor = scheduleTimeToMillis(med.scheduleTime, today, ZoneId.systemDefault())
        val logs = listOf(
            MedicationTestFixtures.log("l1", "m1", "SKIPPED", scheduledFor = scheduledFor),
        )

        val summary = AdherenceCalculator.calculateTodayAdherence(
            logs = logs,
            activeMedications = listOf(med),
            today = today,
        )

        assertEquals(0, summary.totalTaken)
        assertEquals(0, summary.totalPending)
        assertEquals(1, summary.totalScheduled)
    }

    @Test
    fun `negative days in range yields zero scheduled slots`() {
        val med = MedicationTestFixtures.medication()

        val summary = AdherenceCalculator.calculateAdherenceForRange(
            logs = emptyList(),
            activeMedications = listOf(med),
            days = -3,
        )

        assertEquals(0, summary.totalScheduled)
        assertEquals(100, summary.adherencePercentage)
    }
}
