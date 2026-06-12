package com.cuidavoz.mobile.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cuidavoz.mobile.data.model.DeviceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

class OnboardingRepository(
    private val context: Context,
) {
    private object Keys {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val DEVICE_ROLE = stringPreferencesKey("device_role")
        val CAREGIVER_DISPLAY_NAME = stringPreferencesKey("caregiver_display_name")
    }

    val setupCompletedFlow: Flow<Boolean> =
        context.onboardingDataStore.data.map { preferences ->
            preferences[Keys.SETUP_COMPLETED] ?: false
        }

    val deviceRoleFlow: Flow<DeviceRole?> =
        context.onboardingDataStore.data.map { preferences ->
            DeviceRole.fromStorageValue(preferences[Keys.DEVICE_ROLE])
        }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.SETUP_COMPLETED] = completed
        }
    }

    suspend fun setDeviceRole(role: DeviceRole) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.DEVICE_ROLE] = role.toStorageValue()
        }
    }

    suspend fun setCaregiverDisplayName(name: String) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.CAREGIVER_DISPLAY_NAME] = name
        }
    }

    suspend fun resetOnboardingState() {
        context.onboardingDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
