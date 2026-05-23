-- AlterTable
ALTER TABLE "Medication" ADD COLUMN     "isActive" BOOLEAN NOT NULL DEFAULT true;

-- CreateTable
CREATE TABLE "PatientHealthSettings" (
    "id" TEXT NOT NULL,
    "patientId" TEXT NOT NULL,
    "systolicMinNormal" INTEGER,
    "systolicMaxNormal" INTEGER,
    "diastolicMinNormal" INTEGER,
    "diastolicMaxNormal" INTEGER,
    "pulseMinNormal" INTEGER,
    "pulseMaxNormal" INTEGER,
    "doctorRecommendation" TEXT,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "PatientHealthSettings_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "PatientHealthSettings_patientId_key" ON "PatientHealthSettings"("patientId");

-- AddForeignKey
ALTER TABLE "PatientHealthSettings" ADD CONSTRAINT "PatientHealthSettings_patientId_fkey" FOREIGN KEY ("patientId") REFERENCES "Patient"("id") ON DELETE CASCADE ON UPDATE CASCADE;
