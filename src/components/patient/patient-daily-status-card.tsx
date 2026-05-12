"use client";

import {
  AlertCircle,
  ChevronDown,
  CheckCircle2,
  HeartPulse,
  Loader2,
  Pill,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Card, CardContent } from "@/components/ui/card";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";
import { useSpeechSynthesis } from "@/features/voice/useSpeechSynthesis";

type ApiResponse = {
  data?: DailyStatus;
  message?: string;
};

type DailyStatusState =
  | { status: "loading"; data?: never; error?: never }
  | { status: "success"; data: DailyStatus; error?: never }
  | { status: "error"; data?: never; error: string };

type ReminderMedication = DailyStatus["medications"][number];

const riskMessages = {
  low: {
    label: "Todo en orden",
    className: "border-emerald-200 bg-emerald-50 text-emerald-900",
  },
  medium: {
    label: "Revisa tus pendientes",
    className: "border-amber-200 bg-amber-50 text-amber-950",
  },
  high: {
    label: "Atención: revisa tu presión",
    className: "border-rose-200 bg-rose-50 text-rose-900",
  },
};

const pressureStatusLabels = {
  NORMAL: "Normal",
  ELEVATED: "Elevada",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};

const dateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
});

function getLatestPressureDetail(status: DailyStatus) {
  if (!status.latestPressure) {
    return null;
  }

  return {
    value: `${status.latestPressure.systolic}/${status.latestPressure.diastolic}`,
    statusLabel: pressureStatusLabels[status.latestPressure.status],
    measuredAt: dateTimeFormatter.format(new Date(status.latestPressure.measuredAt)),
  };
}

async function fetchDailyStatus(signal?: AbortSignal) {
  const response = await fetch("/api/daily-status", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as ApiResponse;

  if (!response.ok || !payload.data) {
    throw new Error(payload.message ?? "No se pudo cargar el estado de hoy.");
  }

  return payload.data;
}

function minutesFromScheduleTime(time: string) {
  const normalizedTime = time.trim().toUpperCase();
  const match = normalizedTime.match(/^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/);

  if (!match) {
    return null;
  }

  let hours = Number(match[1]);
  const minutes = Number(match[2]);
  const meridiem = match[3];

  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return null;
  }

  if (meridiem === "PM" && hours < 12) {
    hours += 12;
  }

  if (meridiem === "AM" && hours === 12) {
    hours = 0;
  }

  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
    return null;
  }

  return hours * 60 + minutes;
}

function isMedicationDue(medication: ReminderMedication, now: Date) {
  if (medication.statusToday !== "PENDING" || !medication.scheduleTime) {
    return false;
  }

  const scheduleMinutes = minutesFromScheduleTime(medication.scheduleTime);

  if (scheduleMinutes === null) {
    return false;
  }

  return now.getHours() * 60 + now.getMinutes() >= scheduleMinutes;
}

function buildReminderSpeech(medication: ReminderMedication) {
  const details = [medication.dose, medication.color, medication.shape]
    .filter(Boolean)
    .join(", ");

  return `Es hora de tomar tu ${medication.name}.${details ? ` ${details}.` : ""}`;
}

