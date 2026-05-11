"use client";

import { AlertCircle, ClipboardList, Loader2, PlusCircle, RefreshCw } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { AddMedicationForm } from "@/components/medication/add-medication-form";
import { MedicationList } from "@/components/medication/medication-list";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  isValidMedicationTimeParts,
  to24HourTime,
  type MedicationTimePeriod,
} from "@/features/medications/time-format";

export type MedicationScheduleApi = {
  id: string;
  time: string;
  isActive: boolean;
  createdAt: string;
};

export type MedicationApi = {
  id: string;
  name: string;
  dose: string;
  color?: string | null;
  shape?: string | null;
  instructions?: string | null;
  imageUrl?: string | null;
  createdAt: string;
  schedules: MedicationScheduleApi[];
  patient: {
    id: string;
    fullName: string;
  };
};

export type MedicationFormValues = {
  name: string;
  dose: string;
  timeHour: string;
  timeMinute: string;
  timePeriod: MedicationTimePeriod;
  color: string;
  shape: string;
  instructions: string;
};

export type MedicationFormErrors = Partial<
  Record<keyof MedicationFormValues | "time" | "image" | "form", string>
>;

type MedicationResponse = {
  data?: MedicationApi[] | MedicationApi;
  message?: string;
  errors?: Record<string, unknown>;
};

type UploadResponse = {
  url?: string;
  message?: string;
};

type FetchState =
  | { status: "loading"; medications: MedicationApi[]; error?: never }
  | { status: "success"; medications: MedicationApi[]; error?: never }
  | { status: "error"; medications: MedicationApi[]; error: string };

const initialFormValues: MedicationFormValues = {
  name: "",
  dose: "",
  timeHour: "",
  timeMinute: "00",
  timePeriod: "AM",
  color: "",
  shape: "",
  instructions: "",
};

const allowedImageTypes = ["image/png", "image/jpeg", "image/webp"];
const maxImageSize = 5 * 1024 * 1024;
const invalidImageMessage =
  "Solo puedes subir imágenes PNG, JPG o WEBP de hasta 5 MB.";

function firstApiError(error: unknown) {
  if (Array.isArray(error) && typeof error[0] === "string") {
    return error[0];
  }

  return undefined;
}

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "No se pudo completar la acción.";
}

function validateForm(values: MedicationFormValues) {
  const errors: MedicationFormErrors = {};

  if (values.name.trim().length < 2) {
    errors.name = "Escribe un nombre de al menos 2 letras.";
  }

  if (!values.dose.trim()) {
    errors.dose = "La dosis es obligatoria.";
  }

  if (
    !isValidMedicationTimeParts({
      hour: values.timeHour,
      minute: values.timeMinute,
      period: values.timePeriod,
    })
  ) {
    errors.time = "La hora es obligatoria.";
  }

  return errors;
}

async function fetchMedications(signal?: AbortSignal) {
  const response = await fetch("/api/medications", {
    cache: "no-store",
    signal,
  });
  const payload = (await response.json()) as MedicationResponse;

  if (!response.ok) {
    throw new Error(payload.message ?? "No se pudo obtener la lista.");
  }

  return Array.isArray(payload.data) ? payload.data : [];
}

