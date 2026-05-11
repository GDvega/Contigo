"use client";

import { AlertTriangle, Loader2, Save } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type HealthSettings = {
  id?: string;
  systolicMinNormal: number | null;
  systolicMaxNormal: number | null;
  diastolicMinNormal: number | null;
  diastolicMaxNormal: number | null;
  pulseMinNormal: number | null;
  pulseMaxNormal: number | null;
  doctorRecommendation: string | null;
};

type HealthSettingsFormValues = Record<keyof HealthSettings, string>;

type ApiResponse = {
  data?: HealthSettings | null;
  message?: string;
  errors?: Record<string, string[] | undefined>;
};

type FamilyHealthSettingsProps = {
  onChanged?: () => void;
};

const initialValues: HealthSettingsFormValues = {
  id: "",
  systolicMinNormal: "",
  systolicMaxNormal: "",
  diastolicMinNormal: "",
  diastolicMaxNormal: "",
  pulseMinNormal: "",
  pulseMaxNormal: "",
  doctorRecommendation: "",
};

function toValues(settings: HealthSettings | null | undefined) {
  if (!settings) {
    return initialValues;
  }

  return {
    id: settings.id ?? "",
    systolicMinNormal: settings.systolicMinNormal?.toString() ?? "",
    systolicMaxNormal: settings.systolicMaxNormal?.toString() ?? "",
    diastolicMinNormal: settings.diastolicMinNormal?.toString() ?? "",
    diastolicMaxNormal: settings.diastolicMaxNormal?.toString() ?? "",
    pulseMinNormal: settings.pulseMinNormal?.toString() ?? "",
    pulseMaxNormal: settings.pulseMaxNormal?.toString() ?? "",
    doctorRecommendation: settings.doctorRecommendation ?? "",
  };
}

function toNullableNumber(value: string) {
  return value.trim() ? Number(value) : null;
}

async function fetchSettings() {
  const response = await fetch("/api/patient-health-settings", {
    cache: "no-store",
  });
  const payload = (await response.json()) as ApiResponse;

  if (!response.ok) {
    throw new Error(payload.message ?? "No se pudieron cargar los rangos.");
  }

  return payload.data ?? null;
}

