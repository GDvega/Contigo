import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { DailyStatusCard } from "@/components/DailyStatusCard";
import { HelpModal } from "@/components/HelpModal";
import { MedicationReminderCard } from "@/components/MedicationReminderCard";
import { Screen } from "@/components/Screen";
import { StatusBadge } from "@/components/StatusBadge";
import {
  parseVoiceConfirmation,
} from "@/features/voice/parseVoiceConfirmation";
import {
  parseVoiceIntent,
  type VoiceIntent,
} from "@/features/voice/parseVoiceIntent";
import {
  destroyVoice,
  isVoiceError,
  listenOnce,
  speakAsync,
} from "@/lib/mobileVoice";
import {
  confirmMedicationGroupTaken,
  confirmSingleMedicationTaken,
  createBloodPressureReading,
  getDailyStatus,
} from "@/lib/mobileData";
import {
  buildGroupFromReminderEvent,
  scheduleMedicationSnooze,
  speakOpenedMedicationReminder,
} from "@/lib/notifications";
import {
  consumeLatestReminderEvent,
  subscribeToReminderEvents,
  type ReminderEvent,
} from "@/lib/reminderEvents";
import { RegisterPressureScreen } from "@/screens/RegisterPressureScreen";
import { colors } from "@/theme";
import type { DailyMedication, DailyStatus, MedicationGroup } from "@/types";
import { formatScheduleTime } from "@/utils/dates";
import { getNextMedicationGroup as buildNextMedicationGroup } from "@/utils/medications";

