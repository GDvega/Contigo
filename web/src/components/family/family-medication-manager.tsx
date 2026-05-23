"use client";

/* eslint-disable @next/next/no-img-element */

import { ImagePlus, Loader2, Pencil, Pill, PlusCircle, Trash2 } from "lucide-react";
import { ChangeEvent, FormEvent, useEffect, useId, useState } from "react";

import { MedicationTimeSelector } from "@/components/medication/medication-time-selector";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  from24HourTime,
  isValidMedicationTimeParts,
  to24HourTime,
  type MedicationTimePeriod,
} from "@/features/medications/time-format";

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
  patient: {
    id: string;
    fullName: string;
  };
};

type MedicationFormValues = {
  name: string;
  dose: string;
  timeHour: string;
  timeMinute: string;
  timePeriod: MedicationTimePeriod;
  color: string;
  shape: string;
  instructions: string;
  imageUrl: string | null;
};

type ApiResponse<T> = {
  data?: T;
  message?: string;
  errors?: Record<string, string[] | undefined>;
};

type UploadResponse = {
  url?: string;
  message?: string;
};

type FamilyMedicationManagerProps = {
  onChanged?: () => void;
};

const initialFormValues: MedicationFormValues = {
  name: "",
  dose: "",
  timeHour: "",
  timeMinute: "00",
  timePeriod: "AM",
  color: "",
  shape: "",
  instructions: "",
  imageUrl: null,
};

const allowedImageTypes = ["image/png", "image/jpeg", "image/webp"];
const maxImageSize = 5 * 1024 * 1024;
const invalidImageMessage =
  "Solo puedes subir imágenes PNG, JPG o WEBP de hasta 5 MB.";

function activeTime(medication: MedicationApi) {
  return (
    medication.schedules.find((schedule) => schedule.isActive)?.time ??
    medication.schedules[0]?.time ??
    ""
  );
}

function toFormValues(medication: MedicationApi): MedicationFormValues {
  const time = from24HourTime(activeTime(medication));

  return {
    name: medication.name,
    dose: medication.dose,
    timeHour: time.hour,
    timeMinute: time.minute,
    timePeriod: time.period,
    color: medication.color ?? "",
    shape: medication.shape ?? "",
    instructions: medication.instructions ?? "",
    imageUrl: medication.imageUrl ?? null,
  };
}

function validateImage(file: File) {
  return allowedImageTypes.includes(file.type) && file.size <= maxImageSize;
}

function hasValidTime(values: MedicationFormValues) {
  return isValidMedicationTimeParts({
    hour: values.timeHour,
    minute: values.timeMinute,
    period: values.timePeriod,
  });
}

async function uploadImage(file: File) {
  const formData = new FormData();
  formData.append("image", file);

  const response = await fetch("/api/upload/medication-image", {
    method: "POST",
    body: formData,
  });
  const payload = (await response.json()) as UploadResponse;

  if (!response.ok || !payload.url) {
    throw new Error(payload.message ?? invalidImageMessage);
  }

  return payload.url;
}

async function fetchMedications() {
  const response = await fetch("/api/medications", {
    cache: "no-store",
  });
  const payload = (await response.json()) as ApiResponse<MedicationApi[]>;

  if (!response.ok) {
    throw new Error(payload.message ?? "No se pudo cargar medicamentos.");
  }

  return payload.data ?? [];
}

function MedicationImage({
  imageUrl,
  name,
  size = "large",
}: {
  imageUrl?: string | null;
  name: string;
  size?: "large" | "small";
}) {
  const className =
    size === "large"
      ? "h-20 w-20 rounded-2xl"
      : "h-16 w-16 rounded-2xl";

  if (imageUrl) {
    return (
      <img
        src={imageUrl}
        alt={`Imagen de ${name}`}
        className={`${className} shrink-0 border border-border object-cover`}
      />
    );
  }

  return (
    <div
      className={`${className} flex shrink-0 items-center justify-center bg-secondary`}
    >
      <Pill className="size-7 text-primary" aria-hidden="true" />
    </div>
  );
}

