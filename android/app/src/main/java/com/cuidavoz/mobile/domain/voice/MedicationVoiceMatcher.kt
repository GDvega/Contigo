package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationSkipReason
import java.text.Normalizer
import java.util.Locale

enum class MedicationVoiceAction {
    TAKEN,
    SKIPPED,
    ALL_TAKEN,
}

data class MedicationVoiceMatch(
    val medication: MedicationEntity?,
    val action: MedicationVoiceAction,
    val skipReason: MedicationSkipReason? = null,
)

object MedicationVoiceMatcher {
    fun match(
        input: String,
        pendingMedications: List<MedicationEntity>,
    ): MedicationVoiceMatch? {
        if (pendingMedications.isEmpty()) return null
        val normalized = normalize(input)
        if (normalized.isBlank()) return null

        if (matchesAllTaken(normalized)) {
            return MedicationVoiceMatch(
                medication = null,
                action = MedicationVoiceAction.ALL_TAKEN,
            )
        }

        val medication = findMatchingMedication(normalized, pendingMedications) ?: return null
        val skipReason = detectSkipReason(normalized)
        return if (skipReason != null || looksLikeSkip(normalized)) {
            MedicationVoiceMatch(
                medication = medication,
                action = MedicationVoiceAction.SKIPPED,
                skipReason = skipReason ?: MedicationSkipReason.OTHER,
            )
        } else if (looksLikeTaken(normalized)) {
            MedicationVoiceMatch(
                medication = medication,
                action = MedicationVoiceAction.TAKEN,
            )
        } else {
            null
        }
    }

    private fun matchesAllTaken(normalized: String): Boolean {
        return ALL_TAKEN_PHRASES.any(normalized::contains)
    }

    private fun looksLikeTaken(normalized: String): Boolean {
        return TAKEN_PHRASES.any(normalized::contains)
    }

    private fun looksLikeSkip(normalized: String): Boolean {
        return SKIP_PHRASES.any(normalized::contains)
    }

    private fun detectSkipReason(normalized: String): MedicationSkipReason? {
        return when {
            OUT_OF_STOCK_PHRASES.any(normalized::contains) -> MedicationSkipReason.OUT_OF_STOCK
            FELT_UNWELL_PHRASES.any(normalized::contains) -> MedicationSkipReason.FELT_UNWELL
            FORGOT_PHRASES.any(normalized::contains) -> MedicationSkipReason.FORGOT
            else -> null
        }
    }

    private fun findMatchingMedication(
        normalized: String,
        pendingMedications: List<MedicationEntity>,
    ): MedicationEntity? {
        return pendingMedications
            .mapNotNull { medication ->
                val tokens = medicationNameTokens(medication.name)
                val matched = tokens.any { token ->
                    token.length >= 4 && normalized.contains(token)
                }
                if (matched) medication else null
            }
            .maxByOrNull { medicationNameTokens(it.name).maxOfOrNull(String::length) ?: 0 }
    }

    private fun medicationNameTokens(name: String): List<String> {
        return normalize(name)
            .split(WHITESPACE_REGEX)
            .filter { it.length >= 4 }
    }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.lowercase(Locale.getDefault())
            .replace("[,.;:!?]".toRegex(), " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val ALL_TAKEN_PHRASES = listOf(
        "ya tome todas",
        "ya tome todo",
        "ya tome mis pastillas",
        "tome las pastillas",
        "ya tome todas mis pastillas",
    )
    private val TAKEN_PHRASES = listOf(
        "ya tome",
        "ya me tome",
        "tome mi pastilla",
        "tome la pastilla",
        "ya tome mi pastilla",
        "ya me la tome",
    )
    private val SKIP_PHRASES = listOf(
        "no pude",
        "no pude tomar",
        "no tome",
        "no me la tome",
        "no tomar",
        "omit",
    )
    private val OUT_OF_STOCK_PHRASES = listOf(
        "se acabo",
        "se me acabo",
        "no queda",
        "no tengo",
        "no hay",
    )
    private val FELT_UNWELL_PHRASES = listOf(
        "me senti mal",
        "me siento mal",
        "me hizo mal",
        "me cae mal",
    )
    private val FORGOT_PHRASES = listOf(
        "la olvide",
        "olvide tomar",
        "se me olvido",
    )
}
