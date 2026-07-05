package com.cuidavoz.mobile.reminders

import com.cuidavoz.mobile.data.model.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationReminderMessageFactoryTest {
    @Test
    fun buildsTtsMessageForSingleMedication() {
        val message = MedicationReminderMessageFactory.build(
            patientName = "María",
            payload = payload(listOf("Paracetamol")),
            medications = listOf(
                medication(
                    name = "Paracetamol",
                    dose = "1 tableta",
                    color = "blanca",
                    shape = "redonda",
                    instructions = "Tomar después del desayuno",
                ),
            ),
        )

        assertEquals("Es hora de tu pastilla", message.title)
        assertTrue(message.speech.contains("María, es hora de tomar Paracetamol"))
        assertTrue(message.speech.contains("Toma 1 tableta"))
        assertTrue(message.speech.contains("La pastilla es blanca y redonda"))
        assertTrue(message.speech.contains("Tomar después del desayuno"))
    }

    @Test
    fun buildsTtsMessageForMultipleMedications() {
        val message = MedicationReminderMessageFactory.build(
            patientName = "María",
            payload = payload(listOf("Paracetamol", "Aspirina", "Losartán")),
            medications = listOf(
                medication(name = "Paracetamol"),
                medication(name = "Aspirina"),
                medication(name = "Losartán"),
            ),
        )

        assertEquals("Es hora de tus pastillas", message.title)
        assertTrue(message.body.contains("3 pastillas"))
        assertTrue(message.speech.contains("Primera pastilla: Paracetamol"))
        assertTrue(message.speech.contains("Segunda pastilla: Aspirina"))
        assertTrue(message.speech.contains("Tercera pastilla: Losartán"))
        assertTrue(message.speech.contains("describirla por color, forma u hora"))
    }

    private fun payload(names: List<String>) = ReminderPayload(
        reminderId = "rem-1",
        reminderGroupId = "group-1",
        patientId = "patient-1",
        scheduleTime = "07:00",
        targetDate = "2026-05-17",
        scheduledAt = 0L,
        medicationIds = names.mapIndexed { index, _ -> "med-$index" },
        medicationNames = names,
        attemptNumber = 1,
        maxAttempts = 3,
        repeatEveryMinutes = 10,
    )

    private fun medication(
        name: String,
        dose: String = "1 tableta",
        color: String? = null,
        shape: String? = null,
        instructions: String? = null,
    ) = MedicationEntity(
        id = name,
        patientId = "patient-1",
        name = name,
        dose = dose,
        color = color,
        shape = shape,
        instructions = instructions,
        scheduleTime = "07:00",
        imageUri = null,
        isActive = true,
        scheduleType = "ALWAYS",
        startDate = "2026-05-17",
        endDate = null,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        specificDates = emptyList(),
        createdAt = 0L,
        updatedAt = 0L,
    )
}
