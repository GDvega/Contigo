import { z } from "zod";

const optionalText = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : undefined));

const nullableText = z
  .string()
  .trim()
  .nullable()
  .optional()
  .transform((value) => (value ? value : null));

const timeSchema = z
  .string()
  .trim()
  .regex(/^([01]\d|2[0-3]):[0-5]\d$/, "La hora debe tener formato HH:mm.");

export const createMedicationSchema = z.object({
  patientId: z.string().trim().min(1, "El paciente es obligatorio."),
  name: z.string().trim().min(2, "El nombre debe tener al menos 2 caracteres."),
  dose: z.string().trim().min(1, "La dosis es obligatoria."),
  color: optionalText,
  shape: optionalText,
  instructions: optionalText,
  imageUrl: optionalText,
  time: timeSchema,
});

export type CreateMedicationInput = z.infer<typeof createMedicationSchema>;

export const updateMedicationSchema = z.object({
  name: z.string().trim().min(2, "El nombre debe tener al menos 2 caracteres."),
  dose: z.string().trim().min(1, "La dosis es obligatoria."),
  color: nullableText,
  shape: nullableText,
  instructions: nullableText,
  imageUrl: nullableText,
  time: timeSchema,
});

export type UpdateMedicationInput = z.infer<typeof updateMedicationSchema>;
