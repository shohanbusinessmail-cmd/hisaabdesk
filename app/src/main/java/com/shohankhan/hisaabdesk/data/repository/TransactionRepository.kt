package com.shohankhan.hisaabdesk.data.repository

import androidx.room.withTransaction
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.AccountDao
import com.shohankhan.hisaabdesk.data.local.dao.AuditDao
import com.shohankhan.hisaabdesk.data.local.dao.TxnDao
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.AuditEntryEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnStatus
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.domain.accounting.AccountingRules
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val db: HisaabDatabase,
    private val txnDao: TxnDao,
    private val accountDao: AccountDao,
    private val auditDao: AuditDao
) {

    class ValidationException(message: String) : Exception(message)

    fun observeFiltered(
        query: String? = null,
        type: TxnType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        contactId: Long? = null,
        shopId: Long? = null,
        fromDay: Int? = null,
        toDay: Int? = null,
        minPaisa: Long? = null,
        maxPaisa: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Flow<List<TxnEntity>> = txnDao.observeFiltered(
        query = query?.takeIf { it.isNotBlank() },
        type = type?.name,
        accountId = accountId,
        categoryId = categoryId,
        contactId = contactId,
        shopId = shopId,
        fromDay = fromDay,
        toDay = toDay,
        minPaisa = minPaisa,
        maxPaisa = maxPaisa,
        limit = limit,
        offset = offset
    )

    suspend fun countFiltered(
        query: String? = null,
        type: TxnType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        contactId: Long? = null,
        shopId: Long? = null,
        fromDay: Int? = null,
        toDay: Int? = null,
        minPaisa: Long? = null,
        maxPaisa: Long? = null
    ): Int = txnDao.countFiltered(
        query = query?.takeIf { it.isNotBlank() },
        type = type?.name,
        accountId = accountId,
        categoryId = categoryId,
        contactId = contactId,
        shopId = shopId,
        fromDay = fromDay,
        toDay = toDay,
        minPaisa = minPaisa,
        maxPaisa = maxPaisa
    )

    fun observeRecent(limit: Int): Flow<List<TxnEntity>> = txnDao.observeRecent(limit)
    fun observeById(id: Long): Flow<TxnEntity?> = txnDao.observeById(id)
    suspend fun get(id: Long): TxnEntity? = txnDao.getById(id)
    fun observeTrash(): Flow<List<TxnEntity>> = txnDao.observeTrash()
    fun observeArchived(): Flow<List<TxnEntity>> = txnDao.observeArchived()

    /**
     * Inserts a validated txn atomically with its balance guard.
     * Throws [ValidationException] with a user-facing (Bengali) message on any rule violation.
     */
    suspend fun insert(txn: TxnEntity): Long = db.withTransaction {
        validate(txn)
        ensureBalanceAllows(txn, excludeTxnId = null)
        if (txn.idempotencyKey != null) {
            val existing = txnDao.findByIdempotencyKey(txn.idempotencyKey!!)
            if (existing != null) return@withTransaction existing.id
        }
        val id = txnDao.insert(txn.copy(dayKey = txn.dayKey.takeIf { it != 0 } ?: Dates.dayKey(txn.dateMillis)))
        auditDao.insert(
            AuditEntryEntity(
                entityType = "txn",
                entityId = id,
                action = "create",
                afterJson = "{\"type\":\"${txn.type}\",\"amount\":${txn.amountPaisa}}",
                atMillis = System.currentTimeMillis()
            )
        )
        id
    }

    suspend fun update(txn: TxnEntity) = db.withTransaction {
        validate(txn)
        ensureBalanceAllows(txn, excludeTxnId = txn.id)
        txnDao.update(txn)
        auditDao.insert(
            AuditEntryEntity(
                entityType = "txn",
                entityId = txn.id,
                action = "update",
                afterJson = "{\"type\":\"${txn.type}\",\"amount\":${txn.amountPaisa}}",
                atMillis = System.currentTimeMillis()
            )
        )
    }

    /**
     * Reversal: inserts a compensating txn and marks the original REVERSED.
     * Both rows remain in the ledger for audit — nothing silently disappears.
     */
    suspend fun reverse(originalId: Long, reason: String = "") = db.withTransaction {
        val original = txnDao.getById(originalId)
            ?: throw ValidationException("লেনদেন পাওয়া যায়নি")
        if (original.status != TxnStatus.ACTIVE) {
            throw ValidationException("শুধু সক্রিয় লেনদেন বাতিল করা যায়")
        }
        if (original.reversalOfId != null) {
            throw ValidationException("এটি নিজেই একটি বাতিলকরণ রেকর্ড")
        }
        val now = System.currentTimeMillis()
        val compensating = original.copy(
            id = 0,
            flow = when (original.flow) {
                TxnFlow.INFLOW -> TxnFlow.OUTFLOW
                TxnFlow.OUTFLOW -> TxnFlow.INFLOW
                TxnFlow.TRANSFER -> TxnFlow.TRANSFER
            },
            // For transfers the direction swap is handled by swapping the accounts.
            accountId = original.counterAccountId ?: original.accountId,
            counterAccountId = if (original.flow == TxnFlow.TRANSFER) original.accountId else original.counterAccountId,
            dateMillis = Dates.todayMillis(),
            dayKey = Dates.dayKey(System.currentTimeMillis()),
            description = "বাতিলকরণ: ${original.description}",
            note = reason,
            status = TxnStatus.ACTIVE,
            reversalOfId = original.id,
            idempotencyKey = null,
            createdAt = now,
            updatedAt = now
        )
        txnDao.insert(compensating)
        txnDao.update(original.copy(status = TxnStatus.REVERSED, updatedAt = now))
        auditDao.insert(
            AuditEntryEntity(
                entityType = "txn",
                entityId = original.id,
                action = "reverse",
                beforeJson = "{\"status\":\"ACTIVE\"}",
                afterJson = "{\"status\":\"REVERSED\"}",
                atMillis = now
            )
        )
    }

    /** Soft delete → trash; excluded from balances, restorable. */
    suspend fun softDelete(id: Long) {
        txnDao.setStatus(id, TxnStatus.DELETED.name, System.currentTimeMillis())
        auditDao.insert(
            AuditEntryEntity(entityType = "txn", entityId = id, action = "soft_delete", atMillis = System.currentTimeMillis())
        )
    }

    suspend fun restore(id: Long) {
        txnDao.setStatus(id, TxnStatus.ACTIVE.name, System.currentTimeMillis())
        auditDao.insert(
            AuditEntryEntity(entityType = "txn", entityId = id, action = "restore", atMillis = System.currentTimeMillis())
        )
    }

    suspend fun archive(id: Long, archived: Boolean) {
        txnDao.setStatus(id, if (archived) TxnStatus.ARCHIVED.name else TxnStatus.ACTIVE.name, System.currentTimeMillis())
    }

    suspend fun permanentDelete(id: Long) = db.withTransaction {
        txnDao.getById(id)?.let {
            if (it.status != TxnStatus.DELETED) throw ValidationException("শুধু ট্র্যাশে থাকা লেনদেন স্থায়ীভাবে মুছা যায়")
            txnDao.delete(it)
        }
    }

    fun observeAudit(entityId: Long) = auditDao.observeFor("txn", entityId)

    // ------------------------------------------------------------------

    private fun validate(txn: TxnEntity) {
        if (txn.amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (txn.amountPaisa > com.shohankhan.hisaabdesk.core.formatting.Money.MAX_ABS_PAISA) {
            throw ValidationException("পরিমাণ সীমার বাইরে")
        }
        if (!AccountingRules.componentsConsistent(txn.amountPaisa, txn.principalPaisa, txn.interestPaisa, txn.feePaisa)) {
            throw ValidationException("আসল, সুদ ও ফি-র যোগফল মোট পরিমাণের সমান হতে হবে")
        }
        if (AccountingRules.requiresAccount(txn.type) && txn.accountId == null) {
            throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        }
        if (AccountingRules.requiresCounterAccount(txn.type)) {
            if (txn.counterAccountId == null) throw ValidationException("গন্তব্য অ্যাকাউন্ট নির্বাচন করুন")
            if (txn.counterAccountId == txn.accountId) throw ValidationException("উৎস ও গন্তব্য অ্যাকাউন্ট এক হতে পারে না")
        }
        if (txn.flow == TxnFlow.TRANSFER && txn.counterAccountId == null) {
            throw ValidationException("গন্তব্য অ্যাকাউন্ট নির্বাচন করুন")
        }
    }

    private suspend fun ensureBalanceAllows(txn: TxnEntity, excludeTxnId: Long?) {
        val account = txn.accountId ?: return
        val acc = accountDao.getById(account) ?: throw ValidationException("অ্যাকাউন্ট পাওয়া যায়নি")
        if (acc.allowNegative) return
        val outgoing = when (txn.flow) {
            TxnFlow.OUTFLOW -> txn.amountPaisa
            TxnFlow.TRANSFER -> txn.amountPaisa
            TxnFlow.INFLOW -> 0L
        }
        if (outgoing <= 0) return
        val current = accountDao.balanceOf(account)?.balance ?: 0L
        // When editing, add back the effect of the txn being replaced.
        val existingEffect = if (excludeTxnId != null) {
            val old = txnDao.getById(excludeTxnId)
            if (old != null && old.status == TxnStatus.ACTIVE && old.accountId == account) {
                when (old.flow) {
                    TxnFlow.OUTFLOW, TxnFlow.TRANSFER -> old.amountPaisa
                    TxnFlow.INFLOW -> -old.amountPaisa
                }
            } else 0L
        } else 0L
        val effective = current + existingEffect
        if (effective < outgoing) {
            throw ValidationException("অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই")
        }
    }
}
