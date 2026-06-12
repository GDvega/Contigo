package com.cuidavoz.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateTimeUtilsTest {
    @Test
    fun explicitDateRangeUsesRequestedDate() {
        val zoneId = ZoneId.of("America/Lima")
        val date = LocalDate.parse("2026-06-05")

        val (start, end) = todayRangeMillis(date, zoneId)

        assertEquals(date.atStartOfDay(zoneId).toInstant().toEpochMilli(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(), end)
    }

    @Test
    fun nextDateRangeStartsWherePreviousRangeEnds() {
        val zoneId = ZoneId.of("America/Lima")
        val date = LocalDate.parse("2026-06-05")

        val (_, firstEnd) = todayRangeMillis(date, zoneId)
        val (nextStart, _) = todayRangeMillis(date.plusDays(1), zoneId)

        assertEquals(firstEnd, nextStart)
    }
}
