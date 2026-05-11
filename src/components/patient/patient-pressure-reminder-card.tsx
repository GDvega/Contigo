"use client";

import { HeartPulse, Loader2, Volume2 } from "lucide-react";
import { useEffect, useState } from "react";

import { PressureRegistrationDialog } from "@/components/patient/pressure-registration-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";

type ApiResponse = {
  data?: DailyStatus;
  message?: string;
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

function scrollToVoiceAssistant() {
  document.getElementById("voice-assistant-section")?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
}

export function PatientPressureReminderCard() {
  const [dailyStatus, setDailyStatus] = useState<DailyStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isPressureDialogOpen, setIsPressureDialogOpen] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetchDailyStatus(controller.signal)
      .then((data) => setDailyStatus(data))
      .catch(() => setDailyStatus(null))
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, []);

  if (isLoading) {
    return (
      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardContent className="flex items-center gap-4 px-5 py-5">
          <Loader2 className="size-7 animate-spin text-primary" aria-hidden="true" />
          <p className="text-xl font-semibold">Revisando presión de hoy...</p>
        </CardContent>
      </Card>
    );
  }

  if (!dailyStatus || dailyStatus.summary.hasPressureReadingToday) {
    return null;
  }

  const latestPressure = dailyStatus.latestPressure;

  return (
    <>
      <Card className="rounded-[2rem] border border-sky-200 bg-[#edf7fb] shadow-sm">
        <CardContent className="space-y-5 px-5 py-6">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-3xl bg-white text-sky-800 shadow-sm">
              <HeartPulse className="size-7" aria-hidden="true" />
            </div>
            <div>
              <p className="text-base font-semibold uppercase tracking-[0.16em] text-sky-900">
                Presión arterial
              </p>
              <h2 className="mt-2 text-3xl font-semibold tracking-tight text-sky-950 sm:text-4xl">
                Aún no registras tu presión hoy
              </h2>
            </div>
          </div>

          <p className="text-xl font-semibold leading-8 text-sky-950 sm:text-2xl">
            Registra tu presión para llevar un mejor control.
          </p>

          {latestPressure ? (
            <div className="rounded-3xl bg-white px-5 py-4 shadow-sm">
              <p className="text-base font-semibold text-muted-foreground">
                Última presión
              </p>
              <p className="mt-2 text-2xl font-semibold">
                {latestPressure.systolic}/{latestPressure.diastolic} ·{" "}
                {pressureStatusLabels[latestPressure.status]}
              </p>
              {latestPressure.pulse ? (
                <p className="mt-1 text-lg font-semibold text-muted-foreground">
                  Pulso: {latestPressure.pulse} lpm
                </p>
              ) : null}
              <p className="mt-1 text-base text-muted-foreground">
                {dateTimeFormatter.format(new Date(latestPressure.measuredAt))}
              </p>
            </div>
          ) : null}

          <div className="grid gap-3 sm:grid-cols-2">
            <Button
              type="button"
              className="min-h-20 rounded-3xl text-xl font-semibold"
              onClick={scrollToVoiceAssistant}
            >
              <Volume2 className="size-6" aria-hidden="true" />
              Registrar presión
            </Button>
            <Button
              type="button"
              variant="outline"
              className="min-h-20 rounded-3xl bg-white text-xl font-semibold"
              onClick={() => setIsPressureDialogOpen(true)}
            >
              <HeartPulse className="size-6" aria-hidden="true" />
              Registrar manualmente
            </Button>
          </div>
        </CardContent>
      </Card>

      <PressureRegistrationDialog
        open={isPressureDialogOpen}
        onOpenChange={setIsPressureDialogOpen}
      />
    </>
  );
}
