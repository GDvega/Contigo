import { prisma } from "@/lib/prisma";

import type { UpdatePatientHealthSettingsInput } from "./patient-health-settings.schema";

const PATIENT_ID = "patient_maria";

export async function getPatientHealthSettings() {
  return prisma.patientHealthSettings.findUnique({
    where: {
      patientId: PATIENT_ID,
    },
  });
}

export async function updatePatientHealthSettings(
  data: UpdatePatientHealthSettingsInput
) {
  return prisma.patientHealthSettings.upsert({
    where: {
      patientId: PATIENT_ID,
    },
    create: {
      patientId: PATIENT_ID,
      ...data,
    },
    update: data,
  });
}