export function PatientHomeScreen() {
  const [dailyStatus, setDailyStatus] = useState<DailyStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [activeScreen, setActiveScreen] = useState<"home" | "pressure">("home");
  const [refreshError, setRefreshError] = useState<string | null>(null);
  const [voiceStep, setVoiceStep] = useState<VoiceStep>("idle");
  const [voiceTranscript, setVoiceTranscript] = useState("");
  const [voiceMessage, setVoiceMessage] = useState(
    "Puedes decir: mi presión es 120 sobre 70, ya tomé mi pastilla o necesito ayuda."
  );
  const [detectedVoiceIntent, setDetectedVoiceIntent] =
    useState<DetectedVoiceIntent | null>(null);
  const [activeReminderEvent, setActiveReminderEvent] = useState<ReminderEvent | null>(
    null
  );
  const [isSnoozingReminder, setIsSnoozingReminder] = useState(false);
  const [isMarkingOneByOne, setIsMarkingOneByOne] = useState(false);

  const nextMedicationGroup = useMemo(
    () => (dailyStatus ? buildNextMedicationGroup(dailyStatus.medications) : null),
    [dailyStatus]
  );
  const selectedMedication = useMemo(
    () =>
      nextMedicationGroup?.medications.find(
        (medication) => medication.statusToday === "PENDING"
      ) ?? null,
    [nextMedicationGroup]
  );
  const activeReminderGroup = useMemo(
    () => (activeReminderEvent ? buildGroupFromReminderEvent(activeReminderEvent) : null),
    [activeReminderEvent]
  );
  const hasGroupedMedication = Boolean(
    nextMedicationGroup && nextMedicationGroup.pendingMedications > 1
  );
  const hasPendingMedication = Boolean(selectedMedication);
  const patientFirstName = dailyStatus?.patient.fullName.split(" ")[0] ?? "María";

  const loadDailyStatus = useCallback(async () => {
    const data = await getDailyStatus();
    setDailyStatus(data);
    setRefreshError(null);
  }, []);

  useEffect(() => {
    loadDailyStatus()
      .catch(() => {
        Alert.alert("No se pudo cargar", "No pudimos abrir tus datos del celular.");
      })
      .finally(() => setIsLoading(false));
  }, [loadDailyStatus]);

  useEffect(() => {
    return () => {
      void destroyVoice();
    };
  }, []);

  useEffect(() => {
    const handleReminderEvent = (event: ReminderEvent) => {
      setActiveReminderEvent(event);
      setIsMarkingOneByOne(false);
      void loadDailyStatus().catch(() => undefined);

      if (event.source === "notification_opened") {
        setActiveScreen("home");
        void speakOpenedMedicationReminder(buildGroupFromReminderEvent(event));
      }
    };

    const latestEvent = consumeLatestReminderEvent();
    if (latestEvent) {
      handleReminderEvent(latestEvent);
    }

    return subscribeToReminderEvents(handleReminderEvent);
  }, [loadDailyStatus]);

  useEffect(() => {
    if (!nextMedicationGroup || nextMedicationGroup.pendingMedications <= 1) {
      setIsMarkingOneByOne(false);
    }
  }, [nextMedicationGroup]);

  useFocusEffect(
    useCallback(() => {
      if (activeScreen !== "home") {
        return;
      }

      void loadDailyStatus().catch(() => {
        setRefreshError("No pudimos actualizar los datos.");
      });
    }, [activeScreen, loadDailyStatus])
  );

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadDailyStatus();
    } catch {
      setRefreshError("No pudimos actualizar los datos.");
    } finally {
      setIsRefreshing(false);
    }
  }

  async function confirmMedication() {
    if (!selectedMedication) {
      Alert.alert(
        dailyStatus?.medications.length ? "Todo listo" : "Sin pastillas",
        dailyStatus?.medications.length
          ? "Todas tus pastillas de hoy ya fueron registradas."
          : "No hay pastillas configuradas."
      );
      return;
    }

    await confirmMedicationById(selectedMedication.id);
  }

  async function confirmMedicationById(medicationId: string) {
    setIsConfirming(true);
    try {
      const response = await confirmSingleMedicationTaken(medicationId);
      if (response.completed && !response.duplicate) {
        setActiveReminderEvent(null);
        await speakAsync("Muy bien, registré que tomaste tu pastilla.");
      }
      Alert.alert(
        "CuidaVoz",
        response.completed
          ? response.duplicate
            ? "Esta toma ya fue registrada."
            : "Toma registrada correctamente."
          : response.message
      );
      await loadDailyStatus();
    } catch {
      Alert.alert("CuidaVoz", "No se pudo registrar. Intenta otra vez.");
    } finally {
      setIsConfirming(false);
    }
  }

  async function confirmMedicationGroup(scheduleTime?: string) {
    const targetScheduleTime = scheduleTime ?? nextMedicationGroup?.scheduleTime;

    if (!targetScheduleTime) {
      Alert.alert("Sin pastillas", "No hay pastillas pendientes para este horario.");
      return;
    }

    setIsConfirming(true);
    try {
      const response = await confirmMedicationGroupTaken(targetScheduleTime);
      if (response.completed && !response.duplicate) {
        setActiveReminderEvent(null);
        setIsMarkingOneByOne(false);
        await speakAsync("Muy bien, registré tus pastillas.");
      }
      Alert.alert(
        "CuidaVoz",
        response.completed
          ? response.duplicate
            ? "Estas tomas ya fueron registradas."
            : "Tomas registradas correctamente."
          : response.message
      );
      await loadDailyStatus();
    } catch {
      Alert.alert("CuidaVoz", "No se pudo registrar. Intenta otra vez.");
    } finally {
      setIsConfirming(false);
    }
  }

  async function startVoiceFlow() {
    if (!dailyStatus || voiceStep === "saving") {
      return;
    }

    setDetectedVoiceIntent(null);
    setVoiceTranscript("");
    setVoiceMessage("¿En qué puedo ayudarte?");

    try {
      setVoiceStep("speaking_prompt");
      await speakAsync("¿En qué puedo ayudarte?");

      setVoiceStep("listening_command");
      setVoiceMessage("Escuchando…");
      const transcript = await listenOnce({
        onPartial: setVoiceTranscript,
      });
      setVoiceTranscript(transcript);

      await handleVoiceCommand(transcript);
    } catch (error) {
      await showVoiceError(getVoiceErrorMessage(error));
    }
  }

  async function handleVoiceCommand(transcript: string) {
    const intent = parseVoiceIntent(transcript);

    if (intent.type === "unknown") {
      await showVoiceError(
        "No entendí bien. Puedes decir: mi presión es 120 sobre 70, ya tomé mi pastilla o necesito ayuda."
      );
      return;
    }

    if (intent.type === "help") {
      setIsHelpOpen(true);
      setVoiceStep("success");
      setVoiceMessage("Abrí la opción para pedir ayuda.");
      await speakAsync("Abrí la opción para pedir ayuda.");
      return;
    }

    if (intent.type === "blood_pressure") {
      const detected: DetectedVoiceIntent = {
        type: "blood_pressure",
        pressure: intent.pressure,
      };
      setDetectedVoiceIntent(detected);
      setVoiceStep("intent_detected");

      const message = intent.pressure.pulse
        ? `Detecté presión ${intent.pressure.systolic}/${intent.pressure.diastolic} · Pulso ${intent.pressure.pulse} lpm`
        : `Detecté presión ${intent.pressure.systolic}/${intent.pressure.diastolic}`;
      setVoiceMessage(message);

      await askForVoiceConfirmation(detected);
      return;
    }

    if (!nextMedicationGroup || !selectedMedication) {
      const message =
        dailyStatus?.medications.length === 0
          ? "No hay pastillas configuradas."
          : "Todas tus pastillas de hoy ya fueron registradas.";
      setVoiceStep("success");
      setVoiceMessage(message);
      await speakAsync(message);
      return;
    }

    const detected: DetectedVoiceIntent =
      nextMedicationGroup.pendingMedications > 1
        ? {
            type: "medication_group_taken",
            group: nextMedicationGroup,
          }
        : {
            type: "medication_taken",
            medication: selectedMedication,
          };
    setDetectedVoiceIntent(detected);
    setVoiceStep("intent_detected");
    setVoiceMessage(
      detected.type === "medication_group_taken"
        ? `Detecté que tomaste tus pastillas de las ${formatScheduleTime(
            detected.group.scheduleTime
          )}`
        : `Detecté que tomaste ${selectedMedication.name}`
    );
    await askForVoiceConfirmation(detected);
  }

  async function askForVoiceConfirmation(intent: DetectedVoiceIntent) {
    const prompt = getConfirmationPrompt(intent);

    setVoiceStep("speaking_confirmation");
    setVoiceMessage(prompt);
    await speakAsync(`${prompt} Di sí para guardar o no para repetir.`);

    setVoiceStep("listening_confirmation");
    setVoiceMessage("Di sí para guardar o no para repetir.");
    let transcript = "";

    try {
      transcript = await listenOnce({
        onPartial: setVoiceTranscript,
      });
      setVoiceTranscript(transcript);
    } catch (error) {
      await showVoiceError(getVoiceErrorMessage(error));
      return;
    }

    const confirmation = parseVoiceConfirmation(transcript);

    if (confirmation === "yes") {
      await saveDetectedVoiceIntent(intent);
      return;
    }

    if (confirmation === "no") {
      setDetectedVoiceIntent(null);
      setVoiceMessage("Está bien. Intentemos otra vez.");
      await speakAsync("Está bien. Intentemos otra vez.");
      await startVoiceFlow();
      return;
    }

    await showVoiceError(
      "No entendí tu confirmación. Puedes decir sí, correcto, listo o guardar."
    );
  }

  async function saveDetectedVoiceIntent(intent = detectedVoiceIntent) {
    if (!intent) {
      await showVoiceError("No hay una acción lista para guardar.");
      return;
    }

    setVoiceStep("saving");

    try {
      if (intent.type === "blood_pressure") {
        await createBloodPressureReading({
          patientId: "patient_maria",
          systolic: intent.pressure.systolic,
          diastolic: intent.pressure.diastolic,
          ...(intent.pressure.pulse ? { pulse: intent.pressure.pulse } : {}),
        });
        await loadDailyStatus();
        await showVoiceSuccess("Presión registrada correctamente.");
        return;
      }

      const response =
        intent.type === "medication_group_taken"
          ? await confirmMedicationGroupTaken(intent.group.scheduleTime)
          : await confirmSingleMedicationTaken(intent.medication.id);
      await loadDailyStatus();
      if (response.completed && !response.duplicate) {
        setActiveReminderEvent(null);
        setIsMarkingOneByOne(false);
      }
      await showVoiceSuccess(
        response.completed
          ? response.duplicate
            ? intent.type === "medication_group_taken"
              ? "Estas tomas ya fueron registradas."
              : "Esta toma ya fue registrada."
            : intent.type === "medication_group_taken"
              ? "Muy bien, registré tus pastillas."
              : "Muy bien, registré que tomaste tu pastilla."
          : response.message
      );
    } catch {
      await showVoiceError("No se pudo guardar. Inténtalo otra vez.");
    }
  }

  async function snoozeMedicationReminder() {
    const group = activeReminderGroup ?? nextMedicationGroup;

    if (!group) {
      Alert.alert("CuidaVoz", "No hay una pastilla pendiente para recordar.");
      return;
    }

    setIsSnoozingReminder(true);
    try {
      await scheduleMedicationSnooze({
        medicationIds: group.medications.map((medication) => medication.id),
        medicationNames: group.medications.map((medication) => medication.name),
        medicationDoses: group.medications.map((medication) => medication.dose),
        scheduleTime: group.scheduleTime,
      });
      Alert.alert("CuidaVoz", "Te volveré a recordar en 10 minutos.");
    } catch {
      Alert.alert("CuidaVoz", "No pudimos programar otro recordatorio.");
    } finally {
      setIsSnoozingReminder(false);
    }
  }

  async function repeatVoiceFlow() {
    setDetectedVoiceIntent(null);
    await startVoiceFlow();
  }

  async function confirmSingleMedicationTakenFromList(medication: DailyMedication) {
    await confirmMedicationById(medication.id);
  }

  async function showVoiceSuccess(message: string) {
    setVoiceStep("success");
    setVoiceMessage(message);
    setDetectedVoiceIntent(null);
    await speakAsync(message);
  }

  async function showVoiceError(message: string) {
    setVoiceStep("error");
    setVoiceMessage(message);
    await speakAsync(message);
  }

  if (activeScreen === "pressure") {
    return (
      <RegisterPressureScreen
        onBack={() => setActiveScreen("home")}
        onSaved={loadDailyStatus}
      />
    );
  }

  if (isLoading) {
    return (
      <Screen scroll={false}>
        <View style={styles.loading}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.loadingText}>Cargando información...</Text>
        </View>
      </Screen>
    );
  }

  if (!dailyStatus) {
    return (
      <Screen>
        <AppCard>
          <Text style={styles.title}>No pudimos cargar los datos.</Text>
          <Text style={styles.subtitle}>
            Revisa los datos guardados en este celular e intenta otra vez.
          </Text>
          <AppButton label="Reintentar" onPress={() => void refresh()} />
        </AppCard>
      </Screen>
    );
  }

  return (
    <>
      <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
        <View style={styles.greeting}>
          <Text style={styles.eyebrow}>Modo paciente</Text>
          <Text style={styles.hero}>Buenos días, {patientFirstName}</Text>
          <Text style={styles.subtitle}>
            Hoy te acompaño con tus pastillas y tu presión.
          </Text>
        </View>

        {refreshError ? <Text style={styles.refreshError}>{refreshError}</Text> : null}

        <MedicationReminderCard
          group={nextMedicationGroup}
          highlighted={Boolean(
            activeReminderEvent &&
              nextMedicationGroup &&
              activeReminderEvent.scheduleTime === nextMedicationGroup.scheduleTime
          )}
        />

        {activeReminderEvent && activeReminderGroup ? (
          <AppCard tone="teal">
            <Text style={styles.quickActionsTitle}>Abriste un recordatorio</Text>
            <Text style={styles.quickActionsText}>
              {activeReminderGroup.medications.length === 1
                ? `${activeReminderGroup.medications[0].name} · ${activeReminderGroup.medications[0].dose}. Si ya la tomaste, abre CuidaVoz y di: ya tomé mi pastilla.`
                : `Tienes ${activeReminderGroup.medications.length} pastillas programadas para las ${formatScheduleTime(
                    activeReminderGroup.scheduleTime
                  )}. Si ya las tomaste, abre CuidaVoz y di: ya tomé mis pastillas.`}
            </Text>
            <View style={styles.quickActionsButtons}>
              <AppButton
                label={
                  activeReminderGroup.medications.length === 1
                    ? "Ya tomé mi pastilla"
                    : "Ya tomé todas"
                }
                onPress={() =>
                  void (activeReminderGroup.medications.length === 1
                    ? confirmMedicationById(activeReminderGroup.medications[0].id)
                    : confirmMedicationGroup(activeReminderGroup.scheduleTime))
                }
                loading={isConfirming}
                loadingLabel="Guardando..."
              />
              <AppButton
                label="Hablar ahora"
                variant="secondary"
                onPress={() => void startVoiceFlow()}
              />
              <AppButton
                label="Recordarme en 10 minutos"
                variant="soft"
                onPress={() => void snoozeMedicationReminder()}
                loading={isSnoozingReminder}
                loadingLabel="Programando..."
              />
            </View>
          </AppCard>
        ) : null}

        <View style={styles.actions}>
          {hasGroupedMedication && nextMedicationGroup ? (
            <>
              <AppButton
                label="Ya tomé todas"
                onPress={() => void confirmMedicationGroup(nextMedicationGroup.scheduleTime)}
                loading={isConfirming}
                loadingLabel="Guardando..."
              />
              <AppButton
                label="Marcar una por una"
                variant="secondary"
                onPress={() => setIsMarkingOneByOne((current) => !current)}
              />
              {isMarkingOneByOne ? (
                <View style={styles.groupActions}>
                  {nextMedicationGroup.medications
                    .filter((medication) => medication.statusToday === "PENDING")
                    .map((medication) => (
                      <AppButton
                        key={medication.id}
                        label={`Marcar ${medication.name}`}
                        variant="soft"
                        onPress={() => void confirmSingleMedicationTakenFromList(medication)}
                      />
                    ))}
                </View>
              ) : null}
            </>
          ) : hasPendingMedication ? (
            <AppButton
              label="Ya tomé mi pastilla"
              onPress={() => void confirmMedication()}
              loading={isConfirming}
              loadingLabel="Guardando..."
            />
          ) : null}
          <AppButton
            label="Registrar presión"
            variant="secondary"
            onPress={() => setActiveScreen("pressure")}
          />
          <AppButton
            label="Pedir ayuda"
            variant="soft"
            onPress={() => setIsHelpOpen(true)}
          />
        </View>

        <VoiceAssistantCard
          detectedIntent={detectedVoiceIntent}
          message={voiceMessage}
          onConfirm={() => void saveDetectedVoiceIntent()}
          onRepeat={() => void repeatVoiceFlow()}
          onStart={() => void startVoiceFlow()}
          step={voiceStep}
          transcript={voiceTranscript}
        />

        <DailyStatusCard dailyStatus={dailyStatus} />
      </Screen>

      <HelpModal visible={isHelpOpen} onClose={() => setIsHelpOpen(false)} />
    </>
  );
}

