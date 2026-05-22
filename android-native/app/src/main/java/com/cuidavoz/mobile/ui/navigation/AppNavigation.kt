package com.cuidavoz.mobile.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cuidavoz.mobile.CuidaVozAppContainer
import com.cuidavoz.mobile.ui.screens.HelpScreen
import com.cuidavoz.mobile.ui.screens.HistoryScreen
import com.cuidavoz.mobile.ui.screens.MeasurePressureScreen
import com.cuidavoz.mobile.ui.screens.MedicationsScreen
import com.cuidavoz.mobile.ui.screens.PatientHomeScreen
import com.cuidavoz.mobile.ui.screens.PressureSavedData
import com.cuidavoz.mobile.ui.screens.PressureSavedScreen
import com.cuidavoz.mobile.ui.screens.ReportsScreen
import com.cuidavoz.mobile.ui.screens.SettingsScreen
import com.cuidavoz.mobile.ui.screens.caregiver.CaregiverDashboardScreen
import com.cuidavoz.mobile.ui.screens.caregiver.LinkCaregiverScreen
import com.cuidavoz.mobile.ui.viewmodel.BackupViewModel
import com.cuidavoz.mobile.ui.viewmodel.CaregiverDashboardViewModel
import com.cuidavoz.mobile.ui.viewmodel.CuidaVozViewModelFactory
import com.cuidavoz.mobile.ui.viewmodel.HistoryViewModel
import com.cuidavoz.mobile.ui.viewmodel.HomeViewModel
import com.cuidavoz.mobile.ui.viewmodel.MedicationsViewModel
import com.cuidavoz.mobile.ui.viewmodel.ReportsViewModel
import com.cuidavoz.mobile.ui.viewmodel.SettingsViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.formatTimeForVoice

