"use client";

import { useEffect, useRef, useState } from "react";

import { useHelpDialog } from "@/components/patient/help-dialog-provider";
import {
  getMedicationToRegister,
  scheduledForTodayIso,
  type DailyMedication,
} from "@/features/daily-status/medication-scheduling";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";
import {
  isValidPulse,
  looksLikeIncompleteVoiceCommandTranscript,
  type ParsedPressure,
} from "@/features/voice/parsePressure";
import { parseVoiceConfirmation } from "@/features/voice/parseVoiceConfirmation";
import { parseVoiceIntent } from "@/features/voice/parseVoiceIntent";
import { useSpeechRecognition } from "@/features/voice/useSpeechRecognition";
import { useSpeechSynthesis } from "@/features/voice/useSpeechSynthesis";

import { VoiceAssistantButton, type AssistantStep } from "./VoiceAssistantButton";

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function pressureSpeakSummary(pressure: ParsedPressure) {
  const base = `presión ${pressure.systolic} sobre ${pressure.diastolic}`;
  if (pressure.pulse !== undefined && isValidPulse(pressure.pulse)) {
    return `${base}, con pulso ${pressure.pulse}`;
  }
  return base;
}

function pressureUiSummary(pressure: ParsedPressure) {
  const base = `Detecté presión ${pressure.systolic}/${pressure.diastolic}`;
  if (pressure.pulse !== undefined && isValidPulse(pressure.pulse)) {
    return `${base} · Pulso ${pressure.pulse} lpm`;
  }
  return base;
}

async function fetchDailyStatus(): Promise<DailyStatus> {
  const response = await fetch("/api/daily-status", { cache: "no-store" });
  const payload = (await response.json()) as {
    data?: DailyStatus;
    message?: string;
  };
  if (!response.ok || !payload.data) {
    throw new Error(payload.message ?? "No se pudo cargar tu estado de hoy.");
  }
  return payload.data;
}

type PendingSave =
  | { kind: "blood_pressure"; pressure: ParsedPressure }
  | { kind: "medication"; medication: DailyMedication };

const GREETING = "¿En qué puedo ayudarte?";
const UNKNOWN_REPLY =
  "No entendí bien. Puedes decir: mi presión es 120 sobre 70, o ya tomé mi pastilla.";
const RETRY_AFTER_NO =
  "Está bien. Intentemos otra vez. ¿En qué puedo ayudarte?";
const NO_CONFIRM_HEARD =
  "No escuché tu confirmación. Puedes decir sí para guardar o no para repetir.";
const HELP_SPOKEN = "Abrí la opción para pedir ayuda.";
const ALL_MEDS_TAKEN = "Todas tus pastillas de hoy ya fueron registradas.";

