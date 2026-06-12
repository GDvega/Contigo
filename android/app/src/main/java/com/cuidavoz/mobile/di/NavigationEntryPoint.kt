package com.cuidavoz.mobile.di

import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.voice.TextToSpeechManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NavigationEntryPoint {
    fun reminderLaunchState(): ReminderLaunchState
    fun textToSpeechManager(): TextToSpeechManager
}
