package com.cuidavoz.mobile.domain.voice

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationSkipReason
import com.cuidavoz.mobile.util.formatTimeForVoice
import com.cuidavoz.mobile.util.normalizeTimeTo24h
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
    val scheduleTime: String? = medication?.scheduleTime,
)

object MedicationVoiceMatcher {
    fun match(
        input: String,
        pendingMedications: List<MedicationEntity>,
    ): MedicationVoiceMatch? {
        if (pendingMedications.isEmpty()) return null
        val normalized = normalize(input)
        if (normalized.isBlank()) return null

        val allTaken = matchesAllTaken(normalized)
        val skipReason = detectSkipReason(normalized)
        val wantsSkip = skipReason != null || looksLikeSkip(normalized)
        val wantsTaken = allTaken || looksLikeTaken(normalized)
        if (!wantsSkip && !wantsTaken) return null

        findMatchingSchedule(normalized, pendingMedications)?.let { scheduleMatch ->
            if (scheduleMatch.medications.size == 1) {
                return buildMedicationMatch(
                    medication = scheduleMatch.medications.single(),
                    wantsSkip = wantsSkip,
                    skipReason = skipReason,
                )
            }
            if (!wantsSkip && (allTaken || mentionsMedicationPlural(normalized))) {
                return MedicationVoiceMatch(
                    medication = null,
                    action = MedicationVoiceAction.ALL_TAKEN,
                    scheduleTime = scheduleMatch.scheduleTime,
                )
            }
        }

        if (allTaken) {
            return MedicationVoiceMatch(
                medication = null,
                action = MedicationVoiceAction.ALL_TAKEN,
            )
        }

        val medication = findMatchingMedication(normalized, pendingMedications) ?: return null
        return buildMedicationMatch(
            medication = medication,
            wantsSkip = wantsSkip,
            skipReason = skipReason,
        )
    }

