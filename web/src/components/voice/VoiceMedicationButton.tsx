"use client";

import { CheckCircle2, Loader2, Mic, Pill, RotateCcw } from "lucide-react";
import { useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { parseMedicationConfirmation } from "@/features/voice/parseMedicationConfirmation";
import { parseVoiceConfirmation } from "@/features/voice/parseVoiceConfirmation";
import { useSpeechRecognition } from "@/features/voice/useSpeechRecognition";
import { useSpeechSynthesis } from "@/features/voice/useSpeechSynthesis";

type VoiceMedicationButtonProps = {
  medicationId: string;
  medicationName: string;
  scheduleTime: string;
  onSaved?: () => void;
};

type MedicationVoiceStep =
  | "idle"
  | "speaking_instruction"
  | "listening_intake"
  | "intake_detected"
  | "speaking_confirmation"
  | "listening_confirmation"
  | "saving"
  | "success"
  | "duplicate"
  | "error";

const instructionText =
  "Dime si ya tomaste tu pastilla. Por ejemplo: ya tomé mi pastilla.";

function scheduledForToday(time: string) {
  const date = new Date();
  const normalizedTime = time.trim().toUpperCase();
  const match = normalizedTime.match(/^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/);

  if (!match) {
    return date.toISOString();
  }

  let hours = Number(match[1]);
  const minutes = Number(match[2]);
  const meridiem = match[3];

  if (meridiem === "PM" && hours < 12) {
    hours += 12;
  }

  if (meridiem === "AM" && hours === 12) {
    hours = 0;
  }

  date.setHours(hours, minutes, 0, 0);
  return date.toISOString();
}

function delay(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

export function VoiceMedicationButton({
  medicationId,
  medicationName,
  scheduleTime,
  onSaved,
}: VoiceMedicationButtonProps) {
  const { speakAsync } = useSpeechSynthesis();
  const [voiceStep, setVoiceStepState] =
    useState<MedicationVoiceStep>("idle");
  const voiceStepRef = useRef<MedicationVoiceStep>("idle");
  const intentDetectedRef = useRef(false);
  const [intentDetected, setIntentDetected] = useState(false);
  const confirmationHandledRef = useRef(false);
  const [message, setMessage] = useState("");

  function setVoiceStep(nextStep: MedicationVoiceStep) {
    voiceStepRef.current = nextStep;
    setVoiceStepState(nextStep);
  }

  function handleTranscript(transcriptText: string) {
    if (voiceStepRef.current === "listening_intake") {
      const parsed = parseMedicationConfirmation(transcriptText);

      if (!parsed) {
        return false;
      }

      intentDetectedRef.current = true;
      setIntentDetected(true);
      void askForConfirmation();
      return true;
    }

    if (voiceStepRef.current === "listening_confirmation") {
      const confirmation = parseVoiceConfirmation(transcriptText);

      if (confirmation === "yes") {
        confirmationHandledRef.current = true;
        void saveMedicationIntake();
        return true;
      }

      if (confirmation === "no") {
        confirmationHandledRef.current = true;
        void repeat();
        return true;
      }
    }

    return false;
  }

  async function handleRecognitionEnd() {
    if (
      voiceStepRef.current === "listening_confirmation" &&
      !confirmationHandledRef.current
    ) {
      const noConfirmation =
        "No entendí tu confirmación. Puedes decir sí, correcto, listo o guardar.";
      setVoiceStep("error");
      setMessage(noConfirmation);
      await speakAsync(noConfirmation);
      return;
    }

    if (voiceStepRef.current === "listening_intake" && !intentDetectedRef.current) {
      const unclearMessage =
        "No entendí bien. Puedes decir: ya tomé mi pastilla.";
      setVoiceStep("error");
      setMessage(unclearMessage);
      await speakAsync(unclearMessage);
    }
  }

  async function handleRecognitionError() {
    const errorMessage = "No pude escuchar bien. Inténtalo otra vez.";
    setVoiceStep("error");
    setMessage(errorMessage);
    await speakAsync(errorMessage);
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
    onTranscript: handleTranscript,
    onEnd: () => {
      void handleRecognitionEnd();
    },
    onError: () => {
      void handleRecognitionError();
    },
  });

  async function startIntakeListening() {
    resetTranscript();
    intentDetectedRef.current = false;
    setVoiceStep("listening_intake");
    await delay(150);
    startListening();
  }

  async function startConfirmationListening() {
    resetTranscript();
    confirmationHandledRef.current = false;
    setVoiceStep("listening_confirmation");
    await delay(150);
    startListening();
  }

  async function startVoiceFlow() {
    stopListening();
    resetTranscript();
    intentDetectedRef.current = false;
    setIntentDetected(false);
    confirmationHandledRef.current = false;
    setMessage("");
    setVoiceStep("speaking_instruction");
    await speakAsync(instructionText);

    if (voiceStepRef.current === "speaking_instruction") {
      await startIntakeListening();
    }
  }

  async function askForConfirmation() {
    stopListening();
    setVoiceStep("intake_detected");
    setMessage("");
    setVoiceStep("speaking_confirmation");
    await speakAsync(
      `Te escuché decir que ya tomaste ${medicationName}. ¿Es correcto? Di sí para guardar o no para repetir.`
    );

    if (voiceStepRef.current === "speaking_confirmation") {
      await startConfirmationListening();
    }
  }

  async function repeat() {
    stopListening();
    intentDetectedRef.current = false;
    setIntentDetected(false);
    confirmationHandledRef.current = false;
    setMessage("Está bien. Intentemos otra vez.");
    setVoiceStep("speaking_instruction");
    await speakAsync("Está bien. Intentemos otra vez.");

    if (voiceStepRef.current === "speaking_instruction") {
      await startVoiceFlow();
    }
  }

  async function saveMedicationIntake() {
    if (!intentDetectedRef.current) {
      const unclearMessage =
        "No entendí bien. Puedes decir: ya tomé mi pastilla.";
      setVoiceStep("error");
      setMessage(unclearMessage);
      await speakAsync(unclearMessage);
      return;
    }

    stopListening();
    setVoiceStep("saving");
    setMessage("");

    try {
      const response = await fetch("/api/medication-logs", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          medicationId,
          scheduledFor: scheduledForToday(scheduleTime),
        }),
      });
      const payload = (await response.json()) as {
        duplicate?: boolean;
        message?: string;
      };

      if (!response.ok) {
        throw new Error(payload.message ?? "No se pudo registrar la toma.");
      }

      const nextMessage = payload.duplicate
        ? "Esta toma ya fue registrada."
        : `Listo. Registré que tomaste ${medicationName}.`;

      setVoiceStep(payload.duplicate ? "duplicate" : "success");
      setMessage(nextMessage);
      await speakAsync(nextMessage);
      onSaved?.();
    } catch {
      const errorMessage = "No se pudo registrar la toma. Inténtalo otra vez.";
      setVoiceStep("error");
      setMessage(errorMessage);
      await speakAsync(errorMessage);
    }
  }

  function buttonLabel() {
    if (voiceStep === "speaking_instruction") {
      return "Preparando voz...";
    }

    if (voiceStep === "listening_intake") {
      return "Escuchando...";
    }

    if (voiceStep === "speaking_confirmation") {
      return "Confirmando...";
    }

    if (voiceStep === "listening_confirmation") {
      return "Di sí o no...";
    }

    if (voiceStep === "saving") {
      return "Guardando...";
    }

    return "Confirmar pastilla por voz";
  }

  if (unsupported) {
    return (
      <Card className="rounded-3xl border-none bg-muted shadow-sm">
        <CardContent className="px-6 py-6">
          <p className="text-xl font-semibold">
            Tu navegador no permite confirmar por voz. Usa el botón manual.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-3xl border-none shadow-sm ring-1 ring-primary/10">
      <CardContent className="grid gap-4 px-6 py-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary">
            <Mic className="size-6 text-primary" aria-hidden="true" />
          </div>
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
              Voz
            </p>
            <h2 className="text-2xl font-semibold leading-tight">
              Confirmar pastilla por voz
            </h2>
          </div>
        </div>

        <Button
          type="button"
          variant="default"
          className="h-auto min-h-20 rounded-3xl px-6 py-5 text-xl font-semibold sm:text-2xl"
          disabled={
            listening ||
            voiceStep === "speaking_instruction" ||
            voiceStep === "speaking_confirmation" ||
            voiceStep === "saving"
          }
          onClick={() => void startVoiceFlow()}
        >
          {listening || voiceStep === "saving" ? (
            <Loader2 className="size-6 animate-spin" aria-hidden="true" />
          ) : (
            <Pill className="size-6" aria-hidden="true" />
          )}
          {buttonLabel()}
        </Button>

        {voiceStep === "listening_intake" ? (
          <p className="rounded-3xl bg-muted px-5 py-4 text-xl font-semibold">
            Escuchando... dime si ya tomaste tu pastilla.
          </p>
        ) : null}

        {voiceStep === "listening_confirmation" ? (
          <p className="rounded-3xl bg-muted px-5 py-4 text-xl font-semibold">
            Di sí para guardar o no para repetir.
          </p>
        ) : null}

        {transcript ? (
          <div className="rounded-3xl bg-muted px-5 py-4">
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Te escuché
            </p>
            <p className="mt-2 text-2xl font-semibold leading-8">{transcript}</p>
          </div>
        ) : null}

        {intentDetected &&
        ["intake_detected", "speaking_confirmation", "listening_confirmation"].includes(
          voiceStep
        ) ? (
          <div className="grid gap-4 rounded-3xl bg-white px-5 py-5 text-primary ring-1 ring-primary/10">
            <p className="text-2xl font-semibold leading-tight">
              Detecté que tomaste {medicationName}. ¿Es correcto?
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Button
                type="button"
                className="h-16 rounded-3xl text-xl font-semibold"
                onClick={() => void saveMedicationIntake()}
                disabled={voiceStep === "saving"}
              >
                {voiceStep === "saving" ? (
                  <Loader2 className="size-6 animate-spin" aria-hidden="true" />
                ) : (
                  <CheckCircle2 className="size-6" aria-hidden="true" />
                )}
                Sí, guardar
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-16 rounded-3xl text-xl font-semibold"
                onClick={() => void repeat()}
                disabled={voiceStep === "saving"}
              >
                <RotateCcw className="size-6" aria-hidden="true" />
                Repetir
              </Button>
            </div>
          </div>
        ) : null}

        {recognitionError ? (
          <p className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-xl font-semibold text-rose-800">
            {recognitionError}
          </p>
        ) : null}

        {message ? (
          <p
            className={
              voiceStep === "success"
                ? "rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-xl font-semibold text-emerald-800"
                : voiceStep === "duplicate"
                  ? "rounded-3xl border border-amber-200 bg-amber-50 px-5 py-4 text-xl font-semibold text-amber-900"
                  : "rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-xl font-semibold text-rose-800"
            }
          >
            {voiceStep === "success" ? (
              <CheckCircle2 className="mr-2 inline size-6" aria-hidden="true" />
            ) : null}
            {message}
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}