export function VoiceAssistantClient() {
  const { speakAsync } = useSpeechSynthesis();
  const { openHelp } = useHelpDialog();

  const [step, setStep] = useState<AssistantStep>("idle");
  const stepRef = useRef<AssistantStep>("idle");
  const [statusLine, setStatusLine] = useState("");
  const [detailLine, setDetailLine] = useState("");

  const flowGenRef = useRef(0);
  const pendingSaveRef = useRef<PendingSave | null>(null);
  const confirmationHandledRef = useRef(false);

  const speechCtlRef = useRef({
    startListening: () => {},
    stopListening: () => {},
    resetTranscript: () => {},
  });

  function setAssistantStep(next: AssistantStep) {
    stepRef.current = next;
    setStep(next);
  }

  function handleRecognitionEnd() {
    if (
      stepRef.current === "listening_confirmation" &&
      !confirmationHandledRef.current
    ) {
      void (async () => {
        await speakAsync(NO_CONFIRM_HEARD);
        if (stepRef.current !== "listening_confirmation") {
          return;
        }
        confirmationHandledRef.current = false;
        await delay(150);
        speechCtlRef.current.startListening();
      })();
    }
  }

  const {
    transcript,
    listening,
    unsupported,
    error: recognitionError,
    startListening,
    stopListening,
    resetTranscript,
  } = useSpeechRecognition({
    commandIdleDebounceMs:
      step === "listening_command" ? 1200 : undefined,
    incompleteCommandChecker:
      step === "listening_command"
        ? looksLikeIncompleteVoiceCommandTranscript
        : undefined,
    continuous: step === "listening_command",
    onFinalTranscript: (text) => {
      void handleFinalTranscript(text);
    },
    onEnd: handleRecognitionEnd,
    onError: () => {
      setAssistantStep("error");
      setStatusLine("Hubo un problema con el micrófono.");
      setDetailLine("");
    },
  });

  useEffect(() => {
    speechCtlRef.current = { startListening, stopListening, resetTranscript };
  }, [startListening, stopListening, resetTranscript]);

  async function beginListeningCommand(gen: number) {
    setAssistantStep("listening_command");
    setStatusLine("Escuchando…");
    setDetailLine("Puedes hablar cuando quieras.");
    await delay(150);
    if (flowGenRef.current !== gen) {
      return;
    }
    speechCtlRef.current.startListening();
  }

  async function restartAfterNo(gen: number) {
    speechCtlRef.current.stopListening();
    speechCtlRef.current.resetTranscript();
    confirmationHandledRef.current = false;
    pendingSaveRef.current = null;
    setAssistantStep("speaking_greeting");
    setDetailLine("");
    await speakAsync(RETRY_AFTER_NO);
    if (flowGenRef.current !== gen) {
      return;
    }
    await beginListeningCommand(gen);
  }

  async function savePending() {
    const pending = pendingSaveRef.current;
    if (!pending) {
      setAssistantStep("error");
      setStatusLine("No hay nada que guardar.");
      setDetailLine("");
      return;
    }

    setAssistantStep("saving");
    setStatusLine("Guardando…");
    setDetailLine("");

    try {
      if (pending.kind === "blood_pressure") {
        const body: Record<string, unknown> = {
          patientId: "patient_maria",
          systolic: pending.pressure.systolic,
          diastolic: pending.pressure.diastolic,
        };
        if (
          pending.pressure.pulse !== undefined &&
          isValidPulse(pending.pressure.pulse)
        ) {
          body.pulse = pending.pressure.pulse;
        }

        const response = await fetch("/api/blood-pressure", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        if (!response.ok) {
          throw new Error("pressure");
        }

        const ok = "Presión guardada correctamente.";
        await speakAsync(ok);
        setAssistantStep("success");
        setStatusLine(ok);
        setDetailLine("");
      } else {
        const response = await fetch("/api/medication-logs", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            medicationId: pending.medication.id,
            scheduledFor: scheduledForTodayIso(pending.medication.scheduleTime),
          }),
        });
        const payload = (await response.json()) as {
          message?: string;
          duplicate?: boolean;
        };

        if (!response.ok) {
          throw new Error("medication");
        }

        if (payload.duplicate) {
          const msg = "Esta toma ya fue registrada.";
          await speakAsync(msg);
          setAssistantStep("success");
          setStatusLine(msg);
          setDetailLine("");
        } else {
          const msg = `Listo. Registré que tomaste ${pending.medication.name}.`;
          await speakAsync(msg);
          setAssistantStep("success");
          setStatusLine(msg);
          setDetailLine("");
        }
      }
    } catch {
      const err = "No se pudo guardar. Inténtalo otra vez.";
      await speakAsync(err);
      setAssistantStep("error");
      setStatusLine(err);
      setDetailLine("");
    } finally {
      pendingSaveRef.current = null;
      confirmationHandledRef.current = false;
    }
  }

  async function askConfirmationListen(gen: number, spoken: string, uiStatus: string) {
    speechCtlRef.current.stopListening();
    speechCtlRef.current.resetTranscript();
    setAssistantStep("speaking_confirmation");
    setStatusLine(uiStatus);
    setDetailLine("Te voy a pedir confirmación.");
    await speakAsync(`${spoken} ¿Es correcto?`);
    if (flowGenRef.current !== gen) {
      return;
    }
    confirmationHandledRef.current = false;
    setAssistantStep("listening_confirmation");
    setDetailLine("Di sí para guardar o no para repetir.");
    await delay(150);
    if (flowGenRef.current !== gen) {
      return;
    }
    speechCtlRef.current.startListening();
  }

  async function handleCommandFinal(text: string) {
    const gen = flowGenRef.current;
    speechCtlRef.current.stopListening();
    const trimmed = text.trim();
    if (!trimmed) {
      return;
    }

    const intent = parseVoiceIntent(trimmed);

    if (intent.type === "blood_pressure") {
      setAssistantStep("intent_detected");
      setStatusLine(pressureUiSummary(intent.pressure));
      setDetailLine("Preparando confirmación.");
      pendingSaveRef.current = {
        kind: "blood_pressure",
        pressure: intent.pressure,
      };
      const spoken = `Te escuché ${pressureSpeakSummary(intent.pressure)}.`;
      const ui = pressureUiSummary(intent.pressure);
      await askConfirmationListen(gen, spoken, ui);
      return;
    }

    if (intent.type === "medication_taken") {
      setAssistantStep("intent_detected");
      setStatusLine("Revisando tu medicación…");
      setDetailLine("Un momento.");
      try {
        const daily = await fetchDailyStatus();
        const selected = getMedicationToRegister(
          daily.medications,
          new Date()
        );

        if (
          daily.summary.allMedicationsTaken ||
          !selected ||
          daily.medications.every((m) => m.statusToday === "TAKEN")
        ) {
          await speakAsync(ALL_MEDS_TAKEN);
          if (flowGenRef.current !== gen) {
            return;
          }
          setAssistantStep("idle");
          setStatusLine(ALL_MEDS_TAKEN);
          setDetailLine("");
          return;
        }

        pendingSaveRef.current = { kind: "medication", medication: selected };
        const spoken = `Te escuché decir que ya tomaste ${selected.name}.`;
        const ui = `Detecté que tomaste ${selected.name}`;
        await askConfirmationListen(gen, spoken, ui);
      } catch {
        const err = "No pude revisar tus medicinas. Inténtalo otra vez.";
        await speakAsync(err);
        if (flowGenRef.current !== gen) {
          return;
        }
        setAssistantStep("error");
        setStatusLine(err);
        setDetailLine("");
      }
      return;
    }

    if (intent.type === "help") {
      openHelp();
      await speakAsync(HELP_SPOKEN);
      if (flowGenRef.current !== gen) {
        return;
      }
      setAssistantStep("idle");
      setStatusLine("Ayuda abierta.");
      setDetailLine("");
      return;
    }

    await speakAsync(UNKNOWN_REPLY);
    if (flowGenRef.current !== gen) {
      return;
    }
    setAssistantStep("idle");
    setStatusLine("No entendí el pedido.");
    setDetailLine("Prueba decir tu presión o que ya tomaste la pastilla.");
  }

  async function handleConfirmationFinal(text: string) {
    const gen = flowGenRef.current;
    speechCtlRef.current.stopListening();
    const answer = parseVoiceConfirmation(text);

    if (answer === "yes") {
      confirmationHandledRef.current = true;
      await savePending();
      return;
    }

    if (answer === "no") {
      confirmationHandledRef.current = true;
      await restartAfterNo(gen);
      return;
    }

    await speakAsync(NO_CONFIRM_HEARD);
    if (flowGenRef.current !== gen) {
      return;
    }
    confirmationHandledRef.current = false;
    setAssistantStep("listening_confirmation");
    await delay(150);
    if (flowGenRef.current !== gen) {
      return;
    }
    speechCtlRef.current.startListening();
  }

  async function handleFinalTranscript(text: string) {
    if (stepRef.current === "listening_command") {
      await handleCommandFinal(text);
    } else if (stepRef.current === "listening_confirmation") {
      await handleConfirmationFinal(text);
    }
  }

  async function startFlow() {
    const gen = ++flowGenRef.current;
    speechCtlRef.current.stopListening();
    speechCtlRef.current.resetTranscript();
    confirmationHandledRef.current = false;
    pendingSaveRef.current = null;
    setStatusLine("");
    setDetailLine("");
    setAssistantStep("speaking_greeting");
    await speakAsync(GREETING);
    if (flowGenRef.current !== gen) {
      return;
    }
    await beginListeningCommand(gen);
  }

  if (unsupported) {
    return (
      <VoiceAssistantButton
        step="error"
        listening={false}
        transcript=""
        statusLine="Tu navegador no permite reconocimiento de voz."
        detailLine="Usa el registro manual de presión o pastilla."
        showConfirmActions={false}
        disabledStart
        onStart={() => {}}
        onConfirmSave={() => {}}
        onRepeat={() => {}}
      />
    );
  }

  const showConfirm =
    step === "listening_confirmation" ||
    step === "speaking_confirmation" ||
    step === "saving";

  return (
    <VoiceAssistantButton
      step={step}
      listening={listening}
      transcript={transcript}
      statusLine={
        recognitionError
          ? recognitionError
          : statusLine || (listening ? "Escuchando…" : "")
      }
      detailLine={detailLine}
      showConfirmActions={showConfirm}
      disabledStart={
        step === "speaking_greeting" ||
        step === "speaking_confirmation" ||
        step === "saving" ||
        listening
      }
      onStart={() => void startFlow()}
      onConfirmSave={() => {
        confirmationHandledRef.current = true;
        void savePending();
      }}
      onRepeat={() => {
        void restartAfterNo(flowGenRef.current);
      }}
    />
  );
}
