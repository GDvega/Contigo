package com.cuidavoz.mobile.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupCryptoExtendedTest {
    @Test
    fun rejectsPasswordShorterThanMinimum() {
        val zipBytes = buildSampleZip()
        val shortPassword = "abc".toCharArray()

        try {
            BackupCrypto.encryptZip(zipBytes, shortPassword)
            throw AssertionError("Se esperaba IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals(
                "La contraseña debe tener al menos ${BackupCrypto.MIN_PASSWORD_LENGTH} caracteres.",
                error.message,
            )
        } finally {
            shortPassword.fill('\u0000')
        }
    }

    @Test
    fun rejectsEmptyPassword() {
        val zipBytes = buildSampleZip()
        val emptyPassword = CharArray(0)

        try {
            BackupCrypto.encryptZip(zipBytes, emptyPassword)
            throw AssertionError("Se esperaba IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals("La contraseña no puede estar vacía.", error.message)
        }
    }

    @Test
    fun decryptPlainZipThrowsFormatException() {
        val zipBytes = buildSampleZip()
        val password = "password1234".toCharArray()

        try {
            ByteArrayInputStream(zipBytes).use {
                BackupCrypto.decryptToZip(it, password)
            }
            throw AssertionError("Se esperaba BackupFormatException")
        } catch (error: BackupFormatException) {
            assertEquals(
                "El archivo seleccionado no parece ser un respaldo cifrado de Contigo.",
                error.message,
            )
        } finally {
            password.fill('\u0000')
        }
    }

    private fun buildSampleZip(): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("contigo-backup/backup.json"))
                zip.write("{\"app\":\"Contigo\"}".toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }
    }
}