export function PatientDailyStatusCard() {
  const [state, setState] = useState<DailyStatusState>({
    status: "loading",
  });
  const [now, setNow] = useState(() => new Date());
  const [isExpanded, setIsExpanded] = useState(false);
  const spokenReminderIdsRef = useRef(new Set<string>());
  const spokenPressureReminderRef = useRef(false);
  const { speakAsync } = useSpeechSynthesis();

  const loadDailyStatus = useCallback((signal?: AbortSignal) => {
    return fetchDailyStatus(signal)
      .then((data) => {
        setState({
          status: "success",
          data,
        });
        return data;
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return undefined;
        }

        setState({
          status: "error",
          error:
            error instanceof Error
              ? error.message
              : "No se pudo cargar el estado de hoy.",
        });
        return undefined;
      });
  }, []);

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

  const dueMedication = useMemo(() => {
    if (state.status !== "success") {
      return null;
    }

    return (
      state.data.medications.find((medication) => isMedicationDue(medication, now)) ??
      null
    );
  }, [now, state]);

  useEffect(() => {
    if (state.status !== "success") {
      return;
    }

    const dailyStatus = state.data;
    let cancelled = false;

    async function announceReminders() {
      if (dueMedication && !spokenReminderIdsRef.current.has(dueMedication.id)) {
        spokenReminderIdsRef.current.add(dueMedication.id);
        await speakAsync(buildReminderSpeech(dueMedication));
      }

      if (
        !cancelled &&
        !dailyStatus.summary.hasPressureReadingToday &&
        !spokenPressureReminderRef.current
      ) {
        spokenPressureReminderRef.current = true;
        await speakAsync(
          "Aún no registras tu presión de hoy. Puedes registrarla por voz o manualmente."
        );
      }
    }

    void announceReminders();

    return () => {
      cancelled = true;
    };
  }, [dueMedication, speakAsync, state]);

  if (state.status === "loading") {
    return (
      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardContent className="flex items-center gap-4 px-5 py-5">
          <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
          <div>
            <p className="text-lg font-semibold text-muted-foreground">
              Estado de hoy
            </p>
            <p className="text-2xl font-semibold">Revisando pendientes...</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (state.status === "error") {
    return (
      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-amber-200">
        <CardContent className="flex items-center gap-4 px-5 py-5">
          <AlertCircle className="size-8 text-amber-700" aria-hidden="true" />
          <div>
            <p className="text-lg font-semibold text-muted-foreground">
              Estado de hoy
            </p>
            <p className="text-xl font-semibold text-amber-900">{state.error}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const risk = riskMessages[state.data.summary.riskLevel];
  const latestPressureDetail = getLatestPressureDetail(state.data);

  return (
    <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
      <CardContent className="space-y-5 px-5 py-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-base font-semibold uppercase tracking-[0.16em] text-primary">
              Estado de hoy
            </p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight">
              {risk.label}
            </h2>
          </div>
          <div
            className={`rounded-full border px-4 py-2 text-sm font-semibold ${risk.className}`}
          >
            {state.data.summary.riskLevel === "low"
              ? "Bajo"
              : state.data.summary.riskLevel === "medium"
                ? "Medio"
                : "Alto"}
          </div>
        </div>

        <button
          type="button"
          aria-expanded={isExpanded}
          className="flex min-h-14 w-full items-center justify-between gap-3 rounded-3xl bg-[#f5f8fb] px-4 py-3 text-left text-lg font-semibold text-primary transition-colors hover:bg-[#edf5f9] focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          onClick={() => setIsExpanded((current) => !current)}
        >
          <span>{isExpanded ? "Ocultar detalles" : "Ver detalles"}</span>
          <ChevronDown
            className={`size-6 transition-transform ${
              isExpanded ? "rotate-180" : ""
            }`}
            aria-hidden="true"
          />
        </button>

        {isExpanded ? (
          <div className="grid gap-3">
            <div className="flex items-center justify-between gap-3 rounded-3xl bg-[#eef7f3] px-4 py-4">
              <div className="flex items-center gap-3">
                <Pill className="size-7 text-primary" aria-hidden="true" />
                <p className="text-xl font-semibold">Pastillas</p>
              </div>
              <p className="text-right text-xl font-semibold text-primary">
                {state.data.summary.pendingMedications > 0
                  ? `${state.data.summary.pendingMedications} pendiente${
                      state.data.summary.pendingMedications === 1 ? "" : "s"
                    }`
                  : "Todas tomadas"}
              </p>
            </div>

            <div className="grid gap-2 rounded-3xl bg-[#f5f8fb] px-4 py-4">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <HeartPulse className="size-7 text-primary" aria-hidden="true" />
                  <p className="text-xl font-semibold">Presión</p>
                </div>
                <p className="text-right text-xl font-semibold">
                  {state.data.summary.hasPressureReadingToday && latestPressureDetail
                    ? `Registrada ${latestPressureDetail.value}`
                    : "Pendiente de registrar"}
                </p>
              </div>

              {latestPressureDetail ? (
                <p className="text-right text-base font-medium text-muted-foreground">
                  {latestPressureDetail.value} · {latestPressureDetail.statusLabel}
                  {state.data.latestPressure?.pulse
                    ? ` · Pulso ${state.data.latestPressure.pulse} lpm`
                    : ""}
                  {" · "}
                  {latestPressureDetail.measuredAt}
                </p>
              ) : null}
            </div>

            {state.data.latestPressure?.personalizedStatus === "out_of_range" ? (
              <div className="rounded-3xl border border-sky-200 bg-sky-50 px-4 py-4">
                <p className="text-xl font-semibold text-sky-950">
                  Tu presión está fuera del rango recomendado por tu médico.
                </p>
                <p className="mt-2 text-base text-sky-900">
                  Si te sientes mal, avisa a tu familiar o consulta a tu médico.
                </p>
              </div>
            ) : null}

            <div
              className={`flex items-center gap-3 rounded-3xl border px-4 py-4 ${risk.className}`}
            >
              <CheckCircle2 className="size-7" aria-hidden="true" />
              <p className="text-2xl font-semibold">{risk.label}</p>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
