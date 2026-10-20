package com.shohankhan.hisaabdesk.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small AES-GCM wrapper around a non-exportable Android Keystore key.
 * Used to protect the stored PIN verifier blob so it cannot be trivially read
 * out of app preferences on a compromised device backup.
 *
 * Never logs plaintext, keys, or IVs.
 */
class KeystoreBox(private val alias: String = DEFAULT_ALIAS) {

    // Lazily resolved: the AndroidKeyStore provider does not exist on the JVM
    // (unit tests) or on some unusual devices. Absence must degrade gracefully,
    // never crash the app or the PIN screen.
    private val keyStore: KeyStore? = runCatching {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val ks = keyStore ?: throw IllegalStateException("AndroidKeyStore unavailable")
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** Returns iv(12) || ciphertext+tag. */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return iv + ct
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > 12) { "blob too short" }
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    fun isAvailable(): Boolean = try {
        getOrCreateKey()
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        const val DEFAULT_ALIAS = "hisaabdesk_master"
    }
}
