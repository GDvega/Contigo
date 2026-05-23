import { prisma } from "@/lib/prisma";

import type { UpdatePatientInput } from "./patient.schema";

const PATIENT_ID = "patient_maria";

export async function getCurrentPatient() {
  return prisma.patient.findUnique({
    where: {
      id: PATIENT_ID,
    },
  });
}

export async function updateCurrentPatient(data: UpdatePatientInput) {
  return prisma.patient.update({
    where: {
      id: PATIENT_ID,
    },
    data: {
      fullName: data.fullName,
      age: data.age,
      notes: data.notes,
    },
  });
}
