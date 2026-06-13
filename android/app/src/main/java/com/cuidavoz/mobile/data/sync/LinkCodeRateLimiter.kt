package com.cuidavoz.mobile.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.linkCodeRateLimitStore by preferencesDataStore(name = "link_code_rate_limit")

class LinkCodeRateLimiter(
    context: Context,
) {
    private val dataStore = context.applicationContext.linkCodeRateLimitStore

    suspend fun isBlocked(): Boolean {
        val preferences = dataStore.data.first()
        val windowStartedAt = preferences[Keys.WINDOW_STARTED_AT] ?: 0L
        val failures = preferences[Keys.FAILURE_COUNT] ?: 0
        if (failures < MAX_FAILURES) {
            return false
        }
        val elapsed = System.currentTimeMillis() - windowStartedAt
        return elapsed < WINDOW_MS
    }

    suspend fun recordFailure() {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val windowStartedAt = preferences[Keys.WINDOW_STARTED_AT] ?: now
            val elapsed = now - windowStartedAt
            if (elapsed >= WINDOW_MS) {
                preferences[Keys.WINDOW_STARTED_AT] = now
                preferences[Keys.FAILURE_COUNT] = 1
            } else {
                preferences[Keys.FAILURE_COUNT] = (preferences[Keys.FAILURE_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.WINDOW_STARTED_AT)
            preferences.remove(Keys.FAILURE_COUNT)
        }
    }

    suspend fun blockedMessage(): String {
        val preferences = dataStore.data.first()
        val windowStartedAt = preferences[Keys.WINDOW_STARTED_AT] ?: System.currentTimeMillis()
        val remainingMinutes = ((WINDOW_MS - (System.currentTimeMillis() - windowStartedAt)) / 60_000)
            .coerceAtLeast(1)
        return "Demasiados intentos fallidos. Espera unos $remainingMinutes minutos e inténtalo otra vez."
    }

    private object Keys {
        val WINDOW_STARTED_AT = longPreferencesKey("window_started_at")
        val FAILURE_COUNT = intPreferencesKey("failure_count")
    }

    companion object {
        private const val MAX_FAILURES = 5
        private const val WINDOW_MS = 15 * 60 * 1000L
    }
}
