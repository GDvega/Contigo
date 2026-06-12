package com.cuidavoz.mobile.util

object PhoneValidator {
    private val PHONE_REGEX = Regex("^\\+?[0-9]{7,15}$")

    fun isValid(phone: String): Boolean = PHONE_REGEX.matches(phone.trim())

    fun normalize(phone: String): String {
        return phone.filter { (it.isDigit() || it == '+') }
    }
}
