package com.shohankhan.hisaabdesk.feature.transactions

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.ConfirmDialog
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
import com.shohankhan.hisaabdesk.ui.components.SuccessBanner
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.IncomeGreen
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary
import com.shohankhan.hisaabdesk.ui.theme.WarningAmber
import java.time.LocalDate

fun txnTypeLabel(type: TxnType): String = when (type) {
    TxnType.INCOME -> "আয়"
    TxnType.EXPENSE -> "ব্যয়"
    TxnType.TRANSFER -> "টাকা স্থানান্তর"
    TxnType.SAVINGS_DEPOSIT -> "সঞ্চয়ে জমা"
    TxnType.SAVINGS_WITHDRAWAL -> "সঞ্চয় থেকে তোলা"
    TxnType.SAVINGS_INTEREST -> "সঞ্চয়ের মুনাফা/সুদ"
    TxnType.LEND -> "ধার দেওয়া"
    TxnType.LEND_COLLECT -> "ধারের টাকা আদায়"
    TxnType.BORROW -> "ধার নেওয়া"
    TxnType.BORROW_REPAY -> "ধারের টাকা পরিশোধ"
    TxnType.LOAN_DISBURSE -> "ঋণ অর্থ গ্রহণ"
    TxnType.LOAN_INSTALLMENT -> "ঋণের কিস্তি"
    TxnType.RECEIVABLE_CREATE -> "পাওনা তৈরি"
    TxnType.RECEIVABLE_COLLECT -> "পাওনা আদায়"
    TxnType.PAYABLE_CREATE -> "দেনা তৈরি"
    TxnType.PAYABLE_PAY -> "দেনা পরিশোধ"
    TxnType.SHOP_PAYMENT -> "দোকানের বাকি পরিশোধ"
    TxnType.REFUND -> "ফেরত"
    TxnType.ADJUSTMENT -> "সংশোধন"
}

fun methodLabel(m: PaymentMethod): String = when (m) {
    PaymentMethod.CASH -> "নগদ"
    PaymentMethod.BANK -> "ব্যাংক"
    PaymentMethod.MFS -> "মোবাইল ব্যাংকিং"
    PaymentMethod.CARD -> "কার্ড"
    PaymentMethod.CHEQUE -> "চেক"
    PaymentMethod.OTHER -> "অন্যান্য"
}

