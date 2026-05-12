import { prisma } from "@/lib/prisma";

import type { CreateMedicationLogInput } from "./medication-log.schema";

const PATIENT_ID = "patient_maria";

export class MedicationLogMedicationNotFoundError extends Error {
  constructor() {
    super("No se encontró una pastilla activa para registrar la toma.");
    this.name = "MedicationLogMedicationNotFoundError";
  }
}

function getScheduleMinuteWindow(date: Date) {
  const start = new Date(date);
  start.setSeconds(0, 0);

  const end = new Date(start);
  end.setMinutes(end.getMinutes() + 1);

  return { start, end };
}

export async function createMedicationLog(data: CreateMedicationLogInput) {
  const scheduledFor = new Date(data.scheduledFor);
  const { start, end } = getScheduleMinuteWindow(scheduledFor);
  const medication = await prisma.medication.findFirst({
    where: {
      id: data.medicationId,
      patientId: PATIENT_ID,
      isActive: true,
    },
    select: {
      id: true,
    },
  });

  if (!medication) {
    throw new MedicationLogMedicationNotFoundError();
  }

  const existingLog = await prisma.medicationLog.findFirst({
    where: {
      medicationId: data.medicationId,
      status: "TAKEN",
      scheduledFor: {
        gte: start,
        lt: end,
      },
    },
    include: {
      medication: true,
    },
  });

  if (existingLog) {
    return {
      medicationLog: existingLog,
      duplicate: true,
    };
  }

  const medicationLog = await prisma.medicationLog.create({
    data: {
      medicationId: data.medicationId,
      status: "TAKEN",
      scheduledFor,
      takenAt: new Date(),
    },
    include: {
      medication: true,
    },
  });

  return {
    medicationLog,
    duplicate: false,
  };
}

export async function getMedicationLogs() {
  return prisma.medicationLog.findMany({
    where: {
      medication: {
        patientId: PATIENT_ID,
      },
    },
    orderBy: {
      createdAt: "desc",
    },
    take: 50,
    include: {
      medication: true,
    },
  });
}
