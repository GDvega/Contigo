package com.cuidavoz.mobile.domain

import java.security.SecureRandom

object LinkCodeGenerator {
    const val CODE_LENGTH = 10
    private const val LEGACY_CODE_LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(secureRandom: SecureRandom = SecureRandom()): String {
        return buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) {
                append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
            }
        }
    }

    fun normalizeInput(value: String): String {
        return value
            .trim()
            .uppercase()
            .filter { it.isDigit() || it in ALPHABET }
            .take(CODE_LENGTH)
    }

    fun isValid(code: String): Boolean {
        val normalized = code.trim().uppercase()
        return isCurrentFormat(normalized) || isLegacyFormat(normalized)
    }

    fun isCurrentFormat(code: String): Boolean {
        return code.length == CODE_LENGTH && code.all { it in ALPHABET }
    }

    private fun isLegacyFormat(code: String): Boolean {
        return code.length == LEGACY_CODE_LENGTH && code.all(Char::isDigit)
    }
}
