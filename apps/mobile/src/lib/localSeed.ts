import { initializeLocalDb } from "@/lib/localDb";
import {
  LOCAL_PATIENT_ID,
  createMedication,
  getPatientCount,
  upsertFamilyContact,
  upsertPatient,
  updateHealthSettings,
} from "@/lib/localRepositories";

const DOCTOR_RECOMMENDATION =
  "El cardiólogo indicó mantener la presión alrededor de 120/80 y controlar si supera 140/90.";

export async function seedLocalDataIfNeeded() {
  await initializeLocalDb();

  const patientCount = await getPatientCount();
  if (patientCount > 0) {
    return;
  }

  await seedLocalDemoData();
}

export async function seedLocalDemoData() {
  await upsertPatient({
    id: LOCAL_PATIENT_ID,
    fullName: "María Rojas",
    age: 72,
    notes: "Paciente de prueba",
  });

  await upsertFamilyContact({
    fullName: "Juan Rojas",
    phone: "+51 999 999 999",
    relation: "Familiar",
  });

  await createMedication({
    name: "Aspirina",
    dose: "1 tableta",
    time: "07:00",
    color: "Blanca",
    shape: "Ovalada",
    instructions: "Tomar después del desayuno",
  });

  await createMedication({
    name: "Paracetamol",
    dose: "1 tableta",
    time: "13:45",
    color: "Blanco",
    shape: "Redonda",
    instructions: "Tomar después del almuerzo",
  });

  await createMedication({
    name: "Losartán",
    dose: "1 pastilla",
    time: "20:00",
    color: "Blanca",
    shape: "Redonda",
    instructions: "Tomar con agua después de la cena",
  });

  await updateHealthSettings({
    systolicMinNormal: 100,
    systolicMaxNormal: 130,
    diastolicMinNormal: 60,
    diastolicMaxNormal: 85,
    pulseMinNormal: 60,
    pulseMaxNormal: 100,
    doctorRecommendation: DOCTOR_RECOMMENDATION,
  });
}
