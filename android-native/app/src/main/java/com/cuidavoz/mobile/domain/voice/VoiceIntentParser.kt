package com.cuidavoz.mobile.domain.voice
import java.text.Normalizer
import java.util.Locale

sealed class VoiceIntent {
    data object RegisterPressure : VoiceIntent()
    data class PressureValues(
        val systolic: Int,
        val diastolic: Int,
        val pulse: Int?,
    ) : VoiceIntent()
    data object ConfirmMedicationTaken : VoiceIntent()
    data object ConfirmAllMedicationsTaken : VoiceIntent()
    data object AskForHelp : VoiceIntent()
    data object RepeatReminder : VoiceIntent()
    data object Cancel : VoiceIntent()
    data object Unknown : VoiceIntent()
}

enum class ReminderVoiceDecision {
    ConfirmTaken,
    Snooze,
    NeedHelp,
    Uncertain,
}

object VoiceIntentParser {
    fun parse(input: String): VoiceIntent {
        val normalized = normalize(input)

        parsePressureValues(normalized)?.let { return it }

        if (normalized.contains("presion")) {
            return VoiceIntent.RegisterPressure
        }
        if (matchesAny(normalized, "ya tome todas", "ya tome todo", "ya tome mis pastillas", "tome las pastillas")) {
            return VoiceIntent.ConfirmAllMedicationsTaken
        }
        if (matchesAny(normalized, "ya tome mi pastilla", "tome la pastilla")) {
            return VoiceIntent.ConfirmMedicationTaken
        }
        if (matchesAny(normalized, "necesito ayuda", "llama a mi hijo", "llama a mi familiar", "quiero ayuda")) {
            return VoiceIntent.AskForHelp
        }
        if (matchesAny(normalized, "repite", "no entendi", "que pastilla")) {
            return VoiceIntent.RepeatReminder
        }
        if (matchesAny(normalized, "cancelar", "no", "me equivoque")) {
            return VoiceIntent.Cancel
        }

        return VoiceIntent.Unknown
    }

    fun isPositiveConfirmation(input: String): Boolean {
        val normalized = normalize(input)
        return normalized in POSITIVE_CONFIRMATIONS
    }

    fun isNegativeConfirmation(input: String): Boolean {
        val normalized = normalize(input)
        return normalized in NEGATIVE_CONFIRMATIONS
    }

    fun parseReminderResponse(
        input: String,
        reminderActive: Boolean,
    ): ReminderVoiceDecision {
        if (!reminderActive) {
            return ReminderVoiceDecision.Uncertain
        }
        val normalized = normalize(input)
        if (containsThirdPersonMedicationClaim(normalized)) {
            return ReminderVoiceDecision.Uncertain
        }
        if (
            matchesAny(
                normalized,
                "ya tome",
                "ya me tome",
                "tome mi pastilla",
                "ya tome mi pastilla",
                "ya tome mis pastillas",
                "ya tome todas",
            )
        ) {
            return ReminderVoiceDecision.ConfirmTaken
        }
        if (matchesAny(normalized, "despues", "recuerdame despues", "mas tarde")) {
            return ReminderVoiceDecision.Snooze
        }
        if (matchesAny(normalized, "necesito ayuda", "quiero ayuda", "ayudame")) {
            return ReminderVoiceDecision.NeedHelp
        }
        return ReminderVoiceDecision.Uncertain
    }

    private fun parsePressureValues(normalized: String): VoiceIntent.PressureValues? {
        val canBePressure =
            normalized.contains("presion") ||
                normalized.contains("tengo") ||
                normalized.contains("sobre") ||
                normalized.contains("pulso")

        if (!canBePressure) {
            return null
        }

        val numberMatches = NUMBER_REGEX.findAll(normalized).map { it.value.toIntOrNull() }.toList()
        if (numberMatches.size >= 2) {
            val systolic = numberMatches[0] ?: return null
            val diastolic = numberMatches[1] ?: return null
            val pulse = numberMatches.getOrNull(2)

            if (systolic !in 50..250 || diastolic !in 30..160) {
                return null
            }
            if (pulse != null && pulse !in 30..220) {
                return null
            }

            return VoiceIntent.PressureValues(
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
            )
        }

        return parseSpokenPressureValues(normalized)
    }

