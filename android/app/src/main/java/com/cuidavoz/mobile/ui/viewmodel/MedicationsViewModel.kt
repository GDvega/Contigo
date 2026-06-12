package com.cuidavoz.mobile.ui.viewmodel

import android.net.Uri
import com.cuidavoz.mobile.util.ContigoLog
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.ScheduleType
import com.cuidavoz.mobile.domain.encodeDaysOfWeek
import com.cuidavoz.mobile.domain.encodeSpecificDates
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.normalizeTimeTo24h
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedicationsScreenState(
    val activeMedications: List<MedicationEntity> = emptyList(),
    val expiredMedications: List<MedicationEntity> = emptyList(),
    val message: String? = null,
) {
    val isEmpty: Boolean
        get() = activeMedications.isEmpty() && expiredMedications.isEmpty()
}

@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderScheduler: MedicationReminderScheduler,
    private val medicationImageStorage: MedicationImageStorage,
) : ViewModel() {
    private val feedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MedicationsScreenState> = combine(
        medicationRepository.observeActiveMedications(DEFAULT_PATIENT_ID),
        feedback,
    ) { medications, message ->
        val today = LocalDate.now()
        val (expired, active) = medications.partition { it.isExpired(today) }
        MedicationsScreenState(
            activeMedications = active,
            expiredMedications = expired,
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MedicationsScreenState(),
    )

    fun dismissMessage() {
        feedback.value = null
    }

    fun saveMedication(
        editingId: String,
        name: String,
        dose: String,
        scheduleTime: String,
        color: String,
        shape: String,
        instructions: String,
        imageUri: String?,
        scheduleType: ScheduleType,
        startDate: LocalDate?,
        endDate: LocalDate?,
        daysOfWeek: Set<Int>,
        specificDates: Set<LocalDate>,
        onCompleted: (imageCopyFailed: Boolean) -> Unit,
    ) {
        if (name.isBlank()) {
            feedback.value = "Escribe el nombre de la pastilla."
            return
        }
        if (dose.isBlank()) {
            feedback.value = "Escribe la dosis."
            return
        }
        if (scheduleTime.isBlank()) {
            feedback.value = "Elige la hora."
            return
        }
        val normalizedScheduleTime = normalizeTimeTo24h(scheduleTime)
        if (normalizedScheduleTime == null) {
            feedback.value = "Revisa la hora. Ejemplo: 8 AM o 8 PM."
            return
        }
        if (scheduleType == ScheduleType.DATE_RANGE && endDate == null) {
            feedback.value = "Elige hasta qué día debe tomarla."
            return
        }
        if (scheduleType == ScheduleType.WEEKLY_DAYS && daysOfWeek.isEmpty()) {
            feedback.value = "Elige al menos un día."
            return
        }
        if (scheduleType == ScheduleType.SPECIFIC_DATES && specificDates.isEmpty()) {
            feedback.value = "Elige al menos una fecha."
            return
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            feedback.value = "La fecha final debe ser después de la fecha inicial."
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = medicationRepository.getMedicationById(editingId)
            val imageWasRemoved = existing?.imageUri != null && imageUri == null
            val imageResult = resolveImageUri(
                medicationId = editingId,
                existingImageUri = existing?.imageUri,
                selectedImageUri = imageUri,
            )
            val resolvedImageUri = imageResult.imageUri
            val imageSyncOperation = when {
                imageResult.copyFailed -> MedicationImageSyncOperation.KEEP
                imageWasRemoved -> MedicationImageSyncOperation.DELETE
                resolvedImageUri != existing?.imageUri -> MedicationImageSyncOperation.UPLOAD
                else -> MedicationImageSyncOperation.KEEP
            }
            val medication = MedicationEntity(
                id = existing?.id ?: editingId,
                patientId = existing?.patientId ?: DEFAULT_PATIENT_ID,
                name = name.trim(),
                dose = dose.trim(),
                color = color.trim().ifBlank { null },
                shape = shape.trim().ifBlank { null },
                instructions = instructions.trim().ifBlank { null },
                scheduleTime = normalizedScheduleTime,
                imageUri = resolvedImageUri,
                isActive = true,
                scheduleType = scheduleType.name,
                startDate = (startDate ?: LocalDate.now()).toString(),
                endDate = endDate?.toString(),
                daysOfWeek = when (scheduleType) {
                    ScheduleType.ALWAYS,
                    ScheduleType.DATE_RANGE -> MedicationScheduleDefaults.allDaysOfWeek.toList()
                    ScheduleType.WEEKLY_DAYS -> daysOfWeek.toList()
                    ScheduleType.SPECIFIC_DATES -> MedicationScheduleDefaults.allDaysOfWeek.toList()
                },
                specificDates = when (scheduleType) {
                    ScheduleType.SPECIFIC_DATES -> specificDates.toList()
                    else -> emptyList()
                },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            medicationRepository.upsert(medication, imageSyncOperation)
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            feedback.value = when {
                imageResult.copyFailed -> "No pudimos guardar la imagen. Elige otra foto o quita la imagen."
                imageWasRemoved -> "Imagen eliminada correctamente."
                else -> "Pastilla guardada correctamente. Se actualizarán los recordatorios."
            }
            onCompleted(imageResult.copyFailed)
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            medicationRepository.softDelete(id, System.currentTimeMillis())
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            feedback.value = "Pastilla desactivada correctamente. Se actualizarán los recordatorios."
        }
    }

    fun createCameraCaptureUri(medicationId: String): Uri {
        ContigoLog.d(TAG, "Creando URI temporal de camara")
        return medicationImageStorage.createCameraCaptureUri(medicationId)
    }

    fun onCameraPermissionDenied() {
        feedback.value = "No se otorgó permiso para usar la cámara. Puedes elegir una imagen de la galería."
    }

    fun onCameraOpenFailed() {
        feedback.value = "No pudimos abrir la cámara."
    }

    private fun resolveImageUri(
        medicationId: String,
        existingImageUri: String?,
        selectedImageUri: String?,
    ): ImageResolutionResult {
        if (selectedImageUri.isNullOrBlank()) {
            if (!existingImageUri.isNullOrBlank()) {
                medicationImageStorage.deleteMedicationImage(existingImageUri)
            }
            return ImageResolutionResult(imageUri = null, copyFailed = false)
        }

        if (selectedImageUri == existingImageUri) {
            return ImageResolutionResult(imageUri = existingImageUri, copyFailed = false)
        }

        return runCatching {
            val localUri = if (medicationImageStorage.isAppManagedImage(selectedImageUri)) {
                selectedImageUri
            } else {
                medicationImageStorage.saveImageFromUri(selectedImageUri.toUri(), medicationId)
            }
            if (!existingImageUri.isNullOrBlank() && existingImageUri != localUri) {
                medicationImageStorage.deleteMedicationImage(existingImageUri)
            }
            ImageResolutionResult(imageUri = localUri, copyFailed = false)
        }.onFailure { error ->
            ContigoLog.e(TAG, "No se pudo guardar imagen de medicamento", error)
        }.getOrElse {
            ImageResolutionResult(imageUri = existingImageUri, copyFailed = true)
        }
    }

    private companion object {
        const val TAG = "[Contigo][MedicationImage]"
    }
}

private data class ImageResolutionResult(
    val imageUri: String?,
    val copyFailed: Boolean,
)
