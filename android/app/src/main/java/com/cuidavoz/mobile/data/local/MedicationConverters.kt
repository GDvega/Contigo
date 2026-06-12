package com.cuidavoz.mobile.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import java.time.LocalDate

class MedicationConverters {
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val array = JSONArray(value)
            List(array.length()) { array.getInt(it) }
        }.getOrNull()
    }

    @TypeConverter
    fun fromLocalDateList(value: List<LocalDate>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { array.put(it.toString()) }
        return array.toString()
    }

    @TypeConverter
    fun toLocalDateList(value: String?): List<LocalDate>? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val array = JSONArray(value)
            List(array.length()) { LocalDate.parse(array.getString(it)) }
        }.getOrNull()
    }
}