@Composable
fun TransactionListScreen(
    onBack: () -> Unit,
    onOpenTxn: (Long) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    val vm: TransactionListViewModel = appViewModel { TransactionListViewModel(it.container) }
    val txns by vm.txns.collectAsState()
    val filter by vm.filter.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val page by vm.page.collectAsState()

    HisaabScaffold(title = "লেনদেন", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.query,
            onValueChange = { vm.updateFilter { f -> f.copy(query = it) } },
            label = { Text("বর্ণনা, ব্যক্তি, দোকান বা রসিদ খুঁজুন") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter.type == null,
                onClick = { vm.updateFilter { f -> f.copy(type = null) } },
                label = { Text("সব") }
            )
            FilterChip(
                selected = filter.type == TxnType.INCOME,
                onClick = { vm.updateFilter { f -> f.copy(type = TxnType.INCOME) } },
                label = { Text("আয়") }
            )
            FilterChip(
                selected = filter.type == TxnType.EXPENSE,
                onClick = { vm.updateFilter { f -> f.copy(type = TxnType.EXPENSE) } },
                label = { Text("ব্যয়") }
            )
            FilterChip(
                selected = filter.type == TxnType.TRANSFER,
                onClick = { vm.updateFilter { f -> f.copy(type = TxnType.TRANSFER) } },
                label = { Text("স্থানান্তর") }
            )
        }
        Spacer(Modifier.height(6.dp))
        Row {
            TextButton(onClick = onAddIncome) { Text("+ আয়") }
            TextButton(onClick = onAddExpense) { Text("+ ব্যয়") }
            TextButton(onClick = { vm.clearFilters() }) { Text("ফিল্টার মুছুন") }
        }
        SoftDivider()
        if (txns.isEmpty()) {
            EmptyState(
                "কোনো লেনদেন পাওয়া যায়নি",
                if (filter.query.isNotBlank() || filter.type != null) "ফিল্টার পরিবর্তন করে দেখুন" else "নতুন আয় বা ব্যয় যোগ করুন",
                actionLabel = "আয় যোগ করুন",
                onAction = onAddIncome
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(txns, key = { it.id }) { txn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTxn(txn.id) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                txn.description.ifBlank { txnTypeLabel(txn.type) },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                "${txnTypeLabel(txn.type)} · ${Dates.relativeLabel(txn.dateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            Money.format(txn.amountPaisa, digit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                txn.flow == TxnFlow.INFLOW -> IncomeGreen
                                txn.flow == TxnFlow.TRANSFER -> WarningAmber
                                else -> ExpenseRed
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { vm.prevPage() }, enabled = page > 0) { Text("পূর্ববর্তী") }
                Text("পৃষ্ঠা ${Money.toBengaliDigits((page + 1).toString())}", modifier = Modifier.padding(8.dp))
                OutlinedButton(onClick = { vm.nextPage() }, enabled = txns.size >= TransactionListViewModel.PAGE_SIZE) { Text("পরবর্তী") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TransactionDetailScreen(id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: TransactionDetailViewModel = appViewModel { TransactionDetailViewModel(it.container) }
    val state by vm.state.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var showReverse by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) { vm.load(id) }

    HisaabScaffold(title = "লেনদেনের বিস্তারিত", onBack = onBack) {
        val txn = state.txn
        if (txn == null) {
            EmptyState("লেনদেন পাওয়া যায়নি")
            return@HisaabScaffold
        }
        Spacer(Modifier.height(8.dp))
        if (state.error != null) { ErrorBanner(state.error!!); Spacer(Modifier.height(8.dp)) }
        if (state.message != null) { SuccessBanner(state.message!!); Spacer(Modifier.height(8.dp)) }
        SectionCard {
            KeyValueRow("ধরন", txnTypeLabel(txn.type))
            KeyValueRow(
                "পরিমাণ",
                Money.format(txn.amountPaisa, digit),
                if (txn.flow == TxnFlow.INFLOW) IncomeGreen else ExpenseRed
            )
            KeyValueRow("তারিখ", Dates.format(txn.dateMillis, digit))
            KeyValueRow("পদ্ধতি", methodLabel(txn.paymentMethod))
            if (txn.principalPaisa > 0 || txn.interestPaisa > 0 || txn.feePaisa > 0) {
                KeyValueRow("আসলের অংশ", Money.format(txn.principalPaisa, digit))
                KeyValueRow("সুদের অংশ", Money.format(txn.interestPaisa, digit))
                KeyValueRow("ফি", Money.format(txn.feePaisa, digit))
            }
            if (txn.description.isNotBlank()) KeyValueRow("বর্ণনা", txn.description)
            if (txn.receiptNo.isNotBlank()) KeyValueRow("রসিদ নম্বর", txn.receiptNo)
            if (txn.note.isNotBlank()) KeyValueRow("নোট", txn.note)
            KeyValueRow("অবস্থা", when (txn.status) {
                com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.ACTIVE -> "সক্রিয়"
                com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.REVERSED -> "বাতিলকৃত"
                com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.ARCHIVED -> "সংরক্ষণাগারভুক্ত"
                com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.DELETED -> "মুছে ফেলা হয়েছে"
            })
            KeyValueRow("তৈরি", Dates.format(txn.createdAt, digit))
            KeyValueRow("পরিবর্তিত", Dates.format(txn.updatedAt, digit))
        }
        Spacer(Modifier.height(14.dp))
        if (txn.status == com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.ACTIVE) {
            Button(onClick = { onEdit(txn.id) }, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showReverse = true }, modifier = Modifier.fillMaxWidth()) {
                Text("হিসাব বাতিল করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ট্র্যাশে পাঠান", color = ExpenseRed)
            }
        }
        if (txn.status == com.shohankhan.hisaabdesk.data.local.entity.TxnStatus.DELETED) {
            Button(onClick = { vm.restore(txn.id) }, modifier = Modifier.fillMaxWidth()) { Text("ফিরিয়ে আনুন") }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showReverse) {
        ConfirmDialog(
            title = "লেনদেন বাতিল করবেন?",
            body = "লেনদেনটি বাতিল হিসেবে চিহ্নিত হবে এবং এর হিসাব প্রভাব সব রিপোর্ট থেকে সরে যাবে। মূল রেকর্ডটি ইতিহাসে অটুট থাকবে (মুছে যাবে না)।",
            onConfirm = { vm.reverse(id, "ব্যবহারকারী বাতিল করেছেন") },
            onDismiss = { showReverse = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "লেনদেন মুছে ফেলবেন?",
            body = "লেনদেনটি ট্র্যাশে সরানো হবে এবং হিসাব থেকে বাদ পড়বে। ট্র্যাশ থেকে পরে ফিরিয়ে আনা যাবে।",
            onConfirm = { vm.softDelete(id) },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
fun TransactionFormScreen(
    mode: FormMode,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val vm: TransactionFormViewModel = appViewModel { TransactionFormViewModel(it.container) }
    val state by vm.state.collectAsState()
    val title = when (mode) {
        FormMode.Income -> "আয় যোগ করুন"
        FormMode.Expense -> "ব্যয় যোগ করুন"
        FormMode.Transfer -> "টাকা স্থানান্তর"
        is FormMode.Edit -> "লেনদেন সম্পাদনা"
    }

    LaunchedEffect(mode) { vm.load(mode) }

    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var counterAccountId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var contactId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var description by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var receiptNo by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.initial) {
        val initial = state.initial
        if (initial != null && !prefilled) {
            prefilled = true
            amount = Money.format(initial.amountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
            date = Dates.localDateOf(initial.dateMillis)
            accountId = initial.accountId
            counterAccountId = initial.counterAccountId
            categoryId = initial.categoryId
            contactId = initial.contactId
            method = initial.paymentMethod
            description = initial.description
            note = initial.note
            receiptNo = initial.receiptNo
        }
        if (accountId == null && state.accounts.isNotEmpty()) accountId = state.accounts.first().id
    }

    if (state.saved) {
        LaunchedEffect(Unit) { onSaved() }
    }

    HisaabScaffold(title = title, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        val err = state.error ?: localError
        if (err != null) { ErrorBanner(err); Spacer(Modifier.height(8.dp)) }
        if (state.loading) {
            Text("লোড হচ্ছে…")
            return@HisaabScaffold
        }

        MoneyField(label = "পরিমাণ", value = amount, onValueChange = { amount = it }, digitStyle = Money.DigitStyle.BENGALI)
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))

        val accountOptions = state.accounts
        DropdownField(
            label = if (mode == FormMode.Transfer) "কোন অ্যাকাউন্ট থেকে" else "হিসাবের অ্যাকাউন্ট",
            options = accountOptions,
            selected = accountOptions.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        if (mode == FormMode.Transfer) {
            Spacer(Modifier.height(8.dp))
            DropdownField(
                label = "কোন অ্যাকাউন্টে",
                options = accountOptions,
                selected = accountOptions.firstOrNull { it.id == counterAccountId },
                optionLabel = { it.name },
                onSelected = { counterAccountId = it.id }
            )
        } else if (mode != FormMode.Transfer) {
            Spacer(Modifier.height(8.dp))
            val kindName = if (mode == FormMode.Income) "INCOME" else "EXPENSE"
            val catOptions = state.categories.filter { it.kind.name == kindName }
            DropdownField(
                label = "ক্যাটাগরি (ঐচ্ছিক)",
                options = catOptions,
                selected = catOptions.firstOrNull { it.id == categoryId },
                optionLabel = { it.name },
                onSelected = { categoryId = it.id }
            )
            Spacer(Modifier.height(8.dp))
            DropdownField(
                label = "ব্যক্তি/প্রতিষ্ঠান (ঐচ্ছিক)",
                options = state.contacts,
                selected = state.contacts.firstOrNull { it.id == contactId },
                optionLabel = { it.name },
                onSelected = { contactId = it.id }
            )
        }
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পরিশোধ পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("বর্ণনা", description, { description = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("রসিদ নম্বর (ঐচ্ছিক)", receiptNo, { receiptNo = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", note, { note = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                when {
                    paisa == null || paisa <= 0 -> localError = "সঠিক পরিমাণ লিখুন"
                    accountId == null -> localError = "হিসাবের অ্যাকাউন্ট নির্বাচন করুন"
                    mode == FormMode.Transfer && counterAccountId == null -> localError = "গন্তব্য অ্যাকাউন্ট নির্বাচন করুন"
                    mode == FormMode.Transfer && counterAccountId == accountId -> localError = "উৎস ও গন্তব্য অ্যাকাউন্ট এক হতে পারে না"
                    else -> {
                        localError = null
                        vm.save(
                            mode = mode,
                            amountPaisa = paisa,
                            dateMillis = Dates.millisOf(date),
                            accountId = accountId,
                            counterAccountId = counterAccountId,
                            categoryId = categoryId,
                            contactId = contactId,
                            paymentMethod = method,
                            description = description.trim(),
                            note = note.trim(),
                            receiptNo = receiptNo.trim()
                        )
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.saving) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}
