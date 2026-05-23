package com.cuidavoz.mobile.data.backup

data class BackupPreferencesDto(
    val remindersEnabled: Boolean,
    val repeatIntervalMinutes: Int,
    val maxRepeatCount: Int,
    val voiceAssistantEnabled: Boolean,
    val voiceReminderEnabled: Boolean,
    val voiceRepeatCount: Int,
    val easyModeEnabled: Boolean = false,
    val voiceGuidanceEnabled: Boolean = false,
)
