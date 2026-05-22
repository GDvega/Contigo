package com.cuidavoz.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeFormatUtilsTest {
    @Test
    fun `formatTimeForDisplay shows am pm`() {
        assertEquals("7:00 a. m.", formatTimeForDisplay("07:00"))
        assertEquals("1:45 p. m.", formatTimeForDisplay("13:45"))
        assertEquals("8:00 p. m.", formatTimeForDisplay("20:00"))
    }

    @Test
    fun `normalizeTimeTo24h accepts friendly input`() {
        assertEquals("07:00", normalizeTimeTo24h("7 am"))
        assertEquals("20:00", normalizeTimeTo24h("8 de la noche"))
        assertEquals("14:22", normalizeTimeTo24h("2:22 pm"))
        assertEquals("02:22", normalizeTimeTo24h("0222"))
        assertEquals("13:45", normalizeTimeTo24h("1345"))
    }

    @Test
    fun `normalizeTimeTo24h rejects invalid values`() {
        assertNull(normalizeTimeTo24h(""))
        assertNull(normalizeTimeTo24h("25:30"))
        assertNull(normalizeTimeTo24h("13 pm"))
    }

    @Test
    fun `formatTimeForVoice returns spoken spanish`() {
        assertEquals("7 de la mañana", formatTimeForVoice("07:00"))
        assertEquals("1 y 45 de la tarde", formatTimeForVoice("13:45"))
        assertEquals("8 de la noche", formatTimeForVoice("20:00"))
    }
}
