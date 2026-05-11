import { prisma } from "@/lib/prisma";
import { getPressureStatus } from "@/features/blood-pressure/pressure.schema";

const PATIENT_ID = "patient_maria";
const DOCTOR_RECOMMENDATION =
  "El cardiólogo indicó mantener la presión alrededor de 120/80 y controlar si supera 140/90.";

function todayAt(hours: number, minutes: number) {
  const d = new Date();
  d.setHours(hours, minutes, 0, 0);
  return d;
}

function minutesAfter(base: Date, deltaMinutes: number) {
  const d = new Date(base.getTime());
  d.setMinutes(d.getMinutes() + deltaMinutes);
  return d;
}

export async function resetDemoData() {
  return prisma.$transaction(async (tx) => {
    const patient = await tx.patient.upsert({
      where: { id: PATIENT_ID },
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

    const existingMeds = await tx.medication.findMany({
      where: { patientId: PATIENT_ID },
      select: { id: true },
    });
    const medIds = existingMeds.map((m) => m.id);

    if (medIds.length > 0) {
      await tx.medicationLog.deleteMany({
        where: { medicationId: { in: medIds } },
      });
    }

    await tx.bloodPressureReading.deleteMany({
      where: { patientId: PATIENT_ID },
    });

    await tx.medicationSchedule.updateMany({
      where: {
        medication: {
          patientId: PATIENT_ID,
        },
      },
      data: {
        isActive: false,
      },
    });

    await tx.medication.updateMany({
      where: { patientId: PATIENT_ID },
      data: {
        isActive: false,
      },
    });

    const aspirina = await tx.medication.create({
      data: {
        patientId: PATIENT_ID,
        name: "Aspirina",
        dose: "1 tableta",
        color: "Blanca",
        shape: "Ovalada",
        instructions: "Tomar después del desayuno",
        isActive: true,
        schedules: {
          create: { time: "07:00", isActive: true },
        },
      },
    });

    const paracetamol = await tx.medication.create({
      data: {
        patientId: PATIENT_ID,
        name: "Paracetamol",
        dose: "1 tableta",
        color: "Blanco",
        shape: "Redonda",
        instructions: "Tomar después del almuerzo",
        isActive: true,
        schedules: {
          create: { time: "13:45", isActive: true },
        },
      },
    });

    await tx.medication.create({
      data: {
        patientId: PATIENT_ID,
        name: "Losartán",
        dose: "1 pastilla",
        color: "Blanca",
        shape: "Redonda",
        instructions: "Tomar con agua después de la cena",
        isActive: true,
        schedules: {
          create: { time: "20:00", isActive: true },
        },
      },
    });

    const readings: Array<{
      systolic: number;
      diastolic: number;
      pulse: number;
      measuredAt: Date;
    }> = [
      {
        systolic: 120,
        diastolic: 70,
        pulse: 70,
        measuredAt: todayAt(8, 0),
      },
      {
        systolic: 120,
        diastolic: 80,
        pulse: 72,
        measuredAt: todayAt(12, 40),
      },
      {
        systolic: 130,
        diastolic: 90,
        pulse: 78,
        measuredAt: todayAt(13, 40),
      },
    ];

    for (const r of readings) {
      await tx.bloodPressureReading.create({
        data: {
          patientId: PATIENT_ID,
          systolic: r.systolic,
          diastolic: r.diastolic,
          pulse: r.pulse,
          status: getPressureStatus(r.systolic, r.diastolic),
          measuredAt: r.measuredAt,
        },
      });
    }

    const aspirinaTime = todayAt(7, 0);
    await tx.medicationLog.create({
      data: {
        medicationId: aspirina.id,
        status: "TAKEN",
        scheduledFor: aspirinaTime,
        takenAt: minutesAfter(aspirinaTime, 10),
      },
    });

    const paracetamolTime = todayAt(13, 45);
    await tx.medicationLog.create({
      data: {
        medicationId: paracetamol.id,
        status: "TAKEN",
        scheduledFor: paracetamolTime,
        takenAt: minutesAfter(paracetamolTime, 8),
      },
    });

    await tx.patientHealthSettings.upsert({
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

    return {
      patient,
      medicationsCount: 3,
      pressureReadingsCount: readings.length,
      medicationLogsCount: 2,
    };
  });
}
