package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.testing.MedicationTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class MedicationGroupingExtendedTest {
    @Test
    fun medicationsDueOnDateExcludesInactiveMedications() {
        val active = MedicationTestFixtures.medication(id = "active")
        val inactive = MedicationTestFixtures.medication(id = "inactive", isActive = false)

        val due = MedicationGrouping.medicationsDueOnDate(
            medications = listOf(active, inactive),
            date = java.time.LocalDate.parse("2026-06-11"),
        )

        assertEquals(listOf("active"), due.map { it.id })
    }

    @Test
    fun nextOccurrencesGroupsByDateAndScheduleTime() {
        val medA = MedicationTestFixtures.medication(id = "a", scheduleTime = "08:00")
        val medB = MedicationTestFixtures.medication(id = "b", scheduleTime = "08:00")
        val from = LocalDateTime.parse("2026-06-11T07:00:00")

        val occurrences = MedicationGrouping.nextOccurrencesByScheduleTime(
            medications = listOf(medA, medB),
            fromDateTime = from,
        )

        assertTrue(occurrences.isNotEmpty())
        val firstGroup = occurrences.values.first()
        assertEquals(2, firstGroup.size)
    }

    @Test
    fun getNextMedicationGroupFallsBackToFirstPendingWhenAllTimesPassed() {
        val med = MedicationTestFixtures.medication(scheduleTime = "06:00")
        val now = LocalTime.of(22, 0)

        val next = MedicationGrouping.getNextMedicationGroup(
            medications = listOf(med),
            medicationLogs = emptyList(),
            now = now,
        )

        assertEquals("06:00", next?.scheduleTime)
    }

    @Test
    fun groupMedicationsByScheduleTimeSortsTimesAscending() {
        val meds = listOf(
            MedicationTestFixtures.medication(id = "late", scheduleTime = "20:00"),
            MedicationTestFixtures.medication(id = "early", scheduleTime = "08:00"),
        )

        val groups = MedicationGrouping.groupMedicationsByScheduleTime(meds)

        assertEquals(listOf("08:00", "20:00"), groups.map { it.scheduleTime })
    }
}
