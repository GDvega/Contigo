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

    @Test
    fun matchesMedicationByOrdinal() {
        val match = MedicationVoiceMatcher.match("ya tomé la segunda pastilla", listOf(losartan, aspirin))

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.TAKEN, match!!.action)
        assertEquals("aspirin", match.medication?.id)
    }

    @Test
    fun matchesMedicationThatRangFirst() {
        val paracetamol = medication("paracetamol", "Paracetamol", time = "03:30")
        val ibuprofen = medication("ibuprofen", "Ibuprofeno", time = "03:31")

        val match = MedicationVoiceMatcher.match(
            "ya tomé la pastilla que sonó primero",
            listOf(paracetamol, ibuprofen),
        )

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.TAKEN, match!!.action)
        assertEquals("paracetamol", match.medication?.id)
    }

    @Test
    fun matchesMedicationByColorShapeAndDose() {
        val redRound = medication("red", "Pastilla A", dose = "500 mg", color = "roja", shape = "redonda")
        val whiteOval = medication("white", "Pastilla B", dose = "250 mg", color = "blanca", shape = "ovalada")

        val colorMatch = MedicationVoiceMatcher.match("ya tomé la roja", listOf(redRound, whiteOval))
        val shapeMatch = MedicationVoiceMatcher.match("ya tomé la ovalada", listOf(redRound, whiteOval))
        val doseMatch = MedicationVoiceMatcher.match("ya tomé la de 500 miligramos", listOf(redRound, whiteOval))

        assertEquals("red", colorMatch?.medication?.id)
        assertEquals("white", shapeMatch?.medication?.id)
        assertEquals("red", doseMatch?.medication?.id)
    }

    @Test
    fun returnsNullWhenAttributeIsAmbiguous() {
        val firstRed = medication("red-1", "Pastilla A", color = "roja")
        val secondRed = medication("red-2", "Pastilla B", color = "roja")

        val match = MedicationVoiceMatcher.match("ya tomé la roja", listOf(firstRed, secondRed))

        assertNull(match)
    }

    @Test
    fun matchesMedicationByScheduleTime() {
        val early = medication("early", "Paracetamol", time = "03:30")
        val later = medication("later", "Ibuprofeno", time = "03:31")

        val match = MedicationVoiceMatcher.match("ya tomé la pastilla de las 3:30", listOf(early, later))

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.TAKEN, match!!.action)
        assertEquals("early", match.medication?.id)
    }

    @Test
    fun matchesMedicationBySpokenScheduleTime() {
        val early = medication("early", "Paracetamol", time = "03:30")
        val later = medication("later", "Ibuprofeno", time = "03:31")

        val match = MedicationVoiceMatcher.match(
            "ya tomé la pastilla de las tres y treinta de la mañana",
            listOf(early, later),
        )

        assertNotNull(match)
        assertEquals(MedicationVoiceAction.TAKEN, match!!.action)
        assertEquals("early", match.medication?.id)
    }

    @Test
    fun matchesWholeScheduleOnlyWhenPluralIsSaid() {
        val first = medication("first", "Pastilla A", time = "03:30")
        val second = medication("second", "Pastilla B", time = "03:30")

        val singular = MedicationVoiceMatcher.match("ya tomé la pastilla de las 3:30", listOf(first, second))
        val plural = MedicationVoiceMatcher.match("ya tomé las pastillas de las 3:30", listOf(first, second))

        assertNull(singular)
        assertNotNull(plural)
        assertEquals(MedicationVoiceAction.ALL_TAKEN, plural!!.action)
        assertEquals("03:30", plural.scheduleTime)
    }

    private fun medication(
        id: String,
        name: String,
        dose: String = "1 pastilla",
        color: String? = null,
        shape: String? = null,
        time: String = "08:00",
    ): MedicationEntity {
        return MedicationEntity(
            id = id,
            patientId = "patient-1",
            name = name,
            dose = dose,
            color = color,
            shape = shape,
            instructions = null,
            scheduleTime = time,
            imageUri = null,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
