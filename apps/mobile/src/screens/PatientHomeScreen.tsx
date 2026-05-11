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
import { api } from "@/lib/api";
import {
  destroyVoice,
  isVoiceError,
  listenOnce,
  speakAsync,
} from "@/lib/mobileVoice";
import { RegisterPressureScreen } from "@/screens/RegisterPressureScreen";
import { colors } from "@/theme";
import type { DailyMedication, DailyStatus } from "@/types";
import { scheduledForTodayIso } from "@/utils/dates";
import { getMedicationToRegister } from "@/utils/medications";

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

  const selectedMedication = useMemo(
    () => (dailyStatus ? getMedicationToRegister(dailyStatus.medications) : null),
    [dailyStatus]
  );
  const hasPendingMedication = Boolean(selectedMedication);

  const loadDailyStatus = useCallback(async () => {
    const data = await api.getDailyStatus();
    setDailyStatus(data);
    setRefreshError(null);
  }, []);

  useEffect(() => {
    loadDailyStatus()
      .catch(() => {
        Alert.alert("No se pudo cargar", "Revisa la conexión con CuidaVoz.");
      })
      .finally(() => setIsLoading(false));
  }, [loadDailyStatus]);

  useEffect(() => {
    return () => {
      void destroyVoice();
    };
  }, []);

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
    if (!dailyStatus) {
      return;
    }

    if (dailyStatus.medications.length > 0 && !selectedMedication) {
      Alert.alert(
        "Todo listo",
        "Todas tus pastillas de hoy ya fueron registradas."
      );
      return;
    }

    if (!selectedMedication) {
      Alert.alert("Sin pastillas", "No hay pastillas configuradas.");
      return;
    }

    setIsConfirming(true);
    try {
      const response = await api.confirmMedicationIntake({
        medicationId: selectedMedication.id,
        scheduledFor: scheduledForTodayIso(selectedMedication.scheduleTime),
      });
      Alert.alert(
        "CuidaVoz",
        response.duplicate
          ? "Esta toma ya fue registrada."
          : "Toma registrada correctamente."
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

    if (!selectedMedication) {
      const message =
        dailyStatus?.medications.length === 0
          ? "No hay pastillas configuradas."
          : "Todas tus pastillas de hoy ya fueron registradas.";
      setVoiceStep("success");
      setVoiceMessage(message);
      await speakAsync(message);
      return;
    }

    const detected: DetectedVoiceIntent = {
      type: "medication_taken",
      medication: selectedMedication,
    };
    setDetectedVoiceIntent(detected);
    setVoiceStep("intent_detected");
    setVoiceMessage(`Detecté que tomaste ${selectedMedication.name}`);
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
        await api.createBloodPressure({
          patientId: "patient_maria",
          systolic: intent.pressure.systolic,
          diastolic: intent.pressure.diastolic,
          ...(intent.pressure.pulse ? { pulse: intent.pressure.pulse } : {}),
        });
        await loadDailyStatus();
        await showVoiceSuccess("Presión registrada correctamente.");
        return;
      }

      const response = await api.confirmMedicationIntake({
        medicationId: intent.medication.id,
        scheduledFor: scheduledForTodayIso(intent.medication.scheduleTime),
      });
      await loadDailyStatus();
      await showVoiceSuccess(
        response.duplicate
          ? "Esta toma ya fue registrada."
          : `Listo. Registré que tomaste ${intent.medication.name}.`
      );
    } catch {
      await showVoiceError("No se pudo guardar. Inténtalo otra vez.");
    }
  }

  async function repeatVoiceFlow() {
    setDetectedVoiceIntent(null);
    await startVoiceFlow();
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
          <Text style={styles.subtitle}>Revisa tu conexión e intenta otra vez.</Text>
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
          <Text style={styles.hero}>Buenos días, María</Text>
          <Text style={styles.subtitle}>
            Hoy te acompaño con tus pastillas y tu presión.
          </Text>
        </View>

        {refreshError ? <Text style={styles.refreshError}>{refreshError}</Text> : null}

        <MedicationReminderCard medication={selectedMedication} />

        <View style={styles.actions}>
          {hasPendingMedication ? (
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
    };

function getConfirmationPrompt(intent: DetectedVoiceIntent) {
  if (intent.type === "blood_pressure") {
    return intent.pressure.pulse
      ? `Te escuché presión ${intent.pressure.systolic} sobre ${intent.pressure.diastolic}, pulso ${intent.pressure.pulse}. ¿Es correcto?`
      : `Te escuché presión ${intent.pressure.systolic} sobre ${intent.pressure.diastolic}. ¿Es correcto?`;
  }

  return `Te escuché decir que ya tomaste ${intent.medication.name}. ¿Es correcto?`;
}

function getVoiceErrorMessage(error: unknown) {
  if (isVoiceError(error)) {
    if (error.code === "microphone_permission_denied") {
      return "No se otorgó permiso para usar el micrófono.";
    }

    if (error.code === "speech_service_unavailable") {
      return "El reconocimiento de voz no está disponible en este dispositivo.";
    }
  }

  return "No pudimos escucharte bien. Intenta otra vez.";
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
});
