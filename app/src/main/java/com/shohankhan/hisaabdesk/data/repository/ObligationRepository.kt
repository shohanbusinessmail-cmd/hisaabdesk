package com.shohankhan.hisaabdesk.data.repository

import androidx.room.withTransaction
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.BorrowingDao
import com.shohankhan.hisaabdesk.data.local.dao.LendingDao
import com.shohankhan.hisaabdesk.data.local.dao.PayableDao
import com.shohankhan.hisaabdesk.data.local.dao.ReceivableDao
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity
import com.shohankhan.hisaabdesk.data.local.entity.LendingEntity
import com.shohankhan.hisaabdesk.data.local.entity.PayableEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import kotlinx.coroutines.flow.Flow

/**
 * Personal borrowing & lending plus receivables/payables.
 * Every payment references the exact obligation record (never a name match).
 */
class ObligationRepository(
    private val db: HisaabDatabase,
    private val lendingDao: LendingDao,
    private val borrowingDao: BorrowingDao,
    private val receivableDao: ReceivableDao,
    private val payableDao: PayableDao,
    private val txnRepository: TransactionRepository
) {
    class ValidationException(message: String) : Exception(message)

    // ---------------- lending ----------------
    fun observeLendings() = lendingDao.observeActive()
    fun observeLendingsForContact(contactId: Long) = lendingDao.observeForContact(contactId)
    fun observeLending(id: Long) = lendingDao.observeById(id)
    suspend fun getLending(id: Long) = lendingDao.getById(id)

    fun observeLendingPrincipalPaid(id: Long): Flow<Long> = db.txnDao().observeLendingPrincipalPaid(id)
    fun observeLendingInterestPaid(id: Long): Flow<Long> = db.txnDao().observeLendingInterestPaid(id)

    suspend fun saveLending(item: LendingEntity, outflowAccountId: Long?, method: PaymentMethod): Long =
        db.withTransaction {
            if (item.principalPaisa <= 0) throw ValidationException("আসল টাকা শূন্যের বেশি হতে হবে")
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                lendingDao.insert(item.copy(dayKey = item.dayKey.takeIf { it != 0 } ?: Dates.dayKey(item.dateMillis), createdAt = now, updatedAt = now))
            } else {
                lendingDao.update(item.copy(updatedAt = now))
                item.id
            }
            if (item.id == 0L && outflowAccountId != null) {
                txnRepository.insert(
                    TxnEntity(
                        type = TxnType.LEND,
                        flow = TxnFlow.OUTFLOW,
                        amountPaisa = item.principalPaisa,
                        dateMillis = item.dateMillis,
                        dayKey = Dates.dayKey(item.dateMillis),
                        accountId = outflowAccountId,
                        contactId = item.contactId,
                        lendingId = id,
                        paymentMethod = method,
                        description = "ধার দেওয়া: ${item.purpose}",
                        note = item.notes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            id
        }

    suspend fun addLendingCollection(
        lendingId: Long,
        amountPaisa: Long,
        principalPart: Long,
        interestPart: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        note: String
    ): Long = db.withTransaction {
        val lending = lendingDao.getById(lendingId) ?: throw ValidationException("রেকর্ড পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        val already = db.txnDao().sumLendingPrincipalPaid(lendingId)
        val remainingPrincipal = lending.principalPaisa - already
        if (principalPart > remainingPrincipal) {
            throw ValidationException("আসল বাকির চেয়ে বেশি আদায় করা যায় না")
        }
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.LEND_COLLECT,
                flow = TxnFlow.INFLOW,
                amountPaisa = amountPaisa,
                principalPaisa = principalPart,
                interestPaisa = interestPart,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                contactId = lending.contactId,
                lendingId = lendingId,
                paymentMethod = method,
                description = "ধারের টাকা আদায়",
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    // ---------------- borrowing ----------------
    fun observeBorrowings() = borrowingDao.observeActive()
    fun observeBorrowingsForContact(contactId: Long) = borrowingDao.observeForContact(contactId)
    fun observeBorrowing(id: Long) = borrowingDao.observeById(id)
    suspend fun getBorrowing(id: Long) = borrowingDao.getById(id)
    fun observeBorrowingPrincipalPaid(id: Long) = db.txnDao().observeBorrowingPrincipalPaid(id)
    fun observeBorrowingInterestPaid(id: Long) = db.txnDao().observeBorrowingInterestPaid(id)

    suspend fun saveBorrowing(item: BorrowingEntity, inflowAccountId: Long?, method: PaymentMethod): Long =
        db.withTransaction {
            if (item.principalPaisa <= 0) throw ValidationException("আসল টাকা শূন্যের বেশি হতে হবে")
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                borrowingDao.insert(item.copy(dayKey = item.dayKey.takeIf { it != 0 } ?: Dates.dayKey(item.dateMillis), createdAt = now, updatedAt = now))
            } else {
                borrowingDao.update(item.copy(updatedAt = now))
                item.id
            }
            if (item.id == 0L && inflowAccountId != null) {
                txnRepository.insert(
                    TxnEntity(
                        type = TxnType.BORROW,
                        flow = TxnFlow.INFLOW,
                        amountPaisa = item.principalPaisa,
                        dateMillis = item.dateMillis,
                        dayKey = Dates.dayKey(item.dateMillis),
                        accountId = inflowAccountId,
                        contactId = item.contactId,
                        borrowingId = id,
                        paymentMethod = method,
                        description = "ধার নেওয়া: ${item.purpose}",
                        note = item.notes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            id
        }

    suspend fun addBorrowingRepayment(
        borrowingId: Long,
        amountPaisa: Long,
        principalPart: Long,
        interestPart: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        note: String
    ): Long = db.withTransaction {
        val borrowing = borrowingDao.getById(borrowingId) ?: throw ValidationException("রেকর্ড পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        val already = db.txnDao().sumBorrowingPrincipalPaid(borrowingId)
        if (principalPart > borrowing.principalPaisa - already) {
            throw ValidationException("আসল বাকির চেয়ে বেশি পরিশোধ করা যায় না")
        }
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.BORROW_REPAY,
                flow = TxnFlow.OUTFLOW,
                amountPaisa = amountPaisa,
                principalPaisa = principalPart,
                interestPaisa = interestPart,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                contactId = borrowing.contactId,
                borrowingId = borrowingId,
                paymentMethod = method,
                description = "ধার পরিশোধ",
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    // ---------------- receivables ----------------
    fun observeReceivables() = receivableDao.observeActive()
    fun observeReceivablesForContact(contactId: Long) = receivableDao.observeForContact(contactId)
    fun observeReceivable(id: Long) = receivableDao.observeById(id)
    suspend fun getReceivable(id: Long) = receivableDao.getById(id)
    fun observeReceivableCollected(id: Long) = db.txnDao().observeReceivableCollected(id)

    suspend fun saveReceivable(item: ReceivableEntity, outflowAccountId: Long?, method: PaymentMethod): Long =
        db.withTransaction {
            if (item.amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                receivableDao.insert(item.copy(dayKey = item.dayKey.takeIf { it != 0 } ?: Dates.dayKey(item.dateMillis), createdAt = now, updatedAt = now))
            } else {
                receivableDao.update(item.copy(updatedAt = now))
                item.id
            }
            if (item.id == 0L && outflowAccountId != null) {
                txnRepository.insert(
                    TxnEntity(
                        type = TxnType.RECEIVABLE_CREATE,
                        flow = TxnFlow.OUTFLOW,
                        amountPaisa = item.amountPaisa,
                        dateMillis = item.dateMillis,
                        dayKey = Dates.dayKey(item.dateMillis),
                        accountId = outflowAccountId,
                        contactId = item.contactId,
                        receivableId = id,
                        paymentMethod = method,
                        description = "পাওনা: ${item.reason}",
                        note = item.notes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            id
        }

    suspend fun addReceivableCollection(
        receivableId: Long,
        amountPaisa: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        note: String
    ): Long = db.withTransaction {
        val r = receivableDao.getById(receivableId) ?: throw ValidationException("রেকর্ড পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        val collected = db.txnDao().sumReceivableCollected(receivableId)
        if (amountPaisa > r.amountPaisa - collected) {
            throw ValidationException("বাকি পরিমাণের বেশি আদায় করা যায় না")
        }
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.RECEIVABLE_COLLECT,
                flow = TxnFlow.INFLOW,
                amountPaisa = amountPaisa,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                contactId = r.contactId,
                receivableId = receivableId,
                paymentMethod = method,
                description = "পাওনা আদায়",
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    // ---------------- payables ----------------
    fun observePayables() = payableDao.observeActive()
    fun observePayablesForContact(contactId: Long) = payableDao.observeForContact(contactId)
    fun observePayable(id: Long) = payableDao.observeById(id)
    suspend fun getPayable(id: Long) = payableDao.getById(id)
    fun observePayablePaid(id: Long) = db.txnDao().observePayablePaid(id)

    suspend fun savePayable(item: PayableEntity, inflowAccountId: Long?, method: PaymentMethod): Long =
        db.withTransaction {
            if (item.amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                payableDao.insert(item.copy(dayKey = item.dayKey.takeIf { it != 0 } ?: Dates.dayKey(item.dateMillis), createdAt = now, updatedAt = now))
            } else {
                payableDao.update(item.copy(updatedAt = now))
                item.id
            }
            if (item.id == 0L && inflowAccountId != null) {
                txnRepository.insert(
                    TxnEntity(
                        type = TxnType.PAYABLE_CREATE,
                        flow = TxnFlow.INFLOW,
                        amountPaisa = item.amountPaisa,
                        dateMillis = item.dateMillis,
                        dayKey = Dates.dayKey(item.dateMillis),
                        accountId = inflowAccountId,
                        contactId = item.contactId,
                        payableId = id,
                        paymentMethod = method,
                        description = "দেনা: ${item.reason}",
                        note = item.notes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            id
        }

    suspend fun addPayablePayment(
        payableId: Long,
        amountPaisa: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        note: String
    ): Long = db.withTransaction {
        val p = payableDao.getById(payableId) ?: throw ValidationException("রেকর্ড পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        val paid = db.txnDao().sumPayablePaid(payableId)
        if (amountPaisa > p.amountPaisa - paid) {
            throw ValidationException("বাকি পরিমাণের বেশি পরিশোধ করা যায় না")
        }
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.PAYABLE_PAY,
                flow = TxnFlow.OUTFLOW,
                amountPaisa = amountPaisa,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                contactId = p.contactId,
                payableId = payableId,
                paymentMethod = method,
                description = "দেনা পরিশোধ",
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun setLendingArchived(id: Long, archived: Boolean) {
        lendingDao.getById(id)?.let { lendingDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun setBorrowingArchived(id: Long, archived: Boolean) {
        borrowingDao.getById(id)?.let { borrowingDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun setReceivableArchived(id: Long, archived: Boolean) {
        receivableDao.getById(id)?.let { receivableDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun setPayableArchived(id: Long, archived: Boolean) {
        payableDao.getById(id)?.let { payableDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis())) }
    }

}
