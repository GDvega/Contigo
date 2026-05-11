import { prisma } from "@/lib/prisma";
import {
  CreatePressureReadingInput,
  getPressureStatus,
} from "./pressure.schema";

export async function createPressureReading(data: CreatePressureReadingInput) {
  return prisma.bloodPressureReading.create({
    data: {
      patientId: data.patientId,
      systolic: data.systolic,
      diastolic: data.diastolic,
      pulse: data.pulse,
      notes: data.notes,
      status: getPressureStatus(data.systolic, data.diastolic),
    },
  });
}

export async function getPressureReadings() {
  return prisma.bloodPressureReading.findMany({
    orderBy: {
      measuredAt: "desc",
    },
    take: 50,
    include: {
      patient: true,
    },
  });
}
