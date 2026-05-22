package com.cuidavoz.mobile.data.files

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MedicationImageStorage(
    private val context: Context,
) {
    fun getMedicationImagesDirectory(): File {
        val directory = File(context.filesDir, "medications/images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun createImageFile(medicationId: String): File {
        val fileName = buildImageFileName(medicationId, "jpg")
        return File(getMedicationImagesDirectory(), fileName)
    }

    fun createCameraCaptureUri(medicationId: String): Uri {
        val cacheDirectory = File(context.cacheDir, "medications/camera").apply { mkdirs() }
        val fileName = "medication_${medicationId}_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDirectory, fileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun saveImageFromUri(
        sourceUri: Uri,
        medicationId: String,
    ): String {
        val destinationFile = createImageFile(medicationId)
        Log.d(TAG, "Guardando imagen local")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("No se pudo abrir el origen de la imagen")

        if (sourceUri.scheme == "content" && sourceUri.authority == "${context.packageName}.fileprovider") {
            deleteTemporaryCameraUri(sourceUri.toString())
        }

        return Uri.fromFile(destinationFile).toString()
    }

    fun copyImageFromBackup(
        inputStream: InputStream,
        medicationId: String,
        sourceFileName: String,
    ): String {
        val extension = sourceFileName.substringAfterLast('.', "jpg")
            .lowercase()
            .ifBlank { "jpg" }
        val destinationFile = File(
            getMedicationImagesDirectory(),
            buildImageFileName(medicationId, extension),
        )
        FileOutputStream(destinationFile).use { output ->
            inputStream.copyTo(output)
        }
        return Uri.fromFile(destinationFile).toString()
    }

    fun deleteMedicationImage(imageUri: String?) {
        if (imageUri.isNullOrBlank()) {
            return
        }

        runCatching {
            val uri = Uri.parse(imageUri)
            val file = when {
                uri.scheme == "file" -> File(uri.path.orEmpty())
                isAppFileUri(imageUri) -> File(uri.path.orEmpty())
                else -> null
            } ?: return

            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Imagen local eliminada")
            }
        }.onFailure { error ->
            Log.e(TAG, "No se pudo eliminar imagen local", error)
        }
    }

    fun isAppManagedImage(imageUri: String?): Boolean {
        if (imageUri.isNullOrBlank()) {
            return false
        }
        val uri = Uri.parse(imageUri)
        val path = uri.path.orEmpty()
        return path.startsWith(getMedicationImagesDirectory().absolutePath)
    }

    fun validateImageExists(imageUri: String?): Boolean {
        return resolveManagedImageFile(imageUri)?.exists() == true
    }

    fun resolveManagedImageFile(imageUri: String?): File? {
        if (imageUri.isNullOrBlank()) {
            return null
        }
        val uri = Uri.parse(imageUri)
        val file = when {
            uri.scheme == "file" -> File(uri.path.orEmpty())
            isAppFileUri(imageUri) -> File(uri.path.orEmpty())
            else -> null
        } ?: return null
        return file.takeIf { managed ->
            managed.absolutePath.startsWith(getMedicationImagesDirectory().absolutePath)
        }
    }

    fun deleteAllMedicationImages() {
        getMedicationImagesDirectory().listFiles().orEmpty().forEach { file ->
            runCatching { file.delete() }
                .onFailure { error ->
                    Log.e(TAG, "No se pudo eliminar imagen de respaldo", error)
                }
        }
    }

    fun deleteTemporaryCameraUri(uriString: String?) {
        if (uriString.isNullOrBlank()) {
            return
        }
        runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                val fileName = uri.pathSegments.lastOrNull()?.substringAfterLast('/')
                    ?: return@runCatching
                val file = File(context.cacheDir, "medications/camera/$fileName")
                if (file.exists()) {
                    file.delete()
                }
            } else if (uri.scheme == "file") {
                File(uri.path.orEmpty()).takeIf(File::exists)?.delete()
            }
        }.onFailure { error ->
            Log.e(TAG, "No se pudo limpiar imagen temporal", error)
        }
    }

    private fun isAppFileUri(uriString: String): Boolean {
        return uriString.startsWith("file://${getMedicationImagesDirectory().absolutePath}")
    }

    private fun buildImageFileName(
        medicationId: String,
        extension: String,
    ): String {
        return "medication_${medicationId}_${System.currentTimeMillis()}.$extension"
    }

    private companion object {
        const val TAG = "[CuidaVoz][MedicationImageStorage]"
    }
}
