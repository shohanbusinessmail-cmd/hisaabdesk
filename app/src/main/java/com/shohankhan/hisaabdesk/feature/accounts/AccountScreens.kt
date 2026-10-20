package com.shohankhan.hisaabdesk.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.feature.onboarding.accountTypeLabel
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.ConfirmDialog
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

data class AccountWithBalance(val account: AccountEntity, val balance: Long)

class AccountListViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    val items: StateFlow<List<AccountWithBalance>> = combine(
        container.accountRepository.observeAll(),
        container.accountRepository.observeBalances()
    ) { accounts, balances ->
        accounts.map { a -> AccountWithBalance(a, balances.firstOrNull { it.id == a.id }?.balance ?: 0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class AccountFormViewModel(private val container: AppContainer) : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val account: AccountEntity? = null,
        val error: String? = null,
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val acc = if (id != 0L) container.accountRepository.get(id) else null
            _state.value = State(loading = false, account = acc)
        }
    }

    fun save(
        id: Long,
        name: String,
        type: AccountType,
        openingPaisa: Long,
        notes: String,
        allowNegative: Boolean
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val entity = if (id == 0L) {
                    AccountEntity(
                        name = name.trim(),
                        type = type,
                        openingBalancePaisa = openingPaisa,
                        notes = notes,
                        allowNegative = allowNegative,
                        createdAt = now,
                        updatedAt = now
                    )
                } else {
                    val old = _state.value.account ?: error("missing")
                    old.copy(
                        name = name.trim(),
                        type = type,
                        openingBalancePaisa = openingPaisa,
                        notes = notes,
                        allowNegative = allowNegative,
                        updatedAt = now
                    )
                }
                container.accountRepository.save(entity)
                _state.value = _state.value.copy(saved = true, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { container.accountRepository.setArchived(id, archived) }
    }

    fun delete(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val acc = container.accountRepository.get(id) ?: return@launch
            onResult(container.accountRepository.delete(acc))
        }
    }
}

class AccountDetailViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    fun observeAccount(id: Long) = container.accountRepository.observeById(id)
    fun observeBalance(id: Long) = container.accountRepository.observeBalances()
        .map { rows -> rows.firstOrNull { it.id == id }?.balance ?: 0L }
    fun observeTxns(id: Long) = container.transactionRepository.observeFiltered(accountId = id, limit = 50, offset = 0)
}

