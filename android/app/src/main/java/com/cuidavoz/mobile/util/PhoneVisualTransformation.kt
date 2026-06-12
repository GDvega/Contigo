package com.cuidavoz.mobile.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A [VisualTransformation] that formats phone numbers as:
 * +XX X XXXX XXXX or XXXXXXXX depending on input.
 * Simplistic approach for common formats.
 */
class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.filter { (it.isDigit() || it == '+') }
        var out = ""
        
        // Very basic formatting: + (digits) 
        // Example: +56 9 1234 5678
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (trimmed.startsWith("+")) {
                if (i == 2 || i == 3 || i == 7) out += " "
            } else {
                if (i == 1 || i == 5) out += " "
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                var spaces = 0
                val sub = trimmed.take(offset)
                for (i in sub.indices) {
                    if (trimmed.startsWith("+")) {
                        if (i == 2 || i == 3 || i == 7) spaces++
                    } else {
                        if (i == 1 || i == 5) spaces++
                    }
                }
                return offset + spaces
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                var spaces = 0
                for (i in 0 until offset) {
                    val originalIdx = i - spaces
                    if (originalIdx >= trimmed.length) break
                    if (trimmed.startsWith("+")) {
                        if (originalIdx == 2 || originalIdx == 3 || originalIdx == 7) spaces++
                    } else {
                        if (originalIdx == 1 || originalIdx == 5) spaces++
                    }
                }
                return (offset - spaces).coerceAtLeast(0)
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
