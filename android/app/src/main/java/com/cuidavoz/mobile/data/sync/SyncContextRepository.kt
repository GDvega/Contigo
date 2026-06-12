package com.cuidavoz.mobile.data.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncContextDataStore by preferencesDataStore(name = "sync_context")

data class SyncContext(
    val familyId: String? = null,
    val patientId: String? = null,
    val firebaseUserId: String? = null,
    val memberRole: String? = null,
    val syncEnabled: Boolean = true,
    val lastSyncAt: Long? = null,
)

class SyncContextRepository(
    private val context: Context,
) {
    private object Keys {
        val FAMILY_ID = stringPreferencesKey("family_id")
        val PATIENT_ID = stringPreferencesKey("patient_id")
        val FIREBASE_USER_ID = stringPreferencesKey("firebase_user_id")
        val MEMBER_ROLE = stringPreferencesKey("member_role")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val syncContextFlow: Flow<SyncContext> =
        context.syncContextDataStore.data.map { preferences ->
            SyncContext(
                familyId = preferences[Keys.FAMILY_ID],
                patientId = preferences[Keys.PATIENT_ID],
                firebaseUserId = preferences[Keys.FIREBASE_USER_ID],
                memberRole = preferences[Keys.MEMBER_ROLE],
                syncEnabled = preferences[Keys.SYNC_ENABLED] ?: true,
                lastSyncAt = preferences[Keys.LAST_SYNC_AT],
            )
        }

    suspend fun getCurrent(): SyncContext = syncContextFlow.first()

    suspend fun updateFamilyContext(
        familyId: String,
        patientId: String,
        memberRole: String? = null,
    ) {
        context.syncContextDataStore.edit { preferences ->
            preferences[Keys.FAMILY_ID] = familyId
            preferences[Keys.PATIENT_ID] = patientId
            if (memberRole == null) {
                preferences.remove(Keys.MEMBER_ROLE)
            } else {
                preferences[Keys.MEMBER_ROLE] = memberRole
            }
        }
    }

    suspend fun setMemberRole(role: String?) {
        context.syncContextDataStore.edit { preferences ->
            if (role == null) {
                preferences.remove(Keys.MEMBER_ROLE)
            } else {
                preferences[Keys.MEMBER_ROLE] = role
            }
        }
    }

    suspend fun setFirebaseUserId(uid: String?) {
        context.syncContextDataStore.edit { preferences ->
            if (uid == null) {
                preferences.remove(Keys.FIREBASE_USER_ID)
            } else {
                preferences[Keys.FIREBASE_USER_ID] = uid
            }
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.syncContextDataStore.edit { preferences ->
            preferences[Keys.SYNC_ENABLED] = enabled
        }
    }

    suspend fun markSynced(now: Long) {
        context.syncContextDataStore.edit { preferences ->
            preferences[Keys.LAST_SYNC_AT] = now
        }
    }
}