    private fun matchesAny(
        normalized: String,
        vararg phrases: String,
    ): Boolean = phrases.any { normalized.contains(it) }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.lowercase(Locale.getDefault())
            .replace("[,.;:!?]".toRegex(), " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun containsThirdPersonMedicationClaim(normalized: String): Boolean {
        return matchesAny(
            normalized,
            "mi mama tomo",
            "ella tomo",
            "el tomo",
            "maria tomo",
        )
    }

    private fun parseSpokenPressureValues(normalized: String): VoiceIntent.PressureValues? {
        val pressureSegment = normalized.substringBefore(" pulso ").trim()
        val pulseSegment = normalized.substringAfter(" pulso ", missingDelimiterValue = "").trim()
        if (!pressureSegment.contains("sobre")) {
            return null
        }
        val systolic = parseSpanishNumberPhrase(pressureSegment.substringBefore("sobre")) ?: return null
        val diastolic = parseSpanishNumberPhrase(pressureSegment.substringAfter("sobre")) ?: return null
        val pulse = parseSpanishNumberPhrase(pulseSegment)

        if (systolic !in 50..250 || diastolic !in 30..160) {
            return null
        }
        if (pulse != null && pulse !in 30..220) {
            return null
        }

        return VoiceIntent.PressureValues(
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
        )
    }

    private fun parseSpanishNumberPhrase(segment: String): Int? {
        if (segment.isBlank()) {
            return null
        }
        val tokens = segment.split(WHITESPACE_REGEX)
            .map { it.trim() }
            .filter { it in NUMBER_WORDS || it in SPECIAL_NUMBER_WORDS || it == "y" }
        if (tokens.isEmpty()) {
            return null
        }

        var value = 0
        tokens.forEach { token ->
            when (token) {
                "y" -> Unit
                "cien", "ciento" -> value += 100
                else -> value += NUMBER_WORDS[token] ?: return null
            }
        }
        return value.takeIf { it > 0 }
    }

    private val NUMBER_REGEX = Regex("\\b\\d{2,3}\\b")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val SPECIAL_NUMBER_WORDS = setOf("cien", "ciento")
    private val POSITIVE_CONFIRMATIONS = setOf(
        "si",
        "si guardar",
        "guardar",
        "si registrar",
        "registrar",
        "si registrala",
        "si registralo",
        "si abrir llamada",
        "abrir llamada",
        "confirmo",
        "esta bien",
    )
    private val NEGATIVE_CONFIRMATIONS = setOf(
        "no",
        "cancelar",
        "me equivoque",
        "no guardar",
        "no registrar",
        "no llames",
    )
    private val NUMBER_WORDS = mapOf(
        "cero" to 0,
        "uno" to 1,
        "una" to 1,
        "dos" to 2,
        "tres" to 3,
        "cuatro" to 4,
        "cinco" to 5,
        "seis" to 6,
        "siete" to 7,
        "ocho" to 8,
        "nueve" to 9,
        "diez" to 10,
        "once" to 11,
        "doce" to 12,
        "trece" to 13,
        "catorce" to 14,
        "quince" to 15,
        "dieciseis" to 16,
        "diecisiete" to 17,
        "dieciocho" to 18,
        "diecinueve" to 19,
        "veinte" to 20,
        "veintiuno" to 21,
        "veintidos" to 22,
        "veintitres" to 23,
        "veinticuatro" to 24,
        "veinticinco" to 25,
        "veintiseis" to 26,
        "veintisiete" to 27,
        "veintiocho" to 28,
        "veintinueve" to 29,
        "treinta" to 30,
        "cuarenta" to 40,
        "cincuenta" to 50,
        "sesenta" to 60,
        "setenta" to 70,
        "ochenta" to 80,
        "noventa" to 90,
        "doscientos" to 200,
    )
}
