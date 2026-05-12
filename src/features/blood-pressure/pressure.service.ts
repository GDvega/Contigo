import { prisma } from "@/lib/prisma";
import {
  CreatePressureReadingInput,
  getPressureStatus,
} from "./pressure.schema";

const PATIENT_ID = "patient_maria";

export class PressurePatientNotFoundError extends Error {
  constructor() {
    super("No se encontró el paciente para registrar la presión.");
    this.name = "PressurePatientNotFoundError";
  }
}

export async function createPressureReading(data: CreatePressureReadingInput) {
  const patient = await prisma.patient.findUnique({
    where: {
      id: data.patientId,
    },
    select: {
      id: true,
    },
  });

  if (!patient) {
    throw new PressurePatientNotFoundError();
  }

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
    where: {
      patientId: PATIENT_ID,
    },
    orderBy: {
      measuredAt: "desc",
    },
    take: 50,
    include: {
      patient: true,
    },
  });
}
