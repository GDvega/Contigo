package com.cuidavoz.mobile.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val MAGIC = "CTGO"
    private const val FORMAT_VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val GCM_TAG_BITS = 128

    fun isEncryptedBackup(input: InputStream): Boolean {
        val header = ByteArray(MAGIC.length)
        val read = input.read(header)
        return read == MAGIC.length && header.decodeToString() == MAGIC
    }

    fun encryptZip(zipBytes: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "La contraseña no puede estar vacía." }
        require(password.size >= MIN_PASSWORD_LENGTH) {
            "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
        }

        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(zipBytes)

        return ByteArrayOutputStream(MAGIC.length + 1 + SALT_BYTES + IV_BYTES + ciphertext.size).apply {
            write(MAGIC.encodeToByteArray())
            write(FORMAT_VERSION.toInt())
            write(salt)
            write(iv)
            write(ciphertext)
        }.toByteArray()
    }

    fun decryptToZip(encryptedInput: InputStream, password: CharArray): ByteArray {
        val header = ByteArray(MAGIC.length)
        encryptedInput.readFully(header)
        if (header.decodeToString() != MAGIC) {
            throw BackupFormatException("El archivo seleccionado no parece ser un respaldo cifrado de Contigo.")
        }

        val version = encryptedInput.read()
        if (version != FORMAT_VERSION.toInt()) {
            throw BackupFormatException("Este respaldo usa un formato cifrado no compatible.")
        }

        val salt = ByteArray(SALT_BYTES).also { encryptedInput.readFully(it) }
        val iv = ByteArray(IV_BYTES).also { encryptedInput.readFully(it) }
        val ciphertext = encryptedInput.readBytes()
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        return try {
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            throw BackupFormatException("Contraseña incorrecta o respaldo dañado.")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val secret = factory.generateSecret(spec).encoded
        return SecretKeySpec(secret, "AES")
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) {
                throw BackupFormatException("El respaldo cifrado está incompleto.")
            }
            offset += read
        }
    }

    const val MIN_PASSWORD_LENGTH = 8

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
}
