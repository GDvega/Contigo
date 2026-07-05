package com.cuidavoz.mobile.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.ScheduleType
import com.cuidavoz.mobile.domain.dayNumberToLabel
import com.cuidavoz.mobile.domain.toMedicationSchedule
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.viewmodel.MedicationsViewModel
import com.cuidavoz.mobile.util.createLocalId
import com.cuidavoz.mobile.util.formatTimeForDisplay
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.normalizeTimeTo24h
import com.cuidavoz.mobile.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class TreatmentOption {
    ALWAYS,
    FEW_DAYS,
    DATE_RANGE,
    SPECIFIC_DAYS,
}

private enum class SpecificDaysMode {
    WEEKLY,
    EXACT_DATES,
}

private val quickDayOptions = listOf(3, 5, 7, 10, 14, 30)
private fun displayDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

@Composable
fun MedicationsScreen(
    innerPadding: PaddingValues,
    viewModel: MedicationsViewModel,
    easyModeEnabled: Boolean,
    onBack: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editorState by remember { mutableStateOf<MedicationEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<MedicationEntity?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickVisualMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            editorState = editorState?.copy(imageUri = uri.toString())
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            editorState = editorState?.copy(imageUri = pendingCameraUri?.toString())
        } else {
            pendingCameraUri = null
        }
    }
    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            viewModel.onCameraPermissionDenied()
            return@rememberLauncherForActivityResult
        }
        val state = editorState ?: return@rememberLauncherForActivityResult
        runCatching {
            val cameraUri = viewModel.createCameraCaptureUri(state.id)
            pendingCameraUri = cameraUri
            takePictureLauncher.launch(cameraUri)
        }.onFailure {
            viewModel.onCameraOpenFailed()
        }
    }

    ToastMessageEffect(
        message = uiState.message,
        onConsumed = viewModel::dismissMessage,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.caregiver_btn_back))
                    Text(stringResource(R.string.caregiver_btn_back))
                }
                if (showSpeakScreenButton) {
                    FilledTonalButton(onClick = onSpeakScreen, modifier = Modifier.height(56.dp)) {
                        Text(stringResource(R.string.home_btn_listen))
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.meds_title),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.meds_intro),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
        }
        item {
            AppButton(
                label = stringResource(R.string.meds_btn_add),
                onClick = { editorState = MedicationEditorState(id = createLocalId("medication")) },
                minHeight = 60.dp,
                textSize = 22.sp,
            )
        }
        if (uiState.isEmpty) {
            item {
                AppCard {
                    Text(
                        text = stringResource(R.string.meds_empty_title),
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.meds_empty_desc),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }
            }
        }
        items(uiState.activeMedications, key = { it.id }) { medication ->
            MedicationItemCard(
                medication = medication,
                easyModeEnabled = easyModeEnabled,
                onEdit = { editorState = MedicationEditorState.fromEntity(medication) },
                onDeactivate = { deleteTarget = medication },
            )
        }

        if (uiState.expiredMedications.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.meds_section_finished),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.meds_section_finished_desc),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(uiState.expiredMedications, key = { it.id }) { medication ->
                MedicationItemCard(
                    medication = medication,
                    easyModeEnabled = easyModeEnabled,
                    isExpired = true,
                    onEdit = { editorState = MedicationEditorState.fromEntity(medication) },
                    onDeactivate = { deleteTarget = medication },
                )
            }
        }
    }

    editorState?.let { state ->
        MedicationEditorDialog(
            state = state,
            onDismiss = { editorState = null },
            onSave = { draft ->
                val schedule = draft.toPersistedSchedule()
                viewModel.saveMedication(
                    editingId = draft.id,
                    name = draft.name,
                    dose = draft.dose,
                    scheduleTime = draft.scheduleTime,
                    color = draft.color,
                    shape = draft.shape,
                    instructions = draft.instructions,
                    imageUri = draft.imageUri,
                    scheduleType = schedule.scheduleType,
                    startDate = schedule.startDate,
                    endDate = schedule.endDate,
                    daysOfWeek = schedule.daysOfWeek,
                    specificDates = schedule.specificDates,
                ) { imageCopyFailed ->
                    if (imageCopyFailed) {
                        editorState = draft.copy(imageUri = null)
                    } else {
                        editorState = null
                        pendingCameraUri = null
                    }
                }
            },
            onStateChange = { editorState = it },
            onPickGallery = {
                pickVisualMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhoto = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    runCatching {
                        val cameraUri = viewModel.createCameraCaptureUri(state.id)
                        pendingCameraUri = cameraUri
                        takePictureLauncher.launch(cameraUri)
                    }.onFailure {
                        viewModel.onCameraOpenFailed()
                    }
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            },
            onRemoveImage = {
                editorState = state.copy(imageUri = null)
            },
            onChooseTime = {
                showMedicationTimePicker(
                    context = context,
                    currentValue = state.scheduleTime,
                    onSelected = { selected ->
                        editorState = state.copy(scheduleTime = selected)
                    },
                )
            },
        )
    }

    deleteTarget?.let { medication ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.meds_deactivate_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.meds_deactivate_desc),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
            },
            confirmButton = {
                AppButton(
                    label = stringResource(R.string.meds_deactivate_btn_confirm),
                    onClick = {
                        viewModel.deleteMedication(medication.id)
                        deleteTarget = null
                    },
                )
            },
            dismissButton = {
                AppButton(
                    label = stringResource(R.string.meds_deactivate_btn_no),
                    onClick = { deleteTarget = null },
                )
            },
        )
    }
}

