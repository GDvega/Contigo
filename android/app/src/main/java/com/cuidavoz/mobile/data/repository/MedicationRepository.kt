package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.cuidavoz.mobile.domain.sync.SyncOperation

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val firebaseSyncManager: SyncManager? = null,
) {
    fun observeActiveMedications(patientId: String) =
        medicationDao.observeActiveMedications(patientId)

    suspend fun getActiveMedications(patientId: String) =
        medicationDao.getActiveMedications(patientId)

    suspend fun getMedicationById(id: String) =
        medicationDao.getMedicationById(id)

    suspend fun getMedicationsByIds(ids: List<String>) =
        if (ids.isEmpty()) emptyList() else medicationDao.getMedicationsByIds(ids)

    suspend fun upsert(
        medication: MedicationEntity,
        imageSyncOperation: MedicationImageSyncOperation = MedicationImageSyncOperation.KEEP,
    ) {
        medicationDao.upsert(medication)
        firebaseSyncManager?.enqueueMedication(medication, SyncOperation.UPDATE, imageSyncOperation)
    }

    suspend fun softDelete(id: String, updatedAt: Long) {
        medicationDao.softDelete(id, updatedAt)
        medicationDao.getMedicationById(id)?.let { firebaseSyncManager?.enqueueMedication(it, SyncOperation.UPDATE) }
    }

    suspend fun updateMedicationImage(
        medicationId: String,
        imageUri: String?,
        updatedAt: Long,
    ) {
        medicationDao.updateMedicationImage(medicationId, imageUri, updatedAt)
        medicationDao.getMedicationById(medicationId)
            ?.let {
                val imageSyncOperation = if (imageUri.isNullOrBlank()) {
                    MedicationImageSyncOperation.DELETE
                } else {
                    MedicationImageSyncOperation.UPLOAD
                }
                firebaseSyncManager?.enqueueMedication(it, SyncOperation.UPDATE, imageSyncOperation)
            }
    }

    suspend fun removeMedicationImage(
        medicationId: String,
        updatedAt: Long,
    ) {
        medicationDao.removeMedicationImage(medicationId, updatedAt)
        medicationDao.getMedicationById(medicationId)
            ?.let {
                firebaseSyncManager?.enqueueMedication(
                    it,
                    SyncOperation.UPDATE,
                    MedicationImageSyncOperation.DELETE,
                )
            }
    }

    suspend fun deactivateMedicationAndDeleteImage(
        medicationId: String,
        updatedAt: Long,
    ) {
        medicationDao.deactivateMedicationAndDeleteImage(medicationId, updatedAt)
        medicationDao.getMedicationById(medicationId)
            ?.let {
                firebaseSyncManager?.enqueueMedication(
                    it,
                    SyncOperation.UPDATE,
                    MedicationImageSyncOperation.DELETE,
                )
            }
    }
}
