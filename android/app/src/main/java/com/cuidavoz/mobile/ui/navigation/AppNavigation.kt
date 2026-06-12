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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cuidavoz.mobile.di.NavigationEntryPoint
import com.cuidavoz.mobile.ui.screens.BackupScreen
import com.cuidavoz.mobile.ui.screens.FamilyContactScreen
import com.cuidavoz.mobile.ui.screens.HelpScreen
import com.cuidavoz.mobile.ui.screens.HistoryScreen
import com.cuidavoz.mobile.ui.screens.MeasurePressureScreen
import com.cuidavoz.mobile.ui.screens.MedicationsScreen
import com.cuidavoz.mobile.ui.screens.OnboardingScreen
import com.cuidavoz.mobile.ui.screens.PatientHomeScreen
import com.cuidavoz.mobile.ui.screens.PressureSavedData
import com.cuidavoz.mobile.ui.screens.PressureSavedScreen
import com.cuidavoz.mobile.ui.screens.ReportsScreen
import com.cuidavoz.mobile.ui.screens.SettingsScreen
import com.cuidavoz.mobile.ui.screens.caregiver.CaregiverDashboardScreen
import com.cuidavoz.mobile.ui.screens.caregiver.HistoricalPressureScreen
import com.cuidavoz.mobile.ui.screens.caregiver.LinkCaregiverScreen
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.ui.viewmodel.BackupViewModel
import com.cuidavoz.mobile.ui.viewmodel.CaregiverDashboardViewModel
import com.cuidavoz.mobile.ui.viewmodel.HistoricalPressureViewModel
import com.cuidavoz.mobile.ui.viewmodel.HistoryViewModel
import com.cuidavoz.mobile.ui.viewmodel.HomeScreenState
import com.cuidavoz.mobile.ui.viewmodel.HomeViewModel
import com.cuidavoz.mobile.ui.viewmodel.MedicationsViewModel
import com.cuidavoz.mobile.ui.viewmodel.OnboardingViewModel
import com.cuidavoz.mobile.ui.viewmodel.ReportsViewModel
import com.cuidavoz.mobile.ui.viewmodel.SettingsViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.formatTimeForVoice
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = remember(backStackEntry) {
        runCatching { backStackEntry?.toRoute<ContigoDestination>() }.getOrNull()
    }
    val context = LocalContext.current
    val navigationServices = remember(context) {
        EntryPointAccessors.fromApplication(context, NavigationEntryPoint::class.java)
    }
    val reminderPrompt by navigationServices.reminderLaunchState().prompt.collectAsStateWithLifecycle()
    var currentMode by remember { mutableStateOf(UserMode.PATIENT) }
    val isCaregiverTheme = currentMode == UserMode.CAREGIVER

    ContigoTheme(isCaregiverMode = isCaregiverTheme) {
        AppNavigationContent(
            navController = navController,
            currentDestination = currentDestination,
            reminderPrompt = reminderPrompt,
            currentMode = currentMode,
            onModeChange = { currentMode = it },
            textToSpeechManager = navigationServices.textToSpeechManager(),
        )
    }
}

