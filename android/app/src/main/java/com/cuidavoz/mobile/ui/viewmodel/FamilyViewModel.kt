package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.domain.DailyStatusSnapshot
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.formatDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FamilyScreenState(
    val patient: PatientEntity? = null,
    val contact: FamilyContactEntity? = null,
    val dailyStatus: DailyStatusSnapshot? = null,
    val adherencePercentage: Int = 0,
    val latestPressureSummary: String = "Sin registros",
    val latestPressureDetail: String = "Todavía no hay lecturas registradas.",
    val pressureSafetyText: String = "Dentro del rango indicado",
    val alerts: List<String> = emptyList(),
) {
    val hasContact: Boolean
        get() = !contact?.phone.isNullOrBlank()

    val adherenceText: String
        get() {
            val total = dailyStatus?.activeMedicationCount ?: 0
            val taken = dailyStatus?.takenMedicationCount ?: 0
            if (total == 0) return "Sin medicamentos activos"
            return "${((taken.toDouble() / total.toDouble()) * 100.0).toInt()}%"
        }
}

class FamilyViewModel(
    patientRepository: PatientRepository,
    familyContactRepository: FamilyContactRepository,
    settingsRepository: SettingsRepository,
    dailyStatusRepository: DailyStatusRepository,
) : ViewModel() {
    val uiState: StateFlow<FamilyScreenState> = combine(
        patientRepository.observeCurrentPatient(),
        familyContactRepository.observePrimaryContact(DEFAULT_PATIENT_ID),
        settingsRepository.observeHealthSettings(DEFAULT_PATIENT_ID),
        dailyStatusRepository.observeDailyStatus(DEFAULT_PATIENT_ID),
    ) { patient, contact, settings, dailyStatus ->
        val latestPressure = dailyStatus.latestPressureToday
        val pressureStatus = latestPressure?.status?.let { runCatching { PressureStatus.valueOf(it) }.getOrNull() }
        val alerts = buildList {
            when (pressureStatus) {
                PressureStatus.HIGH,
                PressureStatus.CRITICAL,
                PressureStatus.OUT_OF_RANGE ->
                    add("Este valor debe ser revisado por un familiar o profesional de salud.")
                else -> Unit
            }
            if (dailyStatus.pendingMedicationCount > 0) {
                add("Hay medicamentos pendientes hoy.")
            }
        }

        FamilyScreenState(
            patient = patient,
            contact = contact,
            dailyStatus = dailyStatus,
            adherencePercentage = if (dailyStatus.activeMedicationCount == 0) {
                100
            } else {
                ((dailyStatus.takenMedicationCount.toDouble() / dailyStatus.activeMedicationCount.toDouble()) * 100.0).toInt()
            },
            latestPressureSummary = latestPressure?.let {
                "${it.systolic}/${it.diastolic}${it.pulse?.let { pulse -> " · Pulso $pulse" } ?: ""}"
            } ?: "Sin registros",
            latestPressureDetail = latestPressure?.let { formatDateTime(it.measuredAt) }
                ?: "Todavía no hay lecturas registradas.",
            pressureSafetyText = when (pressureStatus) {
                PressureStatus.NORMAL,
                PressureStatus.ELEVATED -> "Dentro del rango indicado"
                PressureStatus.OUT_OF_RANGE -> "Fuera del rango indicado"
                PressureStatus.HIGH,
                PressureStatus.CRITICAL -> "Revisar con un profesional de salud"
                null -> "Dentro del rango indicado"
            },
            alerts = alerts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FamilyScreenState(),
    )
}
