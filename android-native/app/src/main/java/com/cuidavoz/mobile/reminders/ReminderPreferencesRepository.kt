package com.cuidavoz.mobile.reminders

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderPreferencesDataStore by preferencesDataStore(name = "reminder_preferences")

data class ReminderPreferences(
    val remindersEnabled: Boolean = false,
    val repeatIntervalMinutes: Int = 10,
    val maxRepeatCount: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notifyCaregiverOnMissed: Boolean = true,
)

data class VoicePreferences(
    val voiceAssistantEnabled: Boolean = true,
    val voiceReminderEnabled: Boolean = false,
    val voiceRepeatCount: Int = 2,
    val easyModeEnabled: Boolean = true,
    val voiceGuidanceEnabled: Boolean = false,
)

class ReminderPreferencesRepository(
    private val context: Context,
) {
    private object Keys {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REPEAT_INTERVAL_MINUTES = intPreferencesKey("repeat_interval_minutes")
        val MAX_REPEAT_COUNT = intPreferencesKey("max_repeat_count")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFY_CAREGIVER_ON_MISSED = booleanPreferencesKey("notify_caregiver_on_missed")
        val VOICE_ASSISTANT_ENABLED = booleanPreferencesKey("voice_assistant_enabled")
        val VOICE_REMINDER_ENABLED = booleanPreferencesKey("voice_reminder_enabled")
        val VOICE_REPEAT_COUNT = intPreferencesKey("voice_repeat_count")
        val EASY_MODE_ENABLED = booleanPreferencesKey("easy_mode_enabled")
        val VOICE_GUIDANCE_ENABLED = booleanPreferencesKey("voice_guidance_enabled")
    }

    val preferencesFlow: Flow<ReminderPreferences> =
        context.reminderPreferencesDataStore.data.map { preferences ->
            ReminderPreferences(
                remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: false,
                repeatIntervalMinutes = preferences[Keys.REPEAT_INTERVAL_MINUTES] ?: 10,
                maxRepeatCount = preferences[Keys.MAX_REPEAT_COUNT] ?: 3,
                soundEnabled = preferences[Keys.SOUND_ENABLED] ?: true,
                vibrationEnabled = preferences[Keys.VIBRATION_ENABLED] ?: true,
                notifyCaregiverOnMissed = preferences[Keys.NOTIFY_CAREGIVER_ON_MISSED] ?: true,
            )
        }

    val voicePreferencesFlow: Flow<VoicePreferences> =
        context.reminderPreferencesDataStore.data.map { preferences ->
            VoicePreferences(
                voiceAssistantEnabled = preferences[Keys.VOICE_ASSISTANT_ENABLED] ?: true,
                voiceReminderEnabled = preferences[Keys.VOICE_REMINDER_ENABLED] ?: false,
                voiceRepeatCount = preferences[Keys.VOICE_REPEAT_COUNT] ?: 2,
                easyModeEnabled = preferences[Keys.EASY_MODE_ENABLED] ?: true,
                voiceGuidanceEnabled = preferences[Keys.VOICE_GUIDANCE_ENABLED] ?: false,
            )
        }

    suspend fun getCurrentPreferences(): ReminderPreferences = preferencesFlow.first()

    suspend fun getCurrentVoicePreferences(): VoicePreferences = voicePreferencesFlow.first()

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.REMINDERS_ENABLED] = enabled
            preferences[Keys.REPEAT_INTERVAL_MINUTES] = preferences[Keys.REPEAT_INTERVAL_MINUTES] ?: 10
            preferences[Keys.MAX_REPEAT_COUNT] = preferences[Keys.MAX_REPEAT_COUNT] ?: 3
            preferences[Keys.SOUND_ENABLED] = preferences[Keys.SOUND_ENABLED] ?: true
            preferences[Keys.VIBRATION_ENABLED] = preferences[Keys.VIBRATION_ENABLED] ?: true
            preferences[Keys.NOTIFY_CAREGIVER_ON_MISSED] = preferences[Keys.NOTIFY_CAREGIVER_ON_MISSED] ?: true
            preferences[Keys.VOICE_REPEAT_COUNT] = preferences[Keys.VOICE_REPEAT_COUNT] ?: 2
        }
    }

    suspend fun setRepeatIntervalMinutes(minutes: Int) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.REPEAT_INTERVAL_MINUTES] = minutes.coerceIn(5, 15)
        }
    }

    suspend fun setMaxRepeatCount(count: Int) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.MAX_REPEAT_COUNT] = count.coerceIn(2, 5)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setNotifyCaregiverOnMissed(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.NOTIFY_CAREGIVER_ON_MISSED] = enabled
        }
    }

    suspend fun setVoiceAssistantEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.VOICE_ASSISTANT_ENABLED] = true
            preferences[Keys.VOICE_REMINDER_ENABLED] = preferences[Keys.VOICE_REMINDER_ENABLED] ?: false
            preferences[Keys.VOICE_REPEAT_COUNT] = preferences[Keys.VOICE_REPEAT_COUNT] ?: 2
            preferences[Keys.EASY_MODE_ENABLED] = preferences[Keys.EASY_MODE_ENABLED] ?: true
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = preferences[Keys.VOICE_GUIDANCE_ENABLED] ?: false
        }
    }

    suspend fun setVoiceReminderEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.VOICE_REMINDER_ENABLED] = enabled
            preferences[Keys.VOICE_ASSISTANT_ENABLED] = preferences[Keys.VOICE_ASSISTANT_ENABLED] ?: true
            preferences[Keys.VOICE_REPEAT_COUNT] = preferences[Keys.VOICE_REPEAT_COUNT] ?: 2
            preferences[Keys.EASY_MODE_ENABLED] = preferences[Keys.EASY_MODE_ENABLED] ?: true
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = preferences[Keys.VOICE_GUIDANCE_ENABLED] ?: false
        }
    }

    suspend fun setVoiceRepeatCount(count: Int) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.VOICE_REPEAT_COUNT] = count.coerceIn(1, 3)
            preferences[Keys.VOICE_ASSISTANT_ENABLED] = preferences[Keys.VOICE_ASSISTANT_ENABLED] ?: true
            preferences[Keys.VOICE_REMINDER_ENABLED] = preferences[Keys.VOICE_REMINDER_ENABLED] ?: false
            preferences[Keys.EASY_MODE_ENABLED] = preferences[Keys.EASY_MODE_ENABLED] ?: true
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = preferences[Keys.VOICE_GUIDANCE_ENABLED] ?: false
        }
    }

    suspend fun setEasyModeEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.EASY_MODE_ENABLED] = enabled
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = preferences[Keys.VOICE_GUIDANCE_ENABLED] ?: false
        }
    }

    suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = enabled
            preferences[Keys.EASY_MODE_ENABLED] = preferences[Keys.EASY_MODE_ENABLED] ?: true
        }
    }

    suspend fun setAllPreferences(
        remindersEnabled: Boolean,
        repeatIntervalMinutes: Int,
        maxRepeatCount: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        notifyCaregiverOnMissed: Boolean,
        voiceAssistantEnabled: Boolean,
        voiceReminderEnabled: Boolean,
        voiceRepeatCount: Int,
        easyModeEnabled: Boolean,
        voiceGuidanceEnabled: Boolean,
    ) {
        context.reminderPreferencesDataStore.edit { preferences ->
            preferences[Keys.REMINDERS_ENABLED] = remindersEnabled
            preferences[Keys.REPEAT_INTERVAL_MINUTES] = repeatIntervalMinutes.coerceIn(5, 15)
            preferences[Keys.MAX_REPEAT_COUNT] = maxRepeatCount.coerceIn(2, 5)
            preferences[Keys.SOUND_ENABLED] = soundEnabled
            preferences[Keys.VIBRATION_ENABLED] = vibrationEnabled
            preferences[Keys.NOTIFY_CAREGIVER_ON_MISSED] = notifyCaregiverOnMissed
            preferences[Keys.VOICE_ASSISTANT_ENABLED] = true
            preferences[Keys.VOICE_REMINDER_ENABLED] = voiceReminderEnabled
            preferences[Keys.VOICE_REPEAT_COUNT] = voiceRepeatCount.coerceIn(1, 3)
            preferences[Keys.EASY_MODE_ENABLED] = easyModeEnabled
            preferences[Keys.VOICE_GUIDANCE_ENABLED] = voiceGuidanceEnabled
        }
    }
}
