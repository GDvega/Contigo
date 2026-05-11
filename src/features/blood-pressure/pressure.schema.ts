import { z } from "zod";

const optionalPulse = z.preprocess(
  (value) => (value === "" || value === null ? undefined : value),
  z.coerce.number().int().min(30).max(220).optional()
);

const optionalNotes = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : undefined));

export const createPressureReadingSchema = z.object({
  patientId: z.string().min(1, "El paciente es obligatorio"),
  systolic: z.coerce.number().int().min(60).max(250),
  diastolic: z.coerce.number().int().min(40).max(160),
  pulse: optionalPulse,
  notes: optionalNotes,
});

export type CreatePressureReadingInput = z.infer<
  typeof createPressureReadingSchema
>;

export function getPressureStatus(systolic: number, diastolic: number) {
  if (systolic >= 180 || diastolic >= 120) return "CRITICAL";
  if (systolic >= 140 || diastolic >= 90) return "HIGH";
  if (systolic >= 120 || diastolic >= 80) return "ELEVATED";
  return "NORMAL";
}
