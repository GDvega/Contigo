package com.cuidavoz.mobile.domain

enum class MedicationSkipReason {
    OUT_OF_STOCK,
    FELT_UNWELL,
    FORGOT,
    OTHER,
    ;

    fun displayLabel(): String = when (this) {
        OUT_OF_STOCK -> "Se acabó"
        FELT_UNWELL -> "Me sentí mal"
        FORGOT -> "La olvidé"
        OTHER -> "Otro motivo"
    }

    fun alertType(): String = when (this) {
        OUT_OF_STOCK -> "medication_out_of_stock"
        else -> "medication_skipped"
    }

    fun alertSeverity(): String = when (this) {
        FELT_UNWELL -> "high"
        else -> "medium"
    }

    fun alertMessage(medicationName: String, scheduleTimeLabel: String): String = when (this) {
        OUT_OF_STOCK -> "Se acabó $medicationName de las $scheduleTimeLabel."
        FELT_UNWELL -> "El paciente no tomó $medicationName de las $scheduleTimeLabel porque se sintió mal."
        FORGOT -> "El paciente no tomó $medicationName de las $scheduleTimeLabel."
        OTHER -> "El paciente no pudo tomar $medicationName de las $scheduleTimeLabel."
    }

    companion object {
        fun fromStorage(value: String?): MedicationSkipReason? {
            if (value.isNullOrBlank()) return null
            return runCatching { valueOf(value) }.getOrNull()
        }
    }
}

fun medicationSkipReasonLabel(value: String?): String? =
    MedicationSkipReason.fromStorage(value)?.displayLabel()

fun medicationStatusDetail(status: String, skipReason: String?): String {
    val base = when (status) {
        "TAKEN" -> "Tomado"
        "SKIPPED" -> "Omitido"
        else -> "Pendiente"
    }
    val reasonLabel = medicationSkipReasonLabel(skipReason)
    return if (status == "SKIPPED" && reasonLabel != null) {
        "$base · $reasonLabel"
    } else {
        base
    }
}
