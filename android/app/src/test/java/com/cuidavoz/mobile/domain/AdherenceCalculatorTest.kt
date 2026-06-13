package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AdherenceCalculatorTest {
    @Test
    fun `calculate adherence marks scheduled slot as taken with matching log`() {
        val today = LocalDate.now()
        val scheduledFor = scheduleTimeToMillis("08:00", today, ZoneId.systemDefault())
        val medication = medication("med-1")
        val logs = listOf(
            log("log-1", "med-1", scheduledFor = scheduledFor, status = "TAKEN"),
            log("log-2", "med-1", scheduledFor = scheduledFor + 1L, status = "TAKEN"),
        )

        val summary = AdherenceCalculator.calculateTodayAdherence(
            logs = logs,
            activeMedications = listOf(medication),
            today = today,
        )

        assertEquals(1, summary.totalScheduled)
        assertEquals(1, summary.totalTaken)
        assertEquals(0, summary.totalPending)
        assertEquals(100, summary.adherencePercentage)
        assertTrue(summary.hasActiveMedications)
    }

    @Test
    fun `calculate adherence returns safe defaults without medications`() {
        val summary = AdherenceCalculator.calculateAdherenceForRange(emptyList(), emptyList(), days = 7)

        assertEquals(100, summary.adherencePercentage)
        assertFalse(summary.hasActiveMedications)
    }

    private fun medication(id: String) = MedicationEntity(
        id = id,
        patientId = "patient",
        name = "Losartan",
        dose = "1",
        color = null,
        shape = null,
        instructions = null,
        scheduleTime = "08:00",
        imageUri = null,
        isActive = true,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun log(
        id: String,
        medicationId: String,
        scheduledFor: Long,
        status: String,
    ) = MedicationLogEntity(
        id = id,
        medicationId = medicationId,
        patientId = "patient",
        scheduledFor = scheduledFor,
        takenAt = scheduledFor + 100L,
        status = status,
        createdAt = scheduledFor,
    )
}
