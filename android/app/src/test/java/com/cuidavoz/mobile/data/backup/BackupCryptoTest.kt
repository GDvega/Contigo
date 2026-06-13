package com.cuidavoz.mobile.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

class BackupCryptoTest {
    @Test
    fun encryptAndDecryptRoundTrip() {
        val zipBytes = buildSampleZip()
        val password = "mi-clave-segura".toCharArray()
        val encrypted = BackupCrypto.encryptZip(zipBytes, password)

        assertTrue(
            ByteArrayInputStream(encrypted).buffered().use(BackupCrypto::isEncryptedBackup),
        )

        val decrypted = ByteArrayInputStream(encrypted).use {
            BackupCrypto.decryptToZip(it, password)
        }
        assertArrayEquals(zipBytes, decrypted)
        password.fill('\u0000')
    }

    @Test
    fun rejectsWrongPassword() {
        val zipBytes = buildSampleZip()
        val password = "correcta-123456".toCharArray()
        val wrongPassword = "incorrecta-123".toCharArray()
        val encrypted = BackupCrypto.encryptZip(zipBytes, password)

        try {
            ByteArrayInputStream(encrypted).use {
                BackupCrypto.decryptToZip(it, wrongPassword)
            }
            throw AssertionError("Se esperaba BackupFormatException")
        } catch (_: BackupFormatException) {
            // expected
        } finally {
            password.fill('\u0000')
            wrongPassword.fill('\u0000')
        }
    }

    @Test
    fun detectsPlainZipAsNotEncrypted() {
        val zipBytes = buildSampleZip()
        assertFalse(
            ByteArrayInputStream(zipBytes).buffered().use(BackupCrypto::isEncryptedBackup),
        )
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
