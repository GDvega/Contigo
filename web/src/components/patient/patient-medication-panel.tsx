"use client";

import dynamic from "next/dynamic";
import { AlertCircle, ClipboardList, Loader2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Card, CardContent } from "@/components/ui/card";
import { NextMedicationCard } from "@/components/patient/next-medication-card";
import { PatientActions } from "@/components/patient/patient-actions";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";
import {
  getMedicationToRegister,
  isMedicationDue,
  scheduledForTodayIso,
} from "@/features/daily-status/medication-scheduling";

const VoiceAssistantClient = dynamic(
  () =>
    import("@/components/voice/VoiceAssistantClient").then(
      (mod) => mod.VoiceAssistantClient
    ),
  {
    ssr: false,
    loading: () => (
      <div className="rounded-[2rem] bg-[#0f6b6e] px-6 py-6 text-primary-foreground shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.16em] opacity-90">
          Asistente
        </p>
        <p className="mt-2 text-2xl font-semibold">Preparando la voz…</p>
      </div>
    ),
  }
);

type DailyStatusResponse = {
  data?: DailyStatus;
  message?: string;
};

type FetchState =
  | { status: "loading"; dailyStatus?: never; error?: never }
  | { status: "success"; dailyStatus: DailyStatus; error?: never }
  | { status: "error"; dailyStatus?: never; error: string };

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "No se pudo completar la acción.";
}

async function fetchDailyStatus(signal?: AbortSignal) {
  const response = await fetch("/api/daily-status", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as DailyStatusResponse;

  if (!response.ok || !payload.data) {
    throw new Error(payload.message ?? "No se pudo obtener el estado de hoy.");
  }

  return payload.data;
}

export function PatientMedicationPanel() {
  const [state, setState] = useState<FetchState>({
    status: "loading",
  });
  const [now, setNow] = useState(() => new Date());
  const [isSavingIntake, setIsSavingIntake] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "info" | "error";
    message: string;
  } | null>(null);

  const loadDailyStatus = useCallback((signal?: AbortSignal) => {
    return fetchDailyStatus(signal)
      .then((dailyStatus) => {
        setState({
          status: "success",
          dailyStatus,
        });
        return dailyStatus;
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return undefined;
        }

        setState({
          status: "error",
          error: getErrorMessage(error),
        });
        return undefined;
      });
  }, []);

  const selectedMedication = useMemo(() => {
    if (state.status !== "success") {
      return null;
    }

    return getMedicationToRegister(state.dailyStatus.medications, now);
  }, [now, state]);
  const hasConfiguredMedications =
    state.status === "success" && state.dailyStatus.medications.length > 0;
  const isSelectedMedicationDue = selectedMedication
    ? isMedicationDue(selectedMedication, now)
    : false;

  async function handleMedicationTaken() {
    if (
      state.status === "success" &&
      state.dailyStatus.medications.length > 0 &&
      !selectedMedication
    ) {
      setFeedback({
        type: "info",
        message: "Todas tus pastillas de hoy ya fueron registradas.",
      });
      return;
    }

    if (!selectedMedication) {
      setFeedback({
        type: "error",
        message: "No se pudo registrar la toma. Inténtalo otra vez.",
      });
      return;
    }

    setIsSavingIntake(true);
    setFeedback(null);

    try {
      const response = await fetch("/api/medication-logs", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          medicationId: selectedMedication.id,
          scheduledFor: scheduledForTodayIso(selectedMedication.scheduleTime),
        }),
      });
      const payload = (await response.json()) as {
        message?: string;
        duplicate?: boolean;
      };

      if (!response.ok) {
        throw new Error(payload.message ?? "No se pudo registrar la toma.");
      }

      if (payload.duplicate) {
        setFeedback({
          type: "info",
          message: "Esta toma ya fue registrada.",
        });
      } else {
        setFeedback({
          type: "success",
          message: `Listo. Registré que tomaste ${selectedMedication.name}.`,
        });
      }

      await loadDailyStatus();
    } catch {
      setFeedback({
        type: "error",
        message: "No se pudo registrar la toma. Inténtalo otra vez.",
      });
    } finally {
      setIsSavingIntake(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();

    void loadDailyStatus(controller.signal);

    return () => controller.abort();
  }, [loadDailyStatus]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setNow(new Date());
    }, 60_000);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <section className="grid gap-5">
      {state.status === "loading" ? (
        <Card className="rounded-3xl border-none shadow-sm">
          <CardContent className="flex min-h-44 flex-col items-center justify-center gap-4 px-6 py-8 text-center">
            <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
            <p className="text-2xl font-semibold">Buscando tu pastilla</p>
          </CardContent>
        </Card>
      ) : null}

      {state.status === "error" ? (
        <Card className="rounded-3xl border-none shadow-sm">
          <CardContent className="flex min-h-44 flex-col items-center justify-center gap-4 px-6 py-8 text-center">
            <AlertCircle className="size-8 text-rose-700" aria-hidden="true" />
            <div>
              <p className="text-2xl font-semibold">No pudimos cargar la pastilla</p>
              <p className="mt-2 text-lg text-muted-foreground">{state.error}</p>
            </div>
          </CardContent>
        </Card>
      ) : null}

      {state.status === "success" && state.dailyStatus.medications.length === 0 ? (
        <Card className="rounded-3xl border-none shadow-sm">
          <CardContent className="flex min-h-44 flex-col items-center justify-center gap-4 px-6 py-8 text-center">
            <ClipboardList className="size-8 text-primary" aria-hidden="true" />
            <div>
              <p className="text-2xl font-semibold">
                No hay medicamentos configurados.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : null}

      {state.status === "success" &&
      state.dailyStatus.medications.length > 0 &&
      !selectedMedication ? (
        <Card className="rounded-3xl border-none shadow-sm">
          <CardContent className="flex min-h-44 flex-col items-center justify-center gap-4 px-6 py-8 text-center">
            <ClipboardList className="size-8 text-primary" aria-hidden="true" />
            <div>
              <p className="text-2xl font-semibold">
                Todas tus pastillas de hoy ya fueron registradas.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : null}

      {selectedMedication ? (
        <NextMedicationCard
          title={
            isSelectedMedicationDue
              ? "Es hora de tomar tu pastilla"
              : "Siguiente pastilla pendiente"
          }
          urgent={isSelectedMedicationDue}
          medication={{
            name: selectedMedication.name,
            dose: selectedMedication.dose,
            time: selectedMedication.scheduleTime,
            color: selectedMedication.color,
            shape: selectedMedication.shape,
            instructions: selectedMedication.instructions,
            imageUrl: selectedMedication.imageUrl,
          }}
        />
      ) : null}

      <section id="voice-assistant-section" className="scroll-mt-6">
        <VoiceAssistantClient />
      </section>

      <PatientActions
        canConfirmMedication={hasConfiguredMedications}
        isConfirmingMedication={isSavingIntake}
        medicationActionLabel={
          selectedMedication
            ? isSelectedMedicationDue
              ? "Ya tomé mi pastilla"
              : `Registrar toma de ${selectedMedication.name}`
            : "Ya tomé mi pastilla"
        }
        onConfirmMedication={() => void handleMedicationTaken()}
      />

      {feedback ? (
        <div
          className={
            feedback.type === "success"
              ? "rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-xl font-semibold text-emerald-800"
              : feedback.type === "info"
                ? "rounded-3xl border border-amber-200 bg-amber-50 px-5 py-4 text-xl font-semibold text-amber-900"
              : "rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-xl font-semibold text-rose-800"
          }
        >
          {feedback.message}
        </div>
      ) : null}
    </section>
  );
}
