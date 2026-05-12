"use client";

/* eslint-disable @next/next/no-img-element */

import Link from "next/link";
import {
  Activity,
  AlertCircle,
  ClipboardList,
  HeartPulse,
  Loader2,
  Pill,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { FamilyHealthSettings } from "@/components/family/family-health-settings";
import { FamilyMedicationManager } from "@/components/family/family-medication-manager";
import { PressureStatusBadge } from "@/components/pressure/pressure-status-badge";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { DailyRiskLevel, DailyStatus } from "@/features/daily-status/daily-status.types";
import type { PressureStatus } from "@/types";

type PatientApi = {
  id: string;
  fullName: string;
  age?: number | null;
  notes?: string | null;
};

type BloodPressureReadingApi = {
  id: string;
  systolic: number;
  diastolic: number;
  pulse?: number | null;
  status: PressureStatus;
  measuredAt: string;
  patient: PatientApi;
};

type MedicationScheduleApi = {
  id: string;
  time: string;
  isActive: boolean;
};

type MedicationApi = {
  id: string;
  name: string;
  dose: string;
  color?: string | null;
  shape?: string | null;
  instructions?: string | null;
  imageUrl?: string | null;
  schedules: MedicationScheduleApi[];
  patient: PatientApi;
};

type MedicationLogApi = {
  id: string;
  status: "TAKEN" | "MISSED" | "PENDING";
  scheduledFor: string;
  takenAt?: string | null;
  medication: {
    id: string;
    name: string;
    dose: string;
  };
};

type ApiResponse<T> = {
  data?: T;
  message?: string;
};

type DashboardData = {
  pressureReadings: BloodPressureReadingApi[];
  medications: MedicationApi[];
  medicationLogs: MedicationLogApi[];
  dailyStatus: DailyStatus;
};

type DashboardState =
  | { status: "loading"; data?: never; error?: never }
  | { status: "success"; data: DashboardData; error?: never }
  | { status: "error"; data?: never; error: string };

const dateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
});

const riskConfig: Record<
  DailyRiskLevel,
  { label: string; detail: string; className: string }
> = {
  low: {
    label: "Riesgo bajo",
    detail: "Sin alertas importantes hoy.",
    className: "bg-emerald-100 text-emerald-900 hover:bg-emerald-100",
  },
  medium: {
    label: "Riesgo medio",
    detail: "Hay datos incompletos o pastillas pendientes.",
    className: "bg-amber-100 text-amber-950 hover:bg-amber-100",
  },
  high: {
    label: "Riesgo alto",
    detail: "La presión requiere revisión cercana.",
    className: "bg-rose-100 text-rose-900 hover:bg-rose-100",
  },
};

const pressureStatusLabels = {
  NORMAL: "Normal",
  ELEVATED: "Elevada",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "No se pudo cargar el panel familiar.";
}

