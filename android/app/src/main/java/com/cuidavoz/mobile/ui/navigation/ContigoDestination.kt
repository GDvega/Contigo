package com.cuidavoz.mobile.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ContigoDestination {
    @Serializable
    data object Onboarding : ContigoDestination

    @Serializable
    data object PatientHome : ContigoDestination

    @Serializable
    data object MeasurePressure : ContigoDestination

    @Serializable
    data object PressureSaved : ContigoDestination

    @Serializable
    data object Help : ContigoDestination

    @Serializable
    data object CaregiverHome : ContigoDestination

    @Serializable
    data object HistoricalPressure : ContigoDestination

    @Serializable
    data object LinkCaregiver : ContigoDestination

    @Serializable
    data object Medications : ContigoDestination

    @Serializable
    data object Records : ContigoDestination

    @Serializable
    data object Reports : ContigoDestination

    @Serializable
    data object Settings : ContigoDestination

    @Serializable
    data object FamilyContact : ContigoDestination

    @Serializable
    data object Backup : ContigoDestination
}

fun ContigoDestination.voiceGuideText(
    patientHomeAudio: String,
    contactName: String?,
    hasContact: Boolean,
): String? = when (this) {
    ContigoDestination.PatientHome -> patientHomeAudio
    ContigoDestination.MeasurePressure ->
        "Mide tu presión cuando puedas. Escribe la presión alta, la presión baja y, si quieres, tu pulso."
    ContigoDestination.Help ->
        if (!hasContact) {
            "Todavía no hay un contacto de ayuda. Puedes abrir la zona del familiar o cuidador."
        } else {
            "Aquí puedes llamar o enviar un mensaje a ${contactName.orEmpty()}."
        }
    ContigoDestination.CaregiverHome ->
        "Área para familiar o cuidador. Aquí puedes abrir medicamentos, registros, cargar históricos, reporte médico, contacto familiar, ajustes y guardar copia."
    ContigoDestination.Medications -> "Aquí puedes agregar o cambiar pastillas."
    ContigoDestination.Records -> "Aquí puedes revisar presión y tomas."
    ContigoDestination.Reports -> "Aquí puedes crear el reporte para el médico."
    ContigoDestination.Settings -> "Aquí puedes cambiar recordatorios, rangos y voz."
    ContigoDestination.FamilyContact -> "Aquí puedes cambiar el contacto familiar."
    ContigoDestination.Backup -> "Aquí puedes guardar o recuperar una copia de seguridad."
    else -> null
}