export function MedicationManagement() {
  const [state, setState] = useState<FetchState>({
    status: "loading",
    medications: [],
  });
  const [values, setValues] =
    useState<MedicationFormValues>(initialFormValues);
  const [formErrors, setFormErrors] = useState<MedicationFormErrors>({});
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);

  function updateValue(field: keyof MedicationFormValues, value: string) {
    setValues((current) => ({
      ...current,
      [field]: value,
    }));
    const isTimeField =
      field === "timeHour" || field === "timeMinute" || field === "timePeriod";
    setFormErrors((current) => ({
      ...current,
      [field]: undefined,
      time: isTimeField ? undefined : current.time,
      form: undefined,
    }));
    setSuccessMessage("");
  }

  function handleImageChange(file: File | null) {
    if (imagePreviewUrl) {
      URL.revokeObjectURL(imagePreviewUrl);
    }

    setSuccessMessage("");

    if (!file) {
      setImageFile(null);
      setImagePreviewUrl(null);
      setFormErrors((current) => ({
        ...current,
        image: undefined,
        form: undefined,
      }));
      return;
    }

    if (!allowedImageTypes.includes(file.type) || file.size > maxImageSize) {
      setImageFile(null);
      setImagePreviewUrl(null);
      setFormErrors((current) => ({
        ...current,
        image: invalidImageMessage,
        form: invalidImageMessage,
      }));
      return;
    }

    setImageFile(file);
    setImagePreviewUrl(URL.createObjectURL(file));
    setFormErrors((current) => ({
      ...current,
      image: undefined,
      form: undefined,
    }));
  }

  async function refreshMedications() {
    setState((current) => ({
      status: "loading",
      medications: current.medications,
    }));

    try {
      const medications = await fetchMedications();
      setState({
        status: "success",
        medications,
      });
    } catch (error) {
      setState({
        status: "error",
        medications: [],
        error: getErrorMessage(error),
      });
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = validateForm(values);

    if (formErrors.image) {
      setFormErrors({
        ...validationErrors,
        image: formErrors.image,
      });
      setSuccessMessage("");
      return;
    }

    if (Object.keys(validationErrors).length > 0) {
      setFormErrors(validationErrors);
      setSuccessMessage("");
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});
    setSuccessMessage("");

    try {
      let imageUrl: string | undefined;

      if (imageFile) {
        const imageFormData = new FormData();
        imageFormData.append("image", imageFile);

        const uploadResponse = await fetch("/api/upload/medication-image", {
          method: "POST",
          body: imageFormData,
        });
        const uploadPayload = (await uploadResponse.json()) as UploadResponse;

        if (!uploadResponse.ok || !uploadPayload.url) {
          setFormErrors({
            form: uploadPayload.message ?? invalidImageMessage,
          });
          return;
        }

        imageUrl = uploadPayload.url;
      }

      const response = await fetch("/api/medications", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          patientId: "patient_maria",
          name: values.name.trim(),
          dose: values.dose.trim(),
          time: to24HourTime(
            values.timeHour,
            values.timeMinute,
            values.timePeriod
          ),
          color: values.color.trim() || undefined,
          shape: values.shape.trim() || undefined,
          instructions: values.instructions.trim() || undefined,
          imageUrl,
        }),
      });
      const payload = (await response.json()) as MedicationResponse;

      if (!response.ok) {
        setFormErrors({
          name: firstApiError(payload.errors?.name),
          dose: firstApiError(payload.errors?.dose),
          time: firstApiError(payload.errors?.time),
          color: firstApiError(payload.errors?.color),
          shape: firstApiError(payload.errors?.shape),
          instructions: firstApiError(payload.errors?.instructions),
          form: payload.message ?? "No se pudo registrar el medicamento.",
        });
        return;
      }

      setValues(initialFormValues);
      setImageFile(null);
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
      setImagePreviewUrl(null);
      setSuccessMessage(
        payload.message ?? "Medicamento registrado correctamente."
      );
      await refreshMedications();
    } catch {
      setFormErrors({
        form: "No se pudo conectar con medicamentos. Intenta otra vez.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();

    fetchMedications(controller.signal)
      .then((medications) => {
        setState({
          status: "success",
          medications,
        });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setState({
          status: "error",
          medications: [],
          error: getErrorMessage(error),
        });
      });

    return () => controller.abort();
  }, []);

  useEffect(() => {
    return () => {
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  return (
    <div className="grid gap-5 lg:grid-cols-[1.05fr_0.95fr]">
      <section className="space-y-4">
        {state.status === "loading" && state.medications.length === 0 ? (
          <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
            <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
              <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
              <div>
                <h2 className="text-2xl font-semibold">Cargando medicamentos</h2>
                <p className="mt-2 text-base text-muted-foreground">
                  Estamos buscando la lista de María.
                </p>
              </div>
            </CardContent>
          </Card>
        ) : null}

        {state.status === "error" ? (
          <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
            <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100">
                <AlertCircle className="size-7 text-rose-700" aria-hidden="true" />
              </div>
              <div>
                <h2 className="text-2xl font-semibold">No pudimos cargar la lista</h2>
                <p className="mt-2 max-w-md text-base text-muted-foreground">
                  {state.error}
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="h-12 rounded-2xl px-5 text-base"
                onClick={() => void refreshMedications()}
              >
                <RefreshCw className="size-4" aria-hidden="true" />
                Reintentar
              </Button>
            </CardContent>
          </Card>
        ) : null}

        {state.status === "success" && state.medications.length === 0 ? (
          <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
            <CardContent className="flex min-h-64 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary">
                <ClipboardList className="size-7 text-primary" aria-hidden="true" />
              </div>
              <div>
                <h2 className="text-2xl font-semibold">Aún no hay medicamentos</h2>
                <p className="mt-2 max-w-md text-base text-muted-foreground">
                  Agrega el primer medicamento para verlo en esta lista.
                </p>
              </div>
            </CardContent>
          </Card>
        ) : null}

        {state.medications.length > 0 ? (
          <MedicationList medications={state.medications} />
        ) : null}
      </section>

      <AddMedicationForm
        values={values}
        errors={formErrors}
        successMessage={successMessage}
        isSubmitting={isSubmitting}
        onChange={updateValue}
        onSubmit={handleSubmit}
        submitIcon={
          isSubmitting ? (
            <Loader2 className="size-5 animate-spin" aria-hidden="true" />
          ) : (
            <PlusCircle className="size-5" aria-hidden="true" />
          )
        }
        imagePreviewUrl={imagePreviewUrl}
        onImageChange={handleImageChange}
      />
    </div>
  );
}
