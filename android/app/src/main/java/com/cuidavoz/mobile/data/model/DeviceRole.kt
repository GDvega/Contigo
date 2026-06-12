package com.cuidavoz.mobile.data.model

import com.cuidavoz.mobile.ui.navigation.UserMode

enum class DeviceRole {
    PATIENT,
    CAREGIVER,
    ;

    fun toUserMode(): UserMode = when (this) {
        PATIENT -> UserMode.PATIENT
        CAREGIVER -> UserMode.CAREGIVER
    }

    fun toStorageValue(): String = when (this) {
        PATIENT -> "patient"
        CAREGIVER -> "caregiver"
    }

    companion object {
        fun fromStorageValue(value: String?): DeviceRole? = when (value) {
            "patient" -> PATIENT
            "caregiver" -> CAREGIVER
            else -> null
        }
    }
}
