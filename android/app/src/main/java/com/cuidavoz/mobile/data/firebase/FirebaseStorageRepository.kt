package com.cuidavoz.mobile.data.firebase

import android.net.Uri
import com.cuidavoz.mobile.util.ContigoLog
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageRepository @Inject constructor(
    private val storage: FirebaseStorage?,
) {
    suspend fun uploadMedicationImage(
        familyId: String,
        patientId: String,
        medicationId: String,
        localFile: File,
    ): String {
        val storageRef = checkNotNull(storage) { "Firebase Storage no está configurado" }
        check(localFile.exists()) { "La imagen local no existe" }
        val imagePath = medicationImagePath(familyId, patientId, medicationId)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        storageRef.reference.child(imagePath)
            .putFile(Uri.fromFile(localFile), metadata)
            .await()
        return imagePath
    }

    suspend fun deleteMedicationImage(
        familyId: String,
        patientId: String,
        medicationId: String,
    ) {
        val storageRef = checkNotNull(storage) { "Firebase Storage no está configurado" }
        val imagePath = medicationImagePath(familyId, patientId, medicationId)
        runCatching {
            storageRef.reference.child(imagePath).delete().await()
        }.onFailure { error ->
            if (error !is StorageException ||
                error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND
            ) {
                throw error
            }
        }
    }

    suspend fun downloadMedicationImage(
        imagePath: String,
        destinationFile: File,
    ): Boolean {
        val storageRef = storage ?: return false
        destinationFile.parentFile?.mkdirs()
        return runCatching {
            storageRef.reference.child(imagePath)
                .getFile(destinationFile)
                .await()
            true
        }.onFailure { error ->
            destinationFile.delete()
            val detail = when (error) {
                is StorageException -> "code=${error.errorCode}"
                else -> error.message.orEmpty()
            }
            ContigoLog.w(TAG, "downloadMedicationImage failed path=$imagePath: $detail", error)
        }.getOrDefault(false)
    }

    private fun medicationImagePath(
        familyId: String,
        patientId: String,
        medicationId: String,
    ): String = "families/$familyId/patients/$patientId/medications/$medicationId.jpg"

    private companion object {
        const val TAG = "[Contigo][FirebaseStorage]"
    }
}
