package com.cuidavoz.mobile.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth? by lazy {
        if (FirebaseApp.getApps(appContext).isEmpty()) null else FirebaseAuth.getInstance()
    }

    fun isConfigured(): Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    fun isSignedIn(): Boolean = auth?.currentUser != null

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    suspend fun signInAnonymously(): String? {
        val firebaseAuth = auth ?: return null
        firebaseAuth.currentUser?.uid?.let { return it }
        return firebaseAuth.signInAnonymously().await().user?.uid
    }

    suspend fun signOut() {
        auth?.signOut()
    }
}