@Composable
private fun MedicationItemCard(
    medication: MedicationEntity,
    easyModeEnabled: Boolean,
    isExpired: Boolean = false,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
) {
    AppCard(
        containerColor = if (isExpired) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MedicationImagePreview(
                imageUri = medication.imageUri,
                label = medication.name,
                size = 80.dp,
                alpha = if (isExpired) 0.6f else 1f,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (isExpired) {
                    Text(
                        text = stringResource(R.string.meds_label_finished),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = medication.name,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpired) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${medication.dose} a las ${formatScheduleTime(medication.scheduleTime)}",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                Text(
                    text = medication.treatmentSummary(),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                if (!easyModeEnabled) {
                    medication.color?.let {
                        Text(stringResource(R.string.meds_label_color, it), fontSize = 18.sp, lineHeight = 24.sp)
                    }
                    medication.shape?.let {
                        Text(stringResource(R.string.meds_label_shape, it), fontSize = 18.sp, lineHeight = 24.sp)
                    }
                    medication.instructions?.let {
                        Text(it, fontSize = 18.sp, lineHeight = 24.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(
                label = stringResource(R.string.meds_btn_edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                minHeight = 60.dp,
                textSize = 22.sp,
            )
            AppButton(
                label = if (isExpired) stringResource(R.string.meds_btn_delete) else stringResource(R.string.meds_btn_deactivate),
                onClick = onDeactivate,
                modifier = Modifier.weight(1f),
                minHeight = 60.dp,
                textSize = 22.sp,
            )
        }
    }
}

private fun showMedicationTimePicker(
    context: Context,
    currentValue: String,
    onSelected: (String) -> Unit,
) {
    val normalized = normalizeTimeTo24h(currentValue) ?: "08:00"
    val (hour, minute) = normalized.split(":").map { it.toInt() }
    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val selected = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            onSelected(formatTimeForDisplay(selected))
        },
        hour,
        minute,
        false,
    ).show()
}

private data class PersistedMedicationSchedule(
    val scheduleType: ScheduleType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val daysOfWeek: Set<Int>,
    val specificDates: Set<LocalDate>,
)

private data class MedicationEditorState(
    val id: String,
    val name: String = "",
    val dose: String = "",
    val scheduleTime: String = "",
    val color: String = "",
    val shape: String = "",
    val instructions: String = "",
    val imageUri: String? = null,
    val treatmentOption: TreatmentOption = TreatmentOption.ALWAYS,
    val quickDays: Int = 7,
    val rangeStartDate: LocalDate = LocalDate.now(),
    val rangeEndDate: LocalDate? = null,
    val specificDaysMode: SpecificDaysMode = SpecificDaysMode.WEEKLY,
    val selectedDaysOfWeek: Set<Int> = emptySet(),
    val weeklyStartDate: LocalDate = LocalDate.now(),
    val weeklyEndDate: LocalDate? = null,
    val specificDates: Set<LocalDate> = emptySet(),
) {
    fun toPersistedSchedule(today: LocalDate = LocalDate.now()): PersistedMedicationSchedule {
        return when (treatmentOption) {
            TreatmentOption.ALWAYS -> PersistedMedicationSchedule(
                scheduleType = ScheduleType.ALWAYS,
                startDate = rangeStartDate,
                endDate = null,
                daysOfWeek = MedicationScheduleDefaults.allDaysOfWeek,
                specificDates = emptySet(),
            )
            TreatmentOption.FEW_DAYS -> {
                val endDate = today.plusDays((quickDays - 1).toLong())
                PersistedMedicationSchedule(
                    scheduleType = ScheduleType.DATE_RANGE,
                    startDate = today,
                    endDate = endDate,
                    daysOfWeek = MedicationScheduleDefaults.allDaysOfWeek,
                    specificDates = emptySet(),
                )
            }
            TreatmentOption.DATE_RANGE -> PersistedMedicationSchedule(
                scheduleType = ScheduleType.DATE_RANGE,
                startDate = rangeStartDate,
                endDate = rangeEndDate,
                daysOfWeek = MedicationScheduleDefaults.allDaysOfWeek,
                specificDates = emptySet(),
            )
            TreatmentOption.SPECIFIC_DAYS -> {
                if (specificDaysMode == SpecificDaysMode.EXACT_DATES) {
                    val sortedDates = specificDates.sorted()
                    PersistedMedicationSchedule(
                        scheduleType = ScheduleType.SPECIFIC_DATES,
                        startDate = sortedDates.firstOrNull() ?: today,
                        endDate = sortedDates.lastOrNull(),
                        daysOfWeek = MedicationScheduleDefaults.allDaysOfWeek,
                        specificDates = specificDates,
                    )
                } else {
                    PersistedMedicationSchedule(
                        scheduleType = ScheduleType.WEEKLY_DAYS,
                        startDate = weeklyStartDate,
                        endDate = weeklyEndDate,
                        daysOfWeek = selectedDaysOfWeek,
                        specificDates = emptySet(),
                    )
                }
            }
        }
    }

    companion object {
        fun fromEntity(entity: MedicationEntity): MedicationEditorState {
            val schedule = entity.toMedicationSchedule()
            return when (schedule.scheduleType) {
                ScheduleType.ALWAYS -> MedicationEditorState(
                    id = entity.id,
                    name = entity.name,
                    dose = entity.dose,
                    scheduleTime = formatScheduleTime(entity.scheduleTime),
                    color = entity.color.orEmpty(),
                    shape = entity.shape.orEmpty(),
                    instructions = entity.instructions.orEmpty(),
                    imageUri = entity.imageUri,
                    treatmentOption = TreatmentOption.ALWAYS,
                    rangeStartDate = schedule.startDate,
                )
                ScheduleType.DATE_RANGE -> MedicationEditorState(
                    id = entity.id,
                    name = entity.name,
                    dose = entity.dose,
                    scheduleTime = formatScheduleTime(entity.scheduleTime),
                    color = entity.color.orEmpty(),
                    shape = entity.shape.orEmpty(),
                    instructions = entity.instructions.orEmpty(),
                    imageUri = entity.imageUri,
                    treatmentOption = TreatmentOption.DATE_RANGE,
                    rangeStartDate = schedule.startDate,
                    rangeEndDate = schedule.endDate,
                )
                ScheduleType.WEEKLY_DAYS -> MedicationEditorState(
                    id = entity.id,
                    name = entity.name,
                    dose = entity.dose,
                    scheduleTime = formatScheduleTime(entity.scheduleTime),
                    color = entity.color.orEmpty(),
                    shape = entity.shape.orEmpty(),
                    instructions = entity.instructions.orEmpty(),
                    imageUri = entity.imageUri,
                    treatmentOption = TreatmentOption.SPECIFIC_DAYS,
                    specificDaysMode = SpecificDaysMode.WEEKLY,
                    selectedDaysOfWeek = schedule.daysOfWeek,
                    weeklyStartDate = schedule.startDate,
                    weeklyEndDate = schedule.endDate,
                )
                ScheduleType.SPECIFIC_DATES -> MedicationEditorState(
                    id = entity.id,
                    name = entity.name,
                    dose = entity.dose,
                    scheduleTime = formatScheduleTime(entity.scheduleTime),
                    color = entity.color.orEmpty(),
                    shape = entity.shape.orEmpty(),
                    instructions = entity.instructions.orEmpty(),
                    imageUri = entity.imageUri,
                    treatmentOption = TreatmentOption.SPECIFIC_DAYS,
                    specificDaysMode = SpecificDaysMode.EXACT_DATES,
                    specificDates = entity.specificDates.toSet(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationEditorDialog(
    state: MedicationEditorState,
    onDismiss: () -> Unit,
    onSave: (MedicationEditorState) -> Unit,
    onStateChange: (MedicationEditorState) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onChooseTime: () -> Unit,
) {
    var showWeeklyStartPicker by remember { mutableStateOf(false) }
    var showWeeklyEndPicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }
    var showExactDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (state.name.isBlank() && state.dose.isBlank()) stringResource(R.string.meds_editor_title_add) else stringResource(R.string.meds_editor_title_edit),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.meds_editor_section_basic),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                MedicationField(stringResource(R.string.meds_editor_label_name), state.name) {
                    onStateChange(state.copy(name = it))
                }
                MedicationField(stringResource(R.string.meds_editor_label_dose), state.dose) {
                    onStateChange(state.copy(dose = it))
                }
                MedicationField(
                    label = stringResource(R.string.meds_editor_label_time),
                    value = state.scheduleTime,
                    keyboardType = KeyboardType.Text,
                ) {
                    onStateChange(state.copy(scheduleTime = it))
                }
                Text(
                    text = stringResource(R.string.meds_editor_time_help),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                AppButton(
                    label = stringResource(R.string.meds_editor_btn_choose_time),
                    onClick = onChooseTime,
                )
                state.scheduleTime.takeIf(String::isNotBlank)?.let { selectedTime ->
                    Text(
                        text = stringResource(R.string.meds_editor_time_selected, parseTimeLabel(selectedTime)),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }

                Text(
                    text = stringResource(R.string.meds_editor_section_treatment),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.meds_editor_treatment_help),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TreatmentOptionButton(
                        label = stringResource(R.string.meds_editor_opt_always),
                        selected = state.treatmentOption == TreatmentOption.ALWAYS,
                    ) {
                        onStateChange(state.copy(treatmentOption = TreatmentOption.ALWAYS))
                    }
                    TreatmentOptionButton(
                        label = stringResource(R.string.meds_editor_opt_few_days),
                        selected = state.treatmentOption == TreatmentOption.FEW_DAYS,
                    ) {
                        onStateChange(state.copy(treatmentOption = TreatmentOption.FEW_DAYS))
                    }
                    TreatmentOptionButton(
                        label = stringResource(R.string.meds_editor_opt_dates),
                        selected = state.treatmentOption == TreatmentOption.DATE_RANGE,
                    ) {
                        onStateChange(state.copy(treatmentOption = TreatmentOption.DATE_RANGE))
                    }
                    TreatmentOptionButton(
                        label = stringResource(R.string.meds_editor_opt_specific),
                        selected = state.treatmentOption == TreatmentOption.SPECIFIC_DAYS,
                    ) {
                        onStateChange(state.copy(treatmentOption = TreatmentOption.SPECIFIC_DAYS))
                    }
                }

                when (state.treatmentOption) {
                    TreatmentOption.ALWAYS -> {
                        Text(stringResource(R.string.meds_editor_always_desc), fontSize = 18.sp, lineHeight = 24.sp)
                    }
                    TreatmentOption.FEW_DAYS -> {
                        Text(stringResource(R.string.meds_editor_few_days_desc), fontSize = 18.sp, lineHeight = 24.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickDayOptions.forEach { days ->
                                TreatmentOptionButton(
                                    label = stringResource(R.string.meds_editor_day_unit, days),
                                    selected = state.quickDays == days,
                                ) {
                                    onStateChange(state.copy(quickDays = days))
                                }
                            }
                        }
                    }
                    TreatmentOption.DATE_RANGE -> {
                        Text(stringResource(R.string.meds_editor_range_desc), fontSize = 18.sp, lineHeight = 24.sp)
                        AppButton(
                            label = stringResource(R.string.meds_editor_label_from, state.rangeStartDate.toDisplayDate(java.util.Locale.getDefault())),
                            onClick = { showRangeStartPicker = true },
                        )
                        AppButton(
                            label = stringResource(R.string.meds_editor_label_until, state.rangeEndDate?.toDisplayDate(java.util.Locale.getDefault()) ?: stringResource(R.string.meds_editor_label_until_empty)),
                            onClick = { showRangeEndPicker = true },
                        )
                        Text(
                            text = stringResource(R.string.meds_editor_range_summary, state.rangeStartDate.toDisplayDate(java.util.Locale.getDefault()), state.rangeEndDate?.toDisplayDate(java.util.Locale.getDefault()) ?: "-"),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                        )
                    }
                    TreatmentOption.SPECIFIC_DAYS -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TreatmentOptionButton(
                                label = stringResource(R.string.meds_editor_specific_weekly),
                                selected = state.specificDaysMode == SpecificDaysMode.WEEKLY,
                            ) {
                                onStateChange(state.copy(specificDaysMode = SpecificDaysMode.WEEKLY))
                            }
                            TreatmentOptionButton(
                                label = stringResource(R.string.meds_editor_specific_exact),
                                selected = state.specificDaysMode == SpecificDaysMode.EXACT_DATES,
                            ) {
                                onStateChange(state.copy(specificDaysMode = SpecificDaysMode.EXACT_DATES))
                            }
                        }
                        if (state.specificDaysMode == SpecificDaysMode.WEEKLY) {
                            Text(stringResource(R.string.meds_editor_weekly_desc), fontSize = 18.sp, lineHeight = 24.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MedicationScheduleDefaults.allDaysOfWeek.forEach { day ->
                                    TreatmentOptionButton(
                                        label = dayNumberToLabel(day),
                                        selected = day in state.selectedDaysOfWeek,
                                    ) {
                                        val updated = state.selectedDaysOfWeek.toMutableSet().apply {
                                            if (!add(day)) remove(day)
                                        }
                                        onStateChange(state.copy(selectedDaysOfWeek = updated))
                                    }
                                }
                            }
                            Text(stringResource(R.string.meds_editor_weekly_range_help), fontSize = 18.sp, lineHeight = 24.sp)
                            AppButton(
                                label = stringResource(R.string.meds_editor_label_from, state.weeklyStartDate.toDisplayDate(java.util.Locale.getDefault())),
                                onClick = { showWeeklyStartPicker = true },
                            )
                            AppButton(
                                label = stringResource(R.string.meds_editor_label_until, state.weeklyEndDate?.toDisplayDate(java.util.Locale.getDefault()) ?: stringResource(R.string.meds_editor_until_none)),
                                onClick = { showWeeklyEndPicker = true },
                            )
                        } else {
                            Text(stringResource(R.string.meds_editor_exact_desc), fontSize = 18.sp, lineHeight = 24.sp)
                            AppButton(
                                label = stringResource(R.string.meds_editor_btn_add_date),
                                onClick = { showExactDatePicker = true },
                            )
                            if (state.specificDates.isEmpty()) {
                                Text(stringResource(R.string.meds_editor_exact_empty), fontSize = 18.sp, lineHeight = 24.sp)
                            } else {
                                state.specificDates.sorted().forEach { date ->
                                    AppButton(
                                        label = stringResource(R.string.meds_editor_btn_remove_date, date.toDisplayDate(java.util.Locale.getDefault())),
                                        onClick = {
                                            onStateChange(state.copy(specificDates = state.specificDates - date))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.meds_editor_summary_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                buildScheduleSummary(
                    state,
                    java.util.Locale.getDefault(),
                    stringResource(R.string.schedule_every_day),
                    stringResource(R.string.schedule_date_range),
                    stringResource(R.string.schedule_date_from),
                    stringResource(R.string.schedule_exact_dates),
                    stringResource(R.string.schedule_no_time),
                    stringResource(R.string.schedule_choose_days),
                    stringResource(R.string.schedule_days_at_time),
                    stringResource(R.string.meds_editor_exact_empty)
                ).forEach { line ->
                    Text(text = line, fontSize = 18.sp, lineHeight = 24.sp)
                }
                Text(
                    text = stringResource(R.string.meds_editor_save_warning),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(R.string.meds_editor_section_optional),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                MedicationField(stringResource(R.string.meds_editor_label_color), state.color) {
                    onStateChange(state.copy(color = it))
                }
                MedicationField(stringResource(R.string.meds_editor_label_shape), state.shape) {
                    onStateChange(state.copy(shape = it))
                }
                MedicationField(stringResource(R.string.meds_editor_label_instructions), state.instructions, singleLine = false) {
                    onStateChange(state.copy(instructions = it))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.meds_editor_section_image),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.meds_editor_image_help),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                MedicationImagePreview(
                    imageUri = state.imageUri,
                    label = state.name.ifBlank { stringResource(R.string.meds_editor_no_image) },
                    size = 160.dp,
                )
                if (state.imageUri.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.meds_editor_no_image),
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    )
                }
                AppButton(
                    label = if (state.imageUri.isNullOrBlank()) stringResource(R.string.meds_editor_btn_take_photo) else stringResource(R.string.meds_editor_btn_change_photo),
                    onClick = onTakePhoto,
                )
                AppButton(
                    label = if (state.imageUri.isNullOrBlank()) stringResource(R.string.meds_editor_btn_pick_gallery) else stringResource(R.string.meds_editor_btn_change_gallery),
                    onClick = onPickGallery,
                )
                if (!state.imageUri.isNullOrBlank()) {
                    AppButton(
                        label = stringResource(R.string.meds_editor_btn_remove_image),
                        onClick = onRemoveImage,
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                label = stringResource(R.string.meds_editor_btn_save),
                onClick = { onSave(state) },
            )
        },
        dismissButton = {
            AppButton(
                label = stringResource(R.string.btn_cancel),
                onClick = onDismiss,
            )
        },
    )

    if (showWeeklyStartPicker) {
        SingleDatePickerDialog(
            title = stringResource(R.string.meds_editor_label_from, "").substringBefore(":"),
            initialDate = state.weeklyStartDate,
            onDismiss = { showWeeklyStartPicker = false },
            onConfirm = { selected ->
                showWeeklyStartPicker = false
                onStateChange(state.copy(weeklyStartDate = selected))
            },
        )
    }
    if (showRangeStartPicker) {
        SingleDatePickerDialog(
            title = stringResource(R.string.meds_editor_label_from, "").substringBefore(":"),
            initialDate = state.rangeStartDate,
            onDismiss = { showRangeStartPicker = false },
            onConfirm = { selected ->
                showRangeStartPicker = false
                onStateChange(
                    state.copy(
                        rangeStartDate = selected,
                        rangeEndDate = state.rangeEndDate?.takeUnless { it.isBefore(selected) },
                    ),
                )
            },
        )
    }
    if (showRangeEndPicker) {
        SingleDatePickerDialog(
            title = stringResource(R.string.meds_editor_label_until, "").substringBefore(":"),
            initialDate = state.rangeEndDate ?: state.rangeStartDate,
            onDismiss = { showRangeEndPicker = false },
            onConfirm = { selected ->
                showRangeEndPicker = false
                onStateChange(state.copy(rangeEndDate = selected))
            },
        )
    }
    if (showWeeklyEndPicker) {
        SingleDatePickerDialog(
            title = stringResource(R.string.meds_editor_label_until, "").substringBefore(":"),
            initialDate = state.weeklyEndDate ?: state.weeklyStartDate,
            onDismiss = { showWeeklyEndPicker = false },
            onConfirm = { selected ->
                showWeeklyEndPicker = false
                onStateChange(state.copy(weeklyEndDate = selected))
            },
            allowClear = true,
            onClear = {
                showWeeklyEndPicker = false
                onStateChange(state.copy(weeklyEndDate = null))
            },
        )
    }
    if (showExactDatePicker) {
        SingleDatePickerDialog(
            title = stringResource(R.string.meds_editor_btn_add_date),
            initialDate = state.specificDates.minOrNull() ?: LocalDate.now(),
            onDismiss = { showExactDatePicker = false },
            onConfirm = { selected ->
                showExactDatePicker = false
                onStateChange(state.copy(specificDates = state.specificDates + selected))
            },
        )
    }
}

@Composable
private fun TreatmentOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(52.dp),
        enabled = !selected,
    ) {
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    allowClear: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.toLocalDate()?.let(onConfirm)
                },
            ) {
                Text(stringResource(R.string.btn_accept))
            }
        },
        dismissButton = {
            Row {
                if (allowClear && onClear != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.date_picker_btn_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        },
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            DatePicker(state = pickerState)
        }
    }
}

private fun buildScheduleSummary(
    state: MedicationEditorState,
    locale: Locale,
    everyDayPattern: String,
    rangePattern: String,
    fromPattern: String,
    exactDatesPattern: String,
    noTimeLabel: String,
    chooseDaysLabel: String,
    daysAtTimePattern: String,
    exactEmptyLabel: String,
): List<String> {
    val timeText = state.scheduleTime.takeIf { it.isNotBlank() }?.let(::parseTimeLabel) ?: noTimeLabel
    return when (state.treatmentOption) {
        TreatmentOption.ALWAYS -> listOf(everyDayPattern.format(timeText))
        TreatmentOption.FEW_DAYS -> {
            val start = LocalDate.now()
            val end = start.plusDays((state.quickDays - 1).toLong())
            listOf(
                everyDayPattern.format(timeText),
                rangePattern.format(start.toShortDate(locale), end.toShortDate(locale)),
            )
        }
        TreatmentOption.DATE_RANGE -> listOf(
            everyDayPattern.format(timeText),
            rangePattern.format(state.rangeStartDate.toShortDate(locale), state.rangeEndDate?.toShortDate(locale) ?: "-"),
        )
        TreatmentOption.SPECIFIC_DAYS -> {
            if (state.specificDaysMode == SpecificDaysMode.EXACT_DATES) {
                val dates = state.specificDates.sorted().joinToString(", ") { it.toShortDate(locale) }
                listOf(
                    exactDatesPattern.format(timeText),
                    if (dates.isBlank()) exactEmptyLabel else dates,
                )
            } else {
                val daysText = state.selectedDaysOfWeek.sorted().joinToString(", ") { dayNumberToLabel(it).lowercase(locale) }
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                val rangeText = if (state.weeklyEndDate != null) {
                    rangePattern.format(state.weeklyStartDate.toShortDate(locale), state.weeklyEndDate.toShortDate(locale))
                } else {
                    fromPattern.format(state.weeklyStartDate.toShortDate(locale))
                }
                listOf(
                    daysAtTimePattern.format(if (daysText.isBlank()) chooseDaysLabel else daysText, timeText),
                    rangeText,
                )
            }
        }
    }
}

private fun parseTimeLabel(value: String): String {
    return normalizeTimeTo24h(value)?.let(::formatTimeForDisplay) ?: value
}

@Composable
private fun MedicationField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
    )
}

private fun LocalDate.toShortDate(locale: Locale): String = format(displayDateFormatter(locale))

private fun LocalDate.toDisplayDate(locale: Locale): String = format(displayDateFormatter(locale))

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
