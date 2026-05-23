"use client";

import {
  AlertCircle,
  Download,
  FileText,
  HeartPulse,
  Loader2,
  Pill,
  ShieldCheck,
  SquareChartGantt,
} from "lucide-react";
import { useEffect, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { MedicalReportData } from "@/features/reports/medical-report.service";

type ReportResponse = {
  data?: MedicalReportData;
  message?: string;
};

type ReportState =
  | { status: "loading"; data?: never; error?: never }
  | { status: "success"; data: MedicalReportData; error?: never }
  | { status: "error"; data?: never; error: string };

const dateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
});

const riskLabels = {
  low: "Bajo",
  medium: "Medio",
  high: "Alto",
};

const previewItems = [
  "Datos del paciente y fecha de generación",
  "Resumen y tabla de presión arterial",
  "Medicamentos, horarios e instrucciones",
  "Tomas confirmadas y estado diario",
];

function formatRange(min: number | null | undefined, max: number | null | undefined) {
  if (min == null && max == null) {
    return "No configurado";
  }

  if (min != null && max != null) {
    return `${min}–${max}`;
  }

  if (min != null) {
    return `Desde ${min}`;
  }

  return `Hasta ${max}`;
}

async function fetchReport(signal?: AbortSignal) {
  const response = await fetch("/api/reports/medical-summary", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as ReportResponse;

  if (!response.ok || !payload.data) {
    throw new Error(payload.message ?? "No se pudo cargar el reporte.");
  }

  return payload.data;
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export function ReportsDashboard() {
  const [state, setState] = useState<ReportState>({ status: "loading" });
  const [isGeneratingPdf, setIsGeneratingPdf] = useState(false);
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [pdfError, setPdfError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    fetchReport(controller.signal)
      .then((data) => {
        setState({ status: "success", data });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setState({
          status: "error",
          error:
            error instanceof Error
              ? error.message
              : "No se pudo cargar el reporte.",
        });
      });

    return () => controller.abort();
  }, []);

  async function handleGeneratePdf() {
    setIsGeneratingPdf(true);
    setPdfError(null);

    try {
      const response = await fetch("/api/reports/medical-summary?format=pdf", {
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error("No se pudo generar el PDF.");
      }

      setPdfBlob(await response.blob());
    } catch (error: unknown) {
      setPdfError(
        error instanceof Error ? error.message : "No se pudo generar el PDF."
      );
      setPdfBlob(null);
    } finally {
      setIsGeneratingPdf(false);
    }
  }

  if (state.status === "loading") {
    return (
      <Card className="rounded-3xl border-none">
        <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <Loader2 className="size-9 animate-spin text-primary" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">Cargando reporte</h2>
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
          <AlertCircle className="size-9 text-rose-700" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">No pudimos cargar el reporte</h2>
            <p className="mt-2 text-base text-muted-foreground">{state.error}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const { data } = state;
  const latestPressure = data.bloodPressure.latestReading;

  return (
    <div className="grid gap-5">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <HeartPulse className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Lecturas
              </p>
            </div>
            <p className="mt-2 text-3xl font-semibold">
              {data.bloodPressure.readings.length}
            </p>
            {latestPressure ? (
              <p className="mt-2 text-sm text-muted-foreground">
                Última: {latestPressure.systolic}/{latestPressure.diastolic}
              </p>
            ) : null}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <SquareChartGantt className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Pulso
              </p>
            </div>
            <p className="mt-2 text-3xl font-semibold">
              {latestPressure?.pulse ? `${latestPressure.pulse} lpm` : "Sin dato"}
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
                Medicamentos
              </p>
            </div>
            <p className="mt-2 text-3xl font-semibold">{data.medications.length}</p>
            <p className="mt-2 text-sm text-muted-foreground">
              {data.medicationAdherence.takenLogsCount} tomas confirmadas
            </p>
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardContent className="px-5 py-5">
            <div className="flex items-center gap-2 text-muted-foreground">
              <ShieldCheck className="size-5 text-primary" aria-hidden="true" />
              <p className="text-sm font-semibold uppercase tracking-[0.15em]">
                Riesgo
              </p>
            </div>
            <p className="mt-2 text-3xl font-semibold">
              {riskLabels[data.dailyStatus.riskLevel]}
            </p>
            <p className="mt-2 text-sm text-muted-foreground">
              {data.dailyStatus.pendingMedications} medicamentos pendientes
            </p>
          </CardContent>
        </Card>
      </div>

      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardHeader className="gap-3">
          <CardTitle className="text-2xl">Periodo</CardTitle>
          <div className="flex flex-wrap gap-2">
            <Badge className="h-auto rounded-full px-4 py-2 text-sm">
              Últimas 30 lecturas
            </Badge>
            <Badge variant="outline" className="h-auto rounded-full px-4 py-2 text-sm">
              Últimas 50 tomas
            </Badge>
            <Badge variant="outline" className="h-auto rounded-full px-4 py-2 text-sm">
              Generado: {dateTimeFormatter.format(new Date(data.generatedAt))}
            </Badge>
          </div>
        </CardHeader>
      </Card>

      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardHeader>
          <CardTitle className="text-2xl">Rangos recomendados</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 pb-6 sm:grid-cols-3">
          <div className="rounded-3xl bg-[#fffdfa] px-4 py-4">
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-muted-foreground">
              Sistólica normal
            </p>
            <p className="mt-2 text-xl font-semibold">
              {formatRange(
                data.healthSettings?.systolicMinNormal,
                data.healthSettings?.systolicMaxNormal
              )}
            </p>
          </div>
          <div className="rounded-3xl bg-[#fffdfa] px-4 py-4">
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-muted-foreground">
              Diastólica normal
            </p>
            <p className="mt-2 text-xl font-semibold">
              {formatRange(
                data.healthSettings?.diastolicMinNormal,
                data.healthSettings?.diastolicMaxNormal
              )}
            </p>
          </div>
          <div className="rounded-3xl bg-[#fffdfa] px-4 py-4">
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-muted-foreground">
              Pulso normal
            </p>
            <p className="mt-2 text-xl font-semibold">
              {formatRange(
                data.healthSettings?.pulseMinNormal,
                data.healthSettings?.pulseMaxNormal
              )}
            </p>
          </div>
          {data.healthSettings?.doctorRecommendation ? (
            <p className="sm:col-span-3 rounded-3xl bg-sky-50 px-4 py-4 text-base leading-7 text-sky-950">
              {data.healthSettings.doctorRecommendation}
            </p>
          ) : null}
        </CardContent>
      </Card>

      <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-2xl">
              <FileText className="size-5 text-primary" aria-hidden="true" />
              Vista previa del reporte
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 pb-6">
            {previewItems.map((item) => (
              <div
                key={item}
                className="flex items-center gap-3 rounded-3xl border border-border/70 bg-[#fffdfa] px-4 py-4 shadow-sm"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-secondary">
                  <SquareChartGantt
                    className="size-4 text-primary"
                    aria-hidden="true"
                  />
                </div>
                <p className="text-base font-medium">{item}</p>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-[#0f6b6e] text-primary-foreground shadow-sm ring-0">
          <CardHeader>
            <CardTitle className="text-2xl">Generación de archivo</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 pb-6">
            <p className="text-base leading-7 text-primary-foreground/85">
              El PDF se genera con datos reales de María: presión, medicamentos,
              tomas confirmadas y estado diario.
            </p>

            {pdfError ? (
              <div className="rounded-2xl bg-white/15 px-4 py-3 text-sm font-semibold">
                {pdfError}
              </div>
            ) : null}

            {pdfBlob ? (
              <div className="rounded-2xl bg-white/15 px-4 py-3 text-sm font-semibold">
                PDF listo para descargar.
              </div>
            ) : (
              <div className="rounded-2xl bg-white/10 px-4 py-3 text-sm font-semibold text-primary-foreground/85">
                Primero genera el reporte para habilitar la descarga.
              </div>
            )}

            <Button
              className="min-h-14 w-full rounded-2xl bg-white text-primary hover:bg-white/90"
              disabled={isGeneratingPdf}
              onClick={() => void handleGeneratePdf()}
            >
              {isGeneratingPdf ? (
                <>
                  <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                  Generando...
                </>
              ) : (
                <>
                  <FileText className="size-5" aria-hidden="true" />
                  Generar reporte PDF
                </>
              )}
            </Button>

            <Button
              className="min-h-14 w-full rounded-2xl bg-white/15 text-white hover:bg-white/20"
              disabled={!pdfBlob || isGeneratingPdf}
              onClick={() => {
                if (!pdfBlob) {
                  return;
                }

                downloadBlob(pdfBlob, "reporte-medico-cuidavoz.pdf");
              }}
            >
              <Download className="size-5" aria-hidden="true" />
              Descargar PDF
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