@Composable
fun AccountListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: AccountListViewModel = appViewModel { AccountListViewModel(it.container) }
    val items by vm.items.collectAsState()
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "হিসাবের অ্যাকাউন্ট", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            EmptyState("কোনো অ্যাকাউন্ট নেই", "প্রথম অ্যাকাউন্ট যোগ করুন", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.account.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(row.account.id) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.account.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                accountTypeLabel(row.account.type) + if (row.account.isArchived) " · সংরক্ষণাগারভুক্ত" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            Money.format(row.balance, digit),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (row.balance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("অ্যাকাউন্ট যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun AccountFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: AccountFormViewModel = appViewModel { AccountFormViewModel(it.container) }
    val state by vm.state.collectAsState()
    LaunchedEffect(id) { vm.load(id) }

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var opening by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var allowNegative by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var deleteAsk by remember { mutableStateOf(false) }
    var deleteResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.account) {
        val acc = state.account
        if (acc != null && !prefilled) {
            prefilled = true
            name = acc.name
            type = acc.type
            opening = Money.format(acc.openingBalancePaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
            notes = acc.notes
            allowNegative = acc.allowNegative
        }
    }
    if (state.saved) LaunchedEffect(Unit) { onSaved() }

    HisaabScaffold(title = if (id == 0L) "অ্যাকাউন্ট যোগ করুন" else "অ্যাকাউন্ট সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        val err = state.error ?: localError ?: deleteResult
        if (err != null) { ErrorBanner(err); Spacer(Modifier.height(8.dp)) }
        LabeledField("অ্যাকাউন্টের নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "অ্যাকাউন্টের ধরন",
            options = AccountType.entries,
            selected = type,
            optionLabel = { accountTypeLabel(it) },
            onSelected = { type = it }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField(
            label = "শুরুর ব্যালেন্স",
            value = opening,
            onValueChange = { opening = it },
            digitStyle = Money.DigitStyle.BENGALI,
            supportingText = "শুরুর ব্যালেন্স সংরক্ষিত হবে, আয় হিসেবে গণনা হবে না"
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = allowNegative, onCheckedChange = { allowNegative = it })
            Text("নেগেটিভ ব্যালেন্সের অনুমতি", modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(opening) ?: 0L
                if (name.isBlank()) localError = "অ্যাকাউন্টের নাম দিন"
                else if (opening.isNotBlank() && Money.parsePaisa(opening) == null) localError = "সঠিক পরিমাণ লিখুন"
                else {
                    localError = null
                    vm.save(id, name, type, paisa, notes, allowNegative)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        if (id != 0L) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.setArchived(id, !(state.account?.isArchived ?: false)) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.account?.isArchived == true) "সংরক্ষণাগার থেকে ফেরান" else "সংরক্ষণাগারে রাখুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { deleteAsk = true }, modifier = Modifier.fillMaxWidth()) {
                Text("মুছে ফেলুন", color = ExpenseRed)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (deleteAsk) {
        ConfirmDialog(
            title = "অ্যাকাউন্ট মুছে ফেলবেন?",
            body = "এই অ্যাকাউন্টে কোনো লেনদেন না থাকলেই মোছা যাবে।",
            onConfirm = {
                vm.delete(id) { ok ->
                    if (ok) onSaved() else deleteResult = "এই অ্যাকাউন্টে লেনদেন আছে। মুছে ফেলার আগে সেগুলো সরান বা অ্যাকাউন্ট সংরক্ষণাগারে রাখুন।"
                }
            },
            onDismiss = { deleteAsk = false }
        )
    }
}

@Composable
fun AccountDetailScreen(id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm: AccountDetailViewModel = appViewModel { AccountDetailViewModel(it.container) }
    val account by vm.observeAccount(id).collectAsState(initial = null)
    val balance by vm.observeBalance(id).collectAsState(initial = 0L)
    val txns by vm.observeTxns(id).collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = account?.name ?: "অ্যাকাউন্ট", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("বর্তমান ব্যালেন্স", Money.format(balance, digit), if (balance >= 0) IncomeGreen else ExpenseRed)
            account?.let {
                KeyValueRow("ধরন", accountTypeLabel(it.type))
                KeyValueRow("শুরুর ব্যালেন্স", Money.format(it.openingBalancePaisa, digit))
                if (it.notes.isNotBlank()) KeyValueRow("নোট", it.notes)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
        Spacer(Modifier.height(12.dp))
        Text("সাম্প্রতিক লেনদেন", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        if (txns.isEmpty()) {
            EmptyState("কোনো লেনদেন নেই")
        } else {
            txns.forEach { txn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(txn.description.ifBlank { txn.type.name }, style = MaterialTheme.typography.bodyMedium)
                        Text(Dates.relativeLabel(txn.dateMillis), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(
                        Money.format(txn.amountPaisa, digit),
                        color = if (txn.flow == TxnFlow.INFLOW) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

class TransferViewModel(private val container: AppContainer) : ViewModel() {
    data class State(
        val accounts: List<AccountEntity> = emptyList(),
        val saving: Boolean = false,
        val error: String? = null,
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val accounts = container.accountRepository.observeActive().first()
            _state.value = State(accounts = accounts)
        }
    }

    fun transfer(fromId: Long, toId: Long, amountPaisa: Long, feePaisa: Long, dateMillis: Long, note: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                val now = System.currentTimeMillis()
                container.transactionRepository.insert(
                    TxnEntity(
                        type = TxnType.TRANSFER,
                        flow = TxnFlow.TRANSFER,
                        amountPaisa = amountPaisa,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        accountId = fromId,
                        counterAccountId = toId,
                        paymentMethod = PaymentMethod.CASH,
                        description = "টাকা স্থানান্তর",
                        note = note,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                if (feePaisa > 0) {
                    container.transactionRepository.insert(
                        TxnEntity(
                            type = TxnType.EXPENSE,
                            flow = TxnFlow.OUTFLOW,
                            amountPaisa = feePaisa,
                            dateMillis = dateMillis,
                            dayKey = Dates.dayKey(dateMillis),
                            accountId = fromId,
                            paymentMethod = PaymentMethod.CASH,
                            description = "স্থানান্তর ফি",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.message ?: "স্থানান্তর ব্যর্থ")
            }
        }
    }
}

@Composable
fun TransferScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val vm: TransferViewModel = appViewModel { TransferViewModel(it.container) }
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    var from by remember { mutableStateOf<Long?>(null) }
    var to by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    if (state.saved) LaunchedEffect(Unit) { onDone() }

    HisaabScaffold(title = "টাকা স্থানান্তর", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        val err = state.error ?: localError
        if (err != null) { ErrorBanner(err); Spacer(Modifier.height(8.dp)) }
        Text("স্থানান্তর কখনো আয় বা ব্যয় নয় — মোট সম্পদ অপরিবর্তিত থাকে।", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "যে অ্যাকাউন্ট থেকে",
            options = state.accounts,
            selected = state.accounts.firstOrNull { it.id == from },
            optionLabel = { it.name },
            onSelected = { from = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "যে অ্যাকাউন্টে",
            options = state.accounts,
            selected = state.accounts.firstOrNull { it.id == to },
            optionLabel = { it.name },
            onSelected = { to = it.id }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("পরিমাণ", amount, { amount = it }, Money.DigitStyle.BENGALI)
        Spacer(Modifier.height(8.dp))
        MoneyField("ফি (যদি থাকে)", fee, { fee = it }, Money.DigitStyle.BENGALI, supportingText = "ফি আলাদা ব্যয় হিসেবে সংরক্ষিত হবে")
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট", note, { note = it })
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                val feePaisa = if (fee.isBlank()) 0L else Money.parsePaisa(fee)
                when {
                    paisa == null || paisa <= 0 -> localError = "সঠিক পরিমাণ লিখুন"
                    feePaisa == null -> localError = "ফি সঠিকভাবে লিখুন"
                    from == null || to == null -> localError = "দুটি অ্যাকাউন্ট নির্বাচন করুন"
                    from == to -> localError = "উৎস ও গন্তব্য অ্যাকাউন্ট এক হতে পারে না"
                    else -> {
                        localError = null
                        vm.transfer(from!!, to!!, paisa, feePaisa, Dates.millisOf(date), note)
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth()
        ) { Text("স্থানান্তর করুন") }
        Spacer(Modifier.height(24.dp))
    }
}
