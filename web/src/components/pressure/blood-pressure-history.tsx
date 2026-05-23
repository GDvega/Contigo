"use client";

import { AlertCircle, ClipboardList, Loader2, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { BloodPressureReadingApi } from "@/types";

import { PressureHistoryGroup } from "./pressure-history-group";
import { PressureSummaryCards } from "./pressure-summary-cards";

type BloodPressureResponse = {
  data?: BloodPressureReadingApi[];
  message?: string;
};

type FetchState =
  | { status: "loading"; readings: BloodPressureReadingApi[]; error?: never }
  | { status: "success"; readings: BloodPressureReadingApi[]; error?: never }
  | { status: "error"; readings: BloodPressureReadingApi[]; error: string };

const dateGroupFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "full",
});

function groupReadingsByDate(readings: BloodPressureReadingApi[]) {
  return Object.entries(
    readings.reduce<Record<string, BloodPressureReadingApi[]>>((acc, reading) => {
      const date = dateGroupFormatter.format(new Date(reading.measuredAt));
      acc[date] ??= [];
      acc[date].push(reading);
      return acc;
    }, {})
  );
}

async function fetchBloodPressureReadings(signal?: AbortSignal) {
  const response = await fetch("/api/blood-pressure", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as BloodPressureResponse;

  if (!response.ok) {
    throw new Error(
      payload.message ?? "No se pudo obtener el historial de presión arterial."
    );
  }

  return payload.data ?? [];
}

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "No se pudo obtener el historial de presión arterial.";
}

export function BloodPressureHistory() {
  const [state, setState] = useState<FetchState>({
    status: "loading",
    readings: [],
  });

  async function loadReadings() {
    setState((current) => ({
      status: "loading",
      readings: current.readings,
    }));

    try {
      const readings = await fetchBloodPressureReadings();

      setState({
        status: "success",
        readings,
      });
    } catch (error) {
      setState({
        status: "error",
        readings: [],
        error: getErrorMessage(error),
      });
    }
  }

  useEffect(() => {
    const controller = new AbortController();

    fetchBloodPressureReadings(controller.signal)
      .then((readings) => {
        setState({
          status: "success",
          readings,
        });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setState({
          status: "error",
          readings: [],
          error: getErrorMessage(error),
        });
      });

    return () => controller.abort();
  }, []);

  if (state.status === "loading" && state.readings.length === 0) {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">Cargando historial</h2>
            <p className="mt-2 text-base text-muted-foreground">
              Estamos buscando las lecturas registradas.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (state.status === "error") {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100">
            <AlertCircle className="size-7 text-rose-700" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-2xl font-semibold">No pudimos cargar el historial</h2>
            <p className="mt-2 max-w-md text-base text-muted-foreground">
              {state.error}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            className="h-12 rounded-2xl px-5 text-base"
            onClick={() => void loadReadings()}
          >
            <RefreshCw className="size-4" aria-hidden="true" />
            Reintentar
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (state.readings.length === 0) {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary">
            <ClipboardList className="size-7 text-primary" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-2xl font-semibold">Aún no hay lecturas</h2>
            <p className="mt-2 max-w-md text-base text-muted-foreground">
              Cuando se registre una presión arterial, aparecerá aquí con fecha,
              paciente y estado.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const groupedReadings = groupReadingsByDate(state.readings);

  return (
    <>
      <PressureSummaryCards readings={state.readings} />

      <section className="grid gap-4">
        {groupedReadings.map(([date, readings]) => (
          <PressureHistoryGroup key={date} date={date} readings={readings} />
        ))}
      </section>
    </>
  );
}
