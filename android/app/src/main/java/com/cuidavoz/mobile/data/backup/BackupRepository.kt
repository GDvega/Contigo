package com.cuidavoz.mobile.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import com.cuidavoz.mobile.util.ContigoLog
import androidx.room.withTransaction
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class ImportStrategy {
    REPLACE_ALL,
    MERGE,
}

data class BackupResult(
    val exportedMedications: Int,
    val exportedPressureReadings: Int,
    val exportedMedicationLogs: Int,
    val exportedImages: Int,
    val warnings: List<String> = emptyList(),
)

data class BackupSummary(
    val patientName: String,
    val medicationsCount: Int,
    val pressureReadingsCount: Int,
    val medicationLogsCount: Int,
    val imagesCount: Int,
    val createdAt: Long,
    val backupVersion: Int,
)

data class ImportResult(
    val importedMedications: Int,
    val importedPressureReadings: Int,
    val importedMedicationLogs: Int,
    val importedImages: Int,
    val skippedDuplicates: Int,
    val errors: List<String> = emptyList(),
)

class BackupRepository(
    private val context: Context,
    private val database: ContigoDatabase,
    private val medicationImageStorage: MedicationImageStorage,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val firebaseSyncManager: FirebaseSyncManager,
) {
    private val patientDao = database.patientDao()
    private val familyContactDao = database.familyContactDao()
    private val healthSettingsDao = database.healthSettingsDao()
    private val medicationDao = database.medicationDao()
    private val medicationLogDao = database.medicationLogDao()
    private val bloodPressureDao = database.bloodPressureDao()
    private val medicationReminderDao = database.medicationReminderDao()

    suspend fun createBackup(destinationUri: Uri, password: CharArray): BackupResult = withContext(Dispatchers.IO) {
        ContigoLog.d(EXPORT_TAG, "Creando respaldo")

        val patient = patientDao.getCurrentPatient()
            ?: throw BackupFormatException("No encontramos datos del paciente para exportar.")
        val familyContact = familyContactDao.getPrimaryContact(DEFAULT_PATIENT_ID)
        val healthSettings = healthSettingsDao.getSettings(DEFAULT_PATIENT_ID)
        val medications = medicationDao.getAllMedications(DEFAULT_PATIENT_ID)
        val medicationLogs = medicationLogDao.getLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE)
        val pressureReadings = bloodPressureDao.getRecentReadings(DEFAULT_PATIENT_ID)
        val reminderPreferences = reminderPreferencesRepository.getCurrentPreferences()
        val voicePreferences = reminderPreferencesRepository.getCurrentVoicePreferences()

        val warnings = mutableListOf<String>()
        val exportImages = medications.mapNotNull { medication ->
            prepareExportImage(medication, warnings)
        }

        val backup = ContigoBackup(
            app = APP_NAME,
            backupVersion = BACKUP_VERSION,
            createdAt = System.currentTimeMillis(),
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} Android ${Build.VERSION.RELEASE}",
            patient = patient.toBackupDto(),
            familyContact = familyContact?.toBackupDto(),
            healthSettings = healthSettings?.toBackupDto(),
            medications = medications.map { it.toBackupDto() },
            medicationLogs = medicationLogs.map { it.toBackupDto() },
            bloodPressureReadings = pressureReadings.map { it.toBackupDto() },
            preferences = BackupPreferencesDto(
                remindersEnabled = reminderPreferences.remindersEnabled,
                repeatIntervalMinutes = reminderPreferences.repeatIntervalMinutes,
                maxRepeatCount = reminderPreferences.maxRepeatCount,
                voiceAssistantEnabled = voicePreferences.voiceAssistantEnabled,
                voiceReminderEnabled = voicePreferences.voiceReminderEnabled,
                voiceRepeatCount = voicePreferences.voiceRepeatCount,
                easyModeEnabled = voicePreferences.easyModeEnabled,
                voiceGuidanceEnabled = voicePreferences.voiceGuidanceEnabled,
            ),
            images = exportImages.map { it.image },
        )

        val tempZipFile = File(context.cacheDir, "backup/export-${System.currentTimeMillis()}.zip")
        try {
            ZipOutputStream(FileOutputStream(tempZipFile)).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry(ROOT_DIRECTORY))
                zipOutputStream.closeEntry()
                zipOutputStream.putNextEntry(ZipEntry(IMAGES_DIRECTORY))
                zipOutputStream.closeEntry()

                exportImages.forEach { exportImage ->
                    zipOutputStream.putNextEntry(ZipEntry(IMAGES_DIRECTORY + exportImage.image.fileName))
                    exportImage.file.inputStream().use { input ->
                        input.copyTo(zipOutputStream)
                    }
                    zipOutputStream.closeEntry()
                }

                zipOutputStream.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
                zipOutputStream.write(backup.toJsonString().toByteArray(Charsets.UTF_8))
                zipOutputStream.closeEntry()
            }

            val encryptedBytes = BackupCrypto.encryptZip(tempZipFile.readBytes(), password)
            context.contentResolver.openOutputStream(destinationUri, "w")?.use { outputStream ->
                outputStream.write(encryptedBytes)
            } ?: throw IOException("No pudimos abrir el archivo de destino.")
        } finally {
            tempZipFile.delete()
            password.fill('\u0000')
        }

        BackupResult(
            exportedMedications = medications.size,
            exportedPressureReadings = pressureReadings.size,
            exportedMedicationLogs = medicationLogs.size,
            exportedImages = exportImages.size,
            warnings = warnings.distinct(),
        )
    }

    suspend fun readBackupSummary(sourceUri: Uri, password: CharArray?): BackupSummary = withContext(Dispatchers.IO) {
        val backupPackage = openBackupPackage(sourceUri, password)
        try {
            backupPackage.summary
        } finally {
            backupPackage.tempZipFile.delete()
        }
    }

    suspend fun importBackup(
        sourceUri: Uri,
        strategy: ImportStrategy,
        password: CharArray?,
    ): ImportResult = withContext(Dispatchers.IO) {
        ContigoLog.d(IMPORT_TAG, "Importando respaldo con estrategia $strategy")
        val backupPackage = openBackupPackage(sourceUri, password)
        try {
            val outcome = when (strategy) {
                ImportStrategy.REPLACE_ALL -> importReplaceAll(backupPackage)
                ImportStrategy.MERGE -> importMerge(backupPackage)
            }
            firebaseSyncManager.enqueueBackupRestore(outcome.syncPlan)
            outcome.result
        } finally {
            backupPackage.tempZipFile.delete()
        }
    }

    private suspend fun importReplaceAll(backupPackage: BackupPackage): BackupImportOutcome {
        val warnings = backupPackage.warnings.toMutableList()
        val oldManagedImages = medicationDao.getAllMedications(DEFAULT_PATIENT_ID)
            .mapNotNull { medication -> medication.imageUri?.takeIf(medicationImageStorage::isAppManagedImage) }
        val createdImageUris = mutableListOf<String>()

        val restoredMedications = try {
            ZipFile(backupPackage.tempZipFile).use { zipFile ->
                backupPackage.medications.map { medication ->
                    val restoredImage = restoreMedicationImage(
                        zipFile = zipFile,
                        medicationId = medication.id,
                        imageDto = backupPackage.imagesByMedicationId[medication.id],
                        createdImageUris = createdImageUris,
                        warnings = warnings,
                    )
                    RestoredMedicationSync(
                        medication = medication.toEntity(imageUri = restoredImage.imageUri),
                        imageOperation = restoredImage.imageOperation,
                    )
                }
            }
        } catch (error: Exception) {
            createdImageUris.forEach(medicationImageStorage::deleteMedicationImage)
            throw error
        }

        val importedLogs = backupPackage.medicationLogs
            .filter { log ->
                if (backupPackage.medicationIds.contains(log.medicationId)) {
                    true
                } else {
                    warnings += "Se omitieron algunos registros de pastillas porque su medicamento no existe en el respaldo."
                    false
                }
            }
            .map { it.toEntity() }
        val importedPatient = backupPackage.patient.toEntity()
        val importedFamilyContact = backupPackage.familyContact?.toEntity()
        val importedHealthSettings = backupPackage.healthSettings?.toEntity()
        val importedMedications = restoredMedications.map(RestoredMedicationSync::medication)
        val importedPressureReadings = backupPackage.bloodPressureReadings.map { it.toEntity() }

        try {
            database.withTransaction {
                medicationReminderDao.deleteAll()
                medicationLogDao.deleteAll()
                bloodPressureDao.deleteAll()
                medicationDao.deleteAll()
                healthSettingsDao.deleteAll()
                familyContactDao.deleteAll()
                patientDao.deleteAll()

                patientDao.upsert(importedPatient)
                importedFamilyContact?.let { familyContactDao.upsertContact(it) }
                importedHealthSettings?.let { healthSettingsDao.upsert(it) }
                if (importedMedications.isNotEmpty()) {
                    medicationDao.insertAll(importedMedications)
                }
                if (importedPressureReadings.isNotEmpty()) {
                    bloodPressureDao.insertAll(importedPressureReadings)
                }
                if (importedLogs.isNotEmpty()) {
                    medicationLogDao.insertAll(importedLogs)
                }
            }
        } catch (error: Exception) {
            createdImageUris.forEach(medicationImageStorage::deleteMedicationImage)
            throw error
        }

        applyImportedPreferences(backupPackage.preferences, warnings)
        oldManagedImages.forEach { previousUri ->
            if (previousUri !in createdImageUris) {
                medicationImageStorage.deleteMedicationImage(previousUri)
            }
        }

        return BackupImportOutcome(
            result = ImportResult(
                importedMedications = importedMedications.size,
                importedPressureReadings = importedPressureReadings.size,
                importedMedicationLogs = importedLogs.size,
                importedImages = createdImageUris.size,
                skippedDuplicates = 0,
                errors = warnings.distinct(),
            ),
            syncPlan = BackupRestoreSyncPlan(
                strategy = ImportStrategy.REPLACE_ALL,
                patient = importedPatient,
                familyContact = importedFamilyContact,
                healthSettings = importedHealthSettings,
                medications = restoredMedications,
                pressureReadings = importedPressureReadings,
                medicationLogs = importedLogs,
            ),
        )
    }

    private suspend fun importMerge(backupPackage: BackupPackage): BackupImportOutcome {
        val warnings = backupPackage.warnings.toMutableList()
        val createdImageUris = mutableListOf<String>()
        val obsoleteImageUris = mutableSetOf<String>()
        var skippedDuplicates = 0

        val currentPatient = patientDao.getCurrentPatient()
        val currentContact = familyContactDao.getPrimaryContact(DEFAULT_PATIENT_ID)
        val currentSettings = healthSettingsDao.getSettings(DEFAULT_PATIENT_ID)
        val currentMedications = medicationDao.getAllMedications(DEFAULT_PATIENT_ID)
        val currentMedicationMap = currentMedications.associateBy { it.id }
        val currentPressureReadings = bloodPressureDao.getRecentReadings(DEFAULT_PATIENT_ID)
        val currentPressureIds = currentPressureReadings.mapTo(mutableSetOf()) { it.id }
        val currentPressureSignatures = currentPressureReadings.mapTo(mutableSetOf()) { it.signature() }
        val currentLogIds = medicationLogDao.getLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE)
            .mapTo(mutableSetOf()) { it.id }

        val restoredMedicationsToUpsert = try {
            ZipFile(backupPackage.tempZipFile).use { zipFile ->
                backupPackage.medications.mapNotNull { medication ->
                    val existingMedication = currentMedicationMap[medication.id]
                    val shouldImport = existingMedication == null || medication.updatedAt > existingMedication.updatedAt
                    if (!shouldImport) {
                        skippedDuplicates += 1
                        return@mapNotNull null
                    }

                    val restoredImage = restoreMedicationImage(
                        zipFile = zipFile,
                        medicationId = medication.id,
                        imageDto = backupPackage.imagesByMedicationId[medication.id],
                        createdImageUris = createdImageUris,
                        warnings = warnings,
                    )
                    existingMedication?.imageUri
                        ?.takeIf(medicationImageStorage::isAppManagedImage)
                        ?.let { existingImageUri ->
                            if (existingImageUri != restoredImage.imageUri) {
                                obsoleteImageUris += existingImageUri
                            }
                        }
                    RestoredMedicationSync(
                        medication = medication.toEntity(imageUri = restoredImage.imageUri),
                        imageOperation = restoredImage.imageOperation,
                    )
                }
            }
        } catch (error: Exception) {
            createdImageUris.forEach(medicationImageStorage::deleteMedicationImage)
            throw error
        }

        val medicationsToUpsert = restoredMedicationsToUpsert.map(RestoredMedicationSync::medication)
        val finalMedicationIds = currentMedicationMap.keys.toMutableSet().apply {
            addAll(medicationsToUpsert.map { it.id })
        }

        val pressureReadingsToInsert = backupPackage.bloodPressureReadings.mapNotNull { reading ->
            if (reading.id in currentPressureIds || reading.signature() in currentPressureSignatures) {
                skippedDuplicates += 1
                null
            } else {
                currentPressureIds += reading.id
                currentPressureSignatures += reading.signature()
                reading.toEntity()
            }
        }

        val medicationLogsToInsert = backupPackage.medicationLogs.mapNotNull { log ->
            when {
                log.medicationId !in finalMedicationIds -> {
                    warnings += "Se omitieron algunos registros de pastillas porque su medicamento no existe en este celular."
                    null
                }
                log.id in currentLogIds -> {
                    skippedDuplicates += 1
                    null
                }
                else -> {
                    currentLogIds += log.id
                    log.toEntity()
                }
            }
        }
        val patientToUpsert = backupPackage.patient
            .takeIf { currentPatient == null || it.updatedAt > currentPatient.updatedAt }
            ?.toEntity()
        val familyContactToUpsert = backupPackage.familyContact
            ?.takeIf { currentContact == null || it.updatedAt > currentContact.updatedAt }
            ?.toEntity()
        val healthSettingsToUpsert = backupPackage.healthSettings
            ?.takeIf { currentSettings == null || it.updatedAt > currentSettings.updatedAt }
            ?.toEntity()

        try {
            database.withTransaction {
                patientToUpsert?.let { patientDao.upsert(it) }
                familyContactToUpsert?.let { familyContactDao.upsertContact(it) }
                healthSettingsToUpsert?.let { healthSettingsDao.upsert(it) }
                if (medicationsToUpsert.isNotEmpty()) {
                    medicationDao.insertAll(medicationsToUpsert)
                }
                if (pressureReadingsToInsert.isNotEmpty()) {
                    bloodPressureDao.insertAll(pressureReadingsToInsert)
                }
                if (medicationLogsToInsert.isNotEmpty()) {
                    medicationLogDao.insertAll(medicationLogsToInsert)
                }
            }
        } catch (error: Exception) {
            createdImageUris.forEach(medicationImageStorage::deleteMedicationImage)
            throw error
        }

        obsoleteImageUris.forEach(medicationImageStorage::deleteMedicationImage)

        return BackupImportOutcome(
            result = ImportResult(
                importedMedications = medicationsToUpsert.size,
                importedPressureReadings = pressureReadingsToInsert.size,
                importedMedicationLogs = medicationLogsToInsert.size,
                importedImages = createdImageUris.size,
                skippedDuplicates = skippedDuplicates,
                errors = warnings.distinct(),
            ),
            syncPlan = BackupRestoreSyncPlan(
                strategy = ImportStrategy.MERGE,
                patient = patientToUpsert,
                familyContact = familyContactToUpsert,
                healthSettings = healthSettingsToUpsert,
                medications = restoredMedicationsToUpsert,
                pressureReadings = pressureReadingsToInsert,
                medicationLogs = medicationLogsToInsert,
            ),
        )
    }

    private fun prepareExportImage(
        medication: MedicationEntity,
        warnings: MutableList<String>,
    ): ExportImagePayload? {
        val imageUri = medication.imageUri ?: return null
        if (!medicationImageStorage.validateImageExists(imageUri)) {
            warnings += "Se omitieron algunas imágenes porque ya no existen en este celular."
            return null
        }
        val imageFile = medicationImageStorage.resolveManagedImageFile(imageUri)
        if (imageFile == null || !imageFile.exists()) {
            warnings += "Se omitieron algunas imágenes porque ya no existen en este celular."
            return null
        }

        val extension = imageFile.extension.ifBlank { "jpg" }
        val zipFileName = "medication_${sanitizeFileName(medication.id)}_1.$extension"
        return ExportImagePayload(
            image = BackupImageDto(
                medicationId = medication.id,
                fileName = zipFileName,
                originalImageUri = imageUri,
            ),
            file = imageFile,
        )
    }

    private fun restoreMedicationImage(
        zipFile: ZipFile,
        medicationId: String,
        imageDto: BackupImageDto?,
        createdImageUris: MutableList<String>,
        warnings: MutableList<String>,
    ): RestoredMedicationImage {
        val imageMetadata = imageDto
            ?: return RestoredMedicationImage(null, MedicationImageSyncOperation.KEEP)
        val entry = findImageEntry(zipFile, imageMetadata.fileName)
        if (entry == null) {
            warnings += "Algunas imágenes no pudieron restaurarse, pero los datos sí fueron importados."
            return RestoredMedicationImage(null, MedicationImageSyncOperation.KEEP)
        }

        val restoredUri = runCatching {
            zipFile.getInputStream(entry).use { inputStream ->
                medicationImageStorage.copyImageFromBackup(
                    inputStream = inputStream,
                    medicationId = medicationId,
                    sourceFileName = imageMetadata.fileName,
                ).also { restoredUri ->
                    createdImageUris += restoredUri
                }
            }
        }.onFailure { error ->
            ContigoLog.e(IMPORT_TAG, "No se pudo restaurar una imagen", error)
            warnings += "Algunas imágenes no pudieron restaurarse, pero los datos sí fueron importados."
        }.getOrNull()
        return RestoredMedicationImage(
            imageUri = restoredUri,
            imageOperation = if (restoredUri == null) {
                MedicationImageSyncOperation.KEEP
            } else {
                MedicationImageSyncOperation.UPLOAD
            },
        )
    }

    private suspend fun applyImportedPreferences(
        preferences: BackupPreferencesDto?,
        warnings: MutableList<String>,
    ) {
        if (preferences == null) {
            return
        }

        runCatching {
            reminderPreferencesRepository.setAllPreferences(
                remindersEnabled = preferences.remindersEnabled,
                repeatIntervalMinutes = preferences.repeatIntervalMinutes,
                maxRepeatCount = preferences.maxRepeatCount,
                soundEnabled = true,
                vibrationEnabled = true,
                notifyCaregiverOnMissed = true,
                voiceAssistantEnabled = preferences.voiceAssistantEnabled,
                voiceReminderEnabled = preferences.voiceReminderEnabled,
                voiceRepeatCount = preferences.voiceRepeatCount,
                easyModeEnabled = preferences.easyModeEnabled,
                voiceGuidanceEnabled = preferences.voiceGuidanceEnabled,
            )
        }.onFailure { error ->
            ContigoLog.e(IMPORT_TAG, "No se pudieron restaurar preferencias", error)
            warnings += "No pudimos restaurar algunas preferencias de recordatorio."
        }
    }

    private fun openBackupPackage(sourceUri: Uri, password: CharArray?): BackupPackage {
        val copiedFile = copyUriToTemporaryFile(sourceUri)
        val tempZipFile = try {
            val encrypted = copiedFile.inputStream().buffered().use(BackupCrypto::isEncryptedBackup)
            if (encrypted) {
                if (password == null || password.isEmpty()) {
                    throw BackupFormatException("Este respaldo está cifrado. Escribe la contraseña.")
                }
                val decryptedZip = copiedFile.inputStream().use { input ->
                    BackupCrypto.decryptToZip(input, password)
                }
                val decryptedFile = File(context.cacheDir, "backup/import-${System.currentTimeMillis()}.zip")
                decryptedFile.writeBytes(decryptedZip)
                copiedFile.delete()
                decryptedFile
            } else {
                copiedFile
            }
        } catch (error: BackupFormatException) {
            copiedFile.delete()
            throw error
        }

        return try {
            ZipFile(tempZipFile).use { zipFile ->
                val entries = zipEntries(zipFile)
                val backupEntry = entries.firstOrNull { entry ->
                    !entry.isDirectory && entry.name.endsWith("backup.json")
                } ?: throw BackupFormatException("El archivo seleccionado no parece ser un respaldo de Contigo.")

                val jsonText = zipFile.getInputStream(backupEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val backup = parseBackup(jsonText)
                validateBackupHeader(backup)

                val sanitized = sanitizeBackup(
                    backup = backup,
                    zipEntryNames = entries.mapTo(mutableSetOf()) { it.name },
                )
                BackupPackage(
                    tempZipFile = tempZipFile,
                    patient = sanitized.patient,
                    familyContact = sanitized.familyContact,
                    healthSettings = sanitized.healthSettings,
                    medications = sanitized.medications,
                    medicationLogs = sanitized.medicationLogs,
                    bloodPressureReadings = sanitized.bloodPressureReadings,
                    preferences = sanitized.preferences,
                    images = sanitized.images,
                    imagesByMedicationId = sanitized.images.associateBy { it.medicationId },
                    medicationIds = sanitized.medications.mapTo(mutableSetOf()) { it.id },
                    warnings = sanitized.warnings,
                    summary = BackupSummary(
                        patientName = sanitized.patient.fullName,
                        medicationsCount = sanitized.medications.size,
                        pressureReadingsCount = sanitized.bloodPressureReadings.size,
                        medicationLogsCount = sanitized.medicationLogs.size,
                        imagesCount = sanitized.images.size,
                        createdAt = backup.createdAt,
                        backupVersion = backup.backupVersion,
                    ),
                )
            }
        } catch (error: JSONException) {
            tempZipFile.delete()
            throw BackupFormatException("El respaldo esta danado o incompleto.")
        } catch (error: IllegalArgumentException) {
            tempZipFile.delete()
            throw BackupFormatException("El respaldo esta danado o incompleto.")
        } catch (error: BackupFormatException) {
            tempZipFile.delete()
            throw error
        } catch (error: IOException) {
            tempZipFile.delete()
            throw BackupFormatException("El archivo seleccionado no parece ser un respaldo de Contigo.")
        }
    }

    private fun copyUriToTemporaryFile(sourceUri: Uri): File {
        val tempDirectory = File(context.cacheDir, "backup/import").apply { mkdirs() }
        val fileName = "contigo-import-${System.currentTimeMillis()}.bin"
        val destinationFile = File(tempDirectory, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw BackupFormatException("No pudimos leer el archivo seleccionado.")
        return destinationFile
    }

    private fun validateBackupHeader(backup: ContigoBackup) {
        if (backup.app !in SUPPORTED_APP_NAMES) {
            throw BackupFormatException("El archivo seleccionado no parece ser un respaldo de Contigo.")
        }
        if (backup.backupVersion != BACKUP_VERSION) {
            throw BackupFormatException("Este respaldo fue creado con una version no compatible.")
        }
    }

    private fun sanitizeBackup(
        backup: ContigoBackup,
        zipEntryNames: Set<String>,
    ): SanitizedBackup {
        val warnings = mutableListOf<String>()
        val patient = backup.patient
            ?.takeIf { it.id.isNotBlank() && it.fullName.isNotBlank() }
            ?.copy(id = DEFAULT_PATIENT_ID)
            ?: throw BackupFormatException("El respaldo esta danado o incompleto.")

        val healthSettings = backup.healthSettings?.let { settings ->
            if (
                settings.id.isBlank() ||
                settings.systolicMinNormal >= settings.systolicMaxNormal ||
                settings.diastolicMinNormal >= settings.diastolicMaxNormal ||
                settings.pulseMinNormal >= settings.pulseMaxNormal
            ) {
                warnings += "Se omitieron los rangos medicos porque el respaldo contiene valores invalidos."
                null
            } else {
                settings.copy(patientId = DEFAULT_PATIENT_ID)
            }
        }

        val familyContact = backup.familyContact?.let { contact ->
            if (contact.id.isBlank() || contact.fullName.isBlank() || contact.phone.isBlank()) {
                warnings += "Se omitio el contacto familiar porque el respaldo esta incompleto."
                null
            } else {
                contact.copy(patientId = DEFAULT_PATIENT_ID)
            }
        }

        val medicationsById = linkedMapOf<String, BackupMedicationDto>()
        backup.medications.forEach { medication ->
            val isValid = medication.id.isNotBlank() &&
                medication.name.isNotBlank() &&
                medication.dose.isNotBlank() &&
                medication.scheduleTime.matches(SCHEDULE_TIME_REGEX)
            if (!isValid) {
                warnings += "Se omitieron algunos medicamentos porque el respaldo esta incompleto."
                return@forEach
            }

            val normalized = medication.copy(patientId = DEFAULT_PATIENT_ID)
            val existing = medicationsById[normalized.id]
            if (existing == null || normalized.updatedAt > existing.updatedAt) {
                medicationsById[normalized.id] = normalized
            } else {
                warnings += "Se ignoraron medicamentos repetidos dentro del respaldo."
            }
        }

        val pressureIds = mutableSetOf<String>()
        val pressureSignatures = mutableSetOf<String>()
        val pressureReadings = mutableListOf<BackupBloodPressureDto>()
        backup.bloodPressureReadings.forEach { reading ->
            val valid = reading.id.isNotBlank() && reading.systolic > 0 && reading.diastolic > 0
            val signature = reading.signature()
            if (!valid) {
                warnings += "Se omitieron algunas presiones porque el respaldo contiene valores inválidos."
                return@forEach
            }
            if (reading.id in pressureIds || signature in pressureSignatures) {
                warnings += "Se ignoraron presiones repetidas dentro del respaldo."
                return@forEach
            }
            pressureIds += reading.id
            pressureSignatures += signature
            pressureReadings += reading.copy(patientId = DEFAULT_PATIENT_ID)
        }

        val logById = linkedMapOf<String, BackupMedicationLogDto>()
        backup.medicationLogs.forEach { log ->
            if (log.id.isBlank() || log.medicationId.isBlank()) {
                warnings += "Se omitieron algunos registros de pastillas porque el respaldo esta incompleto."
                return@forEach
            }
            val normalized = log.copy(patientId = DEFAULT_PATIENT_ID)
            val existing = logById[normalized.id]
            if (existing == null || normalized.createdAt > existing.createdAt) {
                logById[normalized.id] = normalized
            } else {
                warnings += "Se ignoraron registros repetidos dentro del respaldo."
            }
        }

        val imageByMedicationId = linkedMapOf<String, BackupImageDto>()
        backup.images.forEach { image ->
            val valid = image.medicationId.isNotBlank() &&
                image.fileName.isNotBlank() &&
                medicationsById.containsKey(image.medicationId)
            if (!valid) {
                warnings += "Se omitieron algunas imágenes porque el respaldo está incompleto."
                return@forEach
            }

            val existsInZip = zipEntryNames.any { entryName ->
                entryName == IMAGES_DIRECTORY + image.fileName ||
                    entryName.endsWith("/images/${image.fileName}") ||
                    entryName.endsWith("/${image.fileName}")
            }
            if (!existsInZip) {
                warnings += "Algunas imágenes no pudieron restaurarse, pero los datos sí fueron importados."
            }
            imageByMedicationId[image.medicationId] = image
        }

        val preferences = backup.preferences?.copy(
            repeatIntervalMinutes = backup.preferences.repeatIntervalMinutes.coerceIn(1, 10),
            maxRepeatCount = backup.preferences.maxRepeatCount.coerceIn(1, 3),
            voiceRepeatCount = backup.preferences.voiceRepeatCount.coerceIn(1, 3),
        )

        return SanitizedBackup(
            patient = patient,
            familyContact = familyContact,
            healthSettings = healthSettings,
            medications = medicationsById.values.toList(),
            medicationLogs = logById.values.toList(),
            bloodPressureReadings = pressureReadings,
            preferences = preferences,
            images = imageByMedicationId.values.toList(),
            warnings = warnings,
        )
    }

    private fun parseBackup(jsonText: String): ContigoBackup {
        val root = JSONObject(jsonText)
        return ContigoBackup(
            app = root.optString("app"),
            backupVersion = root.optInt("backupVersion", -1),
            createdAt = root.optLong("createdAt", 0L),
            deviceInfo = root.optNullableString("deviceInfo"),
            patient = root.optJSONObject("patient")?.toPatientDto(),
            familyContact = root.optJSONObject("familyContact")?.toFamilyContactDto(),
            healthSettings = root.optJSONObject("healthSettings")?.toHealthSettingsDto(),
            medications = root.optJSONArray("medications").orEmpty().map { it.toMedicationDto() },
            medicationLogs = root.optJSONArray("medicationLogs").orEmpty().map { it.toMedicationLogDto() },
            bloodPressureReadings = root.optJSONArray("bloodPressureReadings").orEmpty().map { it.toBloodPressureDto() },
            preferences = root.optJSONObject("preferences")?.toPreferencesDto(),
            images = root.optJSONArray("images").orEmpty().map { it.toImageDto() },
        )
    }

    private fun ContigoBackup.toJsonString(): String {
        val root = JSONObject()
            .put("app", app)
            .put("backupVersion", backupVersion)
            .put("createdAt", createdAt)
            .put("deviceInfo", deviceInfo)
            .put("patient", patient?.toJson() ?: JSONObject.NULL)
            .put("familyContact", familyContact?.toJson() ?: JSONObject.NULL)
            .put("healthSettings", healthSettings?.toJson() ?: JSONObject.NULL)
            .put("medications", JSONArray().apply { medications.forEach { put(it.toJson()) } })
            .put("medicationLogs", JSONArray().apply { medicationLogs.forEach { put(it.toJson()) } })
            .put("bloodPressureReadings", JSONArray().apply { bloodPressureReadings.forEach { put(it.toJson()) } })
            .put("preferences", preferences?.toJson() ?: JSONObject.NULL)
            .put("images", JSONArray().apply { images.forEach { put(it.toJson()) } })
        return root.toString(2)
    }

    private fun BackupPatientDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("fullName", fullName)
            .put("age", age)
            .put("notes", notes)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)

    private fun BackupFamilyContactDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("patientId", patientId)
            .put("fullName", fullName)
            .put("phone", phone)
            .put("relationship", relationship)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)

    private fun BackupHealthSettingsDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("patientId", patientId)
            .put("systolicMinNormal", systolicMinNormal)
            .put("systolicMaxNormal", systolicMaxNormal)
            .put("diastolicMinNormal", diastolicMinNormal)
            .put("diastolicMaxNormal", diastolicMaxNormal)
            .put("pulseMinNormal", pulseMinNormal)
            .put("pulseMaxNormal", pulseMaxNormal)
            .put("doctorRecommendation", doctorRecommendation)
            .put("updatedAt", updatedAt)

    private fun BackupMedicationDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("patientId", patientId)
            .put("name", name)
            .put("dose", dose)
            .put("color", color)
            .put("shape", shape)
            .put("instructions", instructions)
            .put("scheduleTime", scheduleTime)
            .put("isActive", isActive)
            .put("scheduleType", scheduleType)
            .put("startDate", startDate)
            .put("endDate", endDate ?: JSONObject.NULL)
            .put("daysOfWeek", JSONArray(daysOfWeek))
            .put("specificDates", JSONArray(specificDates.map { it.toString() }))
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)

    private fun BackupMedicationLogDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("medicationId", medicationId)
            .put("patientId", patientId)
            .put("scheduledFor", scheduledFor)
            .put("takenAt", takenAt)
            .put("status", status)
            .put("skipReason", skipReason)
            .put("createdAt", createdAt)

    private fun BackupBloodPressureDto.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("patientId", patientId)
            .put("systolic", systolic)
            .put("diastolic", diastolic)
            .put("pulse", pulse)
            .put("status", status)
            .put("notes", notes)
            .put("measuredAt", measuredAt)
            .put("createdAt", createdAt)

    private fun BackupPreferencesDto.toJson(): JSONObject =
        JSONObject()
            .put("remindersEnabled", remindersEnabled)
            .put("repeatIntervalMinutes", repeatIntervalMinutes)
            .put("maxRepeatCount", maxRepeatCount)
            .put("voiceAssistantEnabled", voiceAssistantEnabled)
            .put("voiceReminderEnabled", voiceReminderEnabled)
            .put("voiceRepeatCount", voiceRepeatCount)
            .put("easyModeEnabled", easyModeEnabled)
            .put("voiceGuidanceEnabled", voiceGuidanceEnabled)

    private fun BackupImageDto.toJson(): JSONObject =
        JSONObject()
            .put("medicationId", medicationId)
            .put("fileName", fileName)
            .put("originalImageUri", originalImageUri)

    private fun JSONObject.toPatientDto(): BackupPatientDto =
        BackupPatientDto(
            id = optString("id"),
            fullName = optString("fullName"),
            age = optNullableInt("age"),
            notes = optNullableString("notes"),
            createdAt = optLong("createdAt"),
            updatedAt = optLong("updatedAt"),
        )

    private fun JSONObject.toFamilyContactDto(): BackupFamilyContactDto =
        BackupFamilyContactDto(
            id = optString("id"),
            patientId = optString("patientId"),
            fullName = optString("fullName"),
            phone = optString("phone"),
            relationship = optNullableString("relationship"),
            createdAt = optLong("createdAt"),
            updatedAt = optLong("updatedAt"),
        )

    private fun JSONObject.toHealthSettingsDto(): BackupHealthSettingsDto =
        BackupHealthSettingsDto(
            id = optString("id"),
            patientId = optString("patientId"),
            systolicMinNormal = optInt("systolicMinNormal"),
            systolicMaxNormal = optInt("systolicMaxNormal"),
            diastolicMinNormal = optInt("diastolicMinNormal"),
            diastolicMaxNormal = optInt("diastolicMaxNormal"),
            pulseMinNormal = optInt("pulseMinNormal"),
            pulseMaxNormal = optInt("pulseMaxNormal"),
            doctorRecommendation = optNullableString("doctorRecommendation"),
            updatedAt = optLong("updatedAt"),
        )

    private fun JSONObject.toMedicationDto(): BackupMedicationDto =
        BackupMedicationDto(
            id = optString("id"),
            patientId = optString("patientId"),
            name = optString("name"),
            dose = optString("dose"),
            color = optNullableString("color"),
            shape = optNullableString("shape"),
            instructions = optNullableString("instructions"),
            scheduleTime = optString("scheduleTime"),
            isActive = optBoolean("isActive", true),
            scheduleType = optString("scheduleType").ifBlank { "ALWAYS" },
            startDate = optString("startDate").ifBlank { MedicationScheduleDefaults.todayIso() },
            endDate = optNullableString("endDate"),
            daysOfWeek = optJSONArray("daysOfWeek")?.let { array ->
                List(array.length()) { array.getInt(it) }
            } ?: MedicationScheduleDefaults.allDaysOfWeek.toList(),
            specificDates = optJSONArray("specificDates")?.let { array ->
                List(array.length()) { LocalDate.parse(array.getString(it)) }
            } ?: emptyList(),
            createdAt = optLong("createdAt"),
            updatedAt = optLong("updatedAt"),
        )

    private fun JSONObject.toMedicationLogDto(): BackupMedicationLogDto =
        BackupMedicationLogDto(
            id = optString("id"),
            medicationId = optString("medicationId"),
            patientId = optString("patientId"),
            scheduledFor = optLong("scheduledFor"),
            takenAt = optNullableLong("takenAt"),
            status = optString("status"),
            skipReason = optNullableString("skipReason"),
            createdAt = optLong("createdAt"),
        )

    private fun JSONObject.toBloodPressureDto(): BackupBloodPressureDto =
        BackupBloodPressureDto(
            id = optString("id"),
            patientId = optString("patientId"),
            systolic = optInt("systolic"),
            diastolic = optInt("diastolic"),
            pulse = optNullableInt("pulse"),
            status = optString("status"),
            notes = optNullableString("notes"),
            measuredAt = optLong("measuredAt"),
            createdAt = optLong("createdAt"),
        )

    private fun JSONObject.toPreferencesDto(): BackupPreferencesDto =
        BackupPreferencesDto(
            remindersEnabled = optBoolean("remindersEnabled", false),
            repeatIntervalMinutes = optInt("repeatIntervalMinutes", 10),
            maxRepeatCount = optInt("maxRepeatCount", 3),
            voiceAssistantEnabled = optBoolean("voiceAssistantEnabled", false),
            voiceReminderEnabled = optBoolean("voiceReminderEnabled", false),
            voiceRepeatCount = optInt("voiceRepeatCount", 2),
            easyModeEnabled = optBoolean("easyModeEnabled", false),
            voiceGuidanceEnabled = optBoolean("voiceGuidanceEnabled", false),
        )

    private fun JSONObject.toImageDto(): BackupImageDto =
        BackupImageDto(
            medicationId = optString("medicationId"),
            fileName = optString("fileName"),
            originalImageUri = optNullableString("originalImageUri"),
        )

    private fun PatientEntity.toBackupDto(): BackupPatientDto =
        BackupPatientDto(
            id = id,
            fullName = fullName,
            age = age,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun FamilyContactEntity.toBackupDto(): BackupFamilyContactDto =
        BackupFamilyContactDto(
            id = id,
            patientId = patientId,
            fullName = fullName,
            phone = phone,
            relationship = relationship,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun HealthSettingsEntity.toBackupDto(): BackupHealthSettingsDto =
        BackupHealthSettingsDto(
            id = id,
            patientId = patientId,
            systolicMinNormal = systolicMinNormal,
            systolicMaxNormal = systolicMaxNormal,
            diastolicMinNormal = diastolicMinNormal,
            diastolicMaxNormal = diastolicMaxNormal,
            pulseMinNormal = pulseMinNormal,
            pulseMaxNormal = pulseMaxNormal,
            doctorRecommendation = doctorRecommendation,
            updatedAt = updatedAt,
        )

    private fun MedicationEntity.toBackupDto(): BackupMedicationDto =
        BackupMedicationDto(
            id = id,
            patientId = patientId,
            name = name,
            dose = dose,
            color = color,
            shape = shape,
            instructions = instructions,
            scheduleTime = scheduleTime,
            isActive = isActive,
            scheduleType = scheduleType,
            startDate = startDate,
            endDate = endDate,
            daysOfWeek = daysOfWeek,
            specificDates = specificDates,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun MedicationLogEntity.toBackupDto(): BackupMedicationLogDto =
        BackupMedicationLogDto(
            id = id,
            medicationId = medicationId,
            patientId = patientId,
            scheduledFor = scheduledFor,
            takenAt = takenAt,
            status = status,
            skipReason = skipReason,
            createdAt = createdAt,
        )

    private fun BloodPressureEntity.toBackupDto(): BackupBloodPressureDto =
        BackupBloodPressureDto(
            id = id,
            patientId = patientId,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            status = status,
            notes = notes,
            measuredAt = measuredAt,
            createdAt = createdAt,
        )

    private fun BackupPatientDto.toEntity(): PatientEntity =
        PatientEntity(
            id = DEFAULT_PATIENT_ID,
            fullName = fullName.trim(),
            age = age,
            notes = notes?.trim()?.ifBlank { null },
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            updatedAt = updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupFamilyContactDto.toEntity(): FamilyContactEntity =
        FamilyContactEntity(
            id = id,
            patientId = DEFAULT_PATIENT_ID,
            fullName = fullName.trim(),
            phone = phone.trim(),
            relationship = relationship?.trim()?.ifBlank { null },
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            updatedAt = updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupHealthSettingsDto.toEntity(): HealthSettingsEntity =
        HealthSettingsEntity(
            id = id,
            patientId = DEFAULT_PATIENT_ID,
            systolicMinNormal = systolicMinNormal,
            systolicMaxNormal = systolicMaxNormal,
            diastolicMinNormal = diastolicMinNormal,
            diastolicMaxNormal = diastolicMaxNormal,
            pulseMinNormal = pulseMinNormal,
            pulseMaxNormal = pulseMaxNormal,
            doctorRecommendation = doctorRecommendation?.trim()?.ifBlank { null },
            updatedAt = updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupMedicationDto.toEntity(imageUri: String?): MedicationEntity =
        MedicationEntity(
            id = id,
            patientId = DEFAULT_PATIENT_ID,
            name = name.trim(),
            dose = dose.trim(),
            color = color?.trim()?.ifBlank { null },
            shape = shape?.trim()?.ifBlank { null },
            instructions = instructions?.trim()?.ifBlank { null },
            scheduleTime = scheduleTime,
            imageUri = imageUri,
            isActive = isActive,
            scheduleType = scheduleType,
            startDate = startDate.ifBlank { MedicationScheduleDefaults.todayIso() },
            endDate = endDate?.ifBlank { null },
            daysOfWeek = daysOfWeek,
            specificDates = specificDates,
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            updatedAt = updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupMedicationLogDto.toEntity(): MedicationLogEntity =
        MedicationLogEntity(
            id = id,
            medicationId = medicationId,
            patientId = DEFAULT_PATIENT_ID,
            scheduledFor = scheduledFor,
            takenAt = takenAt,
            status = status,
            skipReason = skipReason,
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupBloodPressureDto.toEntity(): BloodPressureEntity =
        BloodPressureEntity(
            id = id,
            patientId = DEFAULT_PATIENT_ID,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            status = status,
            notes = notes?.trim()?.ifBlank { null },
            measuredAt = measuredAt,
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )

    private fun BackupBloodPressureDto.signature(): String =
        "${measuredAt}_${systolic}_${diastolic}"

    private fun BloodPressureEntity.signature(): String =
        "${measuredAt}_${systolic}_${diastolic}"

    private fun JSONArray?.orEmpty(): List<JSONObject> {
        if (this == null) {
            return emptyList()
        }
        return buildList(length()) {
            for (index in 0 until length()) {
                optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key).ifBlank { null }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        return if (isNull(key)) null else optInt(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (isNull(key)) null else optLong(key)
    }

    private fun zipEntries(zipFile: ZipFile): List<ZipEntry> {
        val entries = zipFile.entries()
        val result = mutableListOf<ZipEntry>()
        while (entries.hasMoreElements()) {
            result += entries.nextElement()
        }
        return result
    }

    private fun findImageEntry(
        zipFile: ZipFile,
        fileName: String,
    ): ZipEntry? {
        return zipEntries(zipFile).firstOrNull { entry ->
            !entry.isDirectory && (
                entry.name == IMAGES_DIRECTORY + fileName ||
                    entry.name.endsWith("/images/$fileName") ||
                    entry.name.endsWith("/$fileName")
                )
        }
    }

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9_-]"), "_")
    }

    fun clearTemporaryBackups() {
        val tempDirectory = File(context.cacheDir, "backup/import")
        if (tempDirectory.exists()) {
            tempDirectory.listFiles { file -> file.extension == "zip" }
                ?.forEach { it.delete() }
        }
    }

    data class BackupFileSuggestion(
        val fileName: String,
        val createdAt: Long,
    )

    fun suggestedFileName(createdAt: Long = System.currentTimeMillis()): String {
        val instant = Instant.ofEpochMilli(createdAt)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
            .withZone(ZoneId.systemDefault())
        return "contigo-backup-${formatter.format(instant)}.zip"
    }

    private data class ExportImagePayload(
        val image: BackupImageDto,
        val file: File,
    )

    private data class BackupImportOutcome(
        val result: ImportResult,
        val syncPlan: BackupRestoreSyncPlan,
    )

    private data class RestoredMedicationImage(
        val imageUri: String?,
        val imageOperation: MedicationImageSyncOperation,
    )

    private data class SanitizedBackup(
        val patient: BackupPatientDto,
        val familyContact: BackupFamilyContactDto?,
        val healthSettings: BackupHealthSettingsDto?,
        val medications: List<BackupMedicationDto>,
        val medicationLogs: List<BackupMedicationLogDto>,
        val bloodPressureReadings: List<BackupBloodPressureDto>,
        val preferences: BackupPreferencesDto?,
        val images: List<BackupImageDto>,
        val warnings: List<String>,
    )

    private data class BackupPackage(
        val tempZipFile: File,
        val patient: BackupPatientDto,
        val familyContact: BackupFamilyContactDto?,
        val healthSettings: BackupHealthSettingsDto?,
        val medications: List<BackupMedicationDto>,
        val medicationLogs: List<BackupMedicationLogDto>,
        val bloodPressureReadings: List<BackupBloodPressureDto>,
        val preferences: BackupPreferencesDto?,
        val images: List<BackupImageDto>,
        val imagesByMedicationId: Map<String, BackupImageDto>,
        val medicationIds: Set<String>,
        val warnings: List<String>,
        val summary: BackupSummary,
    )

    companion object {
        private const val APP_NAME = "Contigo"
        private val SUPPORTED_APP_NAMES = setOf(APP_NAME, "CuidaVoz")
        private const val BACKUP_VERSION = 1
        private const val ROOT_DIRECTORY = "contigo-backup/"
        private const val IMAGES_DIRECTORY = "contigo-backup/images/"
        private const val BACKUP_JSON_ENTRY = "contigo-backup/backup.json"
        private val SCHEDULE_TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        private const val EXPORT_TAG = "[Contigo][Export]"
        private const val IMPORT_TAG = "[Contigo][Import]"
    }
}

class BackupFormatException(
    override val message: String,
) : IllegalStateException(message)
