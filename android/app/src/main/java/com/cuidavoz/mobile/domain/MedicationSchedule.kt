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

private fun displayDateFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

fun MedicationEntity.toMedicationSchedule(): MedicationSchedule {
    val defaultSchedule = MedicationScheduleDefaults.defaultSchedule()
    val resolvedType = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.ALWAYS)
    val resolvedStart = runCatching { LocalDate.parse(startDate) }.getOrDefault(defaultSchedule.startDate)
    val resolvedEnd = endDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val resolvedDays = daysOfWeek.toSet().ifEmpty { MedicationScheduleDefaults.allDaysOfWeek }
    val resolvedSpecificDates = specificDates.toSet()
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
    return when (schedule.scheduleType) {
        ScheduleType.ALWAYS -> false
        ScheduleType.DATE_RANGE -> schedule.endDate?.isBefore(onDate) == true
        ScheduleType.WEEKLY_DAYS -> schedule.endDate?.isBefore(onDate) == true
        ScheduleType.SPECIFIC_DATES -> {
            if (schedule.specificDates.isEmpty()) true
            else schedule.specificDates.all { it.isBefore(onDate) }
        }
    }
}

fun MedicationEntity.treatmentSummary(): String {
    val schedule = toMedicationSchedule()
    val formatter = displayDateFormatter()
    return when (schedule.scheduleType) {
        ScheduleType.ALWAYS -> "Todos los días, sin fecha de fin"
        ScheduleType.DATE_RANGE -> {
            val start = schedule.startDate.format(formatter)
            val end = schedule.endDate?.format(formatter) ?: "sin fin"
            "Del $start al $end"
        }
        ScheduleType.WEEKLY_DAYS -> {
            val daysText = schedule.daysOfWeek
                .sorted()
                .map(::dayNumberToLabel)
                .joinToString(", ")
            val rangeText = when {
                schedule.endDate != null ->
                    " (${schedule.startDate.format(formatter)} al ${schedule.endDate.format(formatter)})"
                else -> ""
            }
            "$daysText$rangeText"
        }
        ScheduleType.SPECIFIC_DATES -> {
            val datesText = schedule.specificDates
                .sorted()
                .joinToString(", ") { it.format(formatter) }
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
    val array = org.json.JSONArray()
    days.sorted().forEach { array.put(it) }
    return array.toString()
}

fun encodeSpecificDates(dates: Collection<LocalDate>): String {
    val array = org.json.JSONArray()
    dates.sorted().forEach { array.put(it.toString()) }
    return array.toString()
}

fun parseDaysOfWeekJson(value: String?): Set<Int> {
    if (value.isNullOrBlank()) return emptySet()
    return runCatching {
        val array = org.json.JSONArray(value)
        List(array.length()) { array.getInt(it) }.toSet()
    }.getOrDefault(emptySet())
}

fun parseSpecificDatesJson(value: String?): Set<LocalDate> {
    if (value.isNullOrBlank()) return emptySet()
    return runCatching {
        val array = org.json.JSONArray(value)
        List(array.length()) { LocalDate.parse(array.getString(it)) }.toSet()
    }.getOrDefault(emptySet())
}