async function fetchJson<T>(url: string, signal?: AbortSignal) {
  const response = await fetch(url, {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok) {
    throw new Error(payload.message ?? "No se pudo cargar la información.");
  }

  return payload.data;
}

function latestPressure(readings: BloodPressureReadingApi[]) {
  return [...readings].sort(
    (a, b) =>
      new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime()
  )[0];
}

function latestLogs(logs: MedicationLogApi[]) {
  return [...logs].sort(
    (a, b) =>
      new Date(b.scheduledFor).getTime() - new Date(a.scheduledFor).getTime()
  );
}

function getPatient(data: DashboardData): PatientApi {
  return (
    data.dailyStatus.patient ??
    data.pressureReadings[0]?.patient ??
    data.medications[0]?.patient ?? {
      id: "patient_maria",
      fullName: "María Rojas",
    }
  );
}

function adherenceLabel(logs: MedicationLogApi[]) {
  if (logs.length === 0) {
    return {
      value: "Sin datos suficientes",
      detail: "Aún no hay tomas confirmadas.",
    };
  }

  const takenCount = logs.filter((log) => log.status === "TAKEN").length;
  const percentage = Math.round((takenCount / logs.length) * 100);

  return {
    value: `${percentage}%`,
    detail: `${takenCount} de ${logs.length} tomas confirmadas`,
  };
}

function intakeStatusLabel(status: MedicationLogApi["status"]) {
  if (status === "TAKEN") {
    return "Tomado";
  }

  if (status === "MISSED") {
    return "Omitido";
  }

  return "Pendiente";
}

function dailyMedicationStatusLabel(status: "TAKEN" | "PENDING") {
  return status === "TAKEN" ? "Tomado" : "Pendiente";
}

export function FamilyDashboard() {
  const [state, setState] = useState<DashboardState>({
    status: "loading",
  });

  const loadDashboard = useCallback(() => {
    const controller = new AbortController();

    Promise.all([
      fetchJson<BloodPressureReadingApi[]>("/api/blood-pressure", controller.signal),
      fetchJson<MedicationApi[]>("/api/medications", controller.signal),
      fetchJson<MedicationLogApi[]>("/api/medication-logs", controller.signal),
      fetchJson<DailyStatus>("/api/daily-status", controller.signal),
    ])
      .then(([pressureReadings, medications, medicationLogs, dailyStatus]) => {
        if (!dailyStatus) {
          throw new Error("No se pudo cargar el estado de hoy.");
        }

        setState({
          status: "success",
          data: {
            pressureReadings: pressureReadings ?? [],
            medications: medications ?? [],
            medicationLogs: medicationLogs ?? [],
            dailyStatus,
          },
        });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setState({
          status: "error",
          error: getErrorMessage(error),
        });
      });

    return controller;
  }, []);

  useEffect(() => {
    const controller = loadDashboard();

    return () => controller.abort();
  }, [loadDashboard]);

  if (state.status === "loading") {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <Loader2 className="size-9 animate-spin text-primary" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">Cargando panel familiar</h2>
            <p className="mt-2 text-base text-muted-foreground">
              Estamos reuniendo presión, medicamentos y tomas.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (state.status === "error") {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100">
            <AlertCircle className="size-7 text-rose-700" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-2xl font-semibold">No pudimos cargar el panel</h2>
            <p className="mt-2 max-w-md text-base text-muted-foreground">
              {state.error}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const patient = getPatient(state.data);
  const pressure = latestPressure(state.data.pressureReadings);
  const logs = latestLogs(state.data.medicationLogs);
  const adherence = adherenceLabel(logs);
  const { dailyStatus } = state.data;
  const risk = riskConfig[dailyStatus.summary.riskLevel];
  const pendingMedications = dailyStatus.medications.filter(
    (medication) => medication.statusToday === "PENDING"
  );
  const latestDailyPressure = dailyStatus.latestPressure;
  const isPressureHigh =
    latestDailyPressure?.status === "HIGH" ||
    latestDailyPressure?.status === "CRITICAL";
  const isOutOfRecommendedRange =
    latestDailyPressure?.personalizedStatus === "out_of_range";

  return (
    <div className="grid gap-5">
      <nav
        aria-label="Secciones del panel familiar"
        className="flex gap-2 overflow-x-auto rounded-full bg-white p-2 shadow-sm ring-1 ring-primary/10"
      >
        {[
          { href: "#resumen", label: "Resumen" },
          { href: "#medicamentos", label: "Medicamentos" },
          { href: "#rangos", label: "Rangos recomendados" },
          { href: "/reportes", label: "Reportes" },
        ].map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {item.label}
          </Link>
        ))}
      </nav>

      <section id="resumen" className="grid scroll-mt-6 gap-4 lg:grid-cols-[1.25fr_0.75fr]">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader className="space-y-3 pb-4">
            <div className="inline-flex w-fit items-center gap-2 rounded-full bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
              <UserRound className="size-4" aria-hidden="true" />
              Panel familiar
            </div>
            <CardTitle className="text-3xl tracking-tight">
              Seguimiento diario de {patient.fullName}
            </CardTitle>
          </CardHeader>
          <CardContent className="pb-6">
            <p className="text-lg font-semibold text-foreground/80">
              {patient.age ? `${patient.age} años` : "Paciente activa"}
            </p>
            {patient.notes ? (
              <p className="mt-2 text-base leading-7 text-muted-foreground">
                {patient.notes}
              </p>
            ) : (
              <p className="mt-2 text-base leading-7 text-muted-foreground">
                Seguimiento activo de salud y medicamentos.
              </p>
            )}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-[#0f6b6e] text-primary-foreground shadow-sm ring-0">
          <CardContent className="flex h-full flex-col justify-between gap-4 px-5 py-5">
            <div className="flex items-center gap-3">
              <ShieldCheck className="size-6" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em] text-primary-foreground/75">
                Adherencia
              </p>
            </div>
            <div>
              <p className="text-3xl font-semibold">{adherence.value}</p>
              <p className="mt-2 text-base text-primary-foreground/85">
                {adherence.detail}
              </p>
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <ShieldCheck className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Estado general
              </p>
            </div>
            <p className="mt-3 text-2xl font-semibold">{risk.label}</p>
            <p className="mt-2 text-sm text-muted-foreground">{risk.detail}</p>
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <HeartPulse className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Presión de hoy
              </p>
            </div>
            {dailyStatus.latestPressure &&
            dailyStatus.summary.hasPressureReadingToday ? (
              <>
                <p className="mt-3 text-3xl font-semibold">
                  {dailyStatus.latestPressure.systolic}/
                  {dailyStatus.latestPressure.diastolic}
                </p>
                <p className="mt-2 text-base font-medium text-muted-foreground">
                  {pressureStatusLabels[dailyStatus.latestPressure.status]}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {dateTimeFormatter.format(
                    new Date(dailyStatus.latestPressure.measuredAt)
                  )}
                </p>
                {dailyStatus.latestPressure.pulse ? (
                  <p className="mt-2 text-sm font-semibold text-muted-foreground">
                    Pulso: {dailyStatus.latestPressure.pulse} lpm
                  </p>
                ) : null}
              </>
            ) : (
              <p className="mt-3 text-lg font-semibold">
                Sin lectura registrada hoy
              </p>
            )}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Activity className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Pulso
              </p>
            </div>
            <p className="mt-3 text-2xl font-semibold">
              {dailyStatus.latestPressure?.pulse
                ? `${dailyStatus.latestPressure.pulse} lpm`
                : "Sin dato"}
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              Última lectura disponible
            </p>
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Pill className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Pastillas
              </p>
            </div>
            <p className="mt-3 text-2xl font-semibold">
              {dailyStatus.summary.takenMedications} tomado
              {dailyStatus.summary.takenMedications === 1 ? "" : "s"}
            </p>
            <p className="mt-2 text-base text-muted-foreground">
              {dailyStatus.summary.pendingMedications} pendiente
              {dailyStatus.summary.pendingMedications === 1 ? "" : "s"}
            </p>
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Activity className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Registro de hoy
              </p>
            </div>
            <p className="mt-3 text-2xl font-semibold">
              {dailyStatus.summary.hasPressureReadingToday
                ? "Presión registrada"
                : "Presión pendiente"}
            </p>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4">
        {isPressureHigh && latestDailyPressure ? (
          <Card className="rounded-[2rem] border-rose-200 bg-rose-50 shadow-sm">
            <CardContent className="px-5 py-5">
              <h2 className="text-2xl font-semibold text-rose-950">
                Presión alta detectada
              </h2>
              <p className="mt-2 text-lg text-rose-900">
                {latestDailyPressure.systolic}/{latestDailyPressure.diastolic} ·{" "}
                {pressureStatusLabels[latestDailyPressure.status]}
              </p>
              {latestDailyPressure.pulse ? (
                <p className="mt-1 text-base font-semibold text-rose-800">
                  Pulso: {latestDailyPressure.pulse} lpm
                </p>
              ) : null}
              <p className="mt-1 text-base text-rose-800">
                {dateTimeFormatter.format(new Date(latestDailyPressure.measuredAt))}
              </p>
              <p className="mt-3 text-base text-rose-900">
                Revisa cómo se siente y considera contactar a su familiar de apoyo o
                a su médico si la lectura se mantiene alta.
              </p>
            </CardContent>
          </Card>
        ) : null}

        {isOutOfRecommendedRange && latestDailyPressure ? (
          <Card className="rounded-[2rem] border-sky-200 bg-sky-50 shadow-sm">
            <CardContent className="px-5 py-5">
              <h2 className="text-2xl font-semibold text-sky-950">
                Fuera del rango recomendado por el médico.
              </h2>
              <p className="mt-2 text-base leading-7 text-sky-900">
                Última lectura: {latestDailyPressure.systolic}/
                {latestDailyPressure.diastolic}
                {latestDailyPressure.pulse
                  ? ` · Pulso ${latestDailyPressure.pulse} lpm`
                  : ""}
              </p>
              <p className="mt-3 text-base text-sky-900">
                Usa este dato como apoyo y confirma con el médico si se repite.
              </p>
            </CardContent>
          </Card>
        ) : null}

        {pendingMedications.length > 0 ? (
          <Card className="rounded-[2rem] border-amber-200 bg-amber-50 shadow-sm">
            <CardContent className="px-5 py-5">
              <div className="flex items-start gap-3">
                <Pill className="mt-1 size-6 text-amber-900" aria-hidden="true" />
                <div>
                  <h2 className="text-2xl font-semibold text-amber-950">
                    Hay pastillas pendientes
                  </h2>
                  <div className="mt-3 grid gap-2">
                    {pendingMedications.map((medication) => (
                      <p key={medication.id} className="text-lg text-amber-950">
                        {medication.name} · {medication.scheduleTime}
                      </p>
                    ))}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : !isPressureHigh ? (
          <Card className="rounded-[2rem] border-emerald-200 bg-emerald-50 shadow-sm">
            <CardContent className="px-5 py-5">
              <h2 className="text-2xl font-semibold text-emerald-950">
                Todo en orden por ahora.
              </h2>
              <p className="mt-2 text-base text-emerald-800">
                No hay alertas importantes en el estado diario.
              </p>
            </CardContent>
          </Card>
        ) : null}
      </section>

      <section className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Medicamentos</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 pb-6">
            {dailyStatus.medications.length === 0 ? (
              <EmptyMessage text="No hay medicamentos registrados." />
            ) : (
              dailyStatus.medications.map((medication) => (
                <div
                  key={medication.id}
                  className="rounded-2xl border border-border/80 bg-background p-4"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="flex items-start gap-3">
                      {medication.imageUrl ? (
                        <img
                          src={medication.imageUrl}
                          alt={`Imagen de ${medication.name}`}
                          className="h-14 w-14 shrink-0 rounded-2xl border border-border object-cover"
                        />
                      ) : (
                        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-muted">
                          <Pill className="size-6 text-primary" aria-hidden="true" />
                        </div>
                      )}
                      <div>
                        <p className="text-lg font-semibold">{medication.name}</p>
                        <p className="text-sm text-muted-foreground">
                          {medication.dose} · {medication.scheduleTime}
                        </p>
                      </div>
                    </div>
                    <Badge
                      className={
                        medication.statusToday === "TAKEN"
                          ? "rounded-full bg-emerald-100 px-3 py-1.5 text-sm text-emerald-800 hover:bg-emerald-100"
                          : "rounded-full bg-amber-100 px-3 py-1.5 text-sm text-amber-950 hover:bg-amber-100"
                      }
                    >
                      {dailyMedicationStatusLabel(medication.statusToday)}
                    </Badge>
                  </div>
                  <div className="mt-4 grid gap-2 text-base text-foreground/80">
                    {medication.color ? <p>Color: {medication.color}</p> : null}
                    {medication.shape ? <p>Forma: {medication.shape}</p> : null}
                    {medication.instructions ? (
                      <p>Instrucciones: {medication.instructions}</p>
                    ) : null}
                    {medication.takenAt ? (
                      <p>
                        Tomada hoy:{" "}
                        {dateTimeFormatter.format(new Date(medication.takenAt))}
                      </p>
                    ) : null}
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Últimas tomas</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 pb-6">
            {logs.length === 0 ? (
              <EmptyMessage text="Aún no hay tomas confirmadas." />
            ) : (
              logs.slice(0, 6).map((log) => (
                <div
                  key={log.id}
                  className="rounded-2xl border border-border/80 bg-background px-4 py-4"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-lg font-semibold">{log.medication.name}</p>
                      <p className="text-sm text-muted-foreground">
                        Programada:{" "}
                        {dateTimeFormatter.format(new Date(log.scheduledFor))}
                      </p>
                      {log.takenAt ? (
                        <p className="text-sm text-muted-foreground">
                          Tomada: {dateTimeFormatter.format(new Date(log.takenAt))}
                        </p>
                      ) : null}
                    </div>
                    <Badge className="rounded-full bg-emerald-100 px-3 py-1.5 text-sm text-emerald-800 hover:bg-emerald-100">
                      {intakeStatusLabel(log.status)}
                    </Badge>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </section>

      <FamilyMedicationManager
        onChanged={() => {
          loadDashboard();
        }}
      />

      <FamilyHealthSettings
        onChanged={() => {
          loadDashboard();
        }}
      />

      <section className="grid gap-5 lg:grid-cols-[0.95fr_1.05fr]">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Última presión</CardTitle>
          </CardHeader>
          <CardContent className="pb-6">
            {pressure ? (
              <>
                <p className="text-5xl font-semibold tracking-tight">
                  {pressure.systolic}/{pressure.diastolic}
                </p>
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <PressureStatusBadge status={pressure.status} />
                {pressure.pulse ? (
                  <Badge
                    variant="outline"
                    className="rounded-full px-3 py-1.5 text-sm"
                  >
                      Pulso: {pressure.pulse} lpm
                  </Badge>
                ) : null}
                </div>
                <p className="mt-4 text-base text-muted-foreground">
                  {dateTimeFormatter.format(new Date(pressure.measuredAt))}
                </p>
              </>
            ) : (
              <EmptyMessage text="Sin lecturas registradas." />
            )}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Acciones rápidas</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 pb-6 sm:grid-cols-3">
            {[
              { href: "/historial", label: "Ver historial" },
              { href: "/medicamentos", label: "Ver medicamentos" },
              { href: "/reportes", label: "Generar reporte" },
            ].map((action) => (
              <Link
                key={action.href}
                href={action.href}
                className="flex min-h-14 items-center justify-center rounded-2xl bg-primary px-4 py-3 text-center text-base font-semibold text-primary-foreground transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                {action.label}
              </Link>
            ))}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}

function EmptyMessage({ text }: { text: string }) {
  return (
    <div className="flex min-h-32 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border bg-background px-4 py-8 text-center">
      <ClipboardList className="size-7 text-muted-foreground" aria-hidden="true" />
      <p className="text-base font-medium text-muted-foreground">{text}</p>
    </div>
  );
}
