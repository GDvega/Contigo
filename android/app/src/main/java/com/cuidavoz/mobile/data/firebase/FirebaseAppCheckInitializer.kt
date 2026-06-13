package com.cuidavoz.mobile.data.firebase

import android.content.Context
import com.cuidavoz.mobile.BuildConfig
import com.cuidavoz.mobile.util.ContigoLog
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object FirebaseAppCheckInitializer {
    private const val TAG = "[Contigo][AppCheck]"

    fun install(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return
        }
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            ContigoLog.d(TAG, "App Check debug activado.")
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            ContigoLog.d(TAG, "App Check Play Integrity activado.")
        }
    }
}
