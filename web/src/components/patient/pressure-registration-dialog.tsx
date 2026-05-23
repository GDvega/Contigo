"use client";

import { Loader2, Save, X } from "lucide-react";
import { FormEvent, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type PressureRegistrationDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

type FormValues = {
  systolic: string;
  diastolic: string;
  pulse: string;
  notes: string;
};

type FieldErrors = Partial<Record<keyof FormValues | "form", string>>;

const initialValues: FormValues = {
  systolic: "",
  diastolic: "",
  pulse: "",
  notes: "",
};

function validate(values: FormValues) {
  const errors: FieldErrors = {};
  const systolic = Number(values.systolic);
  const diastolic = Number(values.diastolic);
  const pulse = values.pulse ? Number(values.pulse) : undefined;

  if (!values.systolic) {
    errors.systolic = "Escribe el número de arriba.";
  } else if (!Number.isInteger(systolic) || systolic < 60 || systolic > 250) {
    errors.systolic = "Debe estar entre 60 y 250.";
  }

  if (!values.diastolic) {
    errors.diastolic = "Escribe el número de abajo.";
  } else if (!Number.isInteger(diastolic) || diastolic < 40 || diastolic > 160) {
    errors.diastolic = "Debe estar entre 40 y 160.";
  }

  if (
    values.pulse &&
    (!Number.isInteger(pulse) || pulse === undefined || pulse < 30 || pulse > 220)
  ) {
    errors.pulse = "Debe estar entre 30 y 220.";
  }

  return errors;
}

function firstApiError(error: unknown) {
  if (Array.isArray(error) && typeof error[0] === "string") {
    return error[0];
  }

  return undefined;
}

export function PressureRegistrationDialog({
  open,
  onOpenChange,
}: PressureRegistrationDialogProps) {
  const [values, setValues] = useState<FormValues>(initialValues);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateValue(field: keyof FormValues, value: string) {
    setValues((current) => ({
      ...current,
      [field]: value,
    }));
    setErrors((current) => ({
      ...current,
      [field]: undefined,
      form: undefined,
    }));
    setSuccessMessage("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = validate(values);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      setSuccessMessage("");
      return;
    }

    setIsSubmitting(true);
    setErrors({});
    setSuccessMessage("");

    const body = {
      patientId: "patient_maria",
      systolic: Number(values.systolic),
      diastolic: Number(values.diastolic),
      ...(values.pulse ? { pulse: Number(values.pulse) } : {}),
      ...(values.notes.trim() ? { notes: values.notes.trim() } : {}),
    };

    try {
      const response = await fetch("/api/blood-pressure", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
      const payload = (await response.json()) as {
        message?: string;
        errors?: Record<string, unknown>;
      };

      if (!response.ok) {
        setErrors({
          systolic: firstApiError(payload.errors?.systolic),
          diastolic: firstApiError(payload.errors?.diastolic),
          pulse: firstApiError(payload.errors?.pulse),
          notes: firstApiError(payload.errors?.notes),
          form: payload.message ?? "No se pudo guardar la presión.",
        });
        return;
      }

      setValues(initialValues);
      setSuccessMessage(
        values.pulse
          ? `Presión registrada correctamente. Pulso: ${Number(values.pulse)} lpm.`
          : "Presión registrada correctamente."
      );
    } catch {
      setErrors({
        form: "No se pudo conectar con el registro de presión. Intenta otra vez.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] max-w-2xl overflow-y-auto rounded-3xl p-5 sm:p-7">
        <DialogHeader className="pr-10">
          <DialogTitle className="text-3xl leading-tight sm:text-4xl">
            Registrar presión
          </DialogTitle>
          <DialogDescription className="text-lg leading-7">
            Escribe los números del tensiómetro.
          </DialogDescription>
        </DialogHeader>

        <form className="grid gap-5" onSubmit={handleSubmit}>
          {errors.form ? (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-lg font-semibold text-rose-800">
              {errors.form}
            </div>
          ) : null}

          {successMessage ? (
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-lg font-semibold text-emerald-800">
              {successMessage}
            </div>
          ) : null}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="systolic" className="text-xl">
                Presión alta
              </Label>
              <Input
                id="systolic"
                inputMode="numeric"
                value={values.systolic}
                onChange={(event) => updateValue("systolic", event.target.value)}
                placeholder="Ej. 130"
                aria-invalid={Boolean(errors.systolic)}
                className="h-18 rounded-3xl px-5 text-3xl font-semibold md:text-3xl"
              />
              {errors.systolic ? (
                <p className="text-lg font-semibold text-rose-700">
                  {errors.systolic}
                </p>
              ) : null}
            </div>

            <div className="grid gap-2">
              <Label htmlFor="diastolic" className="text-xl">
                Presión baja
              </Label>
              <Input
                id="diastolic"
                inputMode="numeric"
                value={values.diastolic}
                onChange={(event) => updateValue("diastolic", event.target.value)}
                placeholder="Ej. 85"
                aria-invalid={Boolean(errors.diastolic)}
                className="h-18 rounded-3xl px-5 text-3xl font-semibold md:text-3xl"
              />
              {errors.diastolic ? (
                <p className="text-lg font-semibold text-rose-700">
                  {errors.diastolic}
                </p>
              ) : null}
            </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="pulse" className="text-xl">
              Pulso
            </Label>
            <Input
              id="pulse"
              inputMode="numeric"
              value={values.pulse}
              onChange={(event) => updateValue("pulse", event.target.value)}
              placeholder="Ej. 72"
              aria-invalid={Boolean(errors.pulse)}
              className="h-16 rounded-3xl px-5 text-2xl font-semibold md:text-2xl"
            />
            <p className="text-base font-semibold text-muted-foreground">Opcional</p>
            {errors.pulse ? (
              <p className="text-lg font-semibold text-rose-700">{errors.pulse}</p>
            ) : null}
          </div>

          <div className="grid gap-2">
            <Label htmlFor="notes" className="text-xl">
              Nota
            </Label>
            <Textarea
              id="notes"
              value={values.notes}
              onChange={(event) => updateValue("notes", event.target.value)}
              placeholder="Ej. Medición después del desayuno"
              aria-invalid={Boolean(errors.notes)}
              className="min-h-28 rounded-3xl px-5 py-4 text-xl md:text-xl"
            />
            {errors.notes ? (
              <p className="text-lg font-semibold text-rose-700">{errors.notes}</p>
            ) : null}
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="h-16 rounded-3xl text-xl font-semibold"
            >
              {isSubmitting ? (
                <Loader2 className="size-6 animate-spin" aria-hidden="true" />
              ) : (
                <Save className="size-6" aria-hidden="true" />
              )}
              Guardar presión
            </Button>
            <Button
              type="button"
              variant="outline"
              className="h-16 rounded-3xl text-xl font-semibold"
              onClick={() => onOpenChange(false)}
            >
              <X className="size-6" aria-hidden="true" />
              Cerrar
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
