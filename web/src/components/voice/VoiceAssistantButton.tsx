"use client";

import { CheckCircle2, Loader2, Mic, RotateCcw, Volume2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

export type AssistantStep =
  | "idle"
  | "speaking_greeting"
  | "listening_command"
  | "intent_detected"
  | "speaking_confirmation"
  | "listening_confirmation"
  | "saving"
  | "success"
  | "error";

type VoiceAssistantButtonProps = {
  step: AssistantStep;
  listening: boolean;
  transcript: string;
  statusLine: string;
  detailLine: string;
  showConfirmActions: boolean;
  disabledStart: boolean;
  onStart: () => void;
  onConfirmSave: () => void;
  onRepeat: () => void;
};

function primaryLabel(step: AssistantStep, listening: boolean) {
  if (listening && step === "listening_command") {
    return "Escuchando… di tu pedido.";
  }
  if (listening && step === "listening_confirmation") {
    return "Escuchando… di sí o no.";
  }
  if (step === "speaking_greeting" || step === "speaking_confirmation") {
    return "CuidaVoz está hablando…";
  }
  if (step === "saving") {
    return "Guardando…";
  }
  if (step === "success") {
    return "Listo";
  }
  if (step === "error") {
    return "Intentar otra vez";
  }
  return "Hablar ahora";
}

export function VoiceAssistantButton({
  step,
  listening,
  transcript,
  statusLine,
  detailLine,
  showConfirmActions,
  disabledStart,
  onStart,
  onConfirmSave,
  onRepeat,
}: VoiceAssistantButtonProps) {
  const busySpeaking =
    step === "speaking_greeting" || step === "speaking_confirmation";
  const showLoader =
    listening || step === "saving" || busySpeaking;

  return (
    <Card className="rounded-[2rem] border-none bg-[#0f6b6e] text-primary-foreground shadow-md ring-1 ring-white/20">
      <CardContent className="grid gap-5 px-6 py-6">
        <div className="flex items-start gap-3">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-white text-[#0f6b6e] shadow-sm">
            <Mic className="size-7" aria-hidden="true" />
          </div>
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary-foreground/75">
              Asistente de voz
            </p>
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
              Hablar con CuidaVoz
            </h2>
            <p className="mt-2 text-lg font-medium leading-7 text-primary-foreground/90 sm:text-xl">
              Puedes decir: mi presión es 120 sobre 70, ya tomé mi pastilla o necesito ayuda.
            </p>
          </div>
        </div>

        <Button
          type="button"
          className="h-auto min-h-20 rounded-3xl bg-white px-6 py-5 text-xl font-semibold text-[#0f6b6e] shadow-sm hover:bg-white/90 sm:text-2xl"
          onClick={onStart}
          disabled={disabledStart || listening || step === "saving" || busySpeaking}
        >
          {showLoader ? (
            <Loader2 className="size-6 animate-spin" aria-hidden="true" />
          ) : (
            <Volume2 className="size-6" aria-hidden="true" />
          )}
          {primaryLabel(step, listening)}
        </Button>

        {statusLine ? (
          <div className="rounded-3xl bg-white/12 px-5 py-4 ring-1 ring-white/10">
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-primary-foreground/70">
              Estado
            </p>
            <p className="mt-2 text-2xl font-semibold leading-8">{statusLine}</p>
            {detailLine ? (
              <p className="mt-3 text-xl font-medium leading-7 text-primary-foreground/90">
                {detailLine}
              </p>
            ) : null}
          </div>
        ) : null}

        {transcript ? (
          <div className="rounded-3xl bg-white/12 px-5 py-4 ring-1 ring-white/10">
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-primary-foreground/70">
              Te escuché
            </p>
            <p className="mt-2 text-2xl font-semibold leading-8">{transcript}</p>
          </div>
        ) : null}

        {showConfirmActions ? (
          <div className="grid gap-4 rounded-3xl bg-white px-5 py-5 text-[#0f6b6e]">
            <p className="text-xl font-semibold leading-tight sm:text-2xl">
              ¿Es correcto?
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Button
                type="button"
                className="h-16 rounded-3xl text-xl font-semibold"
                onClick={onConfirmSave}
                disabled={step === "saving"}
              >
                {step === "saving" ? (
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
                onClick={onRepeat}
                disabled={step === "saving"}
              >
                <RotateCcw className="size-6" aria-hidden="true" />
                Repetir
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
