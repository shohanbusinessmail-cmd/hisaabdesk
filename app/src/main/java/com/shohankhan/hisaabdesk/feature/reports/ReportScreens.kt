package com.shohankhan.hisaabdesk.feature.reports

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.dao.CategorySumRow
import com.shohankhan.hisaabdesk.data.local.dao.MonthSumRow
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.DateField
import com.shohankhan.hisaabdesk.ui.components.EmptyState
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.KeyValueRow
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SoftDivider
import com.shohankhan.hisaabdesk.ui.components.StatCard
import com.shohankhan.hisaabdesk.ui.components.SuccessBanner
import com.shohankhan.hisaabdesk.ui.theme.BrandGreen
import com.shohankhan.hisaabdesk.ui.theme.BrandGreenLight
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.IncomeGreen
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary
import com.shohankhan.hisaabdesk.ui.theme.WarningAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportRange(val from: LocalDate, val to: LocalDate)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportViewModel(private val container: AppContainer) : ViewModel() {

    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    private val _range = MutableStateFlow(
        ReportRange(Dates.startOfMonth(LocalDate.now()), Dates.endOfMonth(LocalDate.now()))
    )
    val range: StateFlow<ReportRange> = _range.asStateFlow()

    fun setThisWeek() {
        _range.value = ReportRange(Dates.startOfWeek(LocalDate.now()), Dates.endOfWeek(LocalDate.now()))
    }

    fun setThisMonth() {
        _range.value = ReportRange(Dates.startOfMonth(LocalDate.now()), Dates.endOfMonth(LocalDate.now()))
    }

    fun setThisYear() {
        _range.value = ReportRange(Dates.startOfYear(LocalDate.now()), Dates.endOfYear(LocalDate.now()))
    }

    fun setCustom(from: LocalDate, to: LocalDate) {
        if (to >= from) _range.value = ReportRange(from, to)
    }

    val categories: StateFlow<List<CategoryEntity>> = container.categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val dayRange = _range.map { Dates.dayKeyRange(it.from, it.to) }

    val income: StateFlow<Long> = dayRange
        .map { r -> container.reportRepository.incomeBetween(r.first, r.last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val expense: StateFlow<Long> = dayRange
        .map { r -> container.reportRepository.expenseBetween(r.first, r.last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val byCategory: StateFlow<List<CategorySumRow>> = dayRange
        .flatMapLatest { r -> container.reportRepository.observeExpenseByCategory(r.first, r.last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthly: StateFlow<Pair<List<MonthSumRow>, List<MonthSumRow>>> = dayRange
        .flatMapLatest { r ->
            combine(
                container.reportRepository.observeIncomeByMonth(r.first, r.last),
                container.reportRepository.observeExpenseByMonth(r.first, r.last)
            ) { inc, exp -> inc to exp }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList<MonthSumRow>() to emptyList()
        )

    val accountRows: StateFlow<List<Pair<String, Long>>> = combine(
        container.accountRepository.observeAll(),
        container.accountRepository.observeBalances()
    ) { accounts, balances ->
        accounts.map { a -> a.name to (balances.firstOrNull { it.id == a.id }?.balance ?: 0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val summaryDao = container.database.summaryDao()
    val shopOutstanding = summaryDao.observeShopOutstanding()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val lentOutstanding = summaryDao.observeLendingOutstanding()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val borrowedOutstanding = summaryDao.observeBorrowingOutstanding()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val receivableRemaining = summaryDao.observeReceivableRemaining()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val payableRemaining = summaryDao.observePayableRemaining()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val ngoOutstanding = summaryDao.observeLoanPrincipalOutstanding("NGO")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val bankOutstanding = summaryDao.observeLoanPrincipalOutstanding("BANK")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun clearExportMessage() { _exportMessage.value = null }

    suspend fun exportCsv(context: Context, uri: Uri) {
        try {
            val r = Dates.dayKeyRange(_range.value.from, _range.value.to)
            val txns = container.transactionRepository.observeFiltered(
                fromDay = r.first, toDay = r.last, limit = 100000, offset = 0
            ).first()
            val sb = StringBuilder()
            sb.append('\uFEFF') // BOM so spreadsheet apps render Bengali text correctly
            sb.append("তারিখ,ধরন,পরিমাণ,বর্ণনা,রসিদ,পদ্ধতি,অবস্থা\n")
            txns.forEach { t ->
                val amount = String.format(java.util.Locale.US, "%.2f", t.amountPaisa / 100.0)
                fun esc(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
                sb.append(esc(Dates.format(t.dateMillis, Money.DigitStyle.ENGLISH, iso = true))).append(',')
                sb.append(esc(t.type.name)).append(',')
                sb.append(amount).append(',')
                sb.append(esc(t.description)).append(',')
                sb.append(esc(t.receiptNo)).append(',')
                sb.append(esc(t.paymentMethod.name)).append(',')
                sb.append(esc(t.status.name)).append('\n')
            }
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("cannot open file")
            _exportMessage.value = "রিপোর্ট CSV ফাইল তৈরি হয়েছে"
        } catch (e: Exception) {
            _exportMessage.value = "রপ্তানি ব্যর্থ হয়েছে"
        }
    }
}

@Composable
fun ReportScreen(onBack: () -> Unit) {
    val vm: ReportViewModel = appViewModel { ReportViewModel(it.container) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            kotlinx.coroutines.MainScope().launch { vm.exportCsv(context, uri) }
        }
    }
    val digit by vm.digitStyle.collectAsState()
    val range by vm.range.collectAsState()
    val income by vm.income.collectAsState()
    val expense by vm.expense.collectAsState()
    val byCategory by vm.byCategory.collectAsState()
    val monthly by vm.monthly.collectAsState()
    val categories by vm.categories.collectAsState()
    val accountRows by vm.accountRows.collectAsState()
    val shop by vm.shopOutstanding.collectAsState()
    val lent by vm.lentOutstanding.collectAsState()
    val borrowed by vm.borrowedOutstanding.collectAsState()
    val receivable by vm.receivableRemaining.collectAsState()
    val payable by vm.payableRemaining.collectAsState()
    val ngo by vm.ngoOutstanding.collectAsState()
    val bank by vm.bankOutstanding.collectAsState()
    val exportMessage by vm.exportMessage.collectAsState()
    var showCustom by remember { mutableStateOf(false) }
    var customFrom by remember { mutableStateOf(range.from) }
    var customTo by remember { mutableStateOf(range.to) }

    HisaabScaffold(title = "রিপোর্ট", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = false, onClick = { vm.setThisWeek() }, label = { Text("সপ্তাহ") })
            FilterChip(selected = false, onClick = { vm.setThisMonth() }, label = { Text("মাস") })
            FilterChip(selected = false, onClick = { vm.setThisYear() }, label = { Text("বছর") })
            FilterChip(selected = showCustom, onClick = { showCustom = !showCustom }, label = { Text("নির্দিষ্ট") })
        }
        if (showCustom) {
            Spacer(Modifier.height(8.dp))
            DateField("শুরু", customFrom, { customFrom = it })
            Spacer(Modifier.height(8.dp))
            DateField("শেষ", customTo, { customTo = it })
            TextButton(onClick = { vm.setCustom(customFrom, customTo); showCustom = false }) { Text("প্রয়োগ") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${Dates.format(range.from, digit)} — ${Dates.format(range.to, digit)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        exportMessage?.let { SuccessBanner(it); Spacer(Modifier.height(6.dp)) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    StatCard("আয়", income, digit, IncomeGreen)
                    Spacer(Modifier.width(10.dp))
                    StatCard("ব্যয়", expense, digit, ExpenseRed)
                }
                Spacer(Modifier.height(10.dp))
                SectionCard(title = "নগদ প্রবাহ") {
                    KeyValueRow("আয়", Money.format(income, digit), IncomeGreen)
                    KeyValueRow("ব্যয়", Money.format(expense, digit), ExpenseRed)
                    KeyValueRow("নিট", Money.format(income - expense, digit), if (income >= expense) IncomeGreen else ExpenseRed)
                }
                Spacer(Modifier.height(10.dp))
                SectionCard(title = "মাসিক আয়-ব্যয়") {
                    if (monthly.first.isEmpty() && monthly.second.isEmpty()) {
                        Text("এই সময়ে কোনো তথ্য নেই — তাই চার্ট দেখানো হচ্ছে না", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    } else {
                        val allMonths = (monthly.first.map { it.monthKey } + monthly.second.map { it.monthKey })
                            .distinct().sorted()
                        val maxValue = ((monthly.first.maxOfOrNull { it.total } ?: 0L)
                            .coerceAtLeast(monthly.second.maxOfOrNull { it.total } ?: 0L)).coerceAtLeast(1L)
                        allMonths.forEach { monthKey ->
                            val inc = monthly.first.firstOrNull { it.monthKey == monthKey }?.total ?: 0L
                            val exp = monthly.second.firstOrNull { it.monthKey == monthKey }?.total ?: 0L
                            val label = Dates.format(
                                LocalDate.of(monthKey / 100, monthKey % 100, 1), digit, iso = false
                            ).substring(3) // MM/yyyy
                            Text("মাস $label", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            BarRow("আয়", inc, maxValue, IncomeGreen, digit)
                            BarRow("ব্যয়", exp, maxValue, ExpenseRed, digit)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionCard(title = "ক্যাটাগরি বিভাজন (ব্যয়)") {
                    if (byCategory.isEmpty()) {
                        Text("কোনো ব্যয় নেই", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    byCategory.forEach { row ->
                        val name = categories.firstOrNull { it.id == row.categoryId }?.name ?: "অন্যান্য"
                        val maxCat = byCategory.maxOfOrNull { it.total } ?: 1L
                        BarRow(name, row.total, maxCat, BrandGreen, digit)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionCard(title = "সম্পদ ও দায়") {
                    val assets = accountRows.sumOf { it.second } + lent + receivable
                    val liabilities = borrowed + payable + shop + ngo + bank
                    KeyValueRow("মোট সম্পদ", Money.format(assets, digit), IncomeGreen)
                    KeyValueRow("মোট দায়", Money.format(liabilities, digit), ExpenseRed)
                    KeyValueRow("নিট সম্পদ", Money.format(assets - liabilities, digit), if (assets >= liabilities) IncomeGreen else ExpenseRed)
                    SoftDivider()
                    accountRows.forEach { (name, balance) -> KeyValueRow(name, Money.format(balance, digit)) }
                    KeyValueRow("ধার দেওয়া বাকি", Money.format(lent, digit))
                    KeyValueRow("পাওনা বাকি", Money.format(receivable, digit))
                    KeyValueRow("ধার নেওয়া বাকি", Money.format(borrowed, digit))
                    KeyValueRow("দেনা বাকি", Money.format(payable, digit))
                    KeyValueRow("দোকানের বাকি", Money.format(shop, digit))
                    KeyValueRow("এনজিও ঋণ বাকি", Money.format(ngo, digit))
                    KeyValueRow("ব্যাংক ঋণ বাকি", Money.format(bank, digit))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { exportLauncher.launch("hisaabdesk-report.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("CSV রপ্তানি") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BarRow(label: String, value: Long, max: Long, color: Color, digit: Money.DigitStyle) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp), maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color(0xFFEDF2EE))
        ) {
            val fraction = if (max > 0) (value.toFloat() / max).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(Money.format(value, digit), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(92.dp))
    }
}
