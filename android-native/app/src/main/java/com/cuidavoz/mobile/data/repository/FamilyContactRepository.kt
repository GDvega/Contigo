package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.FamilyContactDao
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager

class FamilyContactRepository(
    private val familyContactDao: FamilyContactDao,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
) {
    fun observePrimaryContact(patientId: String) =
        familyContactDao.observePrimaryContact(patientId)

    suspend fun getPrimaryContact(patientId: String) =
        familyContactDao.getPrimaryContact(patientId)

    suspend fun upsertContact(contact: FamilyContactEntity) {
        familyContactDao.upsertContact(contact)
        firebaseSyncManager?.enqueueFamilyContact(contact)
    }

    suspend fun updateContact(contact: FamilyContactEntity) {
        familyContactDao.updateContact(contact)
        firebaseSyncManager?.enqueueFamilyContact(contact)
    }
}
