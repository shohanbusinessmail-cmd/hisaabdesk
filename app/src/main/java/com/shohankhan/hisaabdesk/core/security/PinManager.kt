package com.shohankhan.hisaabdesk.core.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN security model (docs/SECURITY.md):
 *  - The PIN is NEVER stored. We store a PBKDF2-HMAC-SHA256 verifier with a
 *    unique 16-byte random salt and high iteration count (OWASP guidance for
 *    low-entropy secrets).
 *  - The verifier blob is additionally wrapped with an Android Keystore AES-GCM
 *    key when the device supports it.
 *  - The PIN is not used as an encryption key for user data; it only gates the UI.
 *    Database-at-rest protection is provided by the Android app sandbox (documented
 *    accurately — a PIN screen is NOT database encryption).
 *  - Failed attempts are throttled with exponential backoff.
 *  - Verification is constant-time (MessageDigest.isEqual).
 *  - There is no hidden bypass and no PIN recovery path.
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisaabdesk_security", Context.MODE_PRIVATE)
    private val keystoreBox = KeystoreBox()
    private val secureRandom = SecureRandom()

    data class PinState(
        val isSet: Boolean,
        val failedAttempts: Int,
        val lockUntilMillis: Long
    )

    fun state(): PinState = PinState(
        isSet = prefs.contains(KEY_VERIFIER),
        failedAttempts = prefs.getInt(KEY_FAILED, 0),
        lockUntilMillis = prefs.getLong(KEY_LOCK_UNTIL, 0L)
    )

    fun isLockedOut(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis < prefs.getLong(KEY_LOCK_UNTIL, 0L)

    fun remainingLockSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        return if (until > nowMillis) (until - nowMillis + 999) / 1000 else 0
    }

    fun setPin(pin: String) {
        validatePinOrThrow(pin)
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val verifier = derive(pin, salt)
        val blob = pack(salt, verifier)
        val stored = try {
            if (keystoreBox.isAvailable()) FLAG_KEYSTORE + keystoreBox.encrypt(blob) else FLAG_RAW + blob
        } catch (e: Exception) {
            FLAG_RAW + blob
        }
        prefs.edit()
            .putString(KEY_VERIFIER, encode(stored))
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verify(currentPin)) return false
        setPin(newPin)
        return true
    }

    /** Returns true on success. On failure, updates throttling counters. */
    fun verify(pin: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isLockedOut(nowMillis)) return false
        val encoded = prefs.getString(KEY_VERIFIER, null) ?: return false
        val stored = decode(encoded)
        if (stored.isEmpty()) return false
        val blob = try {
            when (stored[0]) {
                FLAG_KEYSTORE -> keystoreBox.decrypt(stored.copyOfRange(1, stored.size))
                FLAG_RAW -> stored.copyOfRange(1, stored.size)
                else -> return failAttempt(nowMillis)
            }
        } catch (e: Exception) {
            return failAttempt(nowMillis)
        }
        if (blob.size <= SALT_BYTES) return failAttempt(nowMillis)
        val salt = blob.copyOfRange(0, SALT_BYTES)
        val verifier = blob.copyOfRange(SALT_BYTES, blob.size)
        val candidate = try {
            derive(pin, salt)
        } catch (e: Exception) {
            return failAttempt(nowMillis)
        }
        return if (MessageDigest.isEqual(verifier, candidate)) {
            prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
            true
        } else {
            failAttempt(nowMillis)
        }
    }

    private fun failAttempt(nowMillis: Long): Boolean {
        val failed = prefs.getInt(KEY_FAILED, 0) + 1
        val lockMs = backoffMillis(failed)
        prefs.edit()
            .putInt(KEY_FAILED, failed)
            .putLong(KEY_LOCK_UNTIL, if (lockMs > 0) nowMillis + lockMs else 0L)
            .apply()
        return false
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun validatePinOrThrow(pin: String) {
        require(pin.length in 4..6) { "PIN must be 4-6 digits" }
        require(pin.all { it in '0'..'9' }) { "PIN must be digits only" }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun pack(salt: ByteArray, verifier: ByteArray): ByteArray = salt + verifier

    private fun encode(bytes: ByteArray): String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = android.util.Base64.decode(text, android.util.Base64.NO_WRAP)

    companion object {
        private const val KEY_VERIFIER = "pin_verifier"
        private const val KEY_FAILED = "pin_failed"
        private const val KEY_LOCK_UNTIL = "pin_lock_until"

        private const val SALT_BYTES = 16
        private const val ITERATIONS = 210_000
        private const val KEY_BITS = 256

        private const val FLAG_RAW: Byte = 1
        private const val FLAG_KEYSTORE: Byte = 2

        private const val MAX_ATTEMPTS_BEFORE_BACKOFF = 3

        fun backoffMillis(failedAttempts: Int): Long = when {
            failedAttempts < MAX_ATTEMPTS_BEFORE_BACKOFF -> 0L
            failedAttempts == 3 -> 30_000L
            failedAttempts == 4 -> 60_000L
            failedAttempts == 5 -> 5 * 60_000L
            failedAttempts == 6 -> 15 * 60_000L
            else -> 60 * 60_000L
        }
    }
}