@Composable
private fun AppNavigationContent(
    navController: androidx.navigation.NavHostController,
    currentDestination: ContigoDestination?,
    reminderPrompt: com.cuidavoz.mobile.reminders.ReminderPrompt?,
    currentMode: UserMode,
    onModeChange: (UserMode) -> Unit,
    textToSpeechManager: com.cuidavoz.mobile.voice.TextToSpeechManager,
) {
    var lastGuidedDestination by remember { mutableStateOf<ContigoDestination?>(null) }
    var lastGuidedAt by remember { mutableLongStateOf(0L) }
    var lastSavedPressure by remember { mutableStateOf<PressureSavedData?>(null) }

    val homeViewModel: HomeViewModel = hiltViewModel()
    val medicationsViewModel: MedicationsViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val voiceAssistantViewModel: VoiceAssistantViewModel = hiltViewModel()
    val reportsViewModel: ReportsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val backupViewModel: BackupViewModel = hiltViewModel()
    val caregiverDashboardViewModel: CaregiverDashboardViewModel = hiltViewModel()
    val historicalPressureViewModel: HistoricalPressureViewModel = hiltViewModel()

    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val caregiverUiState by caregiverDashboardViewModel.uiState.collectAsStateWithLifecycle()
    val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

    fun speakScreen(destination: ContigoDestination?) {
        if (destination == null) return
        val text = destination.voiceGuideText(
            patientHomeAudio = buildPatientHomeAudio(homeUiState),
            contactName = homeUiState.contact?.fullName,
            hasContact = homeUiState.contact != null,
        ) ?: return
        textToSpeechManager.speak(text)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        LaunchedEffect(reminderPrompt?.nonce, onboardingUiState.setupCompleted) {
            if (
                reminderPrompt != null &&
                onboardingUiState.setupCompleted == true &&
                currentDestination != ContigoDestination.PatientHome
            ) {
                onModeChange(UserMode.PATIENT)
                navController.navigate(ContigoDestination.PatientHome) {
                    popUpTo<ContigoDestination.Onboarding> {
                        saveState = false
                    }
                    launchSingleTop = true
                }
            }
        }
        LaunchedEffect(currentDestination, settingsUiState.voiceGuidanceEnabled, homeUiState.greeting) {
            val destination = currentDestination ?: return@LaunchedEffect
            if (!settingsUiState.voiceGuidanceEnabled) return@LaunchedEffect
            val now = System.currentTimeMillis()
            if (destination == lastGuidedDestination || now - lastGuidedAt < 1_500L) return@LaunchedEffect
            lastGuidedDestination = destination
            lastGuidedAt = now
            speakScreen(destination)
        }

        NavHost(
            navController = navController,
            startDestination = ContigoDestination.Onboarding,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<ContigoDestination.Onboarding> {
                OnboardingScreen(
                    innerPadding = innerPadding,
                    viewModel = onboardingViewModel,
                    backupViewModel = backupViewModel,
                    onSetupFinished = { mode ->
                        onModeChange(mode)
                        val homeDestination = when (mode) {
                            UserMode.PATIENT -> ContigoDestination.PatientHome
                            UserMode.CAREGIVER -> ContigoDestination.CaregiverHome
                        }
                        navController.navigate(homeDestination) {
                            popUpTo<ContigoDestination.Onboarding> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ContigoDestination.PatientHome> {
                LaunchedEffect(Unit) { onModeChange(UserMode.PATIENT) }
                PatientHomeScreen(
                    innerPadding = innerPadding,
                    uiState = homeUiState,
                    voiceUiState = voiceUiState,
                    easyModeEnabled = settingsUiState.easyModeEnabled,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onOpenMeasurePressure = { navController.navigate(ContigoDestination.MeasurePressure) },
                    onOpenHelp = { navController.navigate(ContigoDestination.Help) },
                    onOpenCaregiver = {
                        onModeChange(UserMode.CAREGIVER)
                        navController.navigate(ContigoDestination.CaregiverHome)
                    },
                    onSpeakHome = { speakScreen(ContigoDestination.PatientHome) },
                    onRecordMedicationOutcomes = homeViewModel::recordNextMedicationGroupOutcomes,
                    onRemindLater = homeViewModel::dismissReminderPrompt,
                    onDismissMessage = homeViewModel::dismissMessage,
                )
            }
            composable<ContigoDestination.MeasurePressure> {
                MeasurePressureScreen(
                    innerPadding = innerPadding,
                    viewModel = homeViewModel,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onBack = { navController.popBackStack() },
                    onSpeak = { speakScreen(ContigoDestination.MeasurePressure) },
                    onSaved = { summary ->
                        lastSavedPressure = summary
                        navController.navigate(ContigoDestination.PressureSaved)
                    },
                )
            }
            composable<ContigoDestination.PressureSaved> {
                val summary = lastSavedPressure
                if (summary == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack(ContigoDestination.PatientHome, inclusive = false)
                    }
                } else {
                    PressureSavedScreen(
                        innerPadding = innerPadding,
                        summary = summary,
                        onDone = {
                            navController.popBackStack(ContigoDestination.PatientHome, inclusive = false)
                        },
                    )
                }
            }
            composable<ContigoDestination.Help> {
                HelpScreen(
                    innerPadding = innerPadding,
                    contact = homeUiState.contact,
                    onBack = { navController.popBackStack() },
                    onOpenCaregiverArea = {
                        onModeChange(UserMode.CAREGIVER)
                        navController.navigate(ContigoDestination.CaregiverHome)
                    },
                    onSpeak = { speakScreen(ContigoDestination.Help) },
                )
            }
            composable<ContigoDestination.CaregiverHome> {
                LaunchedEffect(Unit) { onModeChange(UserMode.CAREGIVER) }
                CaregiverDashboardScreen(
                    innerPadding = innerPadding,
                    uiState = caregiverUiState,
                    onDismissMessage = caregiverDashboardViewModel::dismissMessage,
                    onOpenLinking = { navController.navigate(ContigoDestination.LinkCaregiver) },
                    onOpenMedications = { navController.navigate(ContigoDestination.Medications) },
                    onOpenRecords = { navController.navigate(ContigoDestination.Records) },
                    onOpenHistoricalPressure = { navController.navigate(ContigoDestination.HistoricalPressure) },
                    onOpenReports = { navController.navigate(ContigoDestination.Reports) },
                    onOpenFamilyContact = { navController.navigate(ContigoDestination.FamilyContact) },
                    onOpenSettings = { navController.navigate(ContigoDestination.Settings) },
                    onOpenBackup = { navController.navigate(ContigoDestination.Backup) },
                    onRetrySync = caregiverDashboardViewModel::retrySync,
                    onToggleSync = caregiverDashboardViewModel::setSyncEnabled,
                    onCallPatient = { navController.navigate(ContigoDestination.Help) },
                    onBack = {
                        onModeChange(UserMode.PATIENT)
                        navController.popBackStack(ContigoDestination.PatientHome, inclusive = false)
                    },
                    onReturnPatient = {
                        onModeChange(UserMode.PATIENT)
                        navController.popBackStack(ContigoDestination.PatientHome, inclusive = false)
                    },
                )
            }
            composable<ContigoDestination.LinkCaregiver> {
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
            composable<ContigoDestination.HistoricalPressure> {
                HistoricalPressureScreen(
                    innerPadding = innerPadding,
                    viewModel = historicalPressureViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ContigoDestination.Medications> {
                MedicationsScreen(
                    innerPadding = innerPadding,
                    viewModel = medicationsViewModel,
                    easyModeEnabled = settingsUiState.easyModeEnabled,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(ContigoDestination.Medications) },
                )
            }
            composable<ContigoDestination.Records> {
                HistoryScreen(
                    innerPadding = innerPadding,
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(ContigoDestination.Records) },
                )
            }
            composable<ContigoDestination.Reports> {
                ReportsScreen(
                    innerPadding = innerPadding,
                    viewModel = reportsViewModel,
                    onBack = { navController.popBackStack() },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(ContigoDestination.Reports) },
                )
            }
            composable<ContigoDestination.Settings> {
                SettingsScreen(
                    innerPadding = innerPadding,
                    viewModel = settingsViewModel,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenReports = { navController.navigate(ContigoDestination.Reports) },
                    showSpeakScreenButton = settingsUiState.voiceGuidanceEnabled,
                    onSpeakScreen = { speakScreen(ContigoDestination.Settings) },
                )
            }
            composable<ContigoDestination.FamilyContact> {
                FamilyContactScreen(
                    innerPadding = innerPadding,
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ContigoDestination.Backup> {
                BackupScreen(
                    innerPadding = innerPadding,
                    backupViewModel = backupViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenHome = {
                        onModeChange(UserMode.PATIENT)
                        navController.popBackStack(ContigoDestination.PatientHome, inclusive = false)
                    },
                )
            }
        }
    }
}

private fun buildPatientHomeAudio(state: HomeScreenState): String {
    val patientName = state.patientFirstName
    val medications = state.nextGroupMedications
    if (medications.isEmpty()) {
        return "Hola $patientName. Hoy no tienes pastillas pendientes. Puedes tocar Medir presión, Pedir ayuda o Hablar."
    }
    val actionText = if (medications.size > 1) "Registrar tomas" else "Ya tomé"
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
