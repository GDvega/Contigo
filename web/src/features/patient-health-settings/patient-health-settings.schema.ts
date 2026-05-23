import { z } from "zod";

const optionalNumber = (min: number, max: number, message: string) =>
  z.preprocess(
    (value) => {
      if (value === "" || value === undefined || value === null) {
        return null;
      }

      return Number(value);
    },
    z.number().int().min(min, message).max(max, message).nullable()
  );

const optionalText = z
  .string()
  .trim()
  .max(500, "La recomendación no debe superar 500 caracteres.")
  .nullable()
  .optional()
  .transform((value) => (value ? value : null));

export const updatePatientHealthSettingsSchema = z
  .object({
    systolicMinNormal: optionalNumber(
      60,
      250,
      "La presión sistólica debe estar entre 60 y 250."
    ),
    systolicMaxNormal: optionalNumber(
      60,
      250,
      "La presión sistólica debe estar entre 60 y 250."
    ),
    diastolicMinNormal: optionalNumber(
      40,
      160,
      "La presión diastólica debe estar entre 40 y 160."
    ),
    diastolicMaxNormal: optionalNumber(
      40,
      160,
      "La presión diastólica debe estar entre 40 y 160."
    ),
    pulseMinNormal: optionalNumber(30, 220, "El pulso debe estar entre 30 y 220."),
    pulseMaxNormal: optionalNumber(30, 220, "El pulso debe estar entre 30 y 220."),
    doctorRecommendation: optionalText,
  })
  .superRefine((data, ctx) => {
    const ranges = [
      ["systolicMinNormal", "systolicMaxNormal", "sistólica"],
      ["diastolicMinNormal", "diastolicMaxNormal", "diastólica"],
      ["pulseMinNormal", "pulseMaxNormal", "pulso"],
    ] as const;

    for (const [minKey, maxKey, label] of ranges) {
      const min = data[minKey];
      const max = data[maxKey];

      if (min !== null && max !== null && min > max) {
        ctx.addIssue({
          code: "custom",
          path: [minKey],
          message: `La mínima de ${label} no puede ser mayor que la máxima.`,
        });
      }
    }
  });

export type UpdatePatientHealthSettingsInput = z.infer<
  typeof updatePatientHealthSettingsSchema
>;
