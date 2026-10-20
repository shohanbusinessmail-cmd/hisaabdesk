package com.shohankhan.hisaabdesk.feature.savings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.GoalEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.feature.transactions.methodLabel
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SavingsAccountRow(val account: AccountEntity, val balance: Long)

class SavingsViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    val allAccounts = container.accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savingsAccounts: StateFlow<List<SavingsAccountRow>> = combine(
        container.accountRepository.observeActive(),
        container.accountRepository.observeBalances()
    ) { accounts, balances ->
        accounts.filter { it.type == AccountType.SAVINGS || it.type == AccountType.OTHER }
            .map { a -> SavingsAccountRow(a, balances.firstOrNull { it.id == a.id }?.balance ?: 0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeAccountTxns(accountId: Long) =
        container.transactionRepository.observeFiltered(accountId = accountId, limit = 50, offset = 0)

    suspend fun getAccount(id: Long) = container.accountRepository.get(id)

    fun observeGoals() = container.planningRepository.observeGoals()
    fun observeGoal(id: Long) = container.planningRepository.observeGoal(id)
    fun observeGoalContributions(id: Long) = container.planningRepository.observeGoalContributions(id)
    suspend fun getGoal(id: Long) = container.planningRepository.getGoal(id)

    fun saveSavingsAccount(
        id: Long,
        name: String,
        institution: String,
        accountRef: String,
        openingPaisa: Long,
        notes: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val existing = if (id != 0L) container.accountRepository.get(id) else null
                val composedNotes = listOfNotNull(
                    institution.takeIf { it.isNotBlank() }?.let { "প্রতিষ্ঠান: $it" },
                    accountRef.takeIf { it.isNotBlank() }?.let { "হিসাব/স্কিম: $it" },
                    notes.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                container.accountRepository.save(
                    (existing ?: AccountEntity(
                        name = name,
                        type = AccountType.SAVINGS,
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        name = name.trim(),
                        type = AccountType.SAVINGS,
                        openingBalancePaisa = openingPaisa,
                        notes = composedNotes,
                        updatedAt = now
                    )
                )
                onResult(null)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun savingsMovement(
        type: TxnType,
        amount: Long,
        dateMillis: Long,
        savingsAccountId: Long,
        counterAccountId: Long?,
        method: PaymentMethod,
        note: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val (accountId, counterId, flow) = when (type) {
                    TxnType.SAVINGS_DEPOSIT -> Triple(counterAccountId ?: error("source"), savingsAccountId, TxnFlow.TRANSFER)
                    TxnType.SAVINGS_WITHDRAWAL -> Triple(savingsAccountId, counterAccountId ?: error("destination"), TxnFlow.TRANSFER)
                    TxnType.SAVINGS_INTEREST -> Triple(savingsAccountId, null, TxnFlow.INFLOW)
                    else -> error("unsupported")
                }
                container.transactionRepository.insert(
                    TxnEntity(
                        type = type,
                        flow = flow,
                        amountPaisa = amount,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        accountId = accountId,
                        counterAccountId = counterId,
                        paymentMethod = method,
                        description = when (type) {
                            TxnType.SAVINGS_DEPOSIT -> "সঞ্চয়ে জমা"
                            TxnType.SAVINGS_WITHDRAWAL -> "সঞ্চয় থেকে উত্তোলন"
                            else -> "সঞ্চয়ের মুনাফা/সুদ"
                        },
                        note = note,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun saveGoal(goal: GoalEntity, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (goal.name.isBlank()) { onResult("লক্ষ্যের নাম দিন"); return@launch }
                if (goal.targetPaisa <= 0) { onResult("লক্ষ্যমাত্রা দিন"); return@launch }
                container.planningRepository.saveGoal(goal)
                onResult(null)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun addGoalContribution(goalId: Long, amount: Long, fromAccountId: Long?, dateMillis: Long, note: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (fromAccountId == null) { onResult("অ্যাকাউন্ট নির্বাচন করুন"); return@launch }
                container.planningRepository.addContribution(goalId, amount, fromAccountId, dateMillis, note)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun archiveGoal(id: Long, archived: Boolean) {
        viewModelScope.launch { container.planningRepository.setGoalArchived(id, archived) }
    }
}

@Composable
fun SavingsListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit, onOpenGoal: (Long) -> Unit, onAddGoal: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val rows by vm.savingsAccounts.collectAsState()
    val goals by vm.observeGoals().collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()
    val total = rows.sumOf { it.balance }

    HisaabScaffold(title = "সঞ্চয়", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("মোট সঞ্চয়", Money.format(total, digit), IncomeGreen)
            KeyValueRow("সঞ্চয় অ্যাকাউন্ট", Money.toBengaliDigits(rows.size.toString()))
        }
        Spacer(Modifier.height(10.dp))
        Text("সঞ্চয় অ্যাকাউন্ট", style = MaterialTheme.typography.titleSmall)
        if (rows.isEmpty()) {
            EmptyState("কোনো সঞ্চয় অ্যাকাউন্ট নেই", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(row.account.id) }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.account.name, style = MaterialTheme.typography.bodyLarge)
                        if (row.account.notes.isNotBlank()) {
                            Text(row.account.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                        }
                    }
                    Text(Money.format(row.balance, digit), color = IncomeGreen)
                }
                SoftDivider()
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("সঞ্চয় অ্যাকাউন্ট যোগ করুন") }
        Spacer(Modifier.height(16.dp))
        Text("সঞ্চয়ের লক্ষ্য", style = MaterialTheme.typography.titleSmall)
        goals.forEach { goal ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenGoal(goal.id) }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(goal.name, style = MaterialTheme.typography.bodyLarge)
                Text(Money.format(goal.targetPaisa, digit), color = WarningAmber)
            }
            SoftDivider()
        }
        TextButtonRow(label = "লক্ষ্য যোগ করুন", onClick = onAddGoal)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TextButtonRow(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
fun SavingsFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    var name by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var accountRef by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L && !loaded) {
            val acc = vm.getAccount(id)
            if (acc != null) {
                loaded = true
                name = acc.name
                opening = Money.format(acc.openingBalancePaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                notes = acc.notes
            }
        }
    }

    HisaabScaffold(title = if (id == 0L) "সঞ্চয় অ্যাকাউন্ট" else "সঞ্চয় সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("অ্যাকাউন্টের নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("প্রতিষ্ঠান (ঐচ্ছিক)", institution, { institution = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("অ্যাকাউন্ট/স্কিম রেফারেন্স (ঐচ্ছিক)", accountRef, { accountRef = it })
        Spacer(Modifier.height(8.dp))
        MoneyField("শুরুর ব্যালেন্স", opening, { opening = it }, Money.DigitStyle.BENGALI)
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isBlank()) { error = "নাম দিন"; return@Button }
                val paisa = if (opening.isBlank()) 0L else Money.parsePaisa(opening)
                if (paisa == null) { error = "সঠিক পরিমাণ লিখুন"; return@Button }
                vm.saveSavingsAccount(id, name, institution, accountRef, paisa, notes) { err ->
                    if (err == null) onSaved() else error = err
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SavingsDetailScreen(id: Long, onBack: () -> Unit, onTransaction: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val rows by vm.savingsAccounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val row = rows.firstOrNull { it.account.id == id }
    val txns by vm.observeAccountTxns(id).collectAsState(initial = emptyList())

    HisaabScaffold(title = row?.account?.name ?: "সঞ্চয়", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (row == null) {
            EmptyState("অ্যাকাউন্ট পাওয়া যায়নি")
            return@HisaabScaffold
        }
        SectionCard {
            KeyValueRow("বর্তমান ব্যালেন্স", Money.format(row.balance, digit), IncomeGreen)
            KeyValueRow("শুরুর ব্যালেন্স", Money.format(row.account.openingBalancePaisa, digit))
            if (row.account.notes.isNotBlank()) KeyValueRow("নোট", row.account.notes)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "সঞ্চয়ে জমা ব্যয় নয়, উত্তোলন আয় নয় — এগুলো শুধু টাকার স্থানান্তর। সঞ্চয়ের মুনাফা আলাদাভাবে আয় হিসেবে যোগ হয়।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onTransaction, modifier = Modifier.fillMaxWidth()) { Text("জমা / উত্তোলন / মুনাফা") }
        Spacer(Modifier.height(14.dp))
        Text("লেনদেন", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        if (txns.isEmpty()) {
            Text("কোনো লেনদেন নেই", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        txns.forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(txn.description.ifBlank { "লেনদেন" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(Dates.format(txn.dateMillis, digit), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    Money.format(txn.amountPaisa, digit),
                    color = if (txn.flow == TxnFlow.INFLOW) IncomeGreen else WarningAmber
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SavingsTransactionScreen(accountId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val accounts by vm.allAccounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var type by remember { mutableStateOf(TxnType.SAVINGS_DEPOSIT) }
    var amount by remember { mutableStateOf("") }
    var counter by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (counter == null) counter = accounts.firstOrNull { it.id != accountId }?.id
    }

    HisaabScaffold(title = "সঞ্চয় লেনদেন", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = type == TxnType.SAVINGS_DEPOSIT, onClick = { type = TxnType.SAVINGS_DEPOSIT }, label = { Text("জমা") })
            FilterChip(selected = type == TxnType.SAVINGS_WITHDRAWAL, onClick = { type = TxnType.SAVINGS_WITHDRAWAL }, label = { Text("উত্তোলন") })
            FilterChip(selected = type == TxnType.SAVINGS_INTEREST, onClick = { type = TxnType.SAVINGS_INTEREST }, label = { Text("মুনাফা/সুদ") })
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when (type) {
                TxnType.SAVINGS_DEPOSIT -> "কোন অ্যাকাউন্ট থেকে টাকা সঞ্চয়ে যাবে তা নির্বাচন করুন। এটি ব্যয় হিসেবে গণনা হবে না।"
                TxnType.SAVINGS_WITHDRAWAL -> "সঞ্চয় থেকে টাকা কোন অ্যাকাউন্টে যাবে তা নির্বাচন করুন। এটি আয় হিসেবে গণনা হবে না।"
                else -> "সঞ্চয়ের মুনাফা/সুদ এই সঞ্চয় অ্যাকাউন্টে আয় হিসেবে যোগ হবে।"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("পরিমাণ", amount, { amount = it }, digit)
        if (type != TxnType.SAVINGS_INTEREST) {
            Spacer(Modifier.height(8.dp))
            DropdownField(
                label = if (type == TxnType.SAVINGS_DEPOSIT) "যে অ্যাকাউন্ট থেকে" else "যে অ্যাকাউন্টে যাবে",
                options = accounts.filter { it.id != accountId },
                selected = accounts.firstOrNull { it.id == counter },
                optionLabel = { it.name },
                onSelected = { counter = it.id }
            )
        }
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", note, { note = it })
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    type != TxnType.SAVINGS_INTEREST && counter == null -> error = "অ্যাকাউন্ট নির্বাচন করুন"
                    else -> {
                        error = null
                        vm.savingsMovement(type, paisa, Dates.millisOf(date), accountId, counter, method, note) { err ->
                            if (err == null) onDone() else error = err
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun GoalFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val accounts by vm.savingsAccounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var linkedAccount by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getGoal(id)?.let {
                name = it.name
                target = Money.format(it.targetPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                startDate = Dates.localDateOf(it.startDateMillis)
                targetDate = it.targetDateMillis?.let { d -> Dates.localDateOf(d) }
                linkedAccount = it.linkedAccountId
                notes = it.notes
            }
        }
    }

    HisaabScaffold(title = if (id == 0L) "লক্ষ্য যোগ করুন" else "লক্ষ্য সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("লক্ষ্যের নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        MoneyField("লক্ষ্যমাত্রা", target, { target = it }, digit)
        Spacer(Modifier.height(8.dp))
        DateField("শুরুর তারিখ", startDate, { startDate = it })
        Spacer(Modifier.height(8.dp))
        DateField("লক্ষ্য তারিখ (ঐচ্ছিক)", targetDate, { targetDate = it })
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "সংযুক্ত সঞ্চয় অ্যাকাউন্ট",
            options = accounts.map { it.account },
            selected = accounts.map { it.account }.firstOrNull { it.id == linkedAccount },
            optionLabel = { it.name },
            onSelected = { linkedAccount = it.id }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(target)
                if (name.isBlank()) { error = "নাম দিন"; return@Button }
                if (paisa == null || paisa <= 0) { error = "সঠিক লক্ষ্যমাত্রা লিখুন"; return@Button }
                error = null
                val now = System.currentTimeMillis()
                vm.saveGoal(
                    GoalEntity(
                        id = id,
                        name = name.trim(),
                        targetPaisa = paisa,
                        startDateMillis = Dates.millisOf(startDate),
                        dayKey = Dates.dayKey(startDate),
                        targetDateMillis = targetDate?.let { Dates.millisOf(it) },
                        linkedAccountId = linkedAccount,
                        notes = notes,
                        createdAt = now,
                        updatedAt = now
                    )
                ) { err -> if (err == null) onSaved() else error = err }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun GoalDetailScreen(id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val goal by vm.observeGoal(id).collectAsState(initial = null)
    val contributed by vm.observeGoalContributions(id).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()
    val accounts by vm.allAccounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    HisaabScaffold(title = goal?.name ?: "লক্ষ্য", onBack = onBack) {
        val g = goal ?: run { EmptyState("লক্ষ্য পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        val percent = if (g.targetPaisa > 0) ((contributed * 100) / g.targetPaisa).coerceIn(0, 100).toInt() else 0
        SectionCard {
            KeyValueRow("লক্ষ্যমাত্রা", Money.format(g.targetPaisa, digit))
            KeyValueRow("অবদান", Money.format(contributed, digit), IncomeGreen)
            KeyValueRow("বাকি", Money.format((g.targetPaisa - contributed).coerceAtLeast(0), digit))
            KeyValueRow("অগ্রগতি", "${Money.toBengaliDigits(percent.toString())}%")
            if (g.targetDateMillis != null) KeyValueRow("লক্ষ্য তারিখ", Dates.format(g.targetDateMillis!!, digit))
            if (g.notes.isNotBlank()) KeyValueRow("নোট", g.notes)
            KeyValueRow("অবস্থা", if (contributed >= g.targetPaisa) "অর্জিত" else "চলমান")
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        if (g.linkedAccountId == null) {
            Text(
                "অবদান যোগ করতে লক্ষ্যের সাথে একটি সঞ্চয় অ্যাকাউন্ট যুক্ত করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber
            )
        }
        Button(onClick = { showAdd = true }, enabled = g.linkedAccountId != null, modifier = Modifier.fillMaxWidth()) {
            Text("অবদান যোগ করুন")
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
        Spacer(Modifier.height(24.dp))
    }

    if (showAdd) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("লক্ষ্যে অবদান") },
            text = {
                Column {
                    error?.let { Text(it, color = ExpenseRed) }
                    MoneyField("পরিমাণ", amount, { amount = it }, digit)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = "কোন অ্যাকাউন্ট থেকে",
                        options = accounts,
                        selected = accounts.firstOrNull { it.id == fromAccount },
                        optionLabel = { it.name },
                        onSelected = { fromAccount = it.id }
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val paisa = Money.parsePaisa(amount)
                    if (paisa == null || paisa <= 0) { error = "সঠিক পরিমাণ লিখুন"; return@TextButton }
                    vm.addGoalContribution(id, paisa, fromAccount, System.currentTimeMillis(), "") { err ->
                        if (err == null) {
                            showAdd = false
                            amount = ""
                            error = null
                        } else error = err
                    }
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAdd = false }) { Text("বাতিল") }
            }
        )
    }
}


@Composable
fun GoalListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: SavingsViewModel = appViewModel { SavingsViewModel(it.container) }
    val goals by vm.observeGoals().collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()
    var contributions by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }

    LaunchedEffect(goals) {
        val map = mutableMapOf<Long, Long>()
        goals.forEach { goal -> map[goal.id] = vm.observeGoalContributions(goal.id).first() }
        contributions = map
    }

    HisaabScaffold(title = "আর্থিক লক্ষ্য", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (goals.isEmpty()) {
            EmptyState("কোনো লক্ষ্য নেই", "জরুরি তহবিল, শিক্ষা বা অন্য লক্ষ্য ঠিক করুন", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(goals, key = { it.id }) { goal ->
                    val contributed = contributions[goal.id] ?: 0L
                    val percent = if (goal.targetPaisa > 0) ((contributed * 100) / goal.targetPaisa).coerceIn(0, 100).toInt() else 0
                    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(goal.id) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(goal.name, style = MaterialTheme.typography.titleSmall)
                            Text(Money.format(goal.targetPaisa, digit), style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Money.format(contributed, digit) + " / " + Money.format(goal.targetPaisa, digit) +
                                " · " + Money.toBengaliDigits(percent.toString()) + "%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("লক্ষ্য যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}
