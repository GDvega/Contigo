package com.cuidavoz.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class LinkCodeGeneratorExtendedTest {
    private val deterministicRandom = SecureRandom().apply {
        setSeed(1234L)
    }

    @Test
    fun generateUsesAlphabetWithoutAmbiguousChars() {
        val code = LinkCodeGenerator.generate(deterministicRandom)

        assertEquals(LinkCodeGenerator.CODE_LENGTH, code.length)
        assertTrue(code.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
        assertFalse(code.contains('0'))
        assertFalse(code.contains('O'))
    }

    @Test
    fun normalizeInputStripsInvalidCharactersAndUppercases() {
        assertEquals("AB2", LinkCodeGenerator.normalizeInput(" ab-2 "))
    }

    @Test
    fun normalizeInputTruncatesToMaxLength() {
        val normalized = LinkCodeGenerator.normalizeInput("ABCDEFGHJKLMN")

        assertEquals(LinkCodeGenerator.CODE_LENGTH, normalized.length)
    }

    @Test
    fun isValidAcceptsLegacySixDigitCodes() {
        assertTrue(LinkCodeGenerator.isValid("123456"))
    }

    @Test
    fun isValidRejectsWrongLengthAlphanumeric() {
        assertFalse(LinkCodeGenerator.isValid("ABC"))
        assertFalse(LinkCodeGenerator.isValid("1234567"))
    }

    @Test
    fun isCurrentFormatRequiresExactAlphabetAndLength() {
        assertTrue(LinkCodeGenerator.isCurrentFormat("ABCDEFGHJK"))
        assertFalse(LinkCodeGenerator.isCurrentFormat("1234567890"))
    }
}
