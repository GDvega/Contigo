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

    @Test
    fun `partial taken leaves remaining medication pending at same schedule time`() {
        val medications = listOf(
            medication(id = "1", name = "A", schedule = "08:00"),
            medication(id = "2", name = "B", schedule = "08:00"),
        )
        val logs = listOf(
            medicationLog(id = "log-1", medicationId = "1", status = "TAKEN"),
        )

        val pending = MedicationGrouping.getPendingMedicationsForTime(
            scheduleTime = "08:00",
            medications = medications,
            medicationLogs = logs,
        )

        assertEquals(listOf("B"), pending.map { it.name })
    }

    @Test
    fun `skipped medication is removed from pending at same schedule time`() {
        val medications = listOf(
            medication(id = "1", name = "A", schedule = "08:00"),
            medication(id = "2", name = "B", schedule = "08:00"),
        )
        val logs = listOf(
            medicationLog(id = "log-1", medicationId = "1", status = "TAKEN"),
            medicationLog(id = "log-2", medicationId = "2", status = "SKIPPED"),
        )

        val pending = MedicationGrouping.getPendingMedicationsForTime(
            scheduleTime = "08:00",
            medications = medications,
            medicationLogs = logs,
        )

        assertEquals(emptyList<String>(), pending.map { it.name })
    }

    private fun medicationLog(
        id: String,
        medicationId: String,
        status: String,
    ) = MedicationLogEntity(
        id = id,
        medicationId = medicationId,
        patientId = "patient",
        scheduledFor = 1L,
        takenAt = if (status == "TAKEN") 2L else null,
        status = status,
        createdAt = 1L,
    )

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
