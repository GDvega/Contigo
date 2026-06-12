package com.cuidavoz.mobile.domain

enum class MedicationDoseStatus {
    TAKEN,
    SKIPPED,
}

data class MedicationDoseOutcome(
    val medicationId: String,
    val status: MedicationDoseStatus,
    val skipReason: MedicationSkipReason? = null,
)

data class MedicationOutcomeResult(
    val savedCount: Int,
    val skippedCount: Int,
    val stillPendingCount: Int,
) {
    val groupResolved: Boolean
        get() = stillPendingCount == 0

    val anyRecorded: Boolean
        get() = savedCount > 0 || skippedCount > 0
}

fun medicationOutcomeUserMessage(result: MedicationOutcomeResult): String {
    return when {
        !result.anyRecorded -> "No había pastillas pendientes para registrar."
        result.groupResolved && result.skippedCount > 0 && result.savedCount == 0 ->
            "Quedó registrado que no pudiste tomar tus pastillas."
        result.groupResolved && result.skippedCount > 0 ->
            "Tomas registradas. Algunas quedaron marcadas como no tomadas."
        result.groupResolved -> "Pastillas registradas."
        result.stillPendingCount == 1 ->
            "Registrado. Aún queda 1 pastilla pendiente."
        else ->
            "Registrado. Aún quedan ${result.stillPendingCount} pastillas pendientes."
    }
}
