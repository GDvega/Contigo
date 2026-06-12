package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationSkipReason
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
    ConfirmMedicationTaken,
    ConfirmMedicationSkipped,
    Snooze,
    NeedHelp,
    Uncertain,
}

data class ReminderMedicationVoiceTarget(
    val medicationId: String,
    val skipReason: com.cuidavoz.mobile.domain.MedicationSkipReason? = null,
)

object VoiceIntentParser {
    private val CORRECTION_KEYWORDS = setOf(
        "me equivoque", "no", "perdon", "digo", "corrijo", "perdoname", "olvidalo", "correccion"
    )

    fun parse(input: String): VoiceIntent {
        val originalNormalized = normalize(input)
        var normalized = originalNormalized

        // Detección de corrección: si el usuario se arrepiente, nos quedamos con lo último.
        val lastCorrectionIndex = findLastCorrectionIndex(normalized)
        if (lastCorrectionIndex != -1) {
            normalized = normalized.substring(lastCorrectionIndex).trim()
        }

        if (normalized.isBlank()) return VoiceIntent.Unknown

        // Intentamos extraer valores de presión primero
        parsePressureValues(normalized, originalNormalized)?.let { return it }

        if (normalized.contains("presion") || normalized.contains("tension") ||
            originalNormalized.contains("presion") || originalNormalized.contains("tension")) {
            return VoiceIntent.RegisterPressure
        }

        if (matchesAny(normalized,
                "ya tome todas", "ya tome todo", "ya tome mis pastillas", "tome las pastillas",
                "ya tome mis remedios", "tome los remedios", "ya tome mi medicina", "tome la medicina",
                "ya me puse todo", "ya me eche todo", "ya termine el tratamiento"
            )) {
            return VoiceIntent.ConfirmAllMedicationsTaken
        }
        if (matchesAny(normalized,
                "ya tome mi pastilla", "tome la pastilla",
                "ya tome mi remedio", "tome el remedio", "ya tome mi medicina", "tome la medicina",
                "ya me tome el jarabe", "ya me puse las gotas", "ya me eche la pomada", "ya me puse la inyeccion",
                "tome el jarabe", "puse las gotas", "eche la pomada", "puse la inyeccion",
                "ya me lo eche", "ya me lo puse", "ya lo use", "ya lo hice"
            )) {
            return VoiceIntent.ConfirmMedicationTaken
        }
        if (matchesAny(normalized,
                "necesito ayuda", "llama a mi hijo", "llama a mi familiar", "quiero ayuda",
                "me siento mal", "ayudame", "emergencia", "urgencia", "llama a alguien"
            )) {
            return VoiceIntent.AskForHelp
        }
        if (matchesAny(normalized, "repite", "no entendi", "que pastilla", "cual era", "repitelo")) {
            return VoiceIntent.RepeatReminder
        }

        if (isNegativeConfirmation(normalized) || matchesAny(normalized, "me equivoque", "olvidalo", "deja nomas")) {
            return VoiceIntent.Cancel
        }

        return VoiceIntent.Unknown
    }

    private fun parsePressureValues(normalized: String, context: String = ""): VoiceIntent.PressureValues? {
        val canBePressure =
            normalized.contains("presion") ||
                normalized.contains("tengo") ||
                normalized.contains("sobre") ||
                normalized.contains("pulso") ||
                context.contains("presion") ||
                context.contains("tension")

        if (!canBePressure) {
            return null
        }

        // Extracción de números (incluyendo soporte para palabras en español)
        val numberMatches = extractNumbers(normalized)

        if (numberMatches.size == 3) {
            val n0 = numberMatches[0]
            val n1 = numberMatches[1]
            val n2 = numberMatches[2]
            val s = if (n0 in 7..25 && n1 in 4..16) n0 * 10 else n0
            val d = if (n0 in 7..25 && n1 in 4..16) n1 * 10 else n1
            if (s in 50..250 && d in 30..160 && n2 in 30..220) {
                return VoiceIntent.PressureValues(s, d, n2)
            }
        }

        if (numberMatches.size >= 2) {
            for (i in (numberMatches.size - 2) downTo 0) {
                var s = numberMatches[i]
                var d = numberMatches[i + 1]
                val p = numberMatches.getOrNull(i + 2)
                if (s in 7..25 && d in 4..16) { s *= 10; d *= 10 }
                if (s in 50..250 && d in 30..160) {
                    if (p != null && p in 30..220) return VoiceIntent.PressureValues(s, d, p)
                    return VoiceIntent.PressureValues(s, d, null)
                }
            }
        }

        return null
    }

