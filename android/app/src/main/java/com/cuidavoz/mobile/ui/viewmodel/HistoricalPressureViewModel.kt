package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class HistoricalPressureUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
    val notes: String = "",
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val selectedHour: Int = LocalTime.now().hour,
    val selectedMinute: Int = LocalTime.now().minute,
    val isSaving: Boolean = false,
    val message: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class HistoricalPressureViewModel @Inject constructor(
    private val pressureRepository: PressureRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricalPressureUiState())
    val uiState: StateFlow<HistoricalPressureUiState> = _uiState.asStateFlow()

    fun onSystolicChange(value: String) {
        _uiState.update { it.copy(systolic = value.filter(Char::isDigit)) }
    }

    fun onDiastolicChange(value: String) {
        _uiState.update { it.copy(diastolic = value.filter(Char::isDigit)) }
    }

    fun onPulseChange(value: String) {
        _uiState.update { it.copy(pulse = value.filter(Char::isDigit)) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onDateChange(millis: Long) {
        _uiState.update { it.copy(selectedDateMillis = millis) }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        _uiState.update { it.copy(selectedHour = hour, selectedMinute = minute) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun saveReading(keepDate: Boolean, onComplete: () -> Unit = {}) {
        val state = _uiState.value
        val sys = state.systolic.toIntOrNull()
        val dia = state.diastolic.toIntOrNull()
        val pul = state.pulse.toIntOrNull()

        if (sys == null || dia == null) {
            _uiState.update { it.copy(message = "Ingresa los valores de presión.") }
            return
        }

        if (sys !in 50..250 || dia !in 30..160 || (pul != null && pul !in 30..220)) {
            _uiState.update { it.copy(message = "Revisa los valores ingresados.") }
            return
        }

        val measuredAt = calculateTimestamp(state.selectedDateMillis, state.selectedHour, state.selectedMinute)
        
        // Validar que no sea futuro y no más de 30 días
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        
        if (measuredAt > now) {
            _uiState.update { it.copy(message = "No puedes registrar datos del futuro.") }
            return
        }
        if (measuredAt < thirtyDaysAgo) {
            _uiState.update { it.copy(message = "Solo se permiten datos de los últimos 30 días.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            try {
                pressureRepository.recordPressure(
                    patientId = DEFAULT_PATIENT_ID,
                    systolic = sys,
                    diastolic = dia,
                    pulse = pul,
                    notes = state.notes.trim().ifBlank { "Registro histórico manual" },
                    measuredAt = measuredAt
                )
                
                if (keepDate) {
                    _uiState.update { 
                        it.copy(
                            systolic = "",
                            diastolic = "",
                            pulse = "",
                            notes = "",
                            isSaving = false,
                            saveSuccess = false,
                            message = "Registro guardado. Puedes agregar otro para el mismo día."
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    onComplete()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "Error al guardar: ${e.message}") }
            }
        }
    }

    private fun calculateTimestamp(dateMillis: Long, hour: Int, minute: Int): Long {
        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val time = LocalTime.of(hour, minute)
        return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