function MedicationFields({
  values,
  imagePreviewUrl,
  error,
  onChange,
  onImageChange,
  onRemoveImage,
}: {
  values: MedicationFormValues;
  imagePreviewUrl: string | null;
  error?: string;
  onChange: (field: keyof MedicationFormValues, value: string | null) => void;
  onImageChange: (file: File | null) => void;
  onRemoveImage: () => void;
}) {
  const fileInputId = useId();

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    onImageChange(event.target.files?.[0] ?? null);
  }

  return (
    <div className="grid gap-4">
      <div className="grid gap-2">
        <Label htmlFor={`${fileInputId}-name`} className="text-base">
          Nombre
        </Label>
        <Input
          id={`${fileInputId}-name`}
          value={values.name}
          onChange={(event) => onChange("name", event.target.value)}
          className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
          placeholder="Ej. Losartán"
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="grid gap-2">
          <Label htmlFor={`${fileInputId}-dose`} className="text-base">
            Dosis
          </Label>
          <Input
            id={`${fileInputId}-dose`}
            value={values.dose}
            onChange={(event) => onChange("dose", event.target.value)}
            className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            placeholder="Ej. 1 pastilla"
          />
        </div>
        <MedicationTimeSelector
          idPrefix={`${fileInputId}-time`}
          value={{
            hour: values.timeHour,
            minute: values.timeMinute,
            period: values.timePeriod,
          }}
          onChange={(nextTime) => {
            onChange("timeHour", nextTime.hour);
            onChange("timeMinute", nextTime.minute);
            onChange("timePeriod", nextTime.period);
          }}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="grid gap-2">
          <Label htmlFor={`${fileInputId}-color`} className="text-base">
            Color
          </Label>
          <Input
            id={`${fileInputId}-color`}
            value={values.color}
            onChange={(event) => onChange("color", event.target.value)}
            className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            placeholder="Ej. Blanca"
          />
        </div>
        <div className="grid gap-2">
          <Label htmlFor={`${fileInputId}-shape`} className="text-base">
            Forma
          </Label>
          <Input
            id={`${fileInputId}-shape`}
            value={values.shape}
            onChange={(event) => onChange("shape", event.target.value)}
            className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            placeholder="Ej. Redonda"
          />
        </div>
      </div>
      <div className="grid gap-2">
        <Label htmlFor={`${fileInputId}-instructions`} className="text-base">
          Instrucciones
        </Label>
        <Textarea
          id={`${fileInputId}-instructions`}
          value={values.instructions}
          onChange={(event) => onChange("instructions", event.target.value)}
          className="min-h-24 rounded-2xl bg-[#fffdfa] px-4 py-3 text-base"
          placeholder="Ej. Tomar con agua después del desayuno"
        />
      </div>
      <div className="grid gap-3 rounded-3xl border border-border/70 bg-[#fffdfa] p-4">
        <div>
          <Label htmlFor={fileInputId} className="text-base">
            Imagen del medicamento
          </Label>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            Puedes subir una foto de la pastilla, caja o blíster para reconocerla
            mejor.
          </p>
        </div>
        <div className="grid gap-3 sm:grid-cols-[auto_1fr] sm:items-center">
          {imagePreviewUrl || values.imageUrl ? (
            <img
              src={imagePreviewUrl ?? values.imageUrl ?? ""}
              alt="Vista previa del medicamento"
              className="h-24 w-24 rounded-2xl border border-border object-cover"
            />
          ) : (
            <div className="flex h-24 w-24 items-center justify-center rounded-2xl border border-dashed border-primary/30 bg-secondary">
              <ImagePlus className="size-8 text-primary" aria-hidden="true" />
            </div>
          )}
          <div className="grid gap-2">
            <Input
              id={fileInputId}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={handleFileChange}
              className="hidden"
            />
            <Button
              type="button"
              variant="outline"
              className="min-h-14 rounded-2xl bg-white text-base font-semibold"
              onClick={() => document.getElementById(fileInputId)?.click()}
            >
              <ImagePlus className="size-5" aria-hidden="true" />
              Subir imagen
            </Button>
            {imagePreviewUrl || values.imageUrl ? (
              <Button
                type="button"
                variant="ghost"
                className="min-h-12 rounded-2xl text-base font-semibold"
                onClick={onRemoveImage}
              >
                Quitar imagen
              </Button>
            ) : null}
          </div>
        </div>
        {error ? <p className="text-sm font-semibold text-rose-700">{error}</p> : null}
      </div>
    </div>
  );
}

