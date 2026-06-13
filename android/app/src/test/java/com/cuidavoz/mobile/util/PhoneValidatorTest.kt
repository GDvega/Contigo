package com.cuidavoz.mobile.util

import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneValidatorTest {
    @Test
    fun acceptsValidInternationalNumbers() {
        assertTrue(PhoneValidator.isValid("+56912345678"))
        assertTrue(PhoneValidator.isValid("912345678"))
    }

    @Test
    fun rejectsTooShortNumbers() {
        assertFalse(PhoneValidator.isValid("123456"))
    }

    @Test
    fun rejectsLettersAndSpaces() {
        assertFalse(PhoneValidator.isValid("+56 9 1234 5678"))
        assertFalse(PhoneValidator.isValid("abc1234567"))
    }

    @Test
    fun normalizeKeepsDigitsAndPlusOnly() {
        assertEquals("+56912345678", PhoneValidator.normalize("+56 (9) 1234-5678"))
    }

    @Test
    fun trimBeforeValidation() {
        assertTrue(PhoneValidator.isValid("  912345678  "))
    }
}

class DefaultHealthSettingsTest {
    @Test
    fun createsExpectedClinicalRanges() {
        val settings = DefaultHealthSettings.createForPatient(
            patientId = "patient-1",
            now = 42L,
        )

        assertEquals("patient-1", settings.patientId)
        assertEquals(100, settings.systolicMinNormal)
        assertEquals(130, settings.systolicMaxNormal)
        assertEquals(60, settings.diastolicMinNormal)
        assertEquals(85, settings.diastolicMaxNormal)
        assertEquals(42L, settings.updatedAt)
        assertTrue(settings.id.startsWith("health_settings_"))
    }

    @Test
    fun preservesExistingIdWhenProvided() {
        val settings = DefaultHealthSettings.createForPatient(
            patientId = "patient-1",
            existingId = "fixed-id",
        )

        assertEquals("fixed-id", settings.id)
    }
}

class LocalIdTest {
    @Test
    fun generatedIdsContainPrefix() {
        val id = createLocalId("medication")

        assertTrue(id.startsWith("medication_"))
    }

    @Test
    fun generatedIdsAreUnique() {
        val ids = (1..20).map { createLocalId("test") }.toSet()

        assertEquals(20, ids.size)
    }
}
