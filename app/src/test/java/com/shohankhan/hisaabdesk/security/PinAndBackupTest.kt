package com.shohankhan.hisaabdesk.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.hisaabdesk.core.backup.BackupData
import com.shohankhan.hisaabdesk.core.backup.BackupSerializer
import com.shohankhan.hisaabdesk.core.security.PinManager
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PinManagerTest {

    private lateinit var pinManager: PinManager
    private val now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hisaabdesk_security", Context.MODE_PRIVATE).edit().clear().commit()
        pinManager = PinManager(context)
    }

    @Test
    fun `pin is not stored in plaintext`() {
        pinManager.setPin("4821")
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("hisaabdesk_security", Context.MODE_PRIVATE)
        val all = prefs.all.values.joinToString("|") { it.toString() }
        assertFalse("raw PIN must never be persisted", all.contains("4821"))
    }

    @Test
    fun `correct pin verifies`() {
        pinManager.setPin("1234")
        assertTrue(pinManager.verify("1234", now))
    }

    @Test
    fun `wrong pin fails and increments attempts`() {
        pinManager.setPin("1234")
        assertFalse(pinManager.verify("9999", now))
        assertEquals(1, pinManager.state().failedAttempts)
    }

    @Test
    fun `three failures trigger backoff lock`() {
        pinManager.setPin("1234")
        assertFalse(pinManager.verify("0000", now))
        assertFalse(pinManager.verify("0000", now))
        assertFalse(pinManager.verify("0000", now))
        assertTrue(pinManager.isLockedOut(now))
        assertEquals(30L, pinManager.remainingLockSeconds(now))
        // even the correct PIN is refused while locked out
        assertFalse(pinManager.verify("1234", now))
        // after the lock expires a correct PIN works and resets counters
        assertTrue(pinManager.verify("1234", now + 31_000))
        assertEquals(0, pinManager.state().failedAttempts)
    }

    @Test
    fun `backoff escalates with attempts`() {
        assertEquals(0L, PinManager.backoffMillis(1))
        assertEquals(0L, PinManager.backoffMillis(2))
        assertEquals(30_000L, PinManager.backoffMillis(3))
        assertEquals(60_000L, PinManager.backoffMillis(4))
        assertEquals(300_000L, PinManager.backoffMillis(5))
        assertEquals(900_000L, PinManager.backoffMillis(6))
        assertEquals(3_600_000L, PinManager.backoffMillis(9))
    }

    @Test
    fun `pin change requires current pin`() {
        pinManager.setPin("1234")
        assertFalse(pinManager.changePin("0000", "5678"))
        assertTrue(pinManager.verify("1234"))
        assertTrue(pinManager.changePin("1234", "5678"))
        assertTrue(pinManager.verify("5678"))
        assertFalse(pinManager.verify("1234"))
    }

    @Test
    fun `invalid pin formats are rejected`() {
        for (bad in listOf("123", "1234567", "12a4", "")) {
            var threw = false
            try {
                pinManager.setPin(bad)
            } catch (e: IllegalArgumentException) {
                threw = true
            }
            assertTrue("should reject '$bad'", threw)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupSerializerTest {

    private val now = 1_700_000_000_000L

    private fun sampleData(): BackupData {
        val account = AccountEntity(
            id = 1, name = "নগদ", type = AccountType.CASH, openingBalancePaisa = 100_000,
            createdAt = now, updatedAt = now
        )
        val txn = TxnEntity(
            id = 1, type = TxnType.INCOME, flow = TxnFlow.INFLOW, amountPaisa = 50_000,
            dateMillis = now, dayKey = 20231114, accountId = 1, createdAt = now, updatedAt = now
        )
        return BackupData(
            schemaVersion = BackupSerializer.SCHEMA_VERSION,
            appVersion = "1.0.0",
            createdAtMillis = now,
            currency = "BDT",
            settingsJson = "{}",
            accounts = listOf(account),
            categories = emptyList(),
            contacts = emptyList(),
            shops = emptyList(),
            purchases = emptyList(),
            purchaseItems = emptyList(),
            txns = listOf(txn),
            lendings = emptyList(),
            borrowings = emptyList(),
            receivables = emptyList(),
            payables = emptyList(),
            loans = emptyList(),
            installments = emptyList(),
            goals = emptyList(),
            budgets = emptyList(),
            recurringItems = emptyList(),
            recurringOccurrences = emptyList(),
            attachments = emptyList(),
            auditLog = emptyList()
        )
    }

    @Test
    fun `export and parse round trip preserves data`() {
        val json = BackupSerializer.toJson(sampleData())
        val (data, result) = BackupSerializer.parseAndValidate(json)
        assertTrue(result.ok)
        assertNotNull(data)
        assertEquals(1, data!!.accounts.size)
        assertEquals(1, data.txns.size)
        assertEquals(100_000L, data.accounts.first().openingBalancePaisa)
        assertEquals(TxnType.INCOME, data.txns.first().type)
        assertEquals(1, result.summary.txnCount)
    }

    @Test
    fun `unsupported schema version is refused`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json).put("schemaVersion", 99)
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("unsupported schema") })
    }

    @Test
    fun `invalid json is refused`() {
        val (data, result) = BackupSerializer.parseAndValidate("{not json")
        assertNull(data)
        assertFalse(result.ok)
    }

    @Test
    fun `duplicate ids are detected`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json)
        val accounts = obj.getJSONArray("accounts")
        val duplicate = JSONObject(accounts.getJSONObject(0).toString())
        accounts.put(duplicate)
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertTrue(result.errors.any { it.contains("duplicate ids in accounts") })
    }

    @Test
    fun `broken references are detected`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json)
        obj.getJSONArray("txns").getJSONObject(0).put("accountId", 999)
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertTrue(result.errors.any { it.contains("missing account id=999") })
    }

    @Test
    fun `transfer without destination is rejected`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json)
        val txns = obj.getJSONArray("txns")
        txns.getJSONObject(0).put("type", "TRANSFER").put("flow", "TRANSFER")
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertTrue(result.errors.any { it.contains("transfer without destination") })
    }

    @Test
    fun `amount out of bounds is rejected`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json)
        obj.getJSONArray("txns").getJSONObject(0).put("amountPaisa", Long.MAX_VALUE)
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertFalse(result.ok)
    }

    @Test
    fun `unknown format marker is flagged`() {
        val json = BackupSerializer.toJson(sampleData())
        val obj = JSONObject(json).put("format", "something-else")
        val (data, result) = BackupSerializer.parseAndValidate(obj.toString())
        assertNull(data)
        assertTrue(result.errors.any { it.contains("unrecognized backup format") })
    }
}
