package com.cuidavoz.mobile.util

import java.text.Normalizer
import java.util.Locale

fun formatTimeForDisplay(time24: String): String {
    val normalized = normalizeTimeTo24h(time24) ?: return time24
    val (hour, minute) = normalized.split(":").map { it.toInt() }
    val suffix = if (hour < 12) "a. m." else "p. m."
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, suffix)
}

fun parseUserTimeInput(input: String): String? {
    val normalized = normalizeTimeTo24h(input) ?: return null
    return formatTimeForDisplay(normalized)
}

fun normalizeTimeTo24h(input: String): String? {
    val raw = input.trim()
    if (raw.isBlank()) {
        return null
    }

    val cleaned = normalizeInput(raw)
    val period = detectPeriod(cleaned)
    val digitsOnly = cleaned.filter(Char::isDigit)
    val numericInput = cleaned
        .replace(Regex("""\b(am|pm|manana|tarde|noche)\b"""), "")
        .replace("\\s+".toRegex(), " ")
        .trim()

    if (digitsOnly.length == 4 && cleaned.all { it.isDigit() }) {
        val hour = digitsOnly.substring(0, 2).toIntOrNull() ?: return null
        val minute = digitsOnly.substring(2, 4).toIntOrNull() ?: return null
        return validate24Hour(hour, minute)
    }

    val withColon = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(numericInput)
    if (withColon != null) {
        val hour = withColon.groupValues[1].toIntOrNull() ?: return null
        val minute = withColon.groupValues[2].toIntOrNull() ?: return null
        return validateTime(hour, minute, period)
    }

    val singleHour = Regex("""^(\d{1,2})$""").matchEntire(numericInput)
    if (singleHour != null) {
        val hour = singleHour.groupValues[1].toIntOrNull() ?: return null
        return validateTime(hour, 0, period)
    }

    val mixed = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$""").matchEntire(numericInput)
    if (mixed != null) {
        val hour = mixed.groupValues[1].toIntOrNull() ?: return null
        val minute = mixed.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return null
        val explicitPeriod = mixed.groupValues[3].ifBlank { period ?: "" }
        return validateTime(hour, minute, explicitPeriod.ifBlank { null })
    }

    return null
}

fun formatTimeForVoice(time24: String): String {
    val normalized = normalizeTimeTo24h(time24) ?: return time24
    val (hour, minute) = normalized.split(":").map { it.toInt() }
    val period = when {
        hour < 12 -> "de la mañana"
        hour < 19 -> "de la tarde"
        else -> "de la noche"
    }
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return if (minute == 0) {
        "$hour12 $period"
    } else {
        "$hour12 y $minute $period"
    }
}

private fun validateTime(
    hour: Int,
    minute: Int,
    period: String?,
): String? {
    return when (period) {
        "am" -> {
            if (hour !in 1..12 || minute !in 0..59) return null
            val normalizedHour = if (hour == 12) 0 else hour
            String.format(Locale.US, "%02d:%02d", normalizedHour, minute)
        }
        "pm" -> {
            if (hour !in 1..12 || minute !in 0..59) return null
            val normalizedHour = if (hour == 12) 12 else hour + 12
            String.format(Locale.US, "%02d:%02d", normalizedHour, minute)
        }
        null -> validate24Hour(hour, minute)
        else -> null
    }
}

private fun validate24Hour(hour: Int, minute: Int): String? {
    if (hour !in 0..23 || minute !in 0..59) {
        return null
    }
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

private fun detectPeriod(cleaned: String): String? {
    return when {
        cleaned.contains("am") || cleaned.contains("manana") -> "am"
        cleaned.contains("pm") || cleaned.contains("tarde") || cleaned.contains("noche") -> "pm"
        else -> null
    }
}

private fun normalizeInput(input: String): String {
    val noAccents = Normalizer.normalize(input.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    return noAccents
        .replace("a.m.", "am")
        .replace("p.m.", "pm")
        .replace("a. m.", "am")
        .replace("p. m.", "pm")
        .replace("de la manana", "manana")
        .replace("de la tarde", "tarde")
        .replace("de la noche", "noche")
        .replace("\\s+".toRegex(), " ")
        .trim()
}
