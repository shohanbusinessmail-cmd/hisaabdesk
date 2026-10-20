package com.shohankhan.hisaabdesk.core.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted backup file container.
 *
 * Format (version 1):
 *   magic "HDBK1" | formatVersion:Byte | saltLen:Byte | salt | ivLen:Byte | iv | AES-256-GCM(ciphertext||tag)
 *
 * Cryptography policy:
 *  - Key = PBKDF2WithHmacSHA256(password, unique random salt, 210k iterations, 256 bits)
 *  - AES-256-GCM with a fresh random 12-byte IV per backup (never reused with the same key)
 *  - GCM authentication tag verifies integrity — wrong password or corruption fails loudly
 *  - No custom algorithms; platform JCE only.
 */
object BackupCrypto {

    const val MAGIC = "HDBK1"
    const val FORMAT_VERSION = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256

    class BackupCryptoException(message: String) : Exception(message)

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val bytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return SecretKeySpec(bytes, "AES")
    }

    /** Encrypts [plain] into [out] using the container format. */
    fun encrypt(plain: ByteArray, password: CharArray, out: OutputStream) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        val header = ByteArrayOutputStream()
        header.write(MAGIC.toByteArray(Charsets.US_ASCII))
        header.write(FORMAT_VERSION)
        header.write(salt.size)
        header.write(salt)
        header.write(iv.size)
        header.write(iv)
        out.write(header.toByteArray())

        val cos = CipherOutputStream(out, cipher)
        try {
            cos.write(plain)
        } finally {
            cos.close() // writes GCM tag
        }
    }

    /** Decrypts a container stream. Throws [BackupCryptoException] on any failure. */
    fun decrypt(input: InputStream, password: CharArray): ByteArray {
        val magicBytes = ByteArray(MAGIC.length)
        readFully(input, magicBytes)
        if (String(magicBytes, Charsets.US_ASCII) != MAGIC) {
            throw BackupCryptoException("not a HisaabDesk backup file")
        }
        val formatVersion = input.read()
        if (formatVersion != FORMAT_VERSION) {
            throw BackupCryptoException("unsupported container version: $formatVersion")
        }
        val saltLen = input.read()
        if (saltLen <= 0 || saltLen > 64) throw BackupCryptoException("invalid salt length")
        val salt = ByteArray(saltLen)
        readFully(input, salt)
        val ivLen = input.read()
        if (ivLen <= 0 || ivLen > 32) throw BackupCryptoException("invalid iv length")
        val iv = ByteArray(ivLen)
        readFully(input, iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        return try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val rest = input.readBytes()
            if (rest.isEmpty()) throw BackupCryptoException("empty ciphertext")
            cipher.doFinal(rest)
        } catch (e: BackupCryptoException) {
            throw e
        } catch (e: Exception) {
            // Wrong password or corrupted/tampered payload — do not leak details.
            throw BackupCryptoException("wrong password or corrupted file")
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var off = 0
        while (off < buffer.size) {
            val n = input.read(buffer, off, buffer.size - off)
            if (n < 0) throw BackupCryptoException("truncated file")
            off += n
        }
    }
}