private const val PATIENT_HOME_ROUTE = "patient_home"
private const val MEASURE_PRESSURE_ROUTE = "measure_pressure"
private const val PRESSURE_SAVED_ROUTE = "pressure_saved"
private const val HELP_ROUTE = "help"
private const val CAREGIVER_HOME_ROUTE = "caregiver_home"
private const val LINK_CAREGIVER_ROUTE = "link_caregiver"
private const val MEDICATIONS_ROUTE = "medications"
private const val RECORDS_ROUTE = "records"
private const val REPORTS_ROUTE = "reports"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun AppNavigation(
    appContainer: CuidaVozAppContainer,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val reminderPrompt by appContainer.reminderLaunchState.prompt.collectAsStateWithLifecycle()
    var currentMode by remember { mutableStateOf(UserMode.PATIENT) }
    var lastGuidedRoute by remember { mutableStateOf<String?>(null) }
    var lastGuidedAt by remember { mutableLongStateOf(0L) }
    var lastSavedPressure by remember { mutableStateOf<PressureSavedData?>(null) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                HomeViewModel(
                    patientRepository = appContainer.patientRepository,
                    familyContactRepository = appContainer.familyContactRepository,
                    dailyStatusRepository = appContainer.dailyStatusRepository,
                    pressureRepository = appContainer.pressureRepository,
                    reminderLaunchState = appContainer.reminderLaunchState,
                    reminderScheduler = appContainer.reminderScheduler,
                )
            }
        },
    )
    val medicationsViewModel: MedicationsViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                MedicationsViewModel(
                    medicationRepository = appContainer.medicationRepository,
                    reminderScheduler = appContainer.reminderScheduler,
                    medicationImageStorage = appContainer.medicationImageStorage,
                )
            }
        },
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                HistoryViewModel(
                    pressureRepository = appContainer.pressureRepository,
                    medicationRepository = appContainer.medicationRepository,
                    medicationLogRepository = appContainer.medicationLogRepository,
                )
            }
        },
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                SettingsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    familyContactRepository = appContainer.familyContactRepository,
                    reminderScheduler = appContainer.reminderScheduler,
                )
            }
        },
    )
    val voiceAssistantViewModel: VoiceAssistantViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                VoiceAssistantViewModel(
                    patientRepository = appContainer.patientRepository,
                    familyContactRepository = appContainer.familyContactRepository,
                    dailyStatusRepository = appContainer.dailyStatusRepository,
                    pressureRepository = appContainer.pressureRepository,
                    settingsRepository = appContainer.settingsRepository,
                    reminderScheduler = appContainer.reminderScheduler,
                    reminderLaunchState = appContainer.reminderLaunchState,
                    textToSpeechManager = appContainer.textToSpeechManager,
                    speechRecognitionManager = appContainer.speechRecognitionManager,
                )
            }
        },
    )
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                ReportsViewModel(
                    medicalReportRepository = appContainer.medicalReportRepository,
                    pdfReportGenerator = appContainer.pdfReportGenerator,
                )
            }
        },
    )
    val backupViewModel: BackupViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                BackupViewModel(
                    backupRepository = appContainer.backupRepository,
                    reminderScheduler = appContainer.reminderScheduler,
                )
            }
        },
    )
    val caregiverDashboardViewModel: CaregiverDashboardViewModel = viewModel(
        factory = remember(appContainer) {
            CuidaVozViewModelFactory {
                CaregiverDashboardViewModel(
                    patientRepository = appContainer.patientRepository,
                    pressureRepository = appContainer.pressureRepository,
                    medicationRepository = appContainer.medicationRepository,
                    medicationLogRepository = appContainer.medicationLogRepository,
                    syncContextRepository = appContainer.syncContextRepository,
                    firebaseSyncManager = appContainer.firebaseSyncManager,
                )
            }
        },
    )

    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val caregiverUiState by caregiverDashboardViewModel.uiState.collectAsStateWithLifecycle()

    fun speakScreen(route: String?) {
        if (route == null) return
        val text = when (route) {
            PATIENT_HOME_ROUTE -> buildPatientHomeAudio(homeUiState)
            MEASURE_PRESSURE_ROUTE ->
                "Mide tu presión cuando puedas. Escribe la presión alta, la presión baja y, si quieres, tu pulso."
            HELP_ROUTE ->
                if (homeUiState.contact == null) {
                    "Todavía no hay un contacto de ayuda. Puedes abrir la zona del familiar o cuidador."
                } else {
                    "Aquí puedes llamar o enviar un mensaje a ${homeUiState.contact?.fullName.orEmpty()}."
                }
            CAREGIVER_HOME_ROUTE ->
                "Área para familiar o cuidador. Aquí puedes abrir medicamentos, registros, reporte médico, contacto familiar, ajustes y guardar copia."
            MEDICATIONS_ROUTE -> "Aquí puedes agregar o cambiar pastillas."
            RECORDS_ROUTE -> "Aquí puedes revisar presión y tomas."
            REPORTS_ROUTE -> "Aquí puedes crear el reporte para el médico."
            SETTINGS_ROUTE -> "Aquí puedes cambiar el contacto, recordatorios, rangos, voz y copia de seguridad."
            else -> null
        } ?: return
        appContainer.textToSpeechManager.speak(text)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        LaunchedEffect(reminderPrompt?.nonce) {
            if (reminderPrompt != null && currentRoute != PATIENT_HOME_ROUTE) {
                currentMode = UserMode.PATIENT
                navController.navigate(PATIENT_HOME_ROUTE) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = false
                    }
                    launchSingleTop = true
                }
            }
        }
        LaunchedEffect(currentRoute, settingsUiState.voiceGuidanceEnabled, homeUiState.greeting) {
            val route = currentRoute ?: return@LaunchedEffect
            if (!settingsUiState.voiceGuidanceEnabled) return@LaunchedEffect
            val now = System.currentTimeMillis()
            if (route == lastGuidedRoute || now - lastGuidedAt < 1_500L) return@LaunchedEffect
            lastGuidedRoute = route
            lastGuidedAt = now
            speakScreen(route)
        }

        NavHost(
            navController = navController,
            startDestination = PATIENT_HOME_ROUTE,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(PATIENT_HOME_ROUTE) {
                currentMode = UserMode.PATIENT
                PatientHomeScreen(
                    innerPadding = innerPadding,
                    uiState = homeUiState,
                    voiceUiState = voiceUiState,
                    easyModeEnabled = settingsUiState.easyModeEnabled,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onOpenMeasurePressure = { navController.navigate(MEASURE_PRESSURE_ROUTE) },
                    onOpenHelp = { navController.navigate(HELP_ROUTE) },
                    onOpenCaregiver = {
                        currentMode = UserMode.CAREGIVER
                        navController.navigate(CAREGIVER_HOME_ROUTE)
                    },
                    onSpeakHome = { speakScreen(PATIENT_HOME_ROUTE) },
                    onConfirmMedicationTaken = homeViewModel::markNextMedicationGroupTaken,
                    onRemindLater = homeViewModel::dismissReminderPrompt,
                    onDismissMessage = homeViewModel::dismissMessage,
                )
            }
            composable(MEASURE_PRESSURE_ROUTE) {
                MeasurePressureScreen(
                    innerPadding = innerPadding,
                    viewModel = homeViewModel,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onBack = { navController.popBackStack() },
                    onSpeak = { speakScreen(MEASURE_PRESSURE_ROUTE) },
                    onSaved = { summary ->
                        lastSavedPressure = summary
                        navController.navigate(PRESSURE_SAVED_ROUTE)
                    },
                )
            }
            composable(PRESSURE_SAVED_ROUTE) {
                val summary = lastSavedPressure
                if (summary == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack(PATIENT_HOME_ROUTE, false)
                    }
                } else {
                    PressureSavedScreen(
                        innerPadding = innerPadding,
                        summary = summary,
                        onDone = {
                            navController.popBackStack(PATIENT_HOME_ROUTE, false)
                        },
                    )
                }
            }
            composable(HELP_ROUTE) {
                HelpScreen(
                    innerPadding = innerPadding,
                    contact = homeUiState.contact,
                    onBack = { navController.popBackStack() },
                    onOpenCaregiverArea = {
                        currentMode = UserMode.CAREGIVER
                        navController.navigate(CAREGIVER_HOME_ROUTE)
                    },
                    onSpeak = { speakScreen(HELP_ROUTE) },
                )
            }
            composable(CAREGIVER_HOME_ROUTE) {
                currentMode = UserMode.CAREGIVER
                CaregiverDashboardScreen(
                    innerPadding = innerPadding,
                    uiState = caregiverUiState,
                    onDismissMessage = caregiverDashboardViewModel::dismissMessage,
                    onOpenLinking = { navController.navigate(LINK_CAREGIVER_ROUTE) },
                    onOpenMedications = { navController.navigate(MEDICATIONS_ROUTE) },
                    onOpenRecords = { navController.navigate(RECORDS_ROUTE) },
                    onOpenReports = { navController.navigate(REPORTS_ROUTE) },
                    onOpenFamilyContact = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenBackup = { navController.navigate(SETTINGS_ROUTE) },
                    onRetrySync = caregiverDashboardViewModel::retrySync,
                    onToggleSync = caregiverDashboardViewModel::setSyncEnabled,
                    onCallPatient = { navController.navigate(HELP_ROUTE) },
                    onBack = {
                        currentMode = UserMode.PATIENT
                        navController.popBackStack(PATIENT_HOME_ROUTE, false)
                    },
                    onReturnPatient = {
                        currentMode = UserMode.PATIENT
                        navController.popBackStack(PATIENT_HOME_ROUTE, false)
                    },
                )
            }
            composable(LINK_CAREGIVER_ROUTE) {
                LinkCaregiverScreen(
                    innerPadding = innerPadding,
                    uiState = caregiverUiState,
                    onDismissMessage = caregiverDashboardViewModel::dismissMessage,
                    onBack = { navController.popBackStack() },
                    onCreateCode = caregiverDashboardViewModel::createLinkCode,
                    onCodeChanged = caregiverDashboardViewModel::updateLinkCodeInput,
                    onLinkWithCode = caregiverDashboardViewModel::linkWithCode,
                )
            }
            composable(MEDICATIONS_ROUTE) {
                MedicationsScreen(
                    innerPadding = innerPadding,
                    viewModel = medicationsViewModel,
                    easyModeEnabled = settingsUiState.easyModeEnabled,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(MEDICATIONS_ROUTE) },
                )
            }
            composable(RECORDS_ROUTE) {
                HistoryScreen(
                    innerPadding = innerPadding,
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(RECORDS_ROUTE) },
                )
            }
            composable(REPORTS_ROUTE) {
                ReportsScreen(
                    innerPadding = innerPadding,
                    viewModel = reportsViewModel,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(REPORTS_ROUTE) },
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    innerPadding = innerPadding,
                    viewModel = settingsViewModel,
                    backupViewModel = backupViewModel,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenHome = {
                        currentMode = UserMode.PATIENT
                        navController.popBackStack(PATIENT_HOME_ROUTE, false)
                    },
                    onOpenReports = { navController.navigate(REPORTS_ROUTE) },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(SETTINGS_ROUTE) },
                )
            }
        }
    }
}

private fun buildPatientHomeAudio(state: com.cuidavoz.mobile.ui.viewmodel.HomeScreenState): String {
    val patientName = state.patientFirstName
    val medications = state.nextGroupMedications
    if (medications.isEmpty()) {
        return "Hola $patientName. Hoy no tienes pastillas pendientes. Puedes tocar Medir presión, Pedir ayuda o Hablar."
    }
    val actionText = if (medications.size > 1) "Ya tomé todas" else "Ya tomé"
    return if (medications.size == 1) {
        val medication = medications.first()
        "Hola $patientName. Tu próxima pastilla es ${medication.name}, ${medication.dose}, a las " +
            "${formatTimeForVoice(medication.scheduleTime)}. " +
            "${medication.instructions?.takeIf { it.isNotBlank() } ?: ""} " +
            "Puedes tocar $actionText, Medir presión o Pedir ayuda."
    } else {
        val names = medications.joinToString(", ") { it.name }
        "Hola $patientName. A las ${formatScheduleTime(medications.first().scheduleTime)} debes tomar ${medications.size} pastillas: $names. " +
            "Puedes tocar $actionText, Medir presión o Pedir ayuda."
    }
}
