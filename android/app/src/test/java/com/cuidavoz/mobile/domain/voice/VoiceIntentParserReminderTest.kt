package com.cuidavoz.mobile.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIntentParserReminderTest {
    @Test
    fun yaTomeOnlyWorksWhenReminderIsActive() {
        assertEquals(
            ReminderVoiceDecision.Uncertain,
            VoiceIntentParser.parseReminderResponse("ya tomé", reminderActive = false),
        )
        assertEquals(
            ReminderVoiceDecision.ConfirmTaken,
            VoiceIntentParser.parseReminderResponse("ya tomé", reminderActive = true),
        )
    }

    @Test
    fun ignoresThirdPersonMedicationClaims() {
        assertEquals(
            ReminderVoiceDecision.Uncertain,
            VoiceIntentParser.parseReminderResponse("mi mamá tomó la pastilla", reminderActive = true),
        )
        assertEquals(
            ReminderVoiceDecision.Uncertain,
            VoiceIntentParser.parseReminderResponse("María tomó", reminderActive = true),
        )
    }

    @Test
    fun detectsSnoozeAndHelp() {
        assertEquals(
            ReminderVoiceDecision.Snooze,
            VoiceIntentParser.parseReminderResponse("recuérdame después", reminderActive = true),
        )
        assertEquals(
            ReminderVoiceDecision.NeedHelp,
            VoiceIntentParser.parseReminderResponse("necesito ayuda", reminderActive = true),
        )
    }
}
