import "dotenv/config";

import { PrismaPg } from "@prisma/adapter-pg";
import { Pool } from "pg";
import { PrismaClient } from "../src/app/generated/prisma/client";

const PATIENT_ID = "patient_maria";
const DOCTOR_RECOMMENDATION =
  "El cardiólogo indicó mantener la presión alrededor de 120/80 y controlar si supera 140/90.";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

const prisma = new PrismaClient({
  adapter: new PrismaPg(pool),
});

const medications = [
  {
    name: "Aspirina",
    dose: "1 tableta",
    time: "07:00",
    color: "Blanca",
    shape: "Ovalada",
  },
  {
    name: "Paracetamol",
    dose: "1 tableta",
    time: "13:45",
    color: "Blanco",
    shape: "Redonda",
  },
  {
    name: "Losartán",
    dose: "1 pastilla",
    time: "20:00",
    color: "Blanca",
    shape: "Redonda",
  },
] as const;

async function upsertMedicationWithSchedule(
  input: (typeof medications)[number]
) {
  const existingMedication = await prisma.medication.findFirst({
    where: {
      patientId: PATIENT_ID,
      name: input.name,
    },
    orderBy: {
      createdAt: "asc",
    },
    select: {
      id: true,
    },
  });

  const medication = existingMedication
    ? await prisma.medication.update({
        where: {
          id: existingMedication.id,
        },
        data: {
          dose: input.dose,
          color: input.color,
          shape: input.shape,
          isActive: true,
        },
      })
    : await prisma.medication.create({
        data: {
          patientId: PATIENT_ID,
          name: input.name,
          dose: input.dose,
          color: input.color,
          shape: input.shape,
          isActive: true,
        },
      });

  const existingSchedule = await prisma.medicationSchedule.findFirst({
    where: {
      medicationId: medication.id,
    },
    orderBy: {
      createdAt: "asc",
    },
    select: {
      id: true,
    },
  });

  if (existingSchedule) {
    await prisma.medicationSchedule.update({
      where: {
        id: existingSchedule.id,
      },
      data: {
        time: input.time,
        isActive: true,
      },
    });

    await prisma.medicationSchedule.updateMany({
      where: {
        medicationId: medication.id,
        NOT: {
          id: existingSchedule.id,
        },
      },
      data: {
        isActive: false,
      },
    });
  } else {
    await prisma.medicationSchedule.create({
      data: {
        medicationId: medication.id,
        time: input.time,
        isActive: true,
      },
    });
  }

  await prisma.medication.updateMany({
    where: {
      patientId: PATIENT_ID,
      name: input.name,
      NOT: {
        id: medication.id,
      },
    },
    data: {
      isActive: false,
    },
  });

  await prisma.medicationSchedule.updateMany({
    where: {
      medication: {
        patientId: PATIENT_ID,
        name: input.name,
        NOT: {
          id: medication.id,
        },
      },
    },
    data: {
      isActive: false,
    },
  });

  return medication.id;
}

async function main() {
  await prisma.patient.upsert({
    where: {
      id: PATIENT_ID,
    },
    create: {
      id: PATIENT_ID,
      fullName: "María Rojas",
      age: 72,
      notes: "Paciente de prueba",
    },
    update: {
      fullName: "María Rojas",
      age: 72,
      notes: "Paciente de prueba",
    },
  });

  const seededMedicationIds: string[] = [];

  for (const medication of medications) {
    const medicationId = await upsertMedicationWithSchedule(medication);
    seededMedicationIds.push(medicationId);
  }

  await prisma.medication.updateMany({
    where: {
      patientId: PATIENT_ID,
      NOT: {
        id: {
          in: seededMedicationIds,
        },
      },
    },
    data: {
      isActive: false,
    },
  });

  await prisma.medicationSchedule.updateMany({
    where: {
      medication: {
        patientId: PATIENT_ID,
        NOT: {
          id: {
            in: seededMedicationIds,
          },
        },
      },
    },
    data: {
      isActive: false,
    },
  });

  await prisma.patientHealthSettings.upsert({
    where: {
      patientId: PATIENT_ID,
    },
    create: {
      patientId: PATIENT_ID,
      systolicMinNormal: 100,
      systolicMaxNormal: 130,
      diastolicMinNormal: 60,
      diastolicMaxNormal: 85,
      pulseMinNormal: 60,
      pulseMaxNormal: 100,
      doctorRecommendation: DOCTOR_RECOMMENDATION,
    },
    update: {
      systolicMinNormal: 100,
      systolicMaxNormal: 130,
      diastolicMinNormal: 60,
      diastolicMaxNormal: 85,
      pulseMinNormal: 60,
      pulseMaxNormal: 100,
      doctorRecommendation: DOCTOR_RECOMMENDATION,
    },
  });

  const [medicationsCount, activeSchedulesCount, healthSettings] = await Promise.all([
    prisma.medication.count({
      where: {
        patientId: PATIENT_ID,
        isActive: true,
      },
    }),
    prisma.medicationSchedule.count({
      where: {
        medication: {
          patientId: PATIENT_ID,
        },
        isActive: true,
      },
    }),
    prisma.patientHealthSettings.findUnique({
      where: {
        patientId: PATIENT_ID,
      },
      select: {
        id: true,
      },
    }),
  ]);

  console.log(
    JSON.stringify(
      {
        patientId: PATIENT_ID,
        medicationsCount,
        activeSchedulesCount,
        healthSettingsCreated: Boolean(healthSettings),
      },
      null,
      2
    )
  );
}

main()
  .catch((error) => {
    console.error("Seed failed", error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
    await pool.end();
  });
