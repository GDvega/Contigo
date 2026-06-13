package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.domain.voice.VoiceIntentParser.parse
import com.cuidavoz.mobile.domain.voice.VoiceIntentParser.parseReminderResponse
import com.cuidavoz.mobile.testing.MedicationTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentParserExtendedTest {
    @Test
    fun parseSpanishNumberWordsForPressure() {
        val result = parse("mi presion es ciento veinte sobre ochenta")

        assertEquals(VoiceIntent.PressureValues(120, 80, null), result)
    }

    @Test
    fun parseIgnoresUnknownInput() {
        assertEquals(VoiceIntent.Unknown, parse("hola buenos dias"))
    }

    @Test
    fun parseBlankAfterCorrectionIsUnknown() {
        assertEquals(VoiceIntent.Unknown, parse("no me equivoque"))
    }

    @Test
    fun parseReminderResponseSnoozesOnLaterPhrase() {
        val decision = parseReminderResponse(
            input = "recuerdame despues",
            reminderActive = true,
        )

        assertEquals(ReminderVoiceDecision.Snooze, decision)
    }

    @Test
    fun parseReminderResponseUncertainForThirdPersonClaim() {
        val decision = parseReminderResponse(
            input = "mi mama tomo la pastilla",
            reminderActive = true,
        )

        assertEquals(ReminderVoiceDecision.Uncertain, decision)
    }

    @Test
    fun parseReminderResponseInactiveReminderIsUncertain() {
        val decision = parseReminderResponse(
            input = "ya tome",
            reminderActive = false,
        )

        assertEquals(ReminderVoiceDecision.Uncertain, decision)
    }

    @Test
    fun parseReminderResponseMatchesMedicationByName() {
        val med = MedicationTestFixtures.medication(id = "losartan", name = "Losartan")
        val decision = parseReminderResponse(
            input = "ya tome losartan",
            reminderActive = true,
            pendingMedications = listOf(med),
        )

        assertEquals(ReminderVoiceDecision.ConfirmMedicationTaken, decision)
    }

    @Test
    fun negativeConfirmationDetectsExactPhrases() {
        assertTrue(VoiceIntentParser.isNegativeConfirmation("cancelar"))
        assertTrue(VoiceIntentParser.isNegativeConfirmation("no guardar"))
    }
}
