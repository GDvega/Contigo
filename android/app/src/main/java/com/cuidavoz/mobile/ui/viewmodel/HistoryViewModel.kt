package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.domain.MedicationScheduleCalculator
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryTab {
    PRESSURE,
    MEDICATIONS,
}

enum class PressureRangeFilter(val days: Int?) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    ALL(null),
}

enum class MedicationRangeFilter(val days: Int) {
    TODAY(1),
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    ALL(Int.MAX_VALUE),
}

data class MedicationHistoryItem(
    val medicationName: String,
    val scheduledFor: Long,
    val status: String,
    val skipReason: String?,
    val takenAt: Long?,
)

data class HistoryScreenState(
    val selectedTab: HistoryTab = HistoryTab.PRESSURE,
    val pressureFilter: PressureRangeFilter = PressureRangeFilter.SEVEN_DAYS,
    val medicationFilter: MedicationRangeFilter = MedicationRangeFilter.TODAY,
    val pressureReadings: List<BloodPressureEntity> = emptyList(),
    val medicationHistory: List<MedicationHistoryItem> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    pressureRepository: PressureRepository,
    medicationRepository: MedicationRepository,
    medicationLogRepository: MedicationLogRepository,
) : ViewModel() {
    private val _pressureRepository = pressureRepository
    private val filters = MutableStateFlow(HistoryFilterState())

    val uiState: StateFlow<HistoryScreenState> = combine(
        pressureRepository.observeRecentReadings(DEFAULT_PATIENT_ID),
        medicationRepository.observeActiveMedications(DEFAULT_PATIENT_ID),
        medicationLogRepository.observeLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE),
        filters,
    ) { pressureReadings, medications, logs, filterState ->
        HistoryScreenState(
            selectedTab = filterState.selectedTab,
            pressureFilter = filterState.pressureFilter,
            medicationFilter = filterState.medicationFilter,
            pressureReadings = pressureReadings.filterByPressureRange(filterState.pressureFilter),
            medicationHistory = buildMedicationHistory(
                medications = medications,
                logs = logs,
                filter = filterState.medicationFilter,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryScreenState(),
    )

    fun selectTab(tab: HistoryTab) {
        filters.update { it.copy(selectedTab = tab) }
    }

    fun selectPressureFilter(filter: PressureRangeFilter) {
        filters.update { it.copy(pressureFilter = filter) }
    }

    fun selectMedicationFilter(filter: MedicationRangeFilter) {
        filters.update { it.copy(medicationFilter = filter) }
    }

    fun deletePressureReading(reading: BloodPressureEntity) {
        viewModelScope.launch {
            _pressureRepository.deletePressureReading(reading)
        }
    }

    private fun List<BloodPressureEntity>.filterByPressureRange(
        filter: PressureRangeFilter,
    ): List<BloodPressureEntity> {
        val days = filter.days ?: return this
        val start = LocalDate.now().minusDays((days - 1).toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return filter { it.measuredAt >= start }
    }

    private fun buildMedicationHistory(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        filter: MedicationRangeFilter,
    ): List<MedicationHistoryItem> {
        if (medications.isEmpty()) {
            return emptyList()
        }
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val daysList = if (filter == MedicationRangeFilter.ALL) {
            val earliestTimestamp = listOfNotNull(
                medications.minOfOrNull(MedicationEntity::createdAt),
                logs.minOfOrNull(MedicationLogEntity::scheduledFor),
            ).minOrNull()
            val startDate = earliestTimestamp
                ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
                ?: today
            generateSequence(today) { current ->
                current.minusDays(1).takeIf { !it.isBefore(startDate) }
            }.toList()
        } else {
            (0 until filter.days).map { today.minusDays(it.toLong()) }
        }

        val logByKey = logs.associateBy { "${it.medicationId}_${it.scheduledFor}" }

        return daysList.flatMap { day ->
            medications.mapNotNull { medication ->
                if (!MedicationScheduleCalculator.isMedicationDueOnDate(medication, day)) {
                    return@mapNotNull null
                }
                val scheduledFor = scheduleTimeToMillis(medication.scheduleTime, day, zoneId)
                val log = logByKey["${medication.id}_${scheduledFor}"]
                MedicationHistoryItem(
                    medicationName = medication.name,
                    scheduledFor = scheduledFor,
                    status = log?.status ?: "PENDING",
                    skipReason = log?.skipReason,
                    takenAt = log?.takenAt,
                )
            }
        }.sortedByDescending { it.scheduledFor }
    }
}

private data class HistoryFilterState(
    val selectedTab: HistoryTab = HistoryTab.PRESSURE,
    val pressureFilter: PressureRangeFilter = PressureRangeFilter.SEVEN_DAYS,
    val medicationFilter: MedicationRangeFilter = MedicationRangeFilter.TODAY,
)
