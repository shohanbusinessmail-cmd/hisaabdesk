package com.shohankhan.hisaabdesk.data.repository

import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.AccountBalanceRow
import com.shohankhan.hisaabdesk.data.local.dao.LoanDao
import com.shohankhan.hisaabdesk.data.local.dao.ShoppingDao
import com.shohankhan.hisaabdesk.data.local.dao.TxnDao
import com.shohankhan.hisaabdesk.data.local.entity.LoanKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * All dashboard/report figures are computed from saved records via the ledger.
 * Classification is centralized in docs/FINANCIAL_RULES.md and applied identically
 * in dashboard, reports and reconciliation.
 */
class ReportRepository(
    private val txnDao: TxnDao,
    private val shoppingDao: ShoppingDao,
    private val loanDao: LoanDao
) {

    data class DashboardData(
        val todayIncome: Long,
        val todayExpense: Long,
        val monthIncome: Long,
        val monthExpense: Long,
        val savingsBalance: Long,
        val totalReceivable: Long,
        val totalPayable: Long,
        val shopOutstanding: Long,
        val shopOverdue: Long,
        val borrowedOutstanding: Long,
        val lentOutstanding: Long,
        val ngoOutstanding: Long,
        val bankOutstanding: Long,
        val hasAnyData: Boolean
    )

    fun observeDailyTotals(fromDay: Int, toDay: Int) = combine(
        txnDao.observeIncomeBetween(fromDay, toDay),
        txnDao.observeExpenseBetween(fromDay, toDay),
        shoppingDao.observeTotalPurchaseExpenseBetween(fromDay, toDay)
    ) { income, expense, purchases ->
        Triple(income, expense + purchases, purchases)
    }

    suspend fun incomeBetween(fromDay: Int, toDay: Int): Long = txnDao.incomeBetween(fromDay, toDay)

    suspend fun expenseBetween(fromDay: Int, toDay: Int): Long =
        txnDao.expenseBetween(fromDay, toDay) + shoppingDao.totalPurchaseExpenseBetween(fromDay, toDay)

    fun observeIncomeByMonth(fromDay: Int, toDay: Int) = txnDao.observeIncomeByMonth(fromDay, toDay)
    fun observeExpenseByMonth(fromDay: Int, toDay: Int) = txnDao.observeExpenseByMonth(fromDay, toDay)
    fun observeExpenseByCategory(fromDay: Int, toDay: Int) = txnDao.observeExpenseByCategory(fromDay, toDay)
    fun observeIncomeByCategory(fromDay: Int, toDay: Int) = txnDao.observeIncomeByCategory(fromDay, toDay)
    fun observeIncomeByDay(fromDay: Int, toDay: Int) = txnDao.observeIncomeByDay(fromDay, toDay)
    fun observeExpenseByDay(fromDay: Int, toDay: Int) = txnDao.observeExpenseByDay(fromDay, toDay)

    fun observeShopOutstanding() = shoppingDao.observeTotalShopOutstanding()
    fun observeShopOverdue(now: Long) = shoppingDao.observeTotalShopOverdue(now)
    fun observeShopTotals() = shoppingDao.observeShopTotals()

    /** Totals across obligations — computed from ledger sums per record. */
    fun observeObligationTotals(
        lendingPrincipal: Flow<List<Pair<Long, Long>>>,
        borrowingPrincipal: Flow<List<Pair<Long, Long>>>,
        receivableAmounts: Flow<List<Pair<Long, Long>>>,
        payableAmounts: Flow<List<Pair<Long, Long>>>
    ): Flow<ObTotals> = combine(
        lendingPrincipal, borrowingPrincipal, receivableAmounts, payableAmounts
    ) { lend, borrow, recv, pay ->
        ObTotals(
            lentOutstanding = lend.sumOf { (principal, paid) -> (principal - paid).coerceAtLeast(0) },
            borrowedOutstanding = borrow.sumOf { (principal, paid) -> (principal - paid).coerceAtLeast(0) },
            receivableRemaining = recv.sumOf { (amount, collected) -> (amount - collected).coerceAtLeast(0) },
            payableRemaining = pay.sumOf { (amount, paid) -> (amount - paid).coerceAtLeast(0) }
        )
    }

    data class ObTotals(
        val lentOutstanding: Long,
        val borrowedOutstanding: Long,
        val receivableRemaining: Long,
        val payableRemaining: Long
    )

    /**
     * Net worth (documented policy):
     *  assets = Σ account balances (incl. savings) + lending outstanding + receivable remaining
     *  liabilities = borrowing outstanding + payable remaining + shop outstanding + loan principal outstanding
     */
    data class NetWorth(
        val assets: Long,
        val liabilities: Long,
        val net: Long,
        val accountBalances: List<AccountBalanceRow>,
        val lendingOutstanding: Long,
        val receivableRemaining: Long,
        val borrowingOutstanding: Long,
        val payableRemaining: Long,
        val shopOutstanding: Long,
        val ngoPrincipalOutstanding: Long,
        val bankPrincipalOutstanding: Long
    )

    suspend fun netWorth(
        balances: List<AccountBalanceRow>,
        ob: ObTotals,
        shopOutstanding: Long
    ): NetWorth {
        val ngoOut = loanPrincipalOutstanding(LoanKind.NGO)
        val bankOut = loanPrincipalOutstanding(LoanKind.BANK)
        val assets = balances.sumOf { it.balance } + ob.lentOutstanding + ob.receivableRemaining
        val liabilities = ob.borrowedOutstanding + ob.payableRemaining + shopOutstanding + ngoOut + bankOut
        return NetWorth(
            assets = assets,
            liabilities = liabilities,
            net = assets - liabilities,
            accountBalances = balances,
            lendingOutstanding = ob.lentOutstanding,
            receivableRemaining = ob.receivableRemaining,
            borrowingOutstanding = ob.borrowedOutstanding,
            payableRemaining = ob.payableRemaining,
            shopOutstanding = shopOutstanding,
            ngoPrincipalOutstanding = ngoOut,
            bankPrincipalOutstanding = bankOut
        )
    }

    private suspend fun loanPrincipalOutstanding(kind: LoanKind): Long {
        val loans = loanDao.getLoansByKind(kind.name)
        var total = 0L
        for (loan in loans) {
            val paid = txnDao.sumLoanPrincipalPaid(loan.id)
            total += (loan.principalPaisa - paid).coerceAtLeast(0)
        }
        return total
    }

    // ---------------- reconciliation ----------------
    data class ReconcileReport(
        val checks: List<ReconcileCheck>
    ) {
        val allOk: Boolean get() = checks.all { it.ok }
    }

    data class ReconcileCheck(val name: String, val ok: Boolean, val detail: String)

    suspend fun reconcile(): ReconcileReport {
        val checks = mutableListOf<ReconcileCheck>()
        checks += ReconcileCheck(
            name = "ট্রান্সফারের দুই পাশ",
            ok = txnDao.countBrokenTransfers() == 0,
            detail = "প্রতিটি ট্রান্সফারে উৎস ও গন্তব্য আলাদা হতে হবে"
        )
        checks += ReconcileCheck(
            name = "লেনদেনের অংশবিভাজন",
            ok = txnDao.countComponentMismatch() == 0,
            detail = "আসল + সুদ + ফি = মোট পরিমাণ"
        )
        val shopTotal = shoppingDao.totalOutstandingSuspend()
        val shopPaidOk = shoppingDao.purchasesOverpaidCount() == 0
        checks += ReconcileCheck(
            name = "দোকানের বাকি",
            ok = shopPaidOk && shopTotal >= 0,
            detail = "কোনো কেনাকাটায় পরিশোধ মোটের বেশি নয়"
        )
        val loans = loanDao.getLoansByKind(null)
        var loanIssues = 0
        for (loan in loans) {
            val paid = txnDao.sumLoanPayments(loan.id)
            val scheduled = loanDao.sumScheduled(loan.id)
            val installmentsPaid = loanDao.getInstallments(loan.id).sumOf { it.paidPaisa }
            if (installmentsPaid > scheduled + 1) loanIssues++
            if (paid < 0) loanIssues++
        }
        checks += ReconcileCheck(
            name = "ঋণের পরিশোধ",
            ok = loanIssues == 0,
            detail = "কিস্তির পরিশোধ নির্ধারিত পরিমাণের সাথে সামঞ্জস্যপূর্ণ"
        )
        return ReconcileReport(checks)
    }

    companion object {
        fun monthRange(year: Int, month: Int): Pair<Int, Int> {
            val start = LocalDate.of(year, month, 1)
            val end = start.withDayOfMonth(start.lengthOfMonth())
            return Dates.dayKey(start) to Dates.dayKey(end)
        }
    }
}
