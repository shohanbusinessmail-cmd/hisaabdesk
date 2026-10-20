package com.shohankhan.hisaabdesk.core

import com.shohankhan.hisaabdesk.core.backup.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupCryptoTest {

    private fun encrypt(plain: String, password: String): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encrypt(plain.toByteArray(Charsets.UTF_8), password.toCharArray(), out)
        return out.toByteArray()
    }

    private fun decrypt(bytes: ByteArray, password: String): ByteArray =
        BackupCrypto.decrypt(ByteArrayInputStream(bytes), password.toCharArray())

    @Test
    fun `round trip preserves payload`() {
        val payload = "{\"hello\":\"বাংলা টেক্সট\",\"amount\":123456}"
        val encrypted = encrypt(payload, "correct horse battery staple")
        val decrypted = decrypt(encrypted, "correct horse battery staple")
        assertArrayEquals(payload.toByteArray(Charsets.UTF_8), decrypted)
    }

    @Test
    fun `wrong password fails authentication`() {
        val encrypted = encrypt("secret data", "password-one")
        var threw = false
        try {
            decrypt(encrypted, "password-two")
        } catch (e: BackupCrypto.BackupCryptoException) {
            threw = true
        }
        assertTrue("wrong password must fail", threw)
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val encrypted = encrypt("secret data", "password")
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0x01).toByte()
        var threw = false
        try {
            decrypt(encrypted, "password")
        } catch (e: BackupCrypto.BackupCryptoException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `rejects foreign files`() {
        val notBackup = "PK\u0003\u0004 this is a zip".toByteArray()
        var threw = false
        try {
            decrypt(notBackup, "x")
        } catch (e: BackupCrypto.BackupCryptoException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `rejects truncated containers`() {
        val encrypted = encrypt("some data here", "password")
        val truncated = encrypted.copyOfRange(0, encrypted.size / 2)
        var threw = false
        try {
            decrypt(truncated, "password")
        } catch (e: BackupCrypto.BackupCryptoException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `each export uses a fresh salt and iv`() {
        val a = encrypt("same payload", "password")
        val b = encrypt("same payload", "password")
        assertNotEquals(a.toList(), b.toList())
        // both must still decrypt correctly
        assertArrayEquals("same payload".toByteArray(), decrypt(a, "password"))
        assertArrayEquals("same payload".toByteArray(), decrypt(b, "password"))
    }
}
