package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.data.model.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIntentParserReminderTest {
    private val losartan = medication("losartan", "Losartán")
    private val aspirin = medication("aspirin", "Aspirina")

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
    fun detectsNamedMedicationTaken() {
        assertEquals(
            ReminderVoiceDecision.ConfirmMedicationTaken,
            VoiceIntentParser.parseReminderResponse(
                input = "ya tomé la losartán",
                reminderActive = true,
                pendingMedications = listOf(losartan, aspirin),
            ),
        )
    }

    @Test
    fun detectsNamedMedicationSkippedWithReason() {
        assertEquals(
            ReminderVoiceDecision.ConfirmMedicationSkipped,
            VoiceIntentParser.parseReminderResponse(
                input = "no pude tomar la aspirina, se acabó",
                reminderActive = true,
                pendingMedications = listOf(losartan, aspirin),
            ),
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
