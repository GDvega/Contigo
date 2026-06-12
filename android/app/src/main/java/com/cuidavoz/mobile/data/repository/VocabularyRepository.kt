package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.util.ContigoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepository @Inject constructor() {
    private val synonymCache = mutableMapOf<String, Set<String>>()

    suspend fun getSynonyms(word: String): Set<String> = withContext(Dispatchers.IO) {
        val cleanWord = word.lowercase().trim()
        if (cleanWord.isBlank()) return@withContext emptySet()
        
        synonymCache[cleanWord]?.let { return@withContext it }

        runCatching {
            val url = URL("https://api.datamuse.com/words?rel_syn=$cleanWord&v=es")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(response)
                val synonyms = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    synonyms.add(obj.getString("word"))
                }
                synonymCache[cleanWord] = synonyms
                synonyms
            } else {
                emptySet()
            }
        }.getOrElse {
            ContigoLog.e(TAG, "Error fetching synonyms from Datamuse", it)
            emptySet()
        }
    }

    private companion object {
        const val TAG = "VocabularyRepository"
    }
}
