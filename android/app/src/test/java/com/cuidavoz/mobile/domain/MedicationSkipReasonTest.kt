package com.cuidavoz.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationSkipReasonTest {
    @Test
    fun `out of stock uses dedicated alert type`() {
        val reason = MedicationSkipReason.OUT_OF_STOCK

        assertEquals("medication_out_of_stock", reason.alertType())
        assertEquals("medium", reason.alertSeverity())
        assertEquals("Se acabó", reason.displayLabel())
    }

    @Test
    fun `felt unwell uses high severity`() {
        val reason = MedicationSkipReason.FELT_UNWELL

        assertEquals("medication_skipped", reason.alertType())
        assertEquals("high", reason.alertSeverity())
    }

    @Test
    fun `status detail includes skip reason label`() {
        assertEquals(
            "Omitido · Se acabó",
            medicationStatusDetail("SKIPPED", MedicationSkipReason.OUT_OF_STOCK.name),
        )
    }

    @Test
    fun `fromStorage parses persisted reason`() {
        assertEquals(MedicationSkipReason.FORGOT, MedicationSkipReason.fromStorage("FORGOT"))
        assertEquals(null, MedicationSkipReason.fromStorage(null))
    }
}
