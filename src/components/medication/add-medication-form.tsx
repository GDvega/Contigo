/* eslint-disable @next/next/no-img-element */

import { ImagePlus } from "lucide-react";
import type { ChangeEvent, FormEvent, ReactNode } from "react";

import { MedicationTimeSelector } from "@/components/medication/medication-time-selector";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type {
  MedicationFormErrors,
  MedicationFormValues,
} from "@/components/medication/medication-management";

type AddMedicationFormProps = {
  values: MedicationFormValues;
  errors: MedicationFormErrors;
  successMessage: string;
  isSubmitting: boolean;
  submitIcon: ReactNode;
  imagePreviewUrl: string | null;
  onChange: (field: keyof MedicationFormValues, value: string) => void;
  onImageChange: (file: File | null) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function AddMedicationForm({
  values,
  errors,
  successMessage,
  isSubmitting,
  submitIcon,
  imagePreviewUrl,
  onChange,
  onImageChange,
  onSubmit,
}: AddMedicationFormProps) {
  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    onImageChange(event.target.files?.[0] ?? null);
  }

  return (
    <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
      <CardHeader>
        <CardTitle className="text-2xl">Agregar medicamento</CardTitle>
      </CardHeader>
      <CardContent className="pb-6">
        <form className="space-y-5" onSubmit={onSubmit}>
        {errors.form ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-base font-semibold text-rose-800">
            {errors.form}
          </div>
        ) : null}

        {successMessage ? (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-base font-semibold text-emerald-800">
            {successMessage}
          </div>
        ) : null}

        <div className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor="name" className="text-base">
              Nombre
            </Label>
            <Input
              id="name"
              value={values.name}
              onChange={(event) => onChange("name", event.target.value)}
              placeholder="Ej. Losartán"
              aria-invalid={Boolean(errors.name)}
              className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            />
            {errors.name ? (
              <p className="text-sm font-semibold text-rose-700">{errors.name}</p>
            ) : null}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="dose" className="text-base">
              Dosis
            </Label>
            <Input
              id="dose"
              value={values.dose}
              onChange={(event) => onChange("dose", event.target.value)}
              placeholder="Ej. 50 mg"
              aria-invalid={Boolean(errors.dose)}
              className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            />
            {errors.dose ? (
              <p className="text-sm font-semibold text-rose-700">{errors.dose}</p>
            ) : null}
          </div>
          <div className="grid gap-2">
            <MedicationTimeSelector
              idPrefix="medication-time"
              value={{
                hour: values.timeHour,
                minute: values.timeMinute,
                period: values.timePeriod,
              }}
              error={errors.time}
              onChange={(nextTime) => {
                onChange("timeHour", nextTime.hour);
                onChange("timeMinute", nextTime.minute);
                onChange("timePeriod", nextTime.period);
              }}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="color" className="text-base">
              Color
            </Label>
            <Input
              id="color"
              value={values.color}
              onChange={(event) => onChange("color", event.target.value)}
              placeholder="Ej. Blanca"
              aria-invalid={Boolean(errors.color)}
              className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            />
            {errors.color ? (
              <p className="text-sm font-semibold text-rose-700">{errors.color}</p>
            ) : null}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="shape" className="text-base">
              Forma
            </Label>
            <Input
              id="shape"
              value={values.shape}
              onChange={(event) => onChange("shape", event.target.value)}
              placeholder="Ej. Redonda"
              aria-invalid={Boolean(errors.shape)}
              className="h-14 rounded-2xl bg-[#fffdfa] px-4 text-base"
            />
            {errors.shape ? (
              <p className="text-sm font-semibold text-rose-700">{errors.shape}</p>
            ) : null}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="instructions" className="text-base">
              Instrucciones
            </Label>
            <Textarea
              id="instructions"
              value={values.instructions}
              onChange={(event) => onChange("instructions", event.target.value)}
              placeholder="Ej. Tomar con agua después del desayuno"
              aria-invalid={Boolean(errors.instructions)}
              className="min-h-24 rounded-2xl bg-[#fffdfa] px-4 py-3 text-base"
            />
            {errors.instructions ? (
              <p className="text-sm font-semibold text-rose-700">
                {errors.instructions}
              </p>
            ) : null}
          </div>
          <div className="grid gap-3 rounded-3xl border border-border/70 bg-[#fffdfa] p-4">
            <div className="grid gap-1">
              <Label htmlFor="medication-image" className="text-base">
                Imagen del medicamento
              </Label>
              <p className="text-sm leading-6 text-muted-foreground">
                Puedes subir una foto de la pastilla, caja o blíster para
                reconocerla mejor.
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-[auto_1fr] sm:items-center">
              {imagePreviewUrl ? (
                <img
                  src={imagePreviewUrl}
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
                  id="medication-image"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <Button
                  type="button"
                  variant="outline"
                  className="min-h-14 rounded-2xl bg-white text-base font-semibold"
                  onClick={() =>
                    document.getElementById("medication-image")?.click()
                  }
                >
                  <ImagePlus className="size-5" aria-hidden="true" />
                  Subir imagen
                </Button>
                {imagePreviewUrl ? (
                  <Button
                    type="button"
                    variant="ghost"
                    className="min-h-12 rounded-2xl text-base font-semibold"
                    onClick={() => onImageChange(null)}
                  >
                    Quitar imagen
                  </Button>
                ) : null}
              </div>
            </div>
            {errors.image ? (
              <p className="text-sm font-semibold text-rose-700">
                {errors.image}
              </p>
            ) : null}
          </div>
        </div>
        <Button
          type="submit"
          disabled={isSubmitting}
          className="h-14 w-full rounded-2xl text-lg font-semibold shadow-sm"
        >
          {submitIcon}
          Guardar medicamento
        </Button>
        </form>
      </CardContent>
    </Card>
  );
}
