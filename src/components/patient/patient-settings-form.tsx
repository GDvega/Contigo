"use client";

import { AlertCircle, CheckCircle2, Loader2, Save, UserRound } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type PatientApi = {
  id: string;
  fullName: string;
  age: number | null;
  notes: string | null;
};

type PatientResponse = {
  message?: string;
  data?: PatientApi;
  errors?: Record<string, string[] | undefined>;
};

type FormState = {
  fullName: string;
  age: string;
  notes: string;
};

type LoadState =
  | { status: "loading"; error?: never }
  | { status: "ready"; error?: never }
  | { status: "error"; error: string };

function patientToForm(patient: PatientApi): FormState {
  return {
    fullName: patient.fullName,
    age: patient.age?.toString() ?? "",
    notes: patient.notes ?? "",
  };
}

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "No se pudo cargar la configuración.";
}

async function fetchPatient(signal?: AbortSignal) {
  const response = await fetch("/api/patient", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as PatientResponse;

  if (!response.ok || !payload.data) {
    throw new Error(payload.message ?? "No se pudo obtener el paciente.");
  }

  return payload.data;
}

export function PatientSettingsForm() {
  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [form, setForm] = useState<FormState>({
    fullName: "",
    age: "",
    notes: "",
  });
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[] | undefined>
  >({});

  useEffect(() => {
    const controller = new AbortController();

    fetchPatient(controller.signal)
      .then((patient) => {
        setForm(patientToForm(patient));
        setLoadState({ status: "ready" });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setLoadState({
          status: "error",
          error: getErrorMessage(error),
        });
      });

    return () => controller.abort();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setMessage(null);
    setFieldErrors({});

    try {
      const response = await fetch("/api/patient", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          fullName: form.fullName,
          age: form.age ? Number(form.age) : null,
          notes: form.notes,
        }),
      });
      const payload = (await response.json()) as PatientResponse;

      if (!response.ok) {
        setFieldErrors(payload.errors ?? {});
        throw new Error(payload.message ?? "No se pudo guardar el paciente.");
      }

      if (payload.data) {
        setForm(patientToForm(payload.data));
      }

      setMessage({
        type: "success",
        text: payload.message ?? "Paciente actualizado correctamente.",
      });
    } catch (error: unknown) {
      setMessage({
        type: "error",
        text:
          error instanceof Error
            ? error.message
            : "No se pudo guardar el paciente.",
      });
    } finally {
      setIsSaving(false);
    }
  }

  if (loadState.status === "loading") {
    return (
      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <Loader2 className="size-9 animate-spin text-primary" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">Cargando configuración</h2>
            <p className="mt-2 text-base text-muted-foreground">
              Estamos buscando los datos de María.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (loadState.status === "error") {
    return (
      <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
        <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
          <AlertCircle className="size-9 text-rose-700" aria-hidden="true" />
          <div>
            <h2 className="text-2xl font-semibold">No pudimos cargar el perfil</h2>
            <p className="mt-2 text-base text-muted-foreground">
              {loadState.error}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
      <CardHeader className="space-y-3">
        <div className="inline-flex w-fit items-center gap-2 rounded-full bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
          <UserRound className="size-4" aria-hidden="true" />
          Perfil del paciente
        </div>
        <CardTitle className="text-3xl">Configuración de María</CardTitle>
      </CardHeader>
      <CardContent className="pb-6">
        <form className="space-y-5" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <Label htmlFor="fullName" className="text-base">
              Nombre completo
            </Label>
            <Input
              id="fullName"
              className="min-h-14 rounded-2xl bg-[#fffdfa] px-4 text-lg md:text-lg"
              value={form.fullName}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  fullName: event.target.value,
                }))
              }
              placeholder="Ej. María Rojas"
              aria-invalid={Boolean(fieldErrors.fullName?.length)}
            />
            {fieldErrors.fullName?.[0] ? (
              <p className="text-sm font-medium text-destructive">
                {fieldErrors.fullName[0]}
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="age" className="text-base">
              Edad
            </Label>
            <Input
              id="age"
              type="number"
              min={1}
              max={120}
              className="min-h-14 rounded-2xl bg-[#fffdfa] px-4 text-lg md:text-lg"
              value={form.age}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  age: event.target.value,
                }))
              }
              placeholder="Ej. 72"
              aria-invalid={Boolean(fieldErrors.age?.length)}
            />
            {fieldErrors.age?.[0] ? (
              <p className="text-sm font-medium text-destructive">
                {fieldErrors.age[0]}
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes" className="text-base">
              Notas
            </Label>
            <Textarea
              id="notes"
              className="min-h-32 rounded-2xl bg-[#fffdfa] px-4 py-4 text-lg md:text-lg"
              value={form.notes}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  notes: event.target.value,
                }))
              }
              placeholder="Ej. Vive con su hija y prefiere recordatorios por voz."
              aria-invalid={Boolean(fieldErrors.notes?.length)}
            />
            {fieldErrors.notes?.[0] ? (
              <p className="text-sm font-medium text-destructive">
                {fieldErrors.notes[0]}
              </p>
            ) : null}
          </div>

          {message ? (
            <div
              className={
                message.type === "success"
                  ? "flex items-center gap-3 rounded-3xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-base font-semibold text-emerald-800"
                  : "flex items-center gap-3 rounded-3xl border border-rose-200 bg-rose-50 px-4 py-4 text-base font-semibold text-rose-800"
              }
            >
              {message.type === "success" ? (
                <CheckCircle2 className="size-5" aria-hidden="true" />
              ) : (
                <AlertCircle className="size-5" aria-hidden="true" />
              )}
              {message.text}
            </div>
          ) : null}

          <Button
            type="submit"
            className="min-h-14 w-full rounded-2xl text-lg font-semibold"
            disabled={isSaving}
          >
            {isSaving ? (
              <>
                <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                Guardando...
              </>
            ) : (
              <>
                <Save className="size-5" aria-hidden="true" />
                Guardar cambios
              </>
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
