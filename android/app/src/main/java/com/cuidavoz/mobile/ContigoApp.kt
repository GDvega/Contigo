package com.cuidavoz.mobile

import android.app.Application
import com.cuidavoz.mobile.di.ContigoAppInitializer
import com.cuidavoz.mobile.reminders.MedicationNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ContigoApp : Application() {
    @Inject lateinit var appContainer: ContigoAppContainer
    @Inject lateinit var appInitializer: ContigoAppInitializer

    override fun onCreate() {
        super.onCreate()
        MedicationNotificationChannels.createAll(this)
        appInitializer.start()
    }
}
