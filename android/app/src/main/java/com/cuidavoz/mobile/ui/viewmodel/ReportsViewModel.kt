package com.cuidavoz.mobile.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.report.PdfReportGenerator
import com.cuidavoz.mobile.data.repository.MedicalReportRepository
import com.cuidavoz.mobile.domain.report.MedicalReportBuilder
import com.cuidavoz.mobile.domain.report.MedicalReportData
import com.cuidavoz.mobile.domain.report.MedicalReportPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    data class Ready(
        val period: MedicalReportPeriod,
        val reportData: MedicalReportData,
        val cachedPdfUri: Uri? = null,
        val cachedPdfFileName: String? = null,
        val pendingShareUri: Uri? = null,
        val message: String? = null,
        val busyMessage: String? = null,
    ) : ReportsUiState {
        val hasEnoughData: Boolean
            get() = MedicalReportBuilder.hasEnoughData(reportData)
    }

    data class Error(
        val message: String,
    ) : ReportsUiState
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val medicalReportRepository: MedicalReportRepository,
    private val pdfReportGenerator: PdfReportGenerator,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport(MedicalReportPeriod.LAST_7_DAYS)
    }

    fun loadReport(period: MedicalReportPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading
            runCatching {
                medicalReportRepository.buildReportData(period)
            }.onSuccess { reportData ->
                _uiState.value = ReportsUiState.Ready(
                    period = period,
                    reportData = reportData,
                )
            }.onFailure {
                _uiState.value = ReportsUiState.Error("No pudimos preparar el reporte.")
            }
        }
    }

    fun changePeriod(period: MedicalReportPeriod) {
        loadReport(period)
    }

    fun generatePdf() {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                busyMessage = "Preparando reporte...",
                message = null,
            )
            runCatching {
                pdfReportGenerator.generateToCache(currentState.reportData)
            }.onSuccess { generatedPdf ->
                _uiState.value = currentState.copy(
                    cachedPdfUri = generatedPdf.contentUri,
                    cachedPdfFileName = generatedPdf.fileName,
                    busyMessage = null,
                    message = "Reporte listo.",
                )
            }.onFailure {
                _uiState.value = currentState.copy(
                    busyMessage = null,
                    message = "No pudimos preparar el reporte.",
                )
            }
        }
    }

    fun savePdf(destinationUri: Uri?) {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        if (destinationUri == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                busyMessage = "Guardando reporte...",
                message = null,
            )
            runCatching {
                pdfReportGenerator.saveToUri(
                    reportData = currentState.reportData,
                    destinationUri = destinationUri,
                )
            }.onSuccess {
                _uiState.value = currentState.copy(
                    busyMessage = null,
                    message = "Reporte guardado correctamente.",
                )
            }.onFailure {
                _uiState.value = currentState.copy(
                    busyMessage = null,
                    message = "No pudimos guardar el reporte.",
                )
            }
        }
    }

    fun sharePdf() {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        if (currentState.cachedPdfUri != null) {
            _uiState.value = currentState.copy(
                pendingShareUri = currentState.cachedPdfUri,
                message = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                busyMessage = "Compartiendo reporte...",
                message = null,
            )
            runCatching {
                pdfReportGenerator.generateToCache(currentState.reportData)
            }.onSuccess { generatedPdf ->
                _uiState.value = currentState.copy(
                    cachedPdfUri = generatedPdf.contentUri,
                    cachedPdfFileName = generatedPdf.fileName,
                    pendingShareUri = generatedPdf.contentUri,
                    busyMessage = null,
                )
            }.onFailure {
                _uiState.value = currentState.copy(
                    busyMessage = null,
                    message = "No pudimos compartir el reporte.",
                )
            }
        }
    }

    fun consumeMessage() {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        _uiState.value = currentState.copy(message = null)
    }

    fun markShareHandled() {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        _uiState.value = currentState.copy(pendingShareUri = null)
    }

    fun markShareFailed() {
        val currentState = _uiState.value as? ReportsUiState.Ready ?: return
        _uiState.value = currentState.copy(
            pendingShareUri = null,
            message = "No pudimos compartir el reporte.",
        )
    }

    fun suggestedFileName(): String {
        val currentState = _uiState.value as? ReportsUiState.Ready
        return pdfReportGenerator.suggestedFileName(currentState?.reportData?.generatedAt ?: System.currentTimeMillis())
    }
}