function VoiceAssistantCard({
  detectedIntent,
  message,
  onConfirm,
  onRepeat,
  onStart,
  step,
  transcript,
}: {
  detectedIntent: DetectedVoiceIntent | null;
  message: string;
  onConfirm: () => void;
  onRepeat: () => void;
  onStart: () => void;
  step: VoiceStep;
  transcript: string;
}) {
  const isListening =
    step === "listening_command" || step === "listening_confirmation";
  const isBusy =
    step === "speaking_prompt" ||
    step === "speaking_confirmation" ||
    step === "saving";
  const confirmationButtonsDisabled = isBusy || isListening;

  return (
    <AppCard tone="teal">
      <View style={styles.voiceHeader}>
        <View style={styles.voiceIcon}>
          <Ionicons name="mic" size={22} color={colors.primary} />
        </View>
        <StatusBadge label={isListening ? "Escuchando" : "Voz"} tone="blue" />
      </View>
      <Text style={styles.voiceTitle}>Hablar con CuidaVoz</Text>
      <Text style={styles.voiceText}>{message}</Text>
      {transcript ? (
        <Text style={styles.voiceTranscript}>Te escuché: {transcript}</Text>
      ) : null}
      {detectedIntent ? (
        <View style={styles.voiceConfirmActions}>
          <AppButton
            disabled={confirmationButtonsDisabled}
            label="Sí, guardar"
            onPress={onConfirm}
          />
          <AppButton
            disabled={confirmationButtonsDisabled}
            label="Repetir"
            variant="secondary"
            onPress={onRepeat}
          />
        </View>
      ) : null}
      <AppButton
        label={isListening ? "Escuchando…" : "Hablar ahora"}
        disabled={isBusy || isListening}
        loading={isBusy}
        loadingLabel={step === "saving" ? "Guardando..." : "Preparando..."}
        variant="secondary"
        onPress={onStart}
      />
    </AppCard>
  );
}

