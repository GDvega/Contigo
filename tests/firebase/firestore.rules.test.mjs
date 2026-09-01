import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  writeBatch,
} from "firebase/firestore";

const PROJECT_ID = "demo-contigo-rules";
const FAMILY_ID = "family-1";
const PATIENT_ID = "patient-1";
const PATIENT_UID = "patient-user";
const CAREGIVER_UID = "caregiver-user";
const LINK_CODE = "123456";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: await readFile(new URL("../../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, `families/${FAMILY_ID}`), {
      name: "Familia Contigo",
      createdAt: Date.now(),
      createdBy: PATIENT_UID,
    });
    await setDoc(doc(db, `families/${FAMILY_ID}/members/${PATIENT_UID}`), {
      role: "patient",
      displayName: "Paciente",
      phone: null,
      linkedAt: Date.now(),
    });
  });
});

after(async () => {
  await testEnv.cleanup();
});

test("aísla los datos médicos por familia", async () => {
  const patientDb = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider-user").firestore();
  const anonymousDb = testEnv.unauthenticatedContext().firestore();
  const patientPath = `families/${FAMILY_ID}/patients/${PATIENT_ID}`;

  await assertSucceeds(getDoc(doc(patientDb, `families/${FAMILY_ID}`)));
  await assertFails(getDoc(doc(outsiderDb, patientPath)));
  await assertFails(getDoc(doc(anonymousDb, patientPath)));
});

test("permite documentos válidos y bloquea escrituras externas", async () => {
  const patientDb = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider-user").firestore();
  const medicationPath = `families/${FAMILY_ID}/patients/${PATIENT_ID}/medications/med-1`;
  const medication = {
    name: "Medicamento",
    dose: "1 tableta",
    time24: "08:00",
    instructions: null,
    color: null,
    shape: null,
    active: true,
    scheduleType: "ALWAYS",
    startDate: Timestamp.fromDate(new Date("2026-08-28T00:00:00Z")),
    endDate: null,
    daysOfWeek: [],
    specificDates: [],
    createdAt: Date.now(),
    updatedAt: Date.now(),
    updatedBy: PATIENT_UID,
  };

  await assertSucceeds(setDoc(doc(patientDb, medicationPath), medication));
  await assertFails(setDoc(doc(outsiderDb, medicationPath), medication));
});

test("el código no se puede listar y solo se consume al crear al cuidador", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), `linkCodes/${LINK_CODE}`), {
      familyId: FAMILY_ID,
      patientId: PATIENT_ID,
      expiresAt: Date.now() + 60_000,
      createdBy: PATIENT_UID,
    });
  });

  const caregiverDb = testEnv.authenticatedContext(CAREGIVER_UID).firestore();
  const caregiverMember = {
    role: "caregiver",
    displayName: "Cuidador",
    phone: null,
    linkedAt: Date.now(),
    linkCode: LINK_CODE,
  };
  await assertFails(getDocs(collection(caregiverDb, "linkCodes")));
  await assertFails(
    writeBatch(caregiverDb).delete(doc(caregiverDb, `linkCodes/${LINK_CODE}`)).commit(),
  );
  await assertFails(
    setDoc(
      doc(caregiverDb, `families/${FAMILY_ID}/members/${CAREGIVER_UID}`),
      caregiverMember,
    ),
  );

  const batch = writeBatch(caregiverDb);
  batch.set(
    doc(caregiverDb, `families/${FAMILY_ID}/members/${CAREGIVER_UID}`),
    caregiverMember,
  );
  batch.delete(doc(caregiverDb, `linkCodes/${LINK_CODE}`));
  await assertSucceeds(batch.commit());

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const consumedCode = await getDoc(doc(context.firestore(), `linkCodes/${LINK_CODE}`));
    assert.equal(consumedCode.exists(), false);
  });
});
