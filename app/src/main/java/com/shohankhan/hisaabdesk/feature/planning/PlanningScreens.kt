package com.shohankhan.hisaabdesk.feature.planning

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetPeriod
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.data.repository.PlanningRepository
import com.shohankhan.hisaabdesk.feature.transactions.txnTypeLabel
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.DateField
import com.shohankhan.hisaabdesk.ui.components.DropdownField
import com.shohankhan.hisaabdesk.ui.components.EmptyState
import com.shohankhan.hisaabdesk.ui.components.ErrorBanner
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.KeyValueRow
import com.shohankhan.hisaabdesk.ui.components.LabeledField
import com.shohankhan.hisaabdesk.ui.components.MoneyField
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SoftDivider
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.IncomeGreen
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary
import com.shohankhan.hisaabdesk.ui.theme.WarningAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

fun budgetPeriodLabel(p: BudgetPeriod): String = when (p) {
    BudgetPeriod.DAILY -> "দৈনিক"
    BudgetPeriod.WEEKLY -> "সাপ্তাহিক"
    BudgetPeriod.MONTHLY -> "মাসিক"
    BudgetPeriod.CUSTOM -> "নির্দিষ্ট সময়"
}

data class BudgetRow(val budget: BudgetEntity, val spent: Long, val remaining: Long, val percent: Int, val over: Boolean)

class PlanningViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)
    val categories = container.categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val accounts = container.accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _budgetRows = MutableStateFlow<List<BudgetRow>>(emptyList())
    val budgetRows: StateFlow<List<BudgetRow>> = _budgetRows.asStateFlow()

    val recurring = container.planningRepository.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getBudget(id: Long) = container.planningRepository.getBudget(id)
    suspend fun getRecurring(id: Long) = container.planningRepository.getRecurring(id)

    fun refreshBudgets() {
        viewModelScope.launch {
            val budgets = container.planningRepository.observeBudgets().first()
            val rows = budgets.map { b ->
                val usage = container.planningRepository.usage(b)
                BudgetRow(b, usage.spentPaisa, usage.remainingPaisa, usage.percentUsed, usage.overBudget)
            }
            _budgetRows.value = rows
        }
    }

    fun saveBudget(
        id: Long,
        name: String,
        limit: Long,
        period: BudgetPeriod,
        startDate: LocalDate,
        endDate: LocalDate?,
        categoryId: Long?,
        accountId: Long?,
        warnPercent: Int,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) { onResult("নাম দিন"); return@launch }
                if (limit <= 0) { onResult("সীমা দিন"); return@launch }
                if (period == BudgetPeriod.CUSTOM && endDate == null) { onResult("শেষ তারিখ দিন"); return@launch }
                val now = System.currentTimeMillis()
                container.planningRepository.saveBudget(
                    BudgetEntity(
                        id = id,
                        name = name.trim(),
                        limitPaisa = limit,
                        period = period,
                        startDateMillis = Dates.millisOf(startDate),
                        endDateMillis = endDate?.let { Dates.millisOf(it) },
                        categoryId = categoryId,
                        accountId = accountId,
                        warnThresholdPercent = warnPercent,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                refreshBudgets()
                onResult(null)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun saveRecurring(
        id: Long,
        name: String,
        txnType: TxnType,
        amount: Long,
        categoryId: Long?,
        accountId: Long?,
        frequency: String,
        intervalDays: Int,
        startDate: LocalDate,
        remindDays: Int,
        notes: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) { onResult("নাম দিন"); return@launch }
                if (amount <= 0) { onResult("পরিমাণ দিন"); return@launch }
                if (accountId == null) { onResult("অ্যাকাউন্ট নির্বাচন করুন"); return@launch }
                val now = System.currentTimeMillis()
                val existing = if (id != 0L) container.planningRepository.getRecurring(id) else null
                container.planningRepository.saveRecurring(
                    RecurringItemEntity(
                        id = id,
                        name = name.trim(),
                        txnType = txnType,
                        amountPaisa = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        frequency = frequency,
                        intervalDays = intervalDays,
                        startDateMillis = Dates.millisOf(startDate),
                        nextDateMillis = existing?.nextDateMillis ?: Dates.millisOf(startDate),
                        nextDayKey = existing?.nextDayKey ?: Dates.dayKey(startDate),
                        remindDaysBefore = remindDays,
                        notes = notes,
                        isPaused = existing?.isPaused ?: false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                onResult(null)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun markPaid(item: RecurringItemEntity, accountId: Long?, amount: Long?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.planningRepository.markPaid(item.id, item.nextDateMillis, accountId, amount)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun setPaused(id: Long, paused: Boolean) {
        viewModelScope.launch { container.planningRepository.setRecurringPaused(id, paused) }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { container.planningRepository.setRecurringArchived(id, archived) }
    }
}

@Composable
fun BudgetListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val rows by vm.budgetRows.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    LaunchedEffect(Unit) { vm.refreshBudgets() }

    HisaabScaffold(title = "বাজেট", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            EmptyState("কোনো বাজেট নেই", "ব্যয় নিয়ন্ত্রণে বাজেট তৈরি করুন", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            rows.forEach { row ->
                SectionCard(modifier = Modifier.padding(vertical = 5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.budget.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            Money.format(row.spent, digit) + " / " + Money.format(row.budget.limitPaisa, digit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (row.over) ExpenseRed else TextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (row.percent.coerceAtMost(100)) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (row.over) ExpenseRed else IncomeGreen
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(budgetPeriodLabel(row.budget.period), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            if (row.over) "সীমা অতিক্রম" else "বাকি " + Money.format(row.remaining, digit),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.over) ExpenseRed else IncomeGreen
                        )
                    }
                    TextButton(onClick = { onOpen(row.budget.id) }) { Text("বিস্তারিত") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("বাজেট যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun BudgetFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var name by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var warnPercent by remember { mutableStateOf("80") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getBudget(id)?.let {
                name = it.name
                limit = Money.format(it.limitPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                period = it.period
                startDate = Dates.localDateOf(it.startDateMillis)
                endDate = it.endDateMillis?.let { d -> Dates.localDateOf(d) }
                categoryId = it.categoryId
                accountId = it.accountId
                warnPercent = it.warnThresholdPercent.toString()
            }
        }
    }

    HisaabScaffold(title = if (id == 0L) "বাজেট যোগ করুন" else "বাজেট সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("বাজেটের নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        MoneyField("সীমা", limit, { limit = it }, digit)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "সময়কাল",
            options = BudgetPeriod.entries,
            selected = period,
            optionLabel = { budgetPeriodLabel(it) },
            onSelected = { period = it }
        )
        Spacer(Modifier.height(8.dp))
        DateField("শুরুর তারিখ", startDate, { startDate = it })
        if (period == BudgetPeriod.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            DateField("শেষ তারিখ", endDate, { endDate = it })
        }
        Spacer(Modifier.height(8.dp))
        val expenseCats = categories.filter { it.kind.name == "EXPENSE" || it.kind.name == "SHOPPING" }
        DropdownField(
            label = "ক্যাটাগরি (ঐচ্ছিক)",
            options = expenseCats,
            selected = expenseCats.firstOrNull { it.id == categoryId },
            optionLabel = { it.name },
            onSelected = { categoryId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "অ্যাকাউন্ট (ঐচ্ছিক)",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("সতর্কতা সীমা (%)", warnPercent, { warnPercent = it })
        Spacer(Modifier.height(8.dp))
        Text(
            "বাজেটে শুধু ব্যয় গণনা হয় — স্থানান্তর, সঞ্চয় জমা বা ঋণের আসল পরিশোধ ব্যয় হিসেবে ধরা হয় না।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(limit)
                val warn = warnPercent.toIntOrNull()?.coerceIn(1, 100) ?: 80
                if (paisa == null) { error = "সঠিক সীমা লিখুন"; return@Button }
                vm.saveBudget(id, name, paisa, period, startDate, endDate, categoryId, accountId, warn) { err ->
                    if (err == null) onSaved() else error = err
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun BudgetDetailScreen(id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val digit by vm.digitStyle.collectAsState()
    var row by remember { mutableStateOf<BudgetRow?>(null) }
    LaunchedEffect(id) {
        vm.refreshBudgets()
        row = vm.budgetRows.value.firstOrNull { it.budget.id == id }
    }
    HisaabScaffold(title = "বাজেটের বিস্তারিত", onBack = onBack) {
        val r = row ?: run { EmptyState("বাজেট পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("নাম", r.budget.name)
            KeyValueRow("সময়কাল", budgetPeriodLabel(r.budget.period))
            KeyValueRow("সীমা", Money.format(r.budget.limitPaisa, digit))
            KeyValueRow("খরচ", Money.format(r.spent, digit), if (r.over) ExpenseRed else MaterialTheme.colorScheme.onSurface)
            KeyValueRow("বাকি", Money.format(r.remaining, digit), if (r.remaining < 0) ExpenseRed else IncomeGreen)
            KeyValueRow("ব্যবহৃত", "${Money.toBengaliDigits(r.percent.toString())}%")
            KeyValueRow("সতর্কতা সীমা", "${Money.toBengaliDigits(r.budget.warnThresholdPercent.toString())}%")
            if (r.over) KeyValueRow("অবস্থা", "সীমা অতিক্রম", ExpenseRed)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun BillListScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit, onCalendar: () -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val items by vm.recurring.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val now = System.currentTimeMillis()
    var error by remember { mutableStateOf<String?>(null) }

    HisaabScaffold(title = "বিল ও নিয়মিত লেনদেন", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("যোগ করুন") }
            TextButton(onClick = onCalendar, modifier = Modifier.weight(1f)) { Text("ক্যালেন্ডার") }
        }
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            EmptyState("কোনো বিল/নিয়মিত লেনদেন নেই", "বাড়ি ভাড়া, বিদ্যুৎ বিল ইত্যাদি যোগ করুন", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.id }) { item ->
                    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    txnTypeLabel(item.txnType) + " · " +
                                        if (item.frequency == "WEEKLY") "সাপ্তাহিক" else if (item.frequency == "CUSTOM_DAYS") "নির্দিষ্ট দিন পরপর" else "মাসিক",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(Money.format(item.amountPaisa, digit), color = if (item.txnType == TxnType.EXPENSE) ExpenseRed else IncomeGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "পরবর্তী: " + Dates.format(item.nextDateMillis, digit) +
                                if (item.isPaused) " · স্থগিত" else
                                    if (item.nextDateMillis < now) " · মেয়াদ পেরিয়েছে" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.nextDateMillis < now && !item.isPaused) ExpenseRed else TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                vm.markPaid(item, item.accountId, null) { err -> if (err != null) error = err }
                            }) { Text("পরিশোধ রেকর্ড") }
                            TextButton(onClick = { vm.setPaused(item.id, !item.isPaused) }) {
                                Text(if (item.isPaused) "চালু" else "স্থগিত")
                            }
                            TextButton(onClick = { onEdit(item.id) }) { Text("সম্পাদনা") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "স্বয়ংক্রিয়ভাবে কোনো লেনদেন তৈরি হয় না — আপনি নিশ্চিত করলেই পরিশোধ রেকর্ড হয়।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun BillFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var name by remember { mutableStateOf("") }
    var txnType by remember { mutableStateOf(TxnType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var intervalDays by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var remindDays by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getRecurring(id)?.let {
                name = it.name
                txnType = it.txnType
                amount = Money.format(it.amountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                categoryId = it.categoryId
                accountId = it.accountId
                frequency = it.frequency
                intervalDays = if (it.intervalDays > 0) it.intervalDays.toString() else ""
                startDate = Dates.localDateOf(it.startDateMillis)
                remindDays = it.remindDaysBefore.toString()
                notes = it.notes
            }
        }
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    HisaabScaffold(title = if (id == 0L) "বিল/নিয়মিত লেনদেন" else "সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "ধরন",
            options = listOf(TxnType.EXPENSE, TxnType.INCOME),
            selected = txnType,
            optionLabel = { txnTypeLabel(it) },
            onSelected = { txnType = it }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("পরিমাণ", amount, { amount = it }, digit)
        Spacer(Modifier.height(8.dp))
        val cats = categories.filter {
            if (txnType == TxnType.INCOME) it.kind.name == "INCOME" else it.kind.name == "EXPENSE" || it.kind.name == "SHOPPING"
        }
        DropdownField(
            label = "ক্যাটাগরি (ঐচ্ছিক)",
            options = cats,
            selected = cats.firstOrNull { it.id == categoryId },
            optionLabel = { it.name },
            onSelected = { categoryId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "অ্যাকাউন্ট",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পুনরাবৃত্তি",
            options = listOf("WEEKLY", "MONTHLY", "CUSTOM_DAYS"),
            selected = frequency,
            optionLabel = { if (it == "WEEKLY") "সাপ্তাহিক" else if (it == "CUSTOM_DAYS") "নির্দিষ্ট দিন পরপর" else "মাসিক" },
            onSelected = { frequency = it }
        )
        if (frequency == "CUSTOM_DAYS") {
            Spacer(Modifier.height(8.dp))
            LabeledField("ব্যবধান (দিন)", intervalDays, { intervalDays = it })
        }
        Spacer(Modifier.height(8.dp))
        DateField("শুরুর/পরবর্তী তারিখ", startDate, { startDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("কত দিন আগে মনে করাবে", remindDays, { remindDays = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                if (paisa == null) { error = "সঠিক পরিমাণ লিখুন"; return@Button }
                val interval = intervalDays.toIntOrNull() ?: 0
                if (frequency == "CUSTOM_DAYS" && interval <= 0) { error = "ব্যবধান দিন"; return@Button }
                vm.saveRecurring(
                    id = id,
                    name = name,
                    txnType = txnType,
                    amount = paisa,
                    categoryId = categoryId,
                    accountId = accountId,
                    frequency = frequency,
                    intervalDays = interval,
                    startDate = startDate,
                    remindDays = remindDays.toIntOrNull() ?: -1,
                    notes = notes
                ) { err -> if (err == null) onSaved() else error = err }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PaymentCalendarScreen(onBack: () -> Unit, onOpenBills: () -> Unit) {
    val vm: PlanningViewModel = appViewModel { PlanningViewModel(it.container) }
    val items by vm.recurring.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val today = LocalDate.now()
    val upcoming = items.sortedBy { it.nextDateMillis }

    HisaabScaffold(title = "পেমেন্ট ক্যালেন্ডার", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("আজ", Dates.format(today, digit))
            KeyValueRow("আসন্ন বিল", Money.toBengaliDigits(upcoming.count { it.nextDateMillis >= System.currentTimeMillis() }.toString()))
            KeyValueRow("মেয়াদ পেরিয়েছে", Money.toBengaliDigits(upcoming.count { it.nextDateMillis < System.currentTimeMillis() && !it.isPaused }.toString()), ExpenseRed)
        }
        Spacer(Modifier.height(10.dp))
        if (upcoming.isEmpty()) {
            EmptyState("কোনো নির্ধারিত বিল নেই", actionLabel = "বিল যোগ করুন", onAction = onOpenBills)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(upcoming, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                Dates.format(item.nextDateMillis, digit) + if (item.isPaused) " · স্থগিত" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.nextDateMillis < System.currentTimeMillis() && !item.isPaused) ExpenseRed else TextSecondary
                            )
                        }
                        Text(Money.format(item.amountPaisa, digit))
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
