package com.cuidavoz.mobile.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun todayRangeMillis(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    return todayRangeMillis(LocalDate.now(zoneId), zoneId)
}

fun todayRangeMillis(
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Pair<Long, Long> {
    val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return start to end
}

fun scheduleTimeToMillis(
    scheduleTime: String,
    day: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val parts = scheduleTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return day.atTime(LocalTime.of(hour, minute))
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun formatDateTime(timestamp: Long): String {
    val base = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    return "$base ${formatTimeForDisplay(time)}"
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

fun formatScheduleTime(scheduleTime: String): String {
    return formatTimeForDisplay(scheduleTime)
}
