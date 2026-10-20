package com.shohankhan.hisaabdesk.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.InterestMethod
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanKind
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnStatus
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.data.repository.AccountRepository
import com.shohankhan.hisaabdesk.data.repository.ContactRepository
import com.shohankhan.hisaabdesk.data.repository.LoanRepository
import com.shohankhan.hisaabdesk.data.repository.ObligationRepository
import com.shohankhan.hisaabdesk.data.repository.ShoppingRepository
import com.shohankhan.hisaabdesk.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LedgerRepositoryTest {

    private lateinit var db: HisaabDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var txns: TransactionRepository
    private lateinit var shopping: ShoppingRepository
    private lateinit var obligations: ObligationRepository
    private lateinit var loans: LoanRepository
    private lateinit var contacts: ContactRepository

    private val now = System.currentTimeMillis()
    private val todayKey = Dates.dayKey(now)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HisaabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accounts = AccountRepository(db.accountDao(), db.txnDao())
        contacts = ContactRepository(db.contactDao())
        txns = TransactionRepository(db, db.txnDao(), db.accountDao(), db.auditDao())
        shopping = ShoppingRepository(db, db.shoppingDao(), txns)
        obligations = ObligationRepository(db, db.lendingDao(), db.borrowingDao(), db.receivableDao(), db.payableDao(), txns)
        loans = LoanRepository(db, db.loanDao(), txns)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun createAccount(name: String, opening: Long = 0, allowNegative: Boolean = false): Long =
        accounts.save(
            AccountEntity(
                name = name,
                type = AccountType.CASH,
                openingBalancePaisa = opening,
                allowNegative = allowNegative,
                createdAt = now,
                updatedAt = now
            )
        )

    private fun txn(
        type: TxnType,
        flow: TxnFlow,
        amount: Long,
        accountId: Long?,
        counterAccountId: Long? = null,
        key: String? = null
    ) = TxnEntity(
        type = type,
        flow = flow,
        amountPaisa = amount,
        dateMillis = now,
        dayKey = todayKey,
        accountId = accountId,
        counterAccountId = counterAccountId,
        paymentMethod = PaymentMethod.CASH,
        description = type.name,
        idempotencyKey = key,
        createdAt = now,
        updatedAt = now
    )

    // ------------------------------------------------------------------
    @Test
    fun `income increases and expense decreases account balance`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        txns.insert(txn(TxnType.INCOME, TxnFlow.INFLOW, 50_000, acc))
        txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 20_000, acc))
        assertEquals(130_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `expense beyond balance is rejected when negatives disallowed`() = runBlocking {
        val acc = createAccount("নগদ", opening = 10_000)
        var threw = false
        try {
            txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 20_000, acc))
        } catch (e: TransactionRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
        assertEquals(10_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `negative balances allowed only when account policy enables it`() = runBlocking {
        val acc = createAccount("ক্রেডিট", opening = 10_000, allowNegative = true)
        txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 15_000, acc))
        assertEquals(-5_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `transfer moves money without changing total assets or recording income`() = runBlocking {
        val cash = createAccount("নগদ", opening = 60_000)
        val bank = createAccount("ব্যাংক", opening = 40_000)
        txns.insert(txn(TxnType.TRANSFER, TxnFlow.TRANSFER, 25_000, cash, bank))
        assertEquals(35_000L, accounts.balanceOf(cash))
        assertEquals(65_000L, accounts.balanceOf(bank))
        assertEquals(100_000L, accounts.balanceOf(cash) + accounts.balanceOf(bank))
        assertEquals(0L, db.txnDao().incomeBetween(todayKey, todayKey))
        assertEquals(0L, db.txnDao().expenseBetween(todayKey, todayKey))
    }

    @Test
    fun `transfer to same account is rejected`() = runBlocking {
        val acc = createAccount("নগদ", opening = 10_000)
        var threw = false
        try {
            txns.insert(txn(TxnType.TRANSFER, TxnFlow.TRANSFER, 5_000, acc, acc))
        } catch (e: TransactionRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `reversal creates compensating entry and marks original reversed`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        val id = txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 30_000, acc))
        assertEquals(70_000L, accounts.balanceOf(acc))
        txns.reverse(id)
        assertEquals(100_000L, accounts.balanceOf(acc))
        assertEquals(TxnStatus.REVERSED, db.txnDao().getById(id)!!.status)
        val all = db.txnDao().observeFiltered(null, null, null, null, null, null, null, null, null, null, 50, 0).first()
        assertEquals(2, all.size)
        assertTrue(all.any { it.reversalOfId == id })
    }

    @Test
    fun `soft delete hides from totals and restore brings it back`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        val id = txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 30_000, acc))
        txns.softDelete(id)
        assertEquals(100_000L, accounts.balanceOf(acc))
        assertEquals(1, txns.observeTrash().first().size)
        txns.restore(id)
        assertEquals(70_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `permanent delete only allowed from trash`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        val id = txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 30_000, acc))
        var threw = false
        try {
            txns.permanentDelete(id)
        } catch (e: TransactionRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
        txns.softDelete(id)
        txns.permanentDelete(id)
        assertEquals(null, db.txnDao().getById(id))
    }

    @Test
    fun `idempotency key prevents duplicate payments`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        val first = txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 1_000, acc, key = "dup-key"))
        val second = txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 1_000, acc, key = "dup-key"))
        assertEquals(first, second)
        assertEquals(99_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `component mismatch is rejected`() = runBlocking {
        val acc = createAccount("নগদ", opening = 100_000)
        var threw = false
        try {
            txns.insert(
                TxnEntity(
                    type = TxnType.BORROW_REPAY,
                    flow = TxnFlow.OUTFLOW,
                    amountPaisa = 10_000,
                    principalPaisa = 8_000,
                    interestPaisa = 1_000,
                    dateMillis = now,
                    dayKey = todayKey,
                    accountId = acc,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: TransactionRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
    }

    // ---------------- shopping ----------------
    private suspend fun createShopAndPurchase(): Triple<Long, Long, Long> {
        val shopId = db.shopDao().insert(ShopEntity(name = "রহিম স্টোর", createdAt = now, updatedAt = now))
        val acc = createAccount("নগদ", opening = 200_000)
        val items = listOf(
            PurchaseItemEntity(
                purchaseId = 0, name = "চাল", quantityMilli = 5_000, unit = "কেজি",
                unitPricePaisa = 7_000, lineTotalPaisa = 35_000
            ),
            PurchaseItemEntity(
                purchaseId = 0, name = "ডাল", quantityMilli = 2_000, unit = "কেজি",
                unitPricePaisa = 12_000, lineTotalPaisa = 24_000, discountPaisa = 1_000
            )
        )
        val purchaseId = shopping.savePurchase(
            purchase = PurchaseEntity(
                shopId = shopId, dateMillis = now, dayKey = todayKey,
                grossPaisa = 0, discountPaisa = 0, chargesPaisa = 500, totalPaisa = 0,
                createdAt = now, updatedAt = now
            ),
            items = items,
            paidAtPurchasePaisa = 30_000,
            paymentAccountId = acc,
            paymentMethod = PaymentMethod.CASH
        )
        return Triple(shopId, purchaseId, acc)
    }

    @Test
    fun `purchase computes totals and records initial payment`() = runBlocking {
        val (_, purchaseId, acc) = createShopAndPurchase()
        val purchase = shopping.getPurchase(purchaseId)!!
        // gross 590, line discount 10, order charge 5 → payable 585 (in taka*100)
        assertEquals(59_000L, purchase.grossPaisa)
        assertEquals(58_500L, purchase.totalPaisa)
        assertEquals(30_000L, db.txnDao().sumPurchasePayments(purchaseId))
        assertEquals(170_000L, accounts.balanceOf(acc))
    }

    @Test
    fun `shop outstanding reduces with later payments and overpayment is rejected`() = runBlocking {
        val (_, purchaseId, acc) = createShopAndPurchase()
        shopping.addPayment(purchaseId, 20_000, acc, PaymentMethod.CASH, now, "", "")
        assertEquals(58_500L - 50_000L, db.shoppingDao().totalOutstandingSuspend())
        var threw = false
        try {
            shopping.addPayment(purchaseId, 9_000, acc, PaymentMethod.CASH, now, "", "")
        } catch (e: ShoppingRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
        shopping.addPayment(purchaseId, 8_500, acc, PaymentMethod.CASH, now, "", "")
        assertEquals(0L, db.shoppingDao().totalOutstandingSuspend())
        // full payment must not add a second expense for the same purchase
        assertEquals(30_000L + 20_000L + 8_500L, db.txnDao().sumPurchasePayments(purchaseId))
    }

    @Test
    fun `cash purchase creates no shop credit liability`() = runBlocking {
        val shopId = db.shopDao().insert(ShopEntity(name = "নগদ দোকান", createdAt = now, updatedAt = now))
        val acc = createAccount("নগদ", opening = 100_000)
        shopping.savePurchase(
            PurchaseEntity(
                shopId = shopId, dateMillis = now, dayKey = todayKey,
                grossPaisa = 0, discountPaisa = 0, chargesPaisa = 0, totalPaisa = 0,
                createdAt = now, updatedAt = now
            ),
            listOf(
                PurchaseItemEntity(
                    purchaseId = 0, name = "মাছ", quantityMilli = 1_000, unit = "কেজি",
                    unitPricePaisa = 40_000, lineTotalPaisa = 40_000
                )
            ),
            paidAtPurchasePaisa = 40_000,
            paymentAccountId = acc,
            paymentMethod = PaymentMethod.CASH
        )
        assertEquals(0L, db.shoppingDao().totalOutstandingSuspend())
        assertEquals(60_000L, accounts.balanceOf(acc))
    }

    // ---------------- obligations ----------------
    @Test
    fun `borrowing and lending stay separate`() = runBlocking {
        val acc = createAccount("নগদ", opening = 500_000)
        val karim = contacts.save(ContactEntity(name = "করিম", createdAt = now, updatedAt = now))
        val rahim = contacts.save(ContactEntity(name = "রহিম", createdAt = now, updatedAt = now))

        val lendingId = obligations.saveLending(
            com.shohankhan.hisaabdesk.data.local.entity.LendingEntity(
                contactId = karim, principalPaisa = 50_000, dateMillis = now, dayKey = todayKey,
                purpose = "ব্যবসা", createdAt = now, updatedAt = now
            ),
            outflowAccountId = acc, method = PaymentMethod.CASH
        )
        val borrowingId = obligations.saveBorrowing(
            com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity(
                contactId = rahim, principalPaisa = 30_000, dateMillis = now, dayKey = todayKey,
                purpose = "চিকিৎসা", createdAt = now, updatedAt = now
            ),
            inflowAccountId = acc, method = PaymentMethod.CASH
        )
        // money actually moved
        assertEquals(500_000L - 50_000L + 30_000L, accounts.balanceOf(acc))
        // lends/borrows are not income/expense
        assertEquals(0L, db.txnDao().incomeBetween(todayKey, todayKey))
        assertEquals(0L, db.txnDao().expenseBetween(todayKey, todayKey))

        obligations.addLendingCollection(lendingId, 20_000, 20_000, 0, acc, PaymentMethod.CASH, now, "")
        assertEquals(30_000L, db.summaryDao().observeLendingOutstanding().first())
        assertEquals(30_000L, db.summaryDao().observeBorrowingOutstanding().first())

        var threw = false
        try {
            obligations.addLendingCollection(lendingId, 40_000, 40_000, 0, acc, PaymentMethod.CASH, now, "")
        } catch (e: ObligationRepository.ValidationException) {
            threw = true
        }
        assertTrue("cannot collect more principal than outstanding", threw)
    }

    @Test
    fun `receivables and payables reject overpayment`() = runBlocking {
        val acc = createAccount("নগদ", opening = 200_000)
        val person = contacts.save(ContactEntity(name = "সালাম", createdAt = now, updatedAt = now))
        val recvId = obligations.saveReceivable(
            com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity(
                contactId = person, amountPaisa = 10_000, dateMillis = now, dayKey = todayKey,
                createdAt = now, updatedAt = now
            ),
            outflowAccountId = acc, method = PaymentMethod.CASH
        )
        obligations.addReceivableCollection(recvId, 4_000, acc, PaymentMethod.CASH, now, "")
        var threw = false
        try {
            obligations.addReceivableCollection(recvId, 7_000, acc, PaymentMethod.CASH, now, "")
        } catch (e: ObligationRepository.ValidationException) {
            threw = true
        }
        assertTrue(threw)
        assertEquals(6_000L, db.summaryDao().observeReceivableRemaining().first())
    }

    // ---------------- loans ----------------
    @Test
    fun `loan schedule payment allocation and completion`() = runBlocking {
        val acc = createAccount("নগদ", opening = 200_000)
        val loanId = loans.saveLoan(
            loan = LoanEntity(
                kind = LoanKind.NGO,
                institution = "ব্র্যাক",
                startDateMillis = now,
                dayKey = todayKey,
                principalPaisa = 60_000,
                interestMethod = InterestMethod.CONTRACT_TOTAL,
                totalScheduledPaisa = 66_000,
                frequency = "MONTHLY",
                installmentAmountPaisa = 22_000,
                installmentStartMillis = now,
                expectedInstallments = 3,
                createdAt = now,
                updatedAt = now
            ),
            scheduleInput = LoanRepository.ScheduleInput(
                mode = com.shohankhan.hisaabdesk.domain.loan.LoanCalculator.ScheduleMode.CONTRACT_TOTAL,
                frequency = Dates.Frequency.MONTHLY,
                intervalDays = 0,
                installmentAmountPaisa = 22_000,
                installmentStartMillis = now,
                expectedInstallments = 3,
                contractualTotalPaisa = 66_000
            ),
            disburseAccountId = acc,
            method = PaymentMethod.CASH,
            regenerateSchedule = true
        )
        assertEquals(260_000L, accounts.balanceOf(acc)) // opening + disbursement inflow
        val installments = loans.getInstallments(loanId)
        assertEquals(3, installments.size)
        assertEquals(66_000L, installments.sumOf { it.scheduledPaisa })

        val first = installments.first()
        loans.addPayment(loanId, first.id, 22_000, 20_000, 2_000, 0, acc, PaymentMethod.CASH, now, "", "")
        assertEquals(com.shohankhan.hisaabdesk.data.local.entity.InstallmentStatus.PAID, loans.getInstallments(loanId).first().status)

        var threw = false
        try {
            loans.addPayment(loanId, first.id, 1_000, 1_000, 0, 0, acc, PaymentMethod.CASH, now, "", "")
        } catch (e: LoanRepository.ValidationException) {
            threw = true
        }
        assertTrue("cannot overpay a settled installment", threw)

        // principal-only completion is not enough while installments remain
        loans.addPayment(loanId, installments[1].id, 22_000, 22_000, 0, 0, acc, PaymentMethod.CASH, now, "", "")
        assertFalse(loans.getLoan(loanId)!!.isCompleted)
        loans.addPayment(loanId, installments[2].id, 22_000, 18_000, 4_000, 0, acc, PaymentMethod.CASH, now, "", "")
        assertTrue(loans.getLoan(loanId)!!.isCompleted)
    }

    @Test
    fun `budget counts expenses but not transfers`() = runBlocking {
        val cash = createAccount("নগদ", opening = 200_000)
        val bank = createAccount("ব্যাংক", opening = 0)
        val planning = com.shohankhan.hisaabdesk.data.repository.PlanningRepository(
            db, db.goalDao(), db.budgetDao(), db.recurringDao(), txns
        )
        val budgetId = planning.saveBudget(
            com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity(
                name = "মাসিক",
                limitPaisa = 100_000,
                period = com.shohankhan.hisaabdesk.data.local.entity.BudgetPeriod.MONTHLY,
                startDateMillis = now,
                createdAt = now,
                updatedAt = now
            )
        )
        txns.insert(txn(TxnType.EXPENSE, TxnFlow.OUTFLOW, 30_000, cash))
        txns.insert(txn(TxnType.TRANSFER, TxnFlow.TRANSFER, 50_000, cash, bank))
        val usage = planning.usage(planning.getBudget(budgetId)!!)
        assertEquals(30_000L, usage.spentPaisa)
        assertFalse(usage.overBudget)
        assertEquals(70_000L, usage.remainingPaisa)
    }

    @Test
    fun `reconciliation passes on consistent data`() = runBlocking {
        val cash = createAccount("নগদ", opening = 100_000)
        val bank = createAccount("ব্যাংক", opening = 0)
        txns.insert(txn(TxnType.TRANSFER, TxnFlow.TRANSFER, 40_000, cash, bank))
        txns.insert(txn(TxnType.INCOME, TxnFlow.INFLOW, 10_000, bank))
        val reports = com.shohankhan.hisaabdesk.data.repository.ReportRepository(
            db.txnDao(), db.shoppingDao(), db.loanDao()
        )
        val report = reports.reconcile()
        assertTrue(report.checks.all { it.ok })
    }

    @Test
    fun `account with ledger rows cannot be deleted`() = runBlocking {
        val acc = createAccount("নগদ", opening = 0)
        txns.insert(txn(TxnType.INCOME, TxnFlow.INFLOW, 1_000, acc))
        assertFalse(accounts.canDelete(acc))
        val entity = accounts.get(acc)!!
        assertFalse(accounts.delete(entity))
        assertNotNull(accounts.get(acc))
    }
}