type VoiceStep =
  | "idle"
  | "speaking_prompt"
  | "listening_command"
  | "intent_detected"
  | "speaking_confirmation"
  | "listening_confirmation"
  | "saving"
  | "success"
  | "error";

type DetectedVoiceIntent =
  | Extract<VoiceIntent, { type: "blood_pressure" }>
  | {
      type: "medication_taken";
      medication: DailyMedication;
    }
  | {
      type: "medication_group_taken";
      group: MedicationGroup;
    };

function getConfirmationPrompt(intent: DetectedVoiceIntent) {
  if (intent.type === "blood_pressure") {
    return intent.pressure.pulse
      ? `Te escuché presión ${intent.pressure.systolic} sobre ${intent.pressure.diastolic}, pulso ${intent.pressure.pulse}. ¿Es correcto?`
      : `Te escuché presión ${intent.pressure.systolic} sobre ${intent.pressure.diastolic}. ¿Es correcto?`;
  }

  if (intent.type === "medication_group_taken") {
    return `Te escuché decir que ya tomaste tus pastillas de las ${formatScheduleTime(
      intent.group.scheduleTime
    )}. ¿Es correcto?`;
  }

  return `Te escuché decir que ya tomaste ${intent.medication.name}. ¿Es correcto?`;
}

function getVoiceErrorMessage(error: unknown) {
  if (isVoiceError(error)) {
    if (error.code === "microphone_permission_denied") {
      return "No se otorgó permiso para usar el micrófono.";
    }

    if (error.code === "speech_service_unavailable") {
      return "No pudimos usar el reconocimiento de voz en este dispositivo. Puedes usar los botones grandes.";
    }
  }

  return "No pudimos usar el reconocimiento de voz en este dispositivo. Puedes usar los botones grandes.";
}

