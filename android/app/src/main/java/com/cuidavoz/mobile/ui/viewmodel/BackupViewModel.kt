package com.cuidavoz.mobile.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.backup.BackupFormatException
import com.cuidavoz.mobile.data.backup.BackupRepository
import com.cuidavoz.mobile.data.backup.BackupResult
import com.cuidavoz.mobile.data.backup.BackupSummary
import com.cuidavoz.mobile.data.backup.ImportResult
import com.cuidavoz.mobile.data.backup.ImportStrategy
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Exporting : BackupUiState
    data class ExportSuccess(
        val result: BackupResult,
    ) : BackupUiState
    data object ImportReading : BackupUiState
    data class ImportPreview(
        val sourceUri: Uri,
        val summary: BackupSummary,
    ) : BackupUiState
    data class Importing(
        val strategy: ImportStrategy,
    ) : BackupUiState
    data class ImportSuccess(
        val result: ImportResult,
    ) : BackupUiState
    data class Error(
        val message: String,
    ) : BackupUiState
}

class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val reminderScheduler: MedicationReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun suggestedFileName(): String = backupRepository.suggestedFileName()

    fun dismissState() {
        _uiState.value = BackupUiState.Idle
    }

    fun exportBackup(destinationUri: Uri?) {
        if (destinationUri == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.Exporting
            runCatching {
                backupRepository.createBackup(destinationUri)
            }.onSuccess { result ->
                _uiState.value = BackupUiState.ExportSuccess(result)
            }.onFailure { error ->
                _uiState.value = BackupUiState.Error(resolveMessage(error, EXPORT_ERROR_MESSAGE))
            }
        }
    }

    fun readBackupSummary(sourceUri: Uri?) {
        if (sourceUri == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.ImportReading
            runCatching {
                backupRepository.readBackupSummary(sourceUri)
            }.onSuccess { summary ->
                _uiState.value = BackupUiState.ImportPreview(
                    sourceUri = sourceUri,
                    summary = summary,
                )
            }.onFailure { error ->
                _uiState.value = BackupUiState.Error(resolveMessage(error, IMPORT_READ_ERROR_MESSAGE))
            }
        }
    }

    fun importBackup(strategy: ImportStrategy) {
        val currentState = _uiState.value as? BackupUiState.ImportPreview ?: return

        viewModelScope.launch {
            _uiState.value = BackupUiState.Importing(strategy)
            val importResult = runCatching {
                if (strategy == ImportStrategy.REPLACE_ALL) {
                    runCatching {
                        reminderScheduler.cancelAllMedicationReminders(DEFAULT_PATIENT_ID)
                    }.onFailure { error ->
                        Log.e(TAG, "No se pudieron cancelar recordatorios antes de restaurar", error)
                    }
                }

                val result = backupRepository.importBackup(
                    sourceUri = currentState.sourceUri,
                    strategy = strategy,
                )
                attachReminderResult(result)
            }.onFailure {
                runCatching {
                    reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
                }.onFailure { rescheduleError ->
                    Log.e(TAG, "No se pudieron restaurar recordatorios despues de un error", rescheduleError)
                }
            }

            importResult.onSuccess { result ->
                _uiState.value = BackupUiState.ImportSuccess(result)
            }.onFailure { error ->
                _uiState.value = BackupUiState.Error(resolveMessage(error, IMPORT_ERROR_MESSAGE))
            }
        }
    }

    private suspend fun attachReminderResult(result: ImportResult): ImportResult {
        return runCatching {
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            result
        }.getOrElse { error ->
            Log.e(TAG, "No se pudieron reprogramar recordatorios despues de importar", error)
            result.copy(
                errors = result.errors + "Los recordatorios deben reprogramarse otra vez desde Ajustes.",
            )
        }
    }

    private fun resolveMessage(
        error: Throwable,
        fallback: String,
    ): String {
        return when (error) {
            is BackupFormatException -> error.message ?: fallback
            else -> fallback
        }
    }

    private companion object {
        const val TAG = "[CuidaVoz][Backup]"
        const val EXPORT_ERROR_MESSAGE = "No pudimos crear el respaldo. Intenta otra vez."
        const val IMPORT_READ_ERROR_MESSAGE = "El archivo seleccionado no parece ser un respaldo de CuidaVoz."
        const val IMPORT_ERROR_MESSAGE = "No pudimos restaurar los datos. Intenta otra vez."
    }
}
