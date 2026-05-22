package com.cuidavoz.mobile.util

fun createLocalId(prefix: String): String {
    return "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}"
}
