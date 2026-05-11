import { z } from "zod";

export const createMedicationLogSchema = z.object({
  medicationId: z.string().trim().min(1, "El medicamento es obligatorio."),
  scheduledFor: z
    .string()
    .trim()
    .min(1, "La fecha programada es obligatoria.")
    .refine((value) => !Number.isNaN(Date.parse(value)), {
      message: "La fecha programada debe ser válida.",
    }),
});

export type CreateMedicationLogInput = z.infer<
  typeof createMedicationLogSchema
>;
