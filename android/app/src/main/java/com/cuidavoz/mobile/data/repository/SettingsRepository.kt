package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.HealthSettingsDao
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.reminders.ReminderPreferences
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.reminders.VoicePreferences

class SettingsRepository(
    private val healthSettingsDao: HealthSettingsDao,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
) {
    fun observeHealthSettings(patientId: String) = healthSettingsDao.observeSettings(patientId)

    fun observeReminderPreferences() = reminderPreferencesRepository.preferencesFlow

    fun observeVoicePreferences() = reminderPreferencesRepository.voicePreferencesFlow

    suspend fun getHealthSettings(patientId: String) = healthSettingsDao.getSettings(patientId)

    suspend fun upsertHealthSettings(settings: HealthSettingsEntity) {
        healthSettingsDao.upsert(settings)
        firebaseSyncManager?.enqueueHealthSettings(settings)
    }

    suspend fun getReminderPreferences(): ReminderPreferences =
        reminderPreferencesRepository.getCurrentPreferences()

    suspend fun getVoicePreferences(): VoicePreferences =
        reminderPreferencesRepository.getCurrentVoicePreferences()

    suspend fun setRemindersEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setRemindersEnabled(enabled)

    suspend fun setVoiceAssistantEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setVoiceAssistantEnabled(enabled)

    suspend fun setRepeatIntervalMinutes(minutes: Int) =
        reminderPreferencesRepository.setRepeatIntervalMinutes(minutes)

    suspend fun setMaxRepeatCount(count: Int) =
        reminderPreferencesRepository.setMaxRepeatCount(count)

    suspend fun setSoundEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setSoundEnabled(enabled)

    suspend fun setVibrationEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setVibrationEnabled(enabled)

    suspend fun setNotifyCaregiverOnMissed(enabled: Boolean) =
        reminderPreferencesRepository.setNotifyCaregiverOnMissed(enabled)

    suspend fun setVoiceReminderEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setVoiceReminderEnabled(enabled)

    suspend fun setVoiceRepeatCount(count: Int) =
        reminderPreferencesRepository.setVoiceRepeatCount(count)

    suspend fun setEasyModeEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setEasyModeEnabled(enabled)

    suspend fun setVoiceGuidanceEnabled(enabled: Boolean) =
        reminderPreferencesRepository.setVoiceGuidanceEnabled(enabled)
}
