package com.cuidavoz.mobile.data.migration

import com.cuidavoz.mobile.util.ContigoLog
import androidx.room.withTransaction
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Elimina datos de demostración de versiones anteriores que podían aparecer en instalaciones reales.
 * No inserta datos de ejemplo: solo limpia, migra IDs legacy y corrige referencias huérfanas.
 */
@Singleton
class LegacyDemoDataCleaner @Inject constructor(
    private val database: ContigoDatabase,
) {
    suspend fun purgeLegacyDemoData() {
        database.withTransaction {
            migrateLegacyPatientId()
            reassignBlankPatientIds()
            purgeKnownDemoRecords()
        }
    }

    private suspend fun migrateLegacyPatientId() {
        if (LEGACY_PATIENT_ID == DEFAULT_PATIENT_ID) return
        val legacyPatient = database.patientDao().getPatient(LEGACY_PATIENT_ID) ?: return
        val now = System.currentTimeMillis()
        val existingPrimary = database.patientDao().getPatient(DEFAULT_PATIENT_ID)

        database.medicationDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID, now)
        database.medicationLogDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID)
        database.bloodPressureDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID)
        database.healthSettingsDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID, now)
        database.familyContactDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID)
        database.medicationReminderDao().migratePatientId(LEGACY_PATIENT_ID, DEFAULT_PATIENT_ID, now)

        if (existingPrimary == null) {
            database.patientDao().upsert(
                legacyPatient.copy(
                    id = DEFAULT_PATIENT_ID,
                    updatedAt = now,
                ),
            )
        }
        database.patientDao().deleteById(LEGACY_PATIENT_ID)
        ContigoLog.i(TAG, "Migrado paciente legacy $LEGACY_PATIENT_ID → $DEFAULT_PATIENT_ID")
    }

    private suspend fun reassignBlankPatientIds() {
        database.medicationDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
        database.medicationLogDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
        database.bloodPressureDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
        database.healthSettingsDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
        database.familyContactDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
        database.medicationReminderDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
    }

    private suspend fun purgeKnownDemoRecords() {
        val removedPatients = database.patientDao().deleteByIdAndNotes(
            id = DEFAULT_PATIENT_ID,
            notes = LEGACY_DEMO_PATIENT_NOTES,
        ) + database.patientDao().deleteByIdAndNotes(
            id = LEGACY_PATIENT_ID,
            notes = LEGACY_DEMO_PATIENT_NOTES,
        )
        val removedContacts = database.familyContactDao().deleteByIdAndPhone(
            id = LEGACY_DEMO_CONTACT_ID,
            phone = LEGACY_DEMO_CONTACT_PHONE,
        )
        var removedMedications = 0
        LEGACY_DEMO_MEDICATIONS.forEach { (id, name) ->
            removedMedications += database.medicationDao().deleteByIdAndName(id, name)
        }

        if (removedPatients > 0 || removedContacts > 0 || removedMedications > 0) {
            ContigoLog.i(
                TAG,
                "Datos demo legacy eliminados: pacientes=$removedPatients, contactos=$removedContacts, medicamentos=$removedMedications",
            )
        }
    }

    private companion object {
        private const val TAG = "LegacyDemoDataCleaner"
        private const val LEGACY_PATIENT_ID = "patient_maria"
        private const val LEGACY_DEMO_PATIENT_NOTES = "Paciente de prueba para seguimiento diario offline."
        private const val LEGACY_DEMO_CONTACT_ID = "family_contact_juan"
        private const val LEGACY_DEMO_CONTACT_PHONE = "+51 999 999 999"
        private val LEGACY_DEMO_MEDICATIONS = listOf(
            "med_aspirina" to "Aspirina",
            "med_paracetamol" to "Paracetamol",
            "med_losartan" to "Losartan",
        )
    }
}
