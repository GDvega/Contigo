package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class MedicationGroupingTest {
    @Test
    fun `group medications only includes active entries sorted by time`() {
        val groups = MedicationGrouping.groupMedicationsByScheduleTime(
            listOf(
                medication(id = "2", name = "B", schedule = "20:00"),
                medication(id = "1", name = "A", schedule = "08:00"),
                medication(id = "3", name = "C", schedule = "08:00", active = false),
            ),
        )

        assertEquals(listOf("08:00", "20:00"), groups.map { it.scheduleTime })
        assertEquals(listOf("A"), groups.first().medications.map { it.name })
    }

    @Test
    fun `next medication skips already taken schedule`() {
        val medications = listOf(
            medication(id = "1", name = "A", schedule = "08:00"),
            medication(id = "2", name = "B", schedule = "10:00"),
        )
        val logs = listOf(
            MedicationLogEntity(
                id = "log-1",
                medicationId = "1",
                patientId = "patient",
                scheduledFor = 1L,
                takenAt = 2L,
                status = "TAKEN",
                createdAt = 1L,
            ),
        )

        val next = MedicationGrouping.getNextMedicationGroup(medications, logs, now = LocalTime.of(9, 0))

        assertEquals("10:00", next?.scheduleTime)
    }

    @Test
    fun `next medication is null without active medications`() {
        val next = MedicationGrouping.getNextMedicationGroup(emptyList(), emptyList())

        assertNull(next)
    }

    private fun medication(
        id: String,
        name: String,
        schedule: String,
        active: Boolean = true,
    ) = MedicationEntity(
        id = id,
        patientId = "patient",
        name = name,
        dose = "1",
        color = null,
        shape = null,
        instructions = null,
        scheduleTime = schedule,
        imageUri = null,
        isActive = active,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
