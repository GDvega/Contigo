package com.cuidavoz.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationOutcomeUserMessageTest {
    @Test
    fun noRecordedShowsEmptyMessage() {
        val message = medicationOutcomeUserMessage(
            MedicationOutcomeResult(savedCount = 0, skippedCount = 0, stillPendingCount = 2),
        )

        assertEquals("No había pastillas pendientes para registrar.", message)
    }

    @Test
    fun allSkippedResolvedGroup() {
        val message = medicationOutcomeUserMessage(
            MedicationOutcomeResult(savedCount = 0, skippedCount = 2, stillPendingCount = 0),
        )

        assertEquals("Quedó registrado que no pudiste tomar tus pastillas.", message)
    }

    @Test
    fun mixedTakenAndSkipped() {
        val message = medicationOutcomeUserMessage(
            MedicationOutcomeResult(savedCount = 1, skippedCount = 1, stillPendingCount = 0),
        )

        assertEquals("Tomas registradas. Algunas quedaron marcadas como no tomadas.", message)
    }

    @Test
    fun oneStillPendingUsesSingular() {
        val message = medicationOutcomeUserMessage(
            MedicationOutcomeResult(savedCount = 1, skippedCount = 0, stillPendingCount = 1),
        )

        assertEquals("Registrado. Aún queda 1 pastilla pendiente.", message)
    }

    @Test
    fun multipleStillPendingUsesPlural() {
        val message = medicationOutcomeUserMessage(
            MedicationOutcomeResult(savedCount = 1, skippedCount = 0, stillPendingCount = 3),
        )

        assertEquals("Registrado. Aún quedan 3 pastillas pendientes.", message)
    }
}

class MedicationSkipReasonDetailTest {
    @Test
    fun fromStorageReturnsNullForBlank() {
        assertEquals(null, MedicationSkipReason.fromStorage(null))
        assertEquals(null, MedicationSkipReason.fromStorage(" "))
    }

    @Test
    fun fromStorageParsesKnownValues() {
        assertEquals(MedicationSkipReason.FORGOT, MedicationSkipReason.fromStorage("FORGOT"))
    }

    @Test
    fun statusDetailCombinesSkippedWithReason() {
        assertEquals(
            "Omitido · La olvidé",
            medicationStatusDetail("SKIPPED", "FORGOT"),
        )
    }

    @Test
    fun statusDetailWithoutReasonShowsBaseOnly() {
        assertEquals("Tomado", medicationStatusDetail("TAKEN", null))
        assertEquals("Omitido", medicationStatusDetail("SKIPPED", null))
    }

    @Test
    fun alertSeverityForFeltUnwellIsHigh() {
        assertEquals("high", MedicationSkipReason.FELT_UNWELL.alertSeverity())
        assertEquals("medium", MedicationSkipReason.FORGOT.alertSeverity())
    }

    @Test
    fun alertMessageIncludesMedicationAndTime() {
        val message = MedicationSkipReason.OUT_OF_STOCK.alertMessage("Losartan", "08:00")

        assertEquals("Se acabó Losartan de las 08:00.", message)
    }
}
