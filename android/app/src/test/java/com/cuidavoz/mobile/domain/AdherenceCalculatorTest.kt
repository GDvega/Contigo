package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdherenceCalculatorTest {
    @Test
    fun `calculate adherence avoids duplicate taken logs on same day`() {
        val medication = medication("med-1")
        val logs = listOf(
            log("log-1", "med-1", scheduledFor = 1_000L, status = "TAKEN"),
            log("log-2", "med-1", scheduledFor = 2_000L, status = "TAKEN"),
        )

        val summary = AdherenceCalculator.calculateTodayAdherence(logs, listOf(medication))

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
