import { z } from "zod";

const optionalNotes = z
  .string()
  .trim()
  .optional()
  .transform((value) => (value ? value : null));

export const updatePatientSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(2, "El nombre debe tener al menos 2 caracteres."),
  age: z.coerce
    .number()
    .int("La edad debe ser un número entero.")
    .min(1, "La edad debe ser mayor a 0.")
    .max(120, "La edad debe ser menor o igual a 120.")
    .optional()
    .nullable(),
  notes: optionalNotes,
});

export type UpdatePatientInput = z.infer<typeof updatePatientSchema>;