export function FamilyHealthSettings({ onChanged }: FamilyHealthSettingsProps) {
  const [values, setValues] = useState<HealthSettingsFormValues>(initialValues);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  useEffect(() => {
    fetchSettings()
      .then((settings) => setValues(toValues(settings)))
      .catch((error: unknown) => {
        setFeedback({
          type: "error",
          message:
            error instanceof Error
              ? error.message
              : "No se pudieron cargar los rangos.",
        });
      })
      .finally(() => setIsLoading(false));
  }, []);

  function updateValue(field: keyof HealthSettingsFormValues, value: string) {
    setValues((current) => ({
      ...current,
      [field]: value,
    }));
    setFeedback(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const response = await fetch("/api/patient-health-settings", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          systolicMinNormal: toNullableNumber(values.systolicMinNormal),
          systolicMaxNormal: toNullableNumber(values.systolicMaxNormal),
          diastolicMinNormal: toNullableNumber(values.diastolicMinNormal),
          diastolicMaxNormal: toNullableNumber(values.diastolicMaxNormal),
          pulseMinNormal: toNullableNumber(values.pulseMinNormal),
          pulseMaxNormal: toNullableNumber(values.pulseMaxNormal),
          doctorRecommendation: values.doctorRecommendation.trim() || null,
        }),
      });
      const payload = (await response.json()) as ApiResponse;

      if (!response.ok) {
        throw new Error(
          payload.message ?? "No se pudieron actualizar los rangos."
        );
      }

      setValues(toValues(payload.data));
      setFeedback({
        type: "success",
        message: "Rangos actualizados correctamente.",
      });
      onChanged?.();
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudieron actualizar los rangos.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section id="rangos" className="grid gap-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Rangos recomendados
        </p>
        <h2 className="mt-2 text-3xl font-semibold tracking-tight">
          Rangos indicados por el médico
        </h2>
        <p className="mt-2 max-w-3xl text-base leading-7 text-muted-foreground">
          Configura los rangos indicados por el médico para interpretar mejor la
          presión y el pulso.
        </p>
      </div>

      <Card className="rounded-[2rem] border-amber-200 bg-amber-50 shadow-sm">
        <CardContent className="flex items-start gap-4 px-5 py-5">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white text-amber-800">
            <AlertTriangle className="size-6" aria-hidden="true" />
          </div>
          <div>
            <h3 className="text-xl font-semibold text-amber-950">Importante</h3>
            <p className="mt-2 text-base leading-7 text-amber-900">
              Estos rangos deben configurarse según la recomendación del médico.
              CuidaVoz no reemplaza una consulta médica.
            </p>
            <p className="mt-2 text-base leading-7 text-amber-900">
              CuidaVoz es una herramienta de apoyo y seguimiento. No reemplaza la
              evaluación de un profesional de salud.
            </p>
          </div>
        </CardContent>
      </Card>

      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardHeader>
          <CardTitle className="text-2xl">Configurar rangos</CardTitle>
        </CardHeader>
        <CardContent className="pb-6">
          {isLoading ? (
            <div className="flex min-h-40 items-center justify-center gap-3 text-muted-foreground">
              <Loader2 className="size-5 animate-spin" aria-hidden="true" />
              Cargando rangos...
            </div>
          ) : (
            <form className="grid gap-5" onSubmit={handleSubmit}>
              {feedback ? (
                <div
                  className={
                    feedback.type === "success"
                      ? "rounded-3xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-base font-semibold text-emerald-800"
                      : "rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-base font-semibold text-rose-800"
                  }
                >
                  {feedback.message}
                </div>
              ) : null}

              <RangeFields
                title="Presión sistólica"
                minId="systolicMinNormal"
                maxId="systolicMaxNormal"
                minValue={values.systolicMinNormal}
                maxValue={values.systolicMaxNormal}
                onChange={updateValue}
              />
              <RangeFields
                title="Presión diastólica"
                minId="diastolicMinNormal"
                maxId="diastolicMaxNormal"
                minValue={values.diastolicMinNormal}
                maxValue={values.diastolicMaxNormal}
                onChange={updateValue}
              />
              <RangeFields
                title="Pulso"
                minId="pulseMinNormal"
                maxId="pulseMaxNormal"
                minValue={values.pulseMinNormal}
                maxValue={values.pulseMaxNormal}
                onChange={updateValue}
              />

              <div className="grid gap-2">
                <Label htmlFor="doctorRecommendation" className="text-base">
                  Recomendación del médico
                </Label>
                <Textarea
                  id="doctorRecommendation"
                  value={values.doctorRecommendation}
                  onChange={(event) =>
                    updateValue("doctorRecommendation", event.target.value)
                  }
                  className="min-h-28 rounded-2xl bg-[#fffdfa] px-4 py-3 text-base"
                  placeholder="Ej. El cardiólogo indicó mantener la presión alrededor de 120/80 y controlar si supera 140/90."
                />
              </div>

              <Button
                type="submit"
                disabled={isSubmitting}
                className="min-h-14 w-full rounded-2xl text-lg font-semibold sm:w-fit sm:px-8"
              >
                {isSubmitting ? (
                  <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                ) : (
                  <Save className="size-5" aria-hidden="true" />
                )}
                Guardar rangos
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </section>
  );
}

function RangeFields({
  title,
  minId,
  maxId,
  minValue,
  maxValue,
  onChange,
}: {
  title: string;
  minId: keyof HealthSettingsFormValues;
  maxId: keyof HealthSettingsFormValues;
  minValue: string;
  maxValue: string;
  onChange: (field: keyof HealthSettingsFormValues, value: string) => void;
}) {
  return (
    <fieldset className="grid gap-3 rounded-3xl border border-border/70 bg-[#fffdfa] p-4">
      <legend className="px-1 text-base font-semibold">{title}</legend>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="grid gap-2">
          <Label htmlFor={minId} className="text-base">
            Mínima normal
          </Label>
          <Input
            id={minId}
            type="number"
            value={minValue}
            onChange={(event) => onChange(minId, event.target.value)}
            className="h-14 rounded-2xl bg-white px-4 text-base"
          />
        </div>
        <div className="grid gap-2">
          <Label htmlFor={maxId} className="text-base">
            Máxima normal
          </Label>
          <Input
            id={maxId}
            type="number"
            value={maxValue}
            onChange={(event) => onChange(maxId, event.target.value)}
            className="h-14 rounded-2xl bg-white px-4 text-base"
          />
        </div>
      </div>
    </fieldset>
  );
}