    private fun extractNumbers(text: String): List<Int> {
        val result = mutableListOf<Int>()
        val tokens = text.split(WHITESPACE_REGEX)
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val asDigit = token.toIntOrNull()
            if (asDigit != null) {
                result.add(asDigit)
            } else {
                // Intenta parsear frase de número (ej: "ciento veinte")
                val (value, consumed) = parseNumberPhrase(tokens, i)
                if (consumed > 0) {
                    result.add(value)
                    i += consumed - 1
                }
            }
            i++
        }
        return result
    }

    private fun parseNumberPhrase(tokens: List<String>, start: Int): Pair<Int, Int> {
        var value = 0
        var consumed = 0
        for (j in start until tokens.size) {
            val t = tokens[j]
            when {
                t == "y" -> consumed++
                t == "cien" || t == "ciento" -> { value += 100; consumed++ }
                NUMBER_WORDS.containsKey(t) -> { value += NUMBER_WORDS[t]!!; consumed++ }
                else -> break
            }
        }
        return Pair(value, if (value > 0) consumed else 0)
    }

    fun looksLikeMedicationConfirmation(input: String): Boolean {
        val normalized = normalize(input)
        return matchesAny(normalized,
            "tome", "tomi", "puse", "eche", "ingeri", "use", "hice", "listo", "ya esta", "si ya"
        )
    }

    fun parseReminderResponse(
        input: String,
        reminderActive: Boolean,
        pendingMedications: List<MedicationEntity> = emptyList(),
        medicationTarget: ReminderMedicationVoiceTarget? = null,
    ): ReminderVoiceDecision {
        if (!reminderActive) {
            return ReminderVoiceDecision.Uncertain
        }
        val normalized = normalize(input)
        if (containsThirdPersonMedicationClaim(normalized)) {
            return ReminderVoiceDecision.Uncertain
        }
        if (matchesAny(normalized, "despues", "recuerdame despues", "mas tarde")) {
            return ReminderVoiceDecision.Snooze
        }
        if (matchesAny(normalized, "necesito ayuda", "quiero ayuda", "ayudame")) {
            return ReminderVoiceDecision.NeedHelp
        }

        medicationTarget?.let { target ->
            if (isPositiveConfirmation(normalized)) {
                return if (target.skipReason != null) {
                    ReminderVoiceDecision.ConfirmMedicationSkipped
                } else {
                    ReminderVoiceDecision.ConfirmMedicationTaken
                }
            }
            if (isNegativeConfirmation(normalized)) {
                return ReminderVoiceDecision.Uncertain
            }
        }

        if (pendingMedications.isNotEmpty()) {
            MedicationVoiceMatcher.match(input, pendingMedications)?.let { match ->
                return when (match.action) {
                    MedicationVoiceAction.ALL_TAKEN -> ReminderVoiceDecision.ConfirmTaken
                    MedicationVoiceAction.TAKEN -> ReminderVoiceDecision.ConfirmMedicationTaken
                    MedicationVoiceAction.SKIPPED -> ReminderVoiceDecision.ConfirmMedicationSkipped
                }
            }
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
        return ReminderVoiceDecision.Uncertain
    }

    private fun containsThirdPersonMedicationClaim(normalized: String): Boolean {
        return matchesAny(
            normalized,
            "mi mama tomo",
            "mi mami tomo",
            "ella tomo",
            "el tomo",
        )
    }

    fun isPositiveConfirmation(input: String): Boolean {
        val normalized = normalize(input)
        return normalized in POSITIVE_CONFIRMATIONS || normalized in POSITIVE_CULTURAL_FALLBACK
    }

    fun isNegativeConfirmation(input: String): Boolean {
        val normalized = normalize(input)
        return normalized in NEGATIVE_CONFIRMATIONS || normalized in NEGATIVE_CULTURAL_FALLBACK
    }

    private fun findLastCorrectionIndex(text: String): Int {
        var lastIdx = -1
        var keywordLen = 0
        CORRECTION_KEYWORDS.forEach { keyword ->
            val idx = text.lastIndexOf(keyword)
            if (idx > lastIdx) {
                lastIdx = idx
                keywordLen = keyword.length
            }
        }
        return if (lastIdx != -1) lastIdx + keywordLen else -1
    }

    private fun matchesAny(normalized: String, vararg phrases: String): Boolean = phrases.any { normalized.contains(it) }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.lowercase(Locale.getDefault())
            .replace("[,.;:!?]".toRegex(), " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val POSITIVE_CULTURAL_FALLBACK = setOf("ari", "si ari", "hazlo nomas", "dale nomas")
    private val NEGATIVE_CULTURAL_FALLBACK = setOf("mana", "manan")
    private val POSITIVE_CONFIRMATIONS = setOf(
        "si", "si guardar", "guardar", "si guardalo", "si guardala", "guardalo", "guardala",
        "si registrar", "registrar", "si registrala", "si registralo", "registrala", "registralo",
        "si anotar", "anotar", "anotalo", "anotala", "claro", "por supuesto", "esta bien", "vale", "dale"
    )
    private val NEGATIVE_CONFIRMATIONS = setOf("no", "no guardar", "no registrar", "cancelar", "mejor no")

    private val NUMBER_WORDS = mapOf(
        "cero" to 0, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
        "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22,
        "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29, "treinta" to 30, "cuarenta" to 40,
        "cincuenta" to 50, "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90, "doscientos" to 200
    )
}
