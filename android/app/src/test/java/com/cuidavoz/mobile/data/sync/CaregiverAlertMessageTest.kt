package com.cuidavoz.mobile.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaregiverAlertMessageTest {
    @Test
    fun `acepta una alerta con identificadores completos`() {
        val result = CaregiverAlertMessage.from(
            mapOf(
                "kind" to "caregiver_alert",
                "familyId" to "family-1",
                "patientId" to "patient-1",
                "alertId" to "alert-1",
            ),
        )

        assertEquals("alert-1", result?.alertId)
    }

    @Test
    fun `rechaza mensajes incompletos o de otro tipo`() {
        assertNull(CaregiverAlertMessage.from(mapOf("kind" to "caregiver_alert")))
        assertNull(
            CaregiverAlertMessage.from(
                mapOf(
                    "kind" to "other",
                    "familyId" to "family-1",
                    "patientId" to "patient-1",
                    "alertId" to "alert-1",
                ),
            ),
        )
    }
}
