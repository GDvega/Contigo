package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MedicationSchedule(
    val scheduleType: ScheduleType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val daysOfWeek: Set<Int>,
    val specificDates: Set<LocalDate>,
)

object MedicationScheduleDefaults {
    val allDaysOfWeek: Set<Int> = (1..7).toSet()

    fun todayIso(today: LocalDate = LocalDate.now()): String = today.toString()

    fun allDaysJson(): String = encodeDaysOfWeek(allDaysOfWeek)

    fun emptyDatesJson(): String = encodeSpecificDates(emptySet())

    fun defaultSchedule(
        today: LocalDate = LocalDate.now(),
    ): MedicationSchedule {
        return MedicationSchedule(
            scheduleType = ScheduleType.ALWAYS,
            startDate = today,
            endDate = null,
            daysOfWeek = allDaysOfWeek,
            specificDates = emptySet(),
        )
    }
}

private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

fun MedicationEntity.toMedicationSchedule(): MedicationSchedule {
    val defaultSchedule = MedicationScheduleDefaults.defaultSchedule()
    val resolvedType = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.ALWAYS)
    val resolvedStart = runCatching { LocalDate.parse(startDate) }.getOrDefault(defaultSchedule.startDate)
    val resolvedEnd = endDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val resolvedDays = parseDaysOfWeekJson(daysOfWeekJson).ifEmpty { MedicationScheduleDefaults.allDaysOfWeek }
    val resolvedSpecificDates = parseSpecificDatesJson(specificDatesJson)
    return MedicationSchedule(
        scheduleType = resolvedType,
        startDate = resolvedStart,
        endDate = resolvedEnd,
        daysOfWeek = resolvedDays,
        specificDates = resolvedSpecificDates,
    )
}

fun MedicationEntity.isExpired(onDate: LocalDate = LocalDate.now()): Boolean {
    val schedule = toMedicationSchedule()
    return schedule.endDate?.isBefore(onDate) == true && schedule.scheduleType != ScheduleType.ALWAYS
}

fun MedicationEntity.treatmentSummary(): String {
    val schedule = toMedicationSchedule()
    return when (schedule.scheduleType) {
        ScheduleType.ALWAYS -> "Todos los días, sin fecha de fin"
        ScheduleType.DATE_RANGE -> {
            val start = schedule.startDate.format(displayDateFormatter)
            val end = schedule.endDate?.format(displayDateFormatter) ?: "sin fin"
            "Del $start al $end"
        }
        ScheduleType.WEEKLY_DAYS -> {
            val daysText = schedule.daysOfWeek
                .sorted()
                .map(::dayNumberToLabel)
                .joinToString(", ")
            val rangeText = when {
                schedule.endDate != null ->
                    " (${schedule.startDate.format(displayDateFormatter)} al ${schedule.endDate.format(displayDateFormatter)})"
                else -> ""
            }
            "$daysText$rangeText"
        }
        ScheduleType.SPECIFIC_DATES -> {
            val datesText = schedule.specificDates
                .sorted()
                .joinToString(", ") { it.format(displayDateFormatter) }
            "Fechas específicas: $datesText"
        }
    }
}

fun dayNumberToLabel(day: Int): String {
    return when (day) {
        DayOfWeek.MONDAY.value -> "Lunes"
        DayOfWeek.TUESDAY.value -> "Martes"
        DayOfWeek.WEDNESDAY.value -> "Miércoles"
        DayOfWeek.THURSDAY.value -> "Jueves"
        DayOfWeek.FRIDAY.value -> "Viernes"
        DayOfWeek.SATURDAY.value -> "Sábado"
        DayOfWeek.SUNDAY.value -> "Domingo"
        else -> day.toString()
    }
}

fun encodeDaysOfWeek(days: Collection<Int>): String {
    return days.sorted().joinToString(prefix = "[", postfix = "]", separator = ",")
}

fun parseDaysOfWeekJson(value: String?): Set<Int> {
    if (value.isNullOrBlank()) return emptySet()
    return Regex("\\d+").findAll(value)
        .mapNotNull { it.value.toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()
}

fun encodeSpecificDates(dates: Collection<LocalDate>): String {
    return dates.sorted().joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
}

fun parseSpecificDatesJson(value: String?): Set<LocalDate> {
    if (value.isNullOrBlank()) return emptySet()
    return Regex("\\d{4}-\\d{2}-\\d{2}").findAll(value)
        .mapNotNull { match -> runCatching { LocalDate.parse(match.value) }.getOrNull() }
        .toSet()
}
