package com.cuidavoz.mobile.data.sync

import com.cuidavoz.mobile.data.backup.BackupRestoreSyncPlan
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.cuidavoz.mobile.domain.sync.SyncOperation
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderPreferences
import com.cuidavoz.mobile.reminders.VoicePreferences
import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.Flow

interface SyncManager : DefaultLifecycleObserver {
    val syncStatusText: Flow<String>
    
    fun start()
    fun stop()
    
    fun attachReminderScheduler(scheduler: MedicationReminderScheduler)
    
    suspend fun ensureSignedIn(): String?
    
    suspend fun enqueuePatient(patient: PatientEntity)
    
    suspend fun enqueueMedication(
        medication: MedicationEntity,
        operation: SyncOperation = SyncOperation.UPDATE,
        imageSyncOperation: MedicationImageSyncOperation = MedicationImageSyncOperation.KEEP,
    )
    
    suspend fun enqueuePressureReading(reading: BloodPressureEntity)
    
    suspend fun enqueueDeletePressureReading(reading: BloodPressureEntity)
    
    suspend fun enqueueMedicationLog(log: MedicationLogEntity)
    
    suspend fun enqueueHealthSettings(settings: HealthSettingsEntity)
    
    suspend fun enqueueReminderPreferences(
        reminderPrefs: ReminderPreferences,
        voicePrefs: VoicePreferences,
    )
    
    suspend fun enqueueFamilyContact(contact: FamilyContactEntity)
    
    suspend fun enqueueBackupRestore(plan: BackupRestoreSyncPlan)
    
    suspend fun enqueueAlert(
        type: String,
        message: String,
        medicationIds: List<String>,
        scheduledAt: Long?,
        severity: String,
    )
    
    suspend fun createLinkCode(): String?
    
    suspend fun linkCaregiver(code: String): LinkCaregiverResult
    
    suspend fun syncPendingNow()
}
