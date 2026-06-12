package com.cuidavoz.mobile.voice

import org.tartarus.snowball.ext.SpanishStemmer

object SpanishStemmerWrapper {
    fun stem(text: String): String {
        // Lucene includes the original snowball stemmers in the same package name
        val stemmer = SpanishStemmer()
        val tokens = text.lowercase().split(Regex("\\W+")).filter { it.isNotEmpty() }
        return tokens.joinToString(" ") { word ->
            stemmer.current = word
            if (stemmer.stem()) {
                stemmer.current
            } else {
                word
            }
        }
    }
}
