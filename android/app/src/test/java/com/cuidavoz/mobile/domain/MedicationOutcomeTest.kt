package com.cuidavoz.mobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationOutcomeTest {
    @Test
    fun `group is resolved when no medications remain pending`() {
        val result = MedicationOutcomeResult(
            savedCount = 1,
            skippedCount = 1,
            stillPendingCount = 0,
        )

        assertTrue(result.groupResolved)
        assertTrue(result.anyRecorded)
    }

    @Test
    fun `group stays open when one medication remains pending`() {
        val result = MedicationOutcomeResult(
            savedCount = 1,
            skippedCount = 0,
            stillPendingCount = 1,
        )

        assertFalse(result.groupResolved)
        assertTrue(result.anyRecorded)
    }

    @Test
    fun `empty submission is not recorded`() {
        val result = MedicationOutcomeResult(
            savedCount = 0,
            skippedCount = 0,
            stillPendingCount = 2,
        )

        assertFalse(result.anyRecorded)
        assertFalse(result.groupResolved)
    }
}
