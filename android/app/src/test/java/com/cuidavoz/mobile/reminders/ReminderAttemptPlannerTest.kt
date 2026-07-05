package com.cuidavoz.mobile.reminders

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderAttemptPlannerTest {
    @Test
    fun groupsPastillasWithSameHour() {
        val medications = listOf(
            medication(id = "a", name = "Paracetamol", time = "08:00"),
            medication(id = "b", name = "Aspirina", time = "08:00"),
        )

        val plans = ReminderAttemptPlanner.groupUpcomingMedications(
            patientId = "patient-1",
            medications = medications,
            fromDateTime = LocalDateTime.parse("2026-05-17T07:00:00"),
        )

        assertEquals(1, plans.size)
        assertEquals(listOf("a", "b"), plans.first().medicationIds)
    }

    @Test
    fun keepsRegistrationOrderInsideSameHour() {
        val medications = listOf(
            medication(id = "new", name = "Aspirina", time = "08:00", createdAt = 2L),
            medication(id = "old", name = "Paracetamol", time = "08:00", createdAt = 1L),
        )

        val plans = ReminderAttemptPlanner.groupUpcomingMedications(
            patientId = "patient-1",
            medications = medications,
            fromDateTime = LocalDateTime.parse("2026-05-17T07:00:00"),
        )

        assertEquals(listOf("old", "new"), plans.first().medicationIds)
        assertEquals(listOf("Paracetamol", "Aspirina"), plans.first().medicationNames)
    }

    @Test
    fun doesNotPlanExpiredMedication() {
        val plans = ReminderAttemptPlanner.groupUpcomingMedications(
            patientId = "patient-1",
            medications = listOf(
                medication(
                    id = "old",
                    name = "Losartan",
                    time = "08:00",
                    scheduleType = ScheduleType.DATE_RANGE,
                    startDate = "2026-05-01",
                    endDate = "2026-05-05",
                ),
            ),
            fromDateTime = LocalDateTime.parse("2026-05-17T07:00:00"),
        )

        assertTrue(plans.isEmpty())
    }

    @Test
    fun schedulesNextAttemptAtConfiguredMinutes() {
        val next = ReminderAttemptPlanner.nextAttemptTime(
            scheduledAt = 1_000L,
            repeatEveryMinutes = 10,
        )

        assertEquals(601_000L, next)
    }

    private fun medication(
        id: String,
        name: String,
        time: String,
        scheduleType: ScheduleType = ScheduleType.ALWAYS,
        startDate: String = "2026-05-17",
        endDate: String? = null,
        createdAt: Long = 0L,
    ) = MedicationEntity(
        id = id,
        patientId = "patient-1",
        name = name,
        dose = "1 tableta",
        color = null,
        shape = null,
        instructions = null,
        scheduleTime = time,
        imageUri = null,
        isActive = true,
        scheduleType = scheduleType.name,
        startDate = startDate,
        endDate = endDate,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        specificDates = emptyList(),
        createdAt = createdAt,
        updatedAt = 0L,
    )
}
