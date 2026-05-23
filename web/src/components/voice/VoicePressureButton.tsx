"use client";

import { CheckCircle2, Loader2, Mic, RotateCcw, Volume2 } from "lucide-react";
import { useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  isValidPulse,
  looksLikeIncompleteVoiceCommandTranscript,
  parsePressure,
  type ParsedPressure,
} from "@/features/voice/parsePressure";
import { parseVoiceConfirmation } from "@/features/voice/parseVoiceConfirmation";
import { useSpeechRecognition } from "@/features/voice/useSpeechRecognition";
import { useSpeechSynthesis } from "@/features/voice/useSpeechSynthesis";

type VoiceStep =
  | "idle"
  | "speaking_instruction"
  | "listening_pressure"
  | "speaking_confirmation"
  | "listening_confirmation"
  | "saving"
  | "success"
  | "error";

const instructionText =
  "Dime tu presión. Por ejemplo: ciento veinte sobre ochenta.";

function devLog(label: string, value: unknown) {
  if (process.env.NODE_ENV === "development") {
    console.log(`[CuidaVoz voice] ${label}`, value);
  }
}

function delay(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function pressureSummary(pressure: ParsedPressure) {
  return `${pressure.systolic} sobre ${pressure.diastolic}${
    pressure.pulse !== undefined && isValidPulse(pressure.pulse)
      ? `, con pulso ${pressure.pulse}`
      : ""
  }`;
}

function pressureUiSummary(pressure: ParsedPressure) {
  return `${pressure.systolic} sobre ${pressure.diastolic}${
    pressure.pulse !== undefined && isValidPulse(pressure.pulse)
      ? `, pulso ${pressure.pulse}`
      : ""
  }`;
}

export function VoicePressureButton() {
  const { speakAsync } = useSpeechSynthesis();
  const [voiceStep, setVoiceStepState] = useState<VoiceStep>("idle");
  const voiceStepRef = useRef<VoiceStep>("idle");
  const [detectedPressure, setDetectedPressure] = useState<ParsedPressure | null>(
    null
  );
  const detectedPressureRef = useRef<ParsedPressure | null>(null);
  const confirmationHandledRef = useRef(false);
  const pressureTranscriptRef = useRef("");
  const [message, setMessage] = useState("");

  function setVoiceStep(nextStep: VoiceStep) {
    voiceStepRef.current = nextStep;
    setVoiceStepState(nextStep);
    devLog("current voice step", nextStep);
  }

  function parseFinalPressureTranscript(transcriptText: string) {
    const fullTranscript = transcriptText.trim();
    pressureTranscriptRef.current = fullTranscript;

    devLog("final transcript", fullTranscript);

    if (voiceStepRef.current !== "listening_pressure") {
      return false;
    }

    if (looksLikeIncompleteVoiceCommandTranscript(fullTranscript)) {
      devLog("final transcript incomplete", fullTranscript);
      return false;
    }

    const parsed = parsePressure(fullTranscript);
    devLog("parsed pressure", parsed);

    if (!parsed) {
      return false;
    }

    detectedPressureRef.current = parsed;
    setDetectedPressure(parsed);
    void askForConfirmation(parsed);
    return true;
  }

  function handleTranscript(transcriptText: string) {
    devLog("transcript", transcriptText);

    if (voiceStepRef.current === "listening_pressure") {
      return false;
    }

    if (voiceStepRef.current === "listening_confirmation") {
      const confirmation = parseVoiceConfirmation(transcriptText);
      devLog("confirmation result", confirmation);

      if (confirmation === "yes") {
        confirmationHandledRef.current = true;
        void savePressure();
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

  function handleRecognitionEnd() {
    if (voiceStepRef.current === "listening_pressure") {
      const finalTranscript = pressureTranscriptRef.current.trim();

      if (
        finalTranscript &&
        !looksLikeIncompleteVoiceCommandTranscript(finalTranscript)
      ) {
        const parsed = parsePressure(finalTranscript);
        devLog("parsed pressure on end", parsed);

        if (parsed) {
          detectedPressureRef.current = parsed;
          setDetectedPressure(parsed);
          void askForConfirmation(parsed);
          return;
        }
      }
    }

    if (
      voiceStepRef.current === "listening_confirmation" &&
      !confirmationHandledRef.current
    ) {
      const noConfirmation =
        "No entendí tu confirmación. Puedes decir sí, correcto, listo o guardar.";
      setVoiceStep("error");
      setMessage(noConfirmation);
      void speakAsync(noConfirmation);
    }
  }

  function handleRecognitionError(error: string) {
    devLog("recognition errors", error);
    setVoiceStep("error");
    setMessage(error);
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
    onInterimTranscript: (interimTranscript) => {
      devLog("interim transcript", interimTranscript);
      if (voiceStepRef.current === "listening_pressure") {
        pressureTranscriptRef.current = interimTranscript;
      }
    },
    onFinalTranscript: parseFinalPressureTranscript,
    onEnd: handleRecognitionEnd,
    onError: handleRecognitionError,
  });

  async function startPressureListening() {
    confirmationHandledRef.current = false;
    pressureTranscriptRef.current = "";
    resetTranscript();
    setVoiceStep("listening_pressure");
    await delay(150);
    startListening();
  }

  async function startConfirmationListening() {
    confirmationHandledRef.current = false;
    resetTranscript();
    setVoiceStep("listening_confirmation");
    await delay(150);
    startListening();
  }

  async function startVoiceFlow() {
    stopListening();
    detectedPressureRef.current = null;
    pressureTranscriptRef.current = "";
    setDetectedPressure(null);
    setMessage("");
    resetTranscript();
    setVoiceStep("speaking_instruction");
    await speakAsync(instructionText);

    if (voiceStepRef.current === "speaking_instruction") {
      await startPressureListening();
    }
  }

  async function askForConfirmation(pressure: ParsedPressure) {
    stopListening();
    setVoiceStep("speaking_confirmation");
    const confirmationText = `Te escuché ${pressureSummary(
      pressure
    )}. ¿Es correcto? Di sí para guardar o no para repetir.`;
    await speakAsync(confirmationText);

    if (voiceStepRef.current === "speaking_confirmation") {
      await startConfirmationListening();
    }
  }

  async function repeat() {
    stopListening();
    detectedPressureRef.current = null;
    pressureTranscriptRef.current = "";
    setDetectedPressure(null);
    setMessage("");
    await startVoiceFlow();
  }

  async function savePressure() {
    const pressure = detectedPressureRef.current ?? detectedPressure;

    if (!pressure) {
      setVoiceStep("error");
      setMessage("No detecté una presión válida. Repite la medición.");
      return;
    }

    stopListening();
    setVoiceStep("saving");
    setMessage("");

    try {
      const body = {
        patientId: "patient_maria",
        systolic: pressure.systolic,
        diastolic: pressure.diastolic,
        ...(pressure.pulse !== undefined && isValidPulse(pressure.pulse)
          ? { pulse: pressure.pulse }
          : {}),
      };
      devLog("POST /api/blood-pressure body", body);

      const response = await fetch("/api/blood-pressure", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        throw new Error("No se pudo guardar.");
      }

      const success = "Presión registrada correctamente.";
      detectedPressureRef.current = null;
      setDetectedPressure(null);
      setVoiceStep("success");
      setMessage(success);
      await speakAsync(success);
    } catch {
      setVoiceStep("error");
      setMessage("No se pudo guardar la presión. Inténtalo otra vez.");
    }
  }

  function primaryButtonLabel() {
    if (voiceStep === "speaking_instruction") {
      return "Te explicaré cómo decir tu presión...";
    }

    if (voiceStep === "listening_pressure") {
      return "Escuchando... di tu presión ahora.";
    }

    if (voiceStep === "speaking_confirmation") {
      return "Confirmando lo escuchado...";
    }

    if (voiceStep === "listening_confirmation") {
      return "Di sí para guardar o no para repetir.";
    }

    if (voiceStep === "saving") {
      return "Guardando presión...";
    }

    return "Registrar presión por voz";
  }

  if (unsupported) {
    return (
      <Card className="rounded-3xl border-none bg-primary text-primary-foreground ring-0">
        <CardContent className="px-6 py-6">
          <p className="text-2xl font-semibold leading-tight">
            Tu navegador no permite reconocimiento de voz. Usa el registro manual.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-3xl border-none bg-primary text-primary-foreground ring-0">
      <CardContent className="grid gap-5 px-6 py-6">
        <div className="flex items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white text-primary">
            <Mic className="size-7" aria-hidden="true" />
          </div>
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary-foreground/75">
              Voz
            </p>
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
              Registrar presión por voz
            </h2>
          </div>
        </div>

        <Button
          type="button"
          className="h-auto min-h-20 rounded-3xl bg-white px-6 py-5 text-xl font-semibold text-primary hover:bg-white/90 sm:text-2xl"
          onClick={() => void startVoiceFlow()}
          disabled={
            listening ||
            voiceStep === "speaking_instruction" ||
            voiceStep === "speaking_confirmation" ||
            voiceStep === "saving"
          }
        >
          {listening || voiceStep === "saving" ? (
            <Loader2 className="size-6 animate-spin" aria-hidden="true" />
          ) : (
            <Volume2 className="size-6" aria-hidden="true" />
          )}
          {primaryButtonLabel()}
        </Button>

        {transcript ? (
          <div className="rounded-3xl bg-white/12 px-5 py-4">
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-primary-foreground/70">
              Te escuché
            </p>
            <p className="mt-2 text-2xl font-semibold leading-8">{transcript}</p>
          </div>
        ) : null}

        {recognitionError ? (
          <p className="rounded-3xl bg-white px-5 py-4 text-xl font-semibold text-rose-700">
            {recognitionError}
          </p>
        ) : null}

        {detectedPressure ? (
          <div className="grid gap-4 rounded-3xl bg-white px-5 py-5 text-primary">
            <p className="text-2xl font-semibold leading-tight">
              Detecté {pressureUiSummary(detectedPressure)}. ¿Es correcto?
            </p>
            <p className="text-lg font-medium text-primary/75">
              Di sí para guardar o no para repetir.
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Button
                type="button"
                className="h-16 rounded-3xl text-xl font-semibold"
                onClick={() => void savePressure()}
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

        {message ? (
          <p
            className={
              voiceStep === "success"
                ? "rounded-3xl bg-emerald-50 px-5 py-4 text-xl font-semibold text-emerald-800"
                : "rounded-3xl bg-rose-50 px-5 py-4 text-xl font-semibold text-rose-800"
            }
          >
            {message}
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}
