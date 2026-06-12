package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationSkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MedicationVoiceMatcherTest {
    private val losartan = medication("losartan", "Losartán")
    private val aspirin = medication("aspirin", "Aspirina")

    @Test
    fun matchesMedicationByNameForTaken() {
        val match = MedicationVoiceMatcher.match("ya tomé la losartán", listOf(losartan, aspirin))

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.TAKEN, match!!.action)
        assertEquals("losartan", match.medication?.id)
    }

    @Test
    fun matchesMedicationByNameForSkippedWithReason() {
        val match = MedicationVoiceMatcher.match(
            "no pude tomar la aspirina, se acabó",
            listOf(losartan, aspirin),
        )

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.SKIPPED, match!!.action)
        assertEquals("aspirin", match.medication?.id)
        assertEquals(MedicationSkipReason.OUT_OF_STOCK, match.skipReason)
    }

    @Test
    fun matchesAllTakenPhrase() {
        val match = MedicationVoiceMatcher.match("ya tomé todas mis pastillas", listOf(losartan, aspirin))

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.ALL_TAKEN, match!!.action)
        assertNull(match.medication)
    }

    @Test
    fun returnsNullWhenNoMedicationMatches() {
        val match = MedicationVoiceMatcher.match("ya tomé la pastilla", listOf(losartan))

        assertNull(match)
    }

    private fun medication(id: String, name: String): MedicationEntity {
        return MedicationEntity(
            id = id,
            patientId = "patient-1",
            name = name,
            dose = "1 pastilla",
            color = null,
            shape = null,
            instructions = null,
            scheduleTime = "08:00",
            imageUri = null,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
