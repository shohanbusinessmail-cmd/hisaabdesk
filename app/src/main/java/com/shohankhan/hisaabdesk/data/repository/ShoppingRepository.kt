package com.shohankhan.hisaabdesk.data.repository

import androidx.room.withTransaction
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.ShoppingDao
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.domain.shopping.PurchaseCalculator
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val db: HisaabDatabase,
    private val shoppingDao: ShoppingDao,
    private val txnRepository: TransactionRepository
) {

    class ValidationException(message: String) : Exception(message)

    fun observePurchases(shopId: Long?, fromDay: Int?, toDay: Int?, query: String?, limit: Int, offset: Int): Flow<List<PurchaseEntity>> =
        shoppingDao.observePurchasesFiltered(shopId, fromDay, toDay, query, limit, offset)

    fun observePurchase(id: Long) = shoppingDao.observePurchase(id)
    fun observeItems(purchaseId: Long) = shoppingDao.observeItems(purchaseId)
    fun observeShopPurchasesWithPaid(shopId: Long) = shoppingDao.observeShopPurchasesWithPaid(shopId)
    fun observeShopTotals() = shoppingDao.observeShopTotals()
    fun observeTotalShopOutstanding() = shoppingDao.observeTotalShopOutstanding()
    fun observeTotalShopOverdue(now: Long) = shoppingDao.observeTotalShopOverdue(now)
    suspend fun getPurchase(id: Long) = shoppingDao.getPurchase(id)
    suspend fun getItems(id: Long) = shoppingDao.getItems(id)
    suspend fun totalPurchaseExpenseBetween(fromDay: Int, toDay: Int) =
        shoppingDao.totalPurchaseExpenseBetween(fromDay, toDay)

    fun observeTotalPurchaseExpenseBetween(fromDay: Int, toDay: Int) =
        shoppingDao.observeTotalPurchaseExpenseBetween(fromDay, toDay)

    /**
     * Saves a purchase with its item lines and the initial payment (may be zero).
     * Expense is recognized at purchase time (accrual); the payment is a cash
     * movement — it is never double-counted as a second expense.
     */
    suspend fun savePurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        paidAtPurchasePaisa: Long,
        paymentAccountId: Long?,
        paymentMethod: PaymentMethod,
        existingId: Long = 0L
    ): Long = db.withTransaction {
        if (items.isEmpty()) throw ValidationException("কমপক্ষে একটি পণ্য যোগ করুন")
        val totals = PurchaseCalculator.totals(
            lines = items.map {
                PurchaseCalculator.LineInput(it.quantityMilli, it.unitPricePaisa, it.discountPaisa, it.chargePaisa)
            },
            orderDiscountPaisa = purchase.discountPaisa,
            orderChargesPaisa = purchase.chargesPaisa
        )
        val computed = purchase.copy(
            grossPaisa = totals.grossPaisa,
            totalPaisa = totals.payablePaisa
        )
        if (paidAtPurchasePaisa < 0) throw ValidationException("পরিশোধ নেগেটিভ হতে পারে না")

        val now = System.currentTimeMillis()
        val purchaseId: Long
        if (existingId != 0L) {
            val old = shoppingDao.getPurchase(existingId) ?: throw ValidationException("কেনাকাটা পাওয়া যায়নি")
            val alreadyPaid = db.txnDao().sumPurchasePayments(existingId)
            // Preserve payment history; enforce that the new total still covers payments.
            if (paidAtPurchasePaisa > 0 && alreadyPaid > 0) {
                throw ValidationException("এই কেনাকাটায় আগে থেকেই পরিশোধ আছে — নতুন পরিশোধ আলাদা ‘পরিশোধ যোগ করুন’ থেকে দিন")
            }
            if (totals.payablePaisa < alreadyPaid) {
                throw ValidationException("নতুন মোট পরিশোধিত পরিমাণের চেয়ে কম — আগে পরিশোধ সম্পাদনা করুন")
            }
            shoppingDao.updatePurchase(computed.copy(id = existingId, updatedAt = now))
            shoppingDao.replaceItems(existingId, items)
            purchaseId = existingId
        } else {
            purchaseId = shoppingDao.insertPurchase(computed.copy(createdAt = now, updatedAt = now))
            shoppingDao.replaceItems(purchaseId, items)
        }

        if (paidAtPurchasePaisa > 0) {
            if (paymentAccountId == null) throw ValidationException("পরিশোধের অ্যাকাউন্ট নির্বাচন করুন")
            if (paidAtPurchasePaisa > totals.payablePaisa) {
                throw ValidationException("পরিশোধ বাকি পরিমাণের বেশি হতে পারে না")
            }
            txnRepository.insert(
                TxnEntity(
                    type = TxnType.SHOP_PAYMENT,
                    flow = TxnFlow.OUTFLOW,
                    amountPaisa = paidAtPurchasePaisa,
                    dateMillis = purchase.dateMillis,
                    dayKey = Dates.dayKey(purchase.dateMillis),
                    accountId = paymentAccountId,
                    shopId = purchase.shopId,
                    purchaseId = purchaseId,
                    paymentMethod = paymentMethod,
                    description = "কেনাকাটার পরিশোধ",
                    receiptNo = purchase.receiptNo,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        purchaseId
    }

    /** Adds a payment against a purchase. Rejects overpayment; validates shop linkage. */
    suspend fun addPayment(
        purchaseId: Long,
        amountPaisa: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        receiptNo: String,
        note: String
    ): Long = db.withTransaction {
        val purchase = shoppingDao.getPurchase(purchaseId)
            ?: throw ValidationException("কেনাকাটা পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("পরিশোধের অ্যাকাউন্ট নির্বাচন করুন")
        val paid = db.txnDao().sumPurchasePayments(purchaseId)
        val outstanding = purchase.totalPaisa - paid
        if (amountPaisa > outstanding) {
            throw ValidationException("পরিশোধ বাকি পরিমাণের বেশি — বাকি ${outstanding}")
        }
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.SHOP_PAYMENT,
                flow = TxnFlow.OUTFLOW,
                amountPaisa = amountPaisa,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                shopId = purchase.shopId,
                purchaseId = purchaseId,
                paymentMethod = method,
                description = "দোকানের বাকি পরিশোধ",
                receiptNo = receiptNo,
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun setArchived(purchaseId: Long, archived: Boolean) {
        val p = shoppingDao.getPurchase(purchaseId) ?: return
        shoppingDao.updatePurchase(p.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
    }
}
