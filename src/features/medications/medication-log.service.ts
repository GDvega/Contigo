import { prisma } from "@/lib/prisma";

import type { CreateMedicationLogInput } from "./medication-log.schema";

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
    orderBy: {
      createdAt: "desc",
    },
    take: 50,
    include: {
      medication: true,
    },
  });
}