const styles = StyleSheet.create({
  loading: {
    alignItems: "center",
    flex: 1,
    justifyContent: "center",
    gap: 14,
  },
  loadingText: {
    color: colors.text,
    fontSize: 20,
    fontWeight: "800",
  },
  greeting: {
    gap: 8,
  },
  eyebrow: {
    alignSelf: "flex-start",
    backgroundColor: colors.primarySoft,
    borderRadius: 999,
    color: colors.primary,
    fontSize: 14,
    fontWeight: "900",
    letterSpacing: 1,
    paddingHorizontal: 14,
    paddingVertical: 8,
    textTransform: "uppercase",
  },
  hero: {
    color: colors.text,
    fontSize: 40,
    fontWeight: "900",
    letterSpacing: -1,
    lineHeight: 46,
  },
  title: {
    color: colors.text,
    fontSize: 26,
    fontWeight: "900",
  },
  subtitle: {
    color: colors.muted,
    fontSize: 18,
    lineHeight: 26,
  },
  refreshError: {
    backgroundColor: colors.red,
    borderRadius: 18,
    color: colors.redText,
    fontSize: 16,
    fontWeight: "900",
    padding: 14,
  },
  quickActionsTitle: {
    color: "#fff",
    fontSize: 24,
    fontWeight: "900",
  },
  quickActionsText: {
    color: "rgba(255,255,255,0.88)",
    fontSize: 17,
    lineHeight: 24,
  },
  quickActionsButtons: {
    gap: 12,
  },
  voiceIcon: {
    alignItems: "center",
    backgroundColor: "#fff",
    borderRadius: 999,
    height: 44,
    justifyContent: "center",
    width: 44,
  },
  voiceHeader: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 12,
  },
  voiceTitle: {
    color: "#fff",
    fontSize: 24,
    fontWeight: "900",
  },
  voiceText: {
    color: "rgba(255,255,255,0.88)",
    fontSize: 16,
    lineHeight: 23,
  },
  voiceTranscript: {
    backgroundColor: "rgba(255,255,255,0.14)",
    borderRadius: 18,
    color: "#fff",
    fontSize: 16,
    fontWeight: "800",
    lineHeight: 23,
    padding: 12,
  },
  voiceConfirmActions: {
    gap: 10,
  },
  actions: {
    gap: 12,
  },
  groupActions: {
    gap: 10,
  },
});