    private fun buildMedicationMatch(
        medication: MedicationEntity,
        wantsSkip: Boolean,
        skipReason: MedicationSkipReason?,
    ): MedicationVoiceMatch {
        return if (wantsSkip) {
            MedicationVoiceMatch(
                medication = medication,
                action = MedicationVoiceAction.SKIPPED,
                skipReason = skipReason ?: MedicationSkipReason.OTHER,
            )
        } else {
            MedicationVoiceMatch(
                medication = medication,
                action = MedicationVoiceAction.TAKEN,
            )
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
        return findMatchingMedicationByName(normalized, pendingMedications)
            ?: findMatchingMedicationByOrdinal(normalized, pendingMedications)
            ?: findUniqueMedicationByAttributes(normalized, pendingMedications)
    }

    private fun findMatchingMedicationByName(
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

    private fun findMatchingMedicationByOrdinal(
        normalized: String,
        pendingMedications: List<MedicationEntity>,
    ): MedicationEntity? {
        val index = ORDINAL_PHRASES.indexOfFirst { phrases ->
            phrases.any { phrase -> containsPhrase(normalized, phrase) }
        }
        return pendingMedications.getOrNull(index)
    }

    private fun findUniqueMedicationByAttributes(
        normalized: String,
        pendingMedications: List<MedicationEntity>,
    ): MedicationEntity? {
        val matchers = listOf<(MedicationEntity) -> Boolean>(
            { medication -> attributeMatches(normalized, medication.color) },
            { medication -> attributeMatches(normalized, medication.shape) },
            { medication -> doseMatches(normalized, medication.dose) },
        )
        return matchers.firstNotNullOfOrNull { matcher ->
            pendingMedications.filter(matcher).singleOrNull()
        }
    }

    private fun findMatchingSchedule(
        normalized: String,
        pendingMedications: List<MedicationEntity>,
    ): ScheduleMedicationMatch? {
        if (!hasTimeCue(normalized)) return null
        return pendingMedications
            .groupBy { it.scheduleTime }
            .entries
            .firstOrNull { (scheduleTime, _) ->
                scheduleTimeAliases(scheduleTime).any { alias -> containsPhrase(normalized, alias) }
            }
            ?.let { (scheduleTime, medications) ->
                ScheduleMedicationMatch(scheduleTime, medications)
            }
    }

    private fun medicationNameTokens(name: String): List<String> {
        return normalize(name)
            .split(WHITESPACE_REGEX)
            .filter { it.length >= 4 && it !in GENERIC_MEDICATION_TOKENS }
    }

    private fun attributeMatches(
        normalized: String,
        rawValue: String?,
    ): Boolean {
        val value = normalize(rawValue.orEmpty())
        if (value.length < 3) return false
        return attributeAliases(value).any { alias -> containsPhrase(normalized, alias) }
    }

    private fun doseMatches(
        normalized: String,
        rawDose: String,
    ): Boolean {
        val dose = normalize(rawDose)
        if (dose.length < 2) return false
        return doseAliases(dose).any { alias -> containsPhrase(normalized, alias) }
    }

    private fun attributeAliases(value: String): Set<String> {
        return buildSet {
            add(value)
            if (value.endsWith("o")) add(value.dropLast(1) + "a")
            if (value.endsWith("a")) add(value.dropLast(1) + "o")
            val variants = toList()
            variants.forEach { variant ->
                add("${variant}s")
                add("${variant}es")
            }
        }
    }

    private fun doseAliases(dose: String): Set<String> {
        return buildSet {
            add(dose)
            if ("mg" in dose) {
                add(dose.replace("mg", "miligramos"))
                add(dose.replace("mg", " miligramos"))
            }
        }.map { normalize(it) }.toSet()
    }

    private fun scheduleTimeAliases(scheduleTime: String): Set<String> {
        val normalizedTime = normalizeTimeTo24h(scheduleTime) ?: return setOf(normalize(scheduleTime))
        val parts = normalizedTime.split(":")
        val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: return setOf(normalize(scheduleTime))
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return setOf(normalize(scheduleTime))
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val minutePadded = minute.toString().padStart(2, '0')
        val hour24Padded = hour24.toString().padStart(2, '0')
        val hourWord = spanishHour(hour12)
        val minuteWord = spanishNumber(minute)
        return buildSet {
            add("$hour24Padded:$minutePadded")
            add("$hour24:$minutePadded")
            add("$hour12:$minutePadded")
            add("$hour24Padded $minutePadded")
            add("$hour24 $minutePadded")
            add("$hour12 $minutePadded")
            add("$hour12 y $minute")
            add(formatTimeForVoice(normalizedTime))
            if (minute == 0) {
                add("$hour12")
                add("$hourWord")
                add("$hourWord en punto")
            } else {
                add("$hourWord y $minute")
                minuteWord?.let { word ->
                    add("$hourWord y $word")
                    add("$hourWord $word")
                }
                if (minute == 30) add("$hourWord y media")
                if (minute == 15) add("$hourWord y cuarto")
            }
        }.map { normalize(it) }.filter { it.isNotBlank() }.toSet()
    }

    private fun spanishHour(hour: Int): String {
        return if (hour == 1) "una" else spanishNumber(hour) ?: hour.toString()
    }

    private fun spanishNumber(number: Int): String? {
        SPANISH_SMALL_NUMBERS[number]?.let { return it }
        return when (number) {
            in 21..29 -> "veinti${SPANISH_SMALL_NUMBERS[number - 20]}"
            30 -> "treinta"
            40 -> "cuarenta"
            50 -> "cincuenta"
            in 31..39 -> "treinta y ${SPANISH_SMALL_NUMBERS[number - 30]}"
            in 41..49 -> "cuarenta y ${SPANISH_SMALL_NUMBERS[number - 40]}"
            in 51..59 -> "cincuenta y ${SPANISH_SMALL_NUMBERS[number - 50]}"
            else -> null
        }
    }

    private fun containsPhrase(
        normalized: String,
        phrase: String,
    ): Boolean {
        val normalizedPhrase = normalize(phrase)
        if (normalizedPhrase.isBlank()) return false
        return " $normalized ".contains(" $normalizedPhrase ")
    }

    private fun hasTimeCue(normalized: String): Boolean {
        return TIME_CUE_PHRASES.any { phrase -> containsPhrase(normalized, phrase) }
    }

    private fun mentionsMedicationPlural(normalized: String): Boolean {
        return PLURAL_MEDICATION_PHRASES.any { phrase -> containsPhrase(normalized, phrase) }
    }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.lowercase(Locale.getDefault())
            .replace("[,.;:!?]".toRegex(), " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private data class ScheduleMedicationMatch(
        val scheduleTime: String,
        val medications: List<MedicationEntity>,
    )

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val GENERIC_MEDICATION_TOKENS = setOf(
        "pastilla",
        "pastillas",
        "medicamento",
        "medicamentos",
        "tableta",
        "tabletas",
        "capsula",
        "capsulas",
    )
    private val SPANISH_SMALL_NUMBERS = mapOf(
        0 to "cero",
        1 to "uno",
        2 to "dos",
        3 to "tres",
        4 to "cuatro",
        5 to "cinco",
        6 to "seis",
        7 to "siete",
        8 to "ocho",
        9 to "nueve",
        10 to "diez",
        11 to "once",
        12 to "doce",
        13 to "trece",
        14 to "catorce",
        15 to "quince",
        16 to "dieciseis",
        17 to "diecisiete",
        18 to "dieciocho",
        19 to "diecinueve",
        20 to "veinte",
    )
    private val ORDINAL_PHRASES = listOf(
        listOf(
            "primera",
            "primer",
            "primero",
            "pastilla uno",
            "pastilla 1",
            "numero uno",
            "numero 1",
            "la uno",
            "la 1",
            "sono primero",
            "sono primera",
            "que sono primero",
            "que sono primera",
        ),
        listOf(
            "segunda",
            "segundo",
            "pastilla dos",
            "pastilla 2",
            "numero dos",
            "numero 2",
            "la dos",
            "la 2",
            "sono segundo",
            "sono segunda",
        ),
        listOf("tercera", "tercero", "pastilla tres", "pastilla 3", "numero tres", "numero 3", "la tres", "la 3"),
        listOf("cuarta", "cuarto", "pastilla cuatro", "pastilla 4", "numero cuatro", "numero 4", "la cuatro", "la 4"),
        listOf("quinta", "quinto", "pastilla cinco", "pastilla 5", "numero cinco", "numero 5", "la cinco", "la 5"),
        listOf("sexta", "sexto", "pastilla seis", "pastilla 6", "numero seis", "numero 6", "la seis", "la 6"),
        listOf("septima", "septimo", "pastilla siete", "pastilla 7", "numero siete", "numero 7", "la siete", "la 7"),
        listOf("octava", "octavo", "pastilla ocho", "pastilla 8", "numero ocho", "numero 8", "la ocho", "la 8"),
        listOf("novena", "noveno", "pastilla nueve", "pastilla 9", "numero nueve", "numero 9", "la nueve", "la 9"),
        listOf("decima", "decimo", "pastilla diez", "pastilla 10", "numero diez", "numero 10", "la diez", "la 10"),
    )
    private val TIME_CUE_PHRASES = listOf(
        "a las",
        "de las",
        "las",
        "hora",
        "alarma",
        "recordatorio",
        "sono",
        "sonaba",
    )
    private val PLURAL_MEDICATION_PHRASES = listOf(
        "pastillas",
        "medicamentos",
        "todas",
        "todos",
    )
    private val ALL_TAKEN_PHRASES = listOf(
        "ya tome todas",
        "ya tome todo",
        "ya tome mis pastillas",
        "tome las pastillas",
        "ya tome todas mis pastillas",
        "ya tome los medicamentos",
        "ya tome todas las pastillas",
    )
    private val TAKEN_PHRASES = listOf(
        "ya tome",
        "ya me tome",
        "tome mi pastilla",
        "tome la pastilla",
        "tome el medicamento",
        "ya tome el medicamento",
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
