package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.R
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
    val latestPressure: LatestPressureInfo? = null,
    val pressureSafetyResId: Int = R.string.family_pressure_safety_in_range,
    val alertsResIds: List<Int> = emptyList(),
) {
    data class LatestPressureInfo(
        val systolic: Int,
        val diastolic: Int,
        val pulse: Int?,
        val measuredAt: Long,
    )

    val hasContact: Boolean
        get() = !contact?.phone.isNullOrBlank()

    val adherenceText: String
        get() {
            val total = dailyStatus?.activeMedicationCount ?: 0
            val taken = dailyStatus?.takenMedicationCount ?: 0
            if (total == 0) return "0%" // The UI will handle "Sin medicamentos activos" via stringResource if needed, but let's just return the %
            return "${((taken.toDouble() / total.toDouble()) * 100.0).toInt()}%"
        }
    
    val hasAdherence: Boolean
        get() = (dailyStatus?.activeMedicationCount ?: 0) > 0
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
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
        
        val alertsResIds = buildList<Int> {
            when (pressureStatus) {
                PressureStatus.HIGH,
                PressureStatus.CRITICAL,
                PressureStatus.OUT_OF_RANGE ->
                    add(R.string.family_alert_pressure_review)
                else -> Unit
            }
            if (dailyStatus.pendingMedicationCount > 0) {
                add(R.string.family_alert_pending_meds)
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
            latestPressure = latestPressure?.let {
                FamilyScreenState.LatestPressureInfo(
                    systolic = it.systolic,
                    diastolic = it.diastolic,
                    pulse = it.pulse,
                    measuredAt = it.measuredAt
                )
            },
            pressureSafetyResId = when (pressureStatus) {
                PressureStatus.NORMAL,
                PressureStatus.ELEVATED -> R.string.family_pressure_safety_in_range
                PressureStatus.OUT_OF_RANGE -> R.string.family_pressure_safety_out_of_range
                PressureStatus.HIGH,
                PressureStatus.CRITICAL -> R.string.family_pressure_safety_doctor
                null -> R.string.family_pressure_safety_in_range
            },
            alertsResIds = alertsResIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FamilyScreenState(),
    )
}