export function FamilyMedicationManager({
  onChanged,
}: FamilyMedicationManagerProps) {
  const [medications, setMedications] = useState<MedicationApi[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [values, setValues] = useState(initialFormValues);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);
  const [editingMedication, setEditingMedication] = useState<MedicationApi | null>(
    null
  );
  const [editValues, setEditValues] = useState(initialFormValues);
  const [editImageFile, setEditImageFile] = useState<File | null>(null);
  const [editImagePreviewUrl, setEditImagePreviewUrl] = useState<string | null>(
    null
  );
  const [deletingMedication, setDeletingMedication] =
    useState<MedicationApi | null>(null);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [imageError, setImageError] = useState("");

  async function loadMedications() {
    setIsLoading(true);
    try {
      setMedications(await fetchMedications());
    } catch {
      setFeedback({
        type: "error",
        message: "No se pudo cargar la lista de medicamentos.",
      });
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;

    fetchMedications()
      .then((items) => {
        if (!cancelled) {
          setMedications(items);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFeedback({
            type: "error",
            message: "No se pudo cargar la lista de medicamentos.",
          });
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function updateValue(field: keyof MedicationFormValues, value: string | null) {
    setValues((current) => ({ ...current, [field]: value }));
    setFeedback(null);
  }

  function updateEditValue(
    field: keyof MedicationFormValues,
    value: string | null
  ) {
    setEditValues((current) => ({ ...current, [field]: value }));
    setFeedback(null);
  }

  function setValidatedImage(file: File | null, mode: "create" | "edit") {
    setImageError("");

    if (!file) {
      if (mode === "create") {
        setImageFile(null);
        setImagePreviewUrl(null);
      } else {
        setEditImageFile(null);
        setEditImagePreviewUrl(null);
      }
      return;
    }

    if (!validateImage(file)) {
      setImageError(invalidImageMessage);
      return;
    }

    const previewUrl = URL.createObjectURL(file);

    if (mode === "create") {
      setImageFile(file);
      setImagePreviewUrl(previewUrl);
    } else {
      setEditImageFile(file);
      setEditImagePreviewUrl(previewUrl);
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      if (!hasValidTime(values)) {
        throw new Error("Selecciona una hora válida para el medicamento.");
      }

      const imageUrl = imageFile ? await uploadImage(imageFile) : undefined;
      const response = await fetch("/api/medications", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          patientId: "patient_maria",
          name: values.name,
          dose: values.dose,
          time: to24HourTime(
            values.timeHour,
            values.timeMinute,
            values.timePeriod
          ),
          color: values.color || undefined,
          shape: values.shape || undefined,
          instructions: values.instructions || undefined,
          imageUrl,
        }),
      });
      const payload = (await response.json()) as ApiResponse<MedicationApi>;

      if (!response.ok) {
        throw new Error(payload.message ?? "No se pudo crear el medicamento.");
      }

      setValues(initialFormValues);
      setImageFile(null);
      setImagePreviewUrl(null);
      setFeedback({
        type: "success",
        message: "Medicamento registrado correctamente.",
      });
      await loadMedications();
      onChanged?.();
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudo crear el medicamento.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  function openEditDialog(medication: MedicationApi) {
    setEditingMedication(medication);
    setEditValues(toFormValues(medication));
    setEditImageFile(null);
    setEditImagePreviewUrl(null);
    setImageError("");
  }

  async function handleEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!editingMedication) {
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);

    try {
      if (!hasValidTime(editValues)) {
        throw new Error("Selecciona una hora válida para el medicamento.");
      }

      const imageUrl = editImageFile
        ? await uploadImage(editImageFile)
        : editValues.imageUrl;
      const response = await fetch(`/api/medications/${editingMedication.id}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: editValues.name,
          dose: editValues.dose,
          time: to24HourTime(
            editValues.timeHour,
            editValues.timeMinute,
            editValues.timePeriod
          ),
          color: editValues.color || null,
          shape: editValues.shape || null,
          instructions: editValues.instructions || null,
          imageUrl,
        }),
      });
      const payload = (await response.json()) as ApiResponse<MedicationApi>;

      if (!response.ok) {
        throw new Error(
          payload.message ?? "No se pudo actualizar el medicamento."
        );
      }

      setEditingMedication(null);
      setFeedback({
        type: "success",
        message: "Medicamento actualizado correctamente.",
      });
      await loadMedications();
      onChanged?.();
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudo actualizar el medicamento.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deletingMedication) {
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);

    try {
      const response = await fetch(`/api/medications/${deletingMedication.id}`, {
        method: "DELETE",
      });
      const payload = (await response.json()) as ApiResponse<MedicationApi>;

      if (!response.ok) {
        throw new Error(payload.message ?? "No se pudo eliminar el medicamento.");
      }

      setDeletingMedication(null);
      setFeedback({
        type: "success",
        message: "Medicamento eliminado correctamente.",
      });
      await loadMedications();
      onChanged?.();
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudo eliminar el medicamento.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section id="medicamentos" className="grid gap-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Medicamentos
        </p>
        <h2 className="mt-2 text-3xl font-semibold tracking-tight">
          Gestión familiar de medicamentos
        </h2>
      </div>

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

      <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Crear medicamento</CardTitle>
          </CardHeader>
          <CardContent className="pb-6">
            <form className="space-y-5" onSubmit={handleCreate}>
              <MedicationFields
                values={values}
                imagePreviewUrl={imagePreviewUrl}
                error={imageError}
                onChange={updateValue}
                onImageChange={(file) => setValidatedImage(file, "create")}
                onRemoveImage={() => {
                  setImageFile(null);
                  setImagePreviewUrl(null);
                  updateValue("imageUrl", null);
                }}
              />
              <Button
                type="submit"
                disabled={isSubmitting}
                className="min-h-14 w-full rounded-2xl text-lg font-semibold"
              >
                {isSubmitting ? (
                  <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                ) : (
                  <PlusCircle className="size-5" aria-hidden="true" />
                )}
                Guardar medicamento
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
          <CardHeader>
            <CardTitle className="text-2xl">Medicamentos activos</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 pb-6">
            {isLoading ? (
              <div className="flex min-h-40 items-center justify-center gap-3 text-muted-foreground">
                <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                Cargando medicamentos...
              </div>
            ) : medications.length === 0 ? (
              <p className="rounded-3xl bg-muted px-5 py-5 text-base font-medium text-muted-foreground">
                No hay medicamentos activos.
              </p>
            ) : (
              medications.map((medication) => (
                <div
                  key={medication.id}
                  className="grid gap-4 rounded-3xl border border-border/70 bg-[#fffdfa] p-4 shadow-sm"
                >
                  <div className="flex items-start gap-4">
                    <MedicationImage
                      imageUrl={medication.imageUrl}
                      name={medication.name}
                    />
                    <div className="min-w-0 flex-1">
                      <p className="text-xl font-semibold">{medication.name}</p>
                      <p className="mt-1 text-base text-muted-foreground">
                        {medication.dose} · {activeTime(medication) || "Sin hora"}
                      </p>
                      <div className="mt-3 grid gap-1 text-base text-foreground/80">
                        {medication.color ? <p>Color: {medication.color}</p> : null}
                        {medication.shape ? <p>Forma: {medication.shape}</p> : null}
                        {medication.instructions ? (
                          <p>Instrucciones: {medication.instructions}</p>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="min-h-12 rounded-2xl bg-white text-base font-semibold"
                      onClick={() => openEditDialog(medication)}
                    >
                      <Pencil className="size-4" aria-hidden="true" />
                      Editar
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      className="min-h-12 rounded-2xl border-rose-200 bg-rose-50 text-base font-semibold text-rose-800 hover:bg-rose-100"
                      onClick={() => setDeletingMedication(medication)}
                    >
                      <Trash2 className="size-4" aria-hidden="true" />
                      Eliminar
                    </Button>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      <Dialog
        open={Boolean(editingMedication)}
        onOpenChange={(open) => {
          if (!open) {
            setEditingMedication(null);
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto rounded-[2rem] sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-2xl">Editar medicamento</DialogTitle>
            <DialogDescription className="text-base">
              Actualiza los datos, horario o imagen del medicamento.
            </DialogDescription>
          </DialogHeader>
          <form className="space-y-5" onSubmit={handleEdit}>
            <MedicationFields
              values={editValues}
              imagePreviewUrl={editImagePreviewUrl}
              error={imageError}
              onChange={updateEditValue}
              onImageChange={(file) => setValidatedImage(file, "edit")}
              onRemoveImage={() => {
                setEditImageFile(null);
                setEditImagePreviewUrl(null);
                updateEditValue("imageUrl", null);
              }}
            />
            <DialogFooter className="mx-0 mb-0 rounded-3xl border-none bg-transparent p-0">
              <Button
                type="button"
                variant="outline"
                className="min-h-12 rounded-2xl"
                onClick={() => setEditingMedication(null)}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="min-h-12 rounded-2xl"
              >
                {isSubmitting ? (
                  <Loader2 className="size-5 animate-spin" aria-hidden="true" />
                ) : null}
                Guardar cambios
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(deletingMedication)}
        onOpenChange={(open) => {
          if (!open) {
            setDeletingMedication(null);
          }
        }}
      >
        <DialogContent className="rounded-[2rem] sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-2xl">¿Eliminar medicamento?</DialogTitle>
            <DialogDescription className="text-base leading-7">
              Este medicamento dejará de aparecer en los recordatorios, pero el
              historial anterior se conservará.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mx-0 mb-0 rounded-3xl border-none bg-transparent p-0">
            <Button
              type="button"
              variant="outline"
              className="min-h-12 rounded-2xl"
              onClick={() => setDeletingMedication(null)}
            >
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={isSubmitting}
              className="min-h-12 rounded-2xl bg-rose-700 text-white hover:bg-rose-800"
              onClick={() => void handleDelete()}
            >
              {isSubmitting ? (
                <Loader2 className="size-5 animate-spin" aria-hidden="true" />
              ) : (
                <Trash2 className="size-4" aria-hidden="true" />
              )}
              Eliminar medicamento
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  );
}
