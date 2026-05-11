import { prisma } from "@/lib/prisma";

import type {
  CreateMedicationInput,
  UpdateMedicationInput,
} from "./medication.schema";

const PATIENT_ID = "patient_maria";

export async function createMedication(data: CreateMedicationInput) {
  return prisma.medication.create({
    data: {
      patientId: data.patientId,
      name: data.name,
      dose: data.dose,
      color: data.color,
      shape: data.shape,
      instructions: data.instructions,
      imageUrl: data.imageUrl,
      schedules: {
        create: {
          time: data.time,
        },
      },
    },
    include: {
      schedules: {
        orderBy: {
          createdAt: "asc",
        },
      },
      patient: true,
    },
  });
}

export async function getMedications() {
  return prisma.medication.findMany({
    where: {
      patientId: PATIENT_ID,
      isActive: true,
    },
    orderBy: {
      createdAt: "desc",
    },
    include: {
      schedules: {
        orderBy: {
          createdAt: "asc",
        },
      },
      patient: true,
    },
  });
}

export async function updateMedication(id: string, data: UpdateMedicationInput) {
  return prisma.$transaction(async (tx) => {
    const existingMedication = await tx.medication.findFirstOrThrow({
      where: {
        id,
        patientId: PATIENT_ID,
        isActive: true,
      },
    });

    const medication = await tx.medication.update({
      where: {
        id: existingMedication.id,
      },
      data: {
        name: data.name,
        dose: data.dose,
        color: data.color,
        shape: data.shape,
        instructions: data.instructions,
        imageUrl: data.imageUrl,
      },
    });

    const activeSchedule = await tx.medicationSchedule.findFirst({
      where: {
        medicationId: medication.id,
        isActive: true,
      },
      orderBy: {
        createdAt: "asc",
      },
    });

    if (activeSchedule) {
      await tx.medicationSchedule.update({
        where: {
          id: activeSchedule.id,
        },
        data: {
          time: data.time,
        },
      });
    } else {
      await tx.medicationSchedule.create({
        data: {
          medicationId: medication.id,
          time: data.time,
        },
      });
    }

    return tx.medication.findUniqueOrThrow({
      where: {
        id: medication.id,
      },
      include: {
        schedules: {
          orderBy: {
            createdAt: "asc",
          },
        },
        patient: true,
      },
    });
  });
}

export async function deleteMedication(id: string) {
  const medication = await prisma.medication.findFirstOrThrow({
    where: {
      id,
      patientId: PATIENT_ID,
      isActive: true,
    },
  });

  return prisma.medication.update({
    where: {
      id: medication.id,
    },
    data: {
      isActive: false,
      schedules: {
        updateMany: {
          where: {
            isActive: true,
          },
          data: {
            isActive: false,
          },
        },
      },
    },
    include: {
      schedules: {
        orderBy: {
          createdAt: "asc",
        },
      },
      patient: true,
    },
  });
}
