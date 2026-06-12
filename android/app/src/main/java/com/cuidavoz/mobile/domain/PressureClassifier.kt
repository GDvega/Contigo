package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.HealthSettingsEntity

enum class PressureStatus {
    NORMAL,
    ELEVATED,
    HIGH,
    CRITICAL,
    OUT_OF_RANGE,
}

object PressureClassifier {
    fun classify(
        systolic: Int,
        diastolic: Int,
        settings: HealthSettingsEntity?,
    ): PressureStatus {
        // 1. Clinical severity priority
        val baseStatus = when {
            systolic >= 180 || diastolic >= 120 -> PressureStatus.CRITICAL
            systolic >= 140 || diastolic >= 90 -> PressureStatus.HIGH
            else -> null
        }
        if (baseStatus != null) return baseStatus

        // 2. Custom ranges evaluation if they exist
        if (settings != null) {
            val isOutOfRange =
                systolic < settings.systolicMinNormal ||
                    systolic > settings.systolicMaxNormal ||
                    diastolic < settings.diastolicMinNormal ||
                    diastolic > settings.diastolicMaxNormal

            if (isOutOfRange) {
                return PressureStatus.OUT_OF_RANGE
            }
        }

        // 3. Standard classification for non-severe values
        return when {
            systolic >= 120 -> PressureStatus.ELEVATED
            else -> PressureStatus.NORMAL
        }
    }

    fun safeMessage(status: PressureStatus): String? {
        return when (status) {
            PressureStatus.OUT_OF_RANGE ->
                "Valor fuera del rango indicado. Consulta con tu familiar o profesional de salud."
            PressureStatus.HIGH,
            PressureStatus.CRITICAL ->
                "Revisar con tu familiar o profesional de salud."
            else -> null
        }
    }
}
