package com.shohankhan.hisaabdesk.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.EmptyState
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.StatCard
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.IncomeGreen
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary
import com.shohankhan.hisaabdesk.ui.theme.WarningAmber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUi(
    val todayIncome: Long = 0,
    val todayExpense: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val savingsBalance: Long = 0,
    val receivable: Long = 0,
    val payable: Long = 0,
    val shopOutstanding: Long = 0,
    val borrowed: Long = 0,
    val lent: Long = 0,
    val ngoOutstanding: Long = 0,
    val bankOutstanding: Long = 0,
    val upcomingCount: Int = 0,
    val overdueCount: Int = 0,
    val recent: List<TxnEntity> = emptyList(),
    val accountRows: List<AccountRow> = emptyList(),
    val hasData: Boolean = false
)

data class AccountRow(val id: Long, val label: String, val balance: Long, val isSavings: Boolean)

class DashboardViewModel(container: AppContainer) : ViewModel() {

    val digitStyle: StateFlow<Money.DigitStyle> =
        container.settingsRepository.settings
            .map { it.digitStyle }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    val ui: StateFlow<DashboardUi>

    init {
        val today = LocalDate.now()
        val todayKey = Dates.dayKey(today)
        val monthStart = Dates.dayKey(Dates.startOfMonth(today))
        val monthEnd = Dates.dayKey(Dates.endOfMonth(today))

        val summary = container.database.summaryDao()
        val reports = container.reportRepository

        val accountsWithBalances = combine(
            container.accountRepository.observeAll(),
            container.accountRepository.observeBalances()
        ) { accounts, balances ->
            accounts.map { a ->
                AccountRow(
                    id = a.id,
                    label = a.name,
                    balance = balances.firstOrNull { it.id == a.id }?.balance ?: 0L,
                    isSavings = a.type == com.shohankhan.hisaabdesk.data.local.entity.AccountType.SAVINGS
                )
            }
        }

        val obligations = combine(
            summary.observeLendingOutstanding(),
            summary.observeBorrowingOutstanding(),
            summary.observeReceivableRemaining(),
            summary.observePayableRemaining()
        ) { lent, borrowed, recv, pay -> listOf(lent, borrowed, recv, pay) }

        val loans = combine(
            summary.observeLoanPrincipalOutstanding("NGO"),
            summary.observeLoanPrincipalOutstanding("BANK")
        ) { ngo, bank -> listOf(ngo, bank) }

        val counters = combine(
            container.planningRepository.observeRecurring(),
            container.loanRepository.observeOverdueInstallments(System.currentTimeMillis(), 200),
            container.loanRepository.observeUpcomingInstallments(
                System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000, 200
            )
        ) { bills, overdue, upcoming ->
            Triple(bills.count { !it.isPaused }, overdue.size, upcoming.size)
        }

        val core = combine(
            reports.observeDailyTotals(todayKey, todayKey),
            reports.observeDailyTotals(monthStart, monthEnd),
            container.shoppingRepository.observeTotalShopOutstanding(),
            container.transactionRepository.observeRecent(8)
        ) { todayTotals, monthTotals, shop, recent ->
            CoreData(todayTotals, monthTotals, shop, recent)
        }

        ui = combine(core, accountsWithBalances, obligations, loans, counters) { c, accRows, ob, ln, cnt ->
            DashboardUi(
                todayIncome = c.today.first,
                todayExpense = c.today.second,
                monthIncome = c.month.first,
                monthExpense = c.month.second,
                savingsBalance = accRows.filter { it.isSavings }.sumOf { it.balance },
                receivable = ob[2],
                payable = ob[3],
                shopOutstanding = c.shop,
                borrowed = ob[1],
                lent = ob[0],
                ngoOutstanding = ln[0],
                bankOutstanding = ln[1],
                upcomingCount = cnt.third,
                overdueCount = cnt.second,
                recent = c.recent,
                accountRows = accRows,
                hasData = c.recent.isNotEmpty() || c.month.first > 0 || c.month.second > 0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUi())
    }

    private data class CoreData(
        val today: Triple<Long, Long, Long>,
        val month: Triple<Long, Long, Long>,
        val shop: Long,
        val recent: List<TxnEntity>
    )
}

@Composable
fun DashboardScreen(
    onOpenTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenAccounts: () -> Unit,
    onQuickAddIncome: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenReceivables: () -> Unit,
    onOpenPayables: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenLoans: (String) -> Unit
) {
    val vm: DashboardViewModel = appViewModel { DashboardViewModel(it.container) }
    val ui by vm.ui.collectAsState()
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "ড্যাশবোর্ড") {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("আজকের আয়", ui.todayIncome, digit, IncomeGreen, onQuickAddIncome)
            Spacer(Modifier.width(10.dp))
            StatCard("আজকের ব্যয়", ui.todayExpense, digit, ExpenseRed, onQuickAddExpense)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("এ মাসের আয়", ui.monthIncome, digit, IncomeGreen, onOpenTransactions)
            Spacer(Modifier.width(10.dp))
            StatCard("এ মাসের ব্যয়", ui.monthExpense, digit, ExpenseRed, onOpenTransactions)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("মোট পাওনা", ui.receivable, digit, IncomeGreen, onOpenReceivables)
            Spacer(Modifier.width(10.dp))
            StatCard("মোট দেনা", ui.payable, digit, ExpenseRed, onOpenPayables)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("দোকানের বাকি", ui.shopOutstanding, digit, WarningAmber, onOpenShopping)
            Spacer(Modifier.width(10.dp))
            StatCard("সঞ্চয়", ui.savingsBalance, digit, IncomeGreen, onOpenSavings)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("ধার দিয়েছি", ui.lent, digit, WarningAmber, onOpenDebts)
            Spacer(Modifier.width(10.dp))
            StatCard("ধার নিয়েছি", ui.borrowed, digit, ExpenseRed, onOpenDebts)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard("এনজিও ঋণ বাকি", ui.ngoOutstanding, digit, ExpenseRed) { onOpenLoans("NGO") }
            Spacer(Modifier.width(10.dp))
            StatCard("ব্যাংক ঋণ বাকি", ui.bankOutstanding, digit, ExpenseRed) { onOpenLoans("BANK") }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "অ্যাকাউন্ট ব্যালেন্স") {
            if (ui.accountRows.isEmpty()) {
                EmptyState("কোনো অ্যাকাউন্ট নেই", actionLabel = "অ্যাকাউন্ট যোগ করুন", onAction = onOpenAccounts)
            } else {
                ui.accountRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAccounts)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(row.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Money.format(row.balance, digit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (row.balance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard(title = "সাম্প্রতিক লেনদেন") {
            if (ui.recent.isEmpty()) {
                EmptyState(
                    "এখনও কোনো লেনদেন নেই",
                    "প্রথম আয় বা ব্যয় যোগ করলে এখানে পূর্ণ হিসাব দেখতে পাবেন।",
                    actionLabel = "আয় যোগ করুন",
                    onAction = onQuickAddIncome
                )
            } else {
                ui.recent.forEach { txn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTransaction(txn.id) }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(
                                txn.description.ifBlank { txn.type.name },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                Dates.relativeLabel(txn.dateMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            Money.format(txn.amountPaisa, digit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (txn.flow.name == "INFLOW") IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        if (ui.upcomingCount > 0 || ui.overdueCount > 0) {
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "আসন্ন ও মেয়াদোত্তীর্ণ") {
                Text("আসন্ন পরিশোধ: ${Money.toBengaliDigits(ui.upcomingCount.toString())} টি", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "মেয়াদ পেরিয়েছে: ${Money.toBengaliDigits(ui.overdueCount.toString())} টি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ui.overdueCount > 0) ExpenseRed else TextSecondary
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
