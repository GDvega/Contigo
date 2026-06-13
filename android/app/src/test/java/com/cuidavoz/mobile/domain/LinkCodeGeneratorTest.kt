package com.cuidavoz.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCodeGeneratorTest {
    @Test
    fun generateProducesTenCharacterCode() {
        val code = LinkCodeGenerator.generate()
        assertEquals(LinkCodeGenerator.CODE_LENGTH, code.length)
        assertTrue(LinkCodeGenerator.isCurrentFormat(code))
    }

    @Test
    fun acceptsLegacySixDigitCodes() {
        assertTrue(LinkCodeGenerator.isValid("123456"))
    }

    @Test
    fun rejectsShortCodes() {
        assertFalse(LinkCodeGenerator.isValid("ABC"))
    }

    @Test
    fun normalizeInputUppercasesAndFilters() {
        assertEquals("AB23", LinkCodeGenerator.normalizeInput(" ab23! "))
    }
}
