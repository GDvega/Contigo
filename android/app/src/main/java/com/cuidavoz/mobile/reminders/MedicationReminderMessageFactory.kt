package com.cuidavoz.mobile.reminders

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.formatTimeForVoice

data class SpokenReminderMessage(
    val title: String,
    val body: String,
    val bigText: String,
    val speech: String,
    val imageUri: String? = null,
)

object MedicationReminderMessageFactory {
    fun build(
        patientName: String,
        payload: ReminderPayload,
        medications: List<MedicationEntity>,
    ): SpokenReminderMessage {
        return if (medications.size <= 1) {
            buildSingleMessage(patientName, payload, medications.firstOrNull())
        } else {
            buildGroupMessage(patientName, payload, medications)
        }
    }

    private fun buildSingleMessage(
        patientName: String,
        payload: ReminderPayload,
        medication: MedicationEntity?,
    ): SpokenReminderMessage {
        val name = medication?.name ?: payload.medicationNames.firstOrNull().orEmpty()
        val dose = medication?.dose.orEmpty()
        val description = buildVisualDescription(medication)
        val instructions = medication?.instructions.orEmpty().trim()
        val timeLabel = formatScheduleTime(payload.scheduleTime)
        val voiceTimeLabel = formatTimeForVoice(payload.scheduleTime)
        val bigTextParts = listOfNotNull(
            name.takeIf { it.isNotBlank() },
            dose.takeIf { it.isNotBlank() },
            description,
            instructions.takeIf { it.isNotBlank() },
        )
        val speechParts = buildList {
            add("$patientName, es hora de tomar $name.")
            if (dose.isNotBlank()) add("Toma $dose.")
            add("Está programada para las $voiceTimeLabel.")
            description?.let { add("La pastilla $it.") }
            if (instructions.isNotBlank()) add("$instructions.")
            add("Después de tomarla, presiona Ya tomé.")
        }
        return SpokenReminderMessage(
            title = "Es hora de tu pastilla",
            body = listOf(name, dose, timeLabel).filter { it.isNotBlank() }.joinToString(", "),
            bigText = bigTextParts.joinToString(". ") + ".",
            speech = speechParts.joinToString(" "),
            imageUri = medication?.imageUri,
        )
    }

    private fun buildGroupMessage(
        patientName: String,
        payload: ReminderPayload,
        medications: List<MedicationEntity>,
    ): SpokenReminderMessage {
        val timeLabel = formatScheduleTime(payload.scheduleTime)
        val names = medications.map { it.name }.ifEmpty { payload.medicationNames }
        val count = names.size
        val orderedDescriptions = medications.mapIndexed { index, medication ->
            val details = listOfNotNull(
                medication.name.takeIf { it.isNotBlank() },
                medication.dose.takeIf { it.isNotBlank() },
                buildVisualDescription(medication),
            ).joinToString(", ")
            "${ordinalForVoice(index)} pastilla: $details"
        }
        return SpokenReminderMessage(
            title = "Es hora de tus pastillas",
            body = "Toma $count pastillas a las $timeLabel.",
            bigText = medications.joinToString("\n") { medication ->
                listOfNotNull(
                    medication.name.takeIf { it.isNotBlank() },
                    medication.dose.takeIf { it.isNotBlank() },
                    buildVisualDescription(medication),
                    medication.instructions?.takeIf { it.isNotBlank() },
                ).joinToString(". ")
            },
            speech = "$patientName, es hora de tomar tus pastillas. " +
                orderedDescriptions.joinToString(". ") + ". " +
                "Puedes decir ya tomé la primera pastilla, o describirla por color, forma u hora. " +
                "Después de tomarlas, presiona Ya tomé todas.",
            imageUri = medications.firstOrNull { !it.imageUri.isNullOrBlank() }?.imageUri,
        )
    }

    private fun buildVisualDescription(medication: MedicationEntity?): String? {
        val color = medication?.color?.trim().orEmpty()
        val shape = medication?.shape?.trim().orEmpty()
        return when {
            color.isBlank() && shape.isBlank() -> null
            color.isNotBlank() && shape.isNotBlank() -> "es $color y $shape"
            color.isNotBlank() -> "es $color"
            else -> "es $shape"
        }
    }

    private fun ordinalForVoice(index: Int): String {
        return when (index) {
            0 -> "Primera"
            1 -> "Segunda"
            2 -> "Tercera"
            3 -> "Cuarta"
            4 -> "Quinta"
            5 -> "Sexta"
            6 -> "Septima"
            7 -> "Octava"
            8 -> "Novena"
            9 -> "Decima"
            else -> "Pastilla numero ${index + 1}"
        }
    }
}
