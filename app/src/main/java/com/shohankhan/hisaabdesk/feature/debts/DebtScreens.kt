package com.shohankhan.hisaabdesk.feature.debts

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.InterestPolicy
import com.shohankhan.hisaabdesk.data.local.entity.LendingEntity
import com.shohankhan.hisaabdesk.data.local.entity.PayableEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity
import com.shohankhan.hisaabdesk.data.repository.ObligationRepository
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DebtViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    val contacts = container.contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = container.accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lendings = container.obligationRepository.observeLendings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val borrowings = container.obligationRepository.observeBorrowings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val receivables = container.obligationRepository.observeReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payables = container.obligationRepository.observePayables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun contactName(id: Long): String = contacts.value.firstOrNull { it.id == id }?.name ?: "—"

    suspend fun getLending(id: Long) = container.obligationRepository.getLending(id)
    suspend fun getBorrowing(id: Long) = container.obligationRepository.getBorrowing(id)
    suspend fun getReceivable(id: Long) = container.obligationRepository.getReceivable(id)
    suspend fun getPayable(id: Long) = container.obligationRepository.getPayable(id)
    suspend fun getContact(id: Long) = container.contactRepository.get(id)

    fun observeContact(id: Long) = container.contactRepository.observeById(id)
    fun observeLendingsFor(contactId: Long) = container.obligationRepository.observeLendingsForContact(contactId)
    fun observeBorrowingsFor(contactId: Long) = container.obligationRepository.observeBorrowingsForContact(contactId)
    fun observeReceivablesFor(contactId: Long) = container.obligationRepository.observeReceivablesForContact(contactId)
    fun observePayablesFor(contactId: Long) = container.obligationRepository.observePayablesForContact(contactId)

    fun observeLending(id: Long) = container.obligationRepository.observeLending(id)
    fun observeBorrowing(id: Long) = container.obligationRepository.observeBorrowing(id)
    fun observeReceivable(id: Long) = container.obligationRepository.observeReceivable(id)
    fun observePayable(id: Long) = container.obligationRepository.observePayable(id)
    fun observeLendingPaid(id: Long) = container.obligationRepository.observeLendingPrincipalPaid(id)
    fun observeLendingInterest(id: Long) = container.obligationRepository.observeLendingInterestPaid(id)
    fun observeBorrowingPaid(id: Long) = container.obligationRepository.observeBorrowingPrincipalPaid(id)
    fun observeBorrowingInterest(id: Long) = container.obligationRepository.observeBorrowingInterestPaid(id)
    fun observeReceivableCollected(id: Long) = container.obligationRepository.observeReceivableCollected(id)
    fun observePayablePaid(id: Long) = container.obligationRepository.observePayablePaid(id)

    fun observeTxnHistory(contactId: Long) =
        container.transactionRepository.observeFiltered(contactId = contactId, limit = 50, offset = 0)

    fun saveContact(
        id: Long,
        name: String,
        phone: String,
        address: String,
        relationship: String,
        idNotes: String,
        notes: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank()) { onResult("নাম দিন"); return@launch }
            if (container.contactRepository.isDuplicateName(name, id)) {
                onResult("এই নামে তথ্য আগে থেকেই আছে")
                return@launch
            }
            val now = System.currentTimeMillis()
            val existing = if (id != 0L) container.contactRepository.get(id) else null
            container.contactRepository.save(
                (existing ?: ContactEntity(name = name, createdAt = now, updatedAt = now)).copy(
                    name = name.trim(),
                    phone = phone,
                    address = address,
                    relationship = relationship,
                    idNotes = idNotes,
                    notes = notes,
                    updatedAt = now
                )
            )
            onResult(null)
        }
    }

    fun saveLending(
        id: Long,
        contactId: Long?,
        principal: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        purpose: String,
        policy: InterestPolicy,
        interestAmount: Long,
        ratePercent: Double,
        notes: String,
        outflowAccountId: Long?,
        method: PaymentMethod,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (contactId == null) { onResult("ব্যক্তি নির্বাচন করুন"); return@launch }
                val existing = if (id != 0L) getLending(id) else null
                val now = System.currentTimeMillis()
                container.obligationRepository.saveLending(
                    (existing ?: LendingEntity(
                        contactId = contactId,
                        principalPaisa = principal,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        contactId = contactId,
                        principalPaisa = principal,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        dueDateMillis = dueDateMillis,
                        purpose = purpose,
                        interestPolicy = policy,
                        interestAmountPaisa = interestAmount,
                        ratePercent = ratePercent,
                        notes = notes,
                        updatedAt = now
                    ),
                    outflowAccountId = if (id == 0L) outflowAccountId else null,
                    method = method
                )
                onResult(null)
            } catch (e: ObligationRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun saveBorrowing(
        id: Long,
        contactId: Long?,
        principal: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        purpose: String,
        policy: InterestPolicy,
        interestAmount: Long,
        ratePercent: Double,
        notes: String,
        inflowAccountId: Long?,
        method: PaymentMethod,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (contactId == null) { onResult("ব্যক্তি নির্বাচন করুন"); return@launch }
                val existing = if (id != 0L) getBorrowing(id) else null
                val now = System.currentTimeMillis()
                container.obligationRepository.saveBorrowing(
                    (existing ?: BorrowingEntity(
                        contactId = contactId,
                        principalPaisa = principal,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        contactId = contactId,
                        principalPaisa = principal,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        dueDateMillis = dueDateMillis,
                        purpose = purpose,
                        interestPolicy = policy,
                        interestAmountPaisa = interestAmount,
                        ratePercent = ratePercent,
                        notes = notes,
                        updatedAt = now
                    ),
                    inflowAccountId = if (id == 0L) inflowAccountId else null,
                    method = method
                )
                onResult(null)
            } catch (e: ObligationRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun saveReceivable(
        id: Long,
        contactId: Long?,
        amount: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        reason: String,
        notes: String,
        outflowAccountId: Long?,
        method: PaymentMethod,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (contactId == null) { onResult("ব্যক্তি নির্বাচন করুন"); return@launch }
                val existing = if (id != 0L) getReceivable(id) else null
                val now = System.currentTimeMillis()
                container.obligationRepository.saveReceivable(
                    (existing ?: ReceivableEntity(
                        contactId = contactId,
                        amountPaisa = amount,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        contactId = contactId,
                        amountPaisa = amount,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        dueDateMillis = dueDateMillis,
                        reason = reason,
                        notes = notes,
                        updatedAt = now
                    ),
                    outflowAccountId = if (id == 0L) outflowAccountId else null,
                    method = method
                )
                onResult(null)
            } catch (e: ObligationRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun savePayable(
        id: Long,
        contactId: Long?,
        amount: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        reason: String,
        notes: String,
        inflowAccountId: Long?,
        method: PaymentMethod,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (contactId == null) { onResult("ব্যক্তি নির্বাচন করুন"); return@launch }
                val existing = if (id != 0L) getPayable(id) else null
                val now = System.currentTimeMillis()
                container.obligationRepository.savePayable(
                    (existing ?: PayableEntity(
                        contactId = contactId,
                        amountPaisa = amount,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        createdAt = now,
                        updatedAt = now
                    )).copy(
                        contactId = contactId,
                        amountPaisa = amount,
                        dateMillis = dateMillis,
                        dayKey = Dates.dayKey(dateMillis),
                        dueDateMillis = dueDateMillis,
                        reason = reason,
                        notes = notes,
                        updatedAt = now
                    ),
                    inflowAccountId = if (id == 0L) inflowAccountId else null,
                    method = method
                )
                onResult(null)
            } catch (e: ObligationRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun payLending(id: Long, amount: Long, principalPart: Long, interestPart: Long, accountId: Long?, method: PaymentMethod, dateMillis: Long, note: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.obligationRepository.addLendingCollection(id, amount, principalPart, interestPart, accountId, method, dateMillis, note)
                onResult(null)
            } catch (e: Exception) { onResult(e.message ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun payBorrowing(id: Long, amount: Long, principalPart: Long, interestPart: Long, accountId: Long?, method: PaymentMethod, dateMillis: Long, note: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.obligationRepository.addBorrowingRepayment(id, amount, principalPart, interestPart, accountId, method, dateMillis, note)
                onResult(null)
            } catch (e: Exception) { onResult(e.message ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun collectReceivable(id: Long, amount: Long, accountId: Long?, method: PaymentMethod, dateMillis: Long, note: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.obligationRepository.addReceivableCollection(id, amount, accountId, method, dateMillis, note)
                onResult(null)
            } catch (e: Exception) { onResult(e.message ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun payPayable(id: Long, amount: Long, accountId: Long?, method: PaymentMethod, dateMillis: Long, note: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.obligationRepository.addPayablePayment(id, amount, accountId, method, dateMillis, note)
                onResult(null)
            } catch (e: Exception) { onResult(e.message ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun archiveContact(id: Long, archived: Boolean) {
        viewModelScope.launch { container.contactRepository.setArchived(id, archived) }
    }
}

fun interestPolicyLabel(p: InterestPolicy): String = when (p) {
    InterestPolicy.NONE -> "সুদ নেই"
    InterestPolicy.FIXED -> "নির্দিষ্ট সুদ"
    InterestPolicy.SIMPLE -> "সরল সুদ"
    InterestPolicy.EXPLICIT_TOTAL -> "মোট সুদ নিজে লিখব"
}

@Composable
fun ContactListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contacts by vm.contacts.collectAsState()
    HisaabScaffold(title = "ব্যক্তি/প্রতিষ্ঠান", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (contacts.isEmpty()) {
            EmptyState("কোনো ব্যক্তি যোগ করা হয়নি", "ধার-দেনা, পাওনা বা দেনার জন্য ব্যক্তি যোগ করুন", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(contacts, key = { it.id }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(c.id) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            val sub = listOfNotNull(
                                c.phone.takeIf { it.isNotBlank() },
                                c.relationship.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (c.isArchived) Text("সংরক্ষণাগার", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ব্যক্তি যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ContactFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var idNotes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L && !loaded) {
            vm.getContact(id)?.let {
                loaded = true
                name = it.name
                phone = it.phone
                address = it.address
                relationship = it.relationship
                idNotes = it.idNotes
                notes = it.notes
            }
        }
    }

    HisaabScaffold(title = if (id == 0L) "ব্যক্তি যোগ করুন" else "ব্যক্তি সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("ফোন নম্বর (ঐচ্ছিক)", phone, { phone = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("ঠিকানা (ঐচ্ছিক)", address, { address = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("সম্পর্ক (ঐচ্ছিক)", relationship, { relationship = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("শনাক্তকরণ নোট (ঐচ্ছিক)", idNotes, { idNotes = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                vm.saveContact(id, name, phone, address, relationship, idNotes, notes) { err ->
                    if (err == null) onSaved() else error = err
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ContactDetailScreen(contactId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contact by vm.observeContact(contactId).collectAsState(initial = null)
    val lendings by vm.observeLendingsFor(contactId).collectAsState(initial = emptyList())
    val borrowings by vm.observeBorrowingsFor(contactId).collectAsState(initial = emptyList())
    val receivables by vm.observeReceivablesFor(contactId).collectAsState(initial = emptyList())
    val payables by vm.observePayablesFor(contactId).collectAsState(initial = emptyList())
    val history by vm.observeTxnHistory(contactId).collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = contact?.name ?: "ব্যক্তি", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            contact?.let {
                if (it.phone.isNotBlank()) KeyValueRow("ফোন", it.phone)
                if (it.address.isNotBlank()) KeyValueRow("ঠিকানা", it.address)
                if (it.relationship.isNotBlank()) KeyValueRow("সম্পর্ক", it.relationship)
                if (it.idNotes.isNotBlank()) KeyValueRow("শনাক্তকরণ নোট", it.idNotes)
                if (it.notes.isNotBlank()) KeyValueRow("নোট", it.notes)
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "সারসংক্ষেপ") {
            KeyValueRow("ধার দেওয়া রেকর্ড", Money.toBengaliDigits(lendings.size.toString()))
            KeyValueRow("ধার নেওয়া রেকর্ড", Money.toBengaliDigits(borrowings.size.toString()))
            KeyValueRow("পাওনা", Money.format(receivables.sumOf { it.amountPaisa }, digit), IncomeGreen)
            KeyValueRow("দেনা", Money.format(payables.sumOf { it.amountPaisa }, digit), ExpenseRed)
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
        Spacer(Modifier.height(10.dp))
        Text("লেনদেনের ইতিহাস", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        if (history.isEmpty()) EmptyState("কোনো লেনদেন নেই")
        history.take(15).forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(txn.description.ifBlank { "লেনদেন" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(Dates.format(txn.dateMillis, digit), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(Money.format(txn.amountPaisa, digit))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DebtListScreen(onBack: () -> Unit, onAddLending: () -> Unit, onAddBorrowing: () -> Unit, onOpenLending: (Long) -> Unit, onOpenBorrowing: (Long) -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val lendings by vm.lendings.collectAsState()
    val borrowings by vm.borrowings.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var tab by remember { mutableStateOf(0) }

    HisaabScaffold(title = "ধার-দেনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("ধার দিয়েছি") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("ধার নিয়েছি") })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            if (lendings.isEmpty()) {
                EmptyState("কোনো ধার দেওয়ার রেকর্ড নেই", actionLabel = "যোগ করুন", onAction = onAddLending)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(lendings, key = { it.id }) { l ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenLending(l.id) }.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(vm.contactName(l.contactId), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    Dates.format(l.dateMillis, digit) + if (l.purpose.isNotBlank()) " · ${l.purpose}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(Money.format(l.principalPaisa, digit), color = IncomeGreen)
                        }
                        SoftDivider()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddLending, modifier = Modifier.fillMaxWidth()) { Text("ধার দেওয়ার রেকর্ড যোগ করুন") }
        } else {
            if (borrowings.isEmpty()) {
                EmptyState("কোনো ধার নেওয়ার রেকর্ড নেই", actionLabel = "যোগ করুন", onAction = onAddBorrowing)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(borrowings, key = { it.id }) { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenBorrowing(b.id) }.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(vm.contactName(b.contactId), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    Dates.format(b.dateMillis, digit) + if (b.purpose.isNotBlank()) " · ${b.purpose}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(Money.format(b.principalPaisa, digit), color = ExpenseRed)
                        }
                        SoftDivider()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddBorrowing, modifier = Modifier.fillMaxWidth()) { Text("ধার নেওয়ার রেকর্ড যোগ করুন") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InterestPolicyField(
    policy: InterestPolicy,
    onPolicy: (InterestPolicy) -> Unit,
    interestAmount: String,
    onInterestAmount: (String) -> Unit,
    rate: String,
    onRate: (String) -> Unit,
    digit: Money.DigitStyle
) {
    DropdownField(
        label = "সুদের নিয়ম",
        options = InterestPolicy.entries,
        selected = policy,
        optionLabel = { interestPolicyLabel(it) },
        onSelected = onPolicy
    )
    if (policy == InterestPolicy.FIXED || policy == InterestPolicy.EXPLICIT_TOTAL) {
        Spacer(Modifier.height(8.dp))
        MoneyField("সুদের পরিমাণ", interestAmount, onInterestAmount, digit)
    }
    if (policy == InterestPolicy.SIMPLE) {
        Spacer(Modifier.height(8.dp))
        LabeledField("সুদের হার (বার্ষিক %)", rate, onRate)
    }
}

@Composable
fun LendingFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contacts by vm.contacts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var contactId by remember { mutableStateOf<Long?>(null) }
    var principal by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var purpose by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf(InterestPolicy.NONE) }
    var interestAmount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getLending(id)?.let {
                contactId = it.contactId
                principal = Money.format(it.principalPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                date = Dates.localDateOf(it.dateMillis)
                dueDate = it.dueDateMillis?.let { d -> Dates.localDateOf(d) }
                purpose = it.purpose
                policy = it.interestPolicy
                interestAmount = if (it.interestAmountPaisa > 0) Money.format(it.interestAmountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                rate = if (it.ratePercent > 0) it.ratePercent.toString() else ""
                notes = it.notes
            }
        }
        if (accountId == null) accountId = accounts.firstOrNull()?.id
        if (contactId == null) contactId = contacts.firstOrNull()?.id
    }

    HisaabScaffold(title = if (id == 0L) "ধার দেওয়ার রেকর্ড" else "ধার দেওয়া সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        DropdownField(
            label = "কাকে ধার দিয়েছেন",
            options = contacts,
            selected = contacts.firstOrNull { it.id == contactId },
            optionLabel = { it.name },
            onSelected = { contactId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("আসল টাকা", principal, { principal = it }, digit)
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DateField("পরিশোধের মেয়াদ (ঐচ্ছিক)", dueDate, { dueDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("উদ্দেশ্য", purpose, { purpose = it })
        Spacer(Modifier.height(8.dp))
        InterestPolicyField(policy, { policy = it }, interestAmount, { interestAmount = it }, rate, { rate = it }, digit)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "কোন অ্যাকাউন্ট থেকে টাকা গেছে",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        Text(
            "সুদের হিসাব আপনার দেওয়া তথ্যের ভিত্তিতে মডেল অনুযায়ী; চুক্তির সুনির্দিষ্ট হিসাব না জানা থাকলে চুক্তির কাগজ অনুযায়ী মিলিয়ে নিন।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(principal)
                val intAmount = if (interestAmount.isBlank()) 0L else Money.parsePaisa(interestAmount)
                val rateValue = rate.toDoubleOrNull() ?: 0.0
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    contactId == null -> error = "ব্যক্তি নির্বাচন করুন"
                    intAmount == null -> error = "সুদের পরিমাণ সঠিকভাবে লিখুন"
                    else -> {
                        error = null
                        vm.saveLending(id, contactId, paisa, Dates.millisOf(date), dueDate?.let { Dates.millisOf(it) }, purpose, policy, intAmount, rateValue, notes, accountId, method) { err ->
                            if (err == null) onSaved() else error = err
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
fun BorrowingFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contacts by vm.contacts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var contactId by remember { mutableStateOf<Long?>(null) }
    var principal by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var purpose by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf(InterestPolicy.NONE) }
    var interestAmount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getBorrowing(id)?.let {
                contactId = it.contactId
                principal = Money.format(it.principalPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                date = Dates.localDateOf(it.dateMillis)
                dueDate = it.dueDateMillis?.let { d -> Dates.localDateOf(d) }
                purpose = it.purpose
                policy = it.interestPolicy
                interestAmount = if (it.interestAmountPaisa > 0) Money.format(it.interestAmountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                rate = if (it.ratePercent > 0) it.ratePercent.toString() else ""
                notes = it.notes
            }
        }
        if (accountId == null) accountId = accounts.firstOrNull()?.id
        if (contactId == null) contactId = contacts.firstOrNull()?.id
    }

    HisaabScaffold(title = if (id == 0L) "ধার নেওয়ার রেকর্ড" else "ধার নেওয়া সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        DropdownField(
            label = "কার থেকে ধার নিয়েছেন",
            options = contacts,
            selected = contacts.firstOrNull { it.id == contactId },
            optionLabel = { it.name },
            onSelected = { contactId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("আসল টাকা", principal, { principal = it }, digit)
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DateField("পরিশোধের মেয়াদ (ঐচ্ছিক)", dueDate, { dueDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("উদ্দেশ্য", purpose, { purpose = it })
        Spacer(Modifier.height(8.dp))
        InterestPolicyField(policy, { policy = it }, interestAmount, { interestAmount = it }, rate, { rate = it }, digit)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "কোন অ্যাকাউন্টে টাকা এসেছে",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(principal)
                val intAmount = if (interestAmount.isBlank()) 0L else Money.parsePaisa(interestAmount)
                val rateValue = rate.toDoubleOrNull() ?: 0.0
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    contactId == null -> error = "ব্যক্তি নির্বাচন করুন"
                    intAmount == null -> error = "সুদের পরিমাণ সঠিকভাবে লিখুন"
                    else -> {
                        error = null
                        vm.saveBorrowing(id, contactId, paisa, Dates.millisOf(date), dueDate?.let { Dates.millisOf(it) }, purpose, policy, intAmount, rateValue, notes, accountId, method) { err ->
                            if (err == null) onSaved() else error = err
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
fun LendingDetailScreen(id: Long, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val lending by vm.observeLending(id).collectAsState(initial = null)
    val paid by vm.observeLendingPaid(id).collectAsState(initial = 0L)
    val interestPaid by vm.observeLendingInterest(id).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "ধার দেওয়ার বিস্তারিত", onBack = onBack) {
        val l = lending ?: run { EmptyState("রেকর্ড পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("ব্যক্তি", vm.contactName(l.contactId))
            KeyValueRow("আসল টাকা", Money.format(l.principalPaisa, digit))
            KeyValueRow("তারিখ", Dates.format(l.dateMillis, digit))
            if (l.dueDateMillis != null) KeyValueRow("মেয়াদ", Dates.format(l.dueDateMillis!!, digit))
            if (l.purpose.isNotBlank()) KeyValueRow("উদ্দেশ্য", l.purpose)
            KeyValueRow("সুদের নিয়ম", interestPolicyLabel(l.interestPolicy))
            KeyValueRow("বাকি আসল", Money.format(l.principalPaisa - paid, digit), if (l.principalPaisa - paid > 0) WarningAmber else IncomeGreen)
            KeyValueRow("আদায়কৃত সুদ", Money.format(interestPaid, digit), IncomeGreen)
            if (l.notes.isNotBlank()) KeyValueRow("নোট", l.notes)
            KeyValueRow(
                "অবস্থা",
                when {
                    paid >= l.principalPaisa -> "সম্পূর্ণ পরিশোধিত"
                    paid > 0 -> "আংশিক পরিশোধিত"
                    else -> "অপরিশোধিত"
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("আদায় যোগ করুন") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun BorrowingDetailScreen(id: Long, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val borrowing by vm.observeBorrowing(id).collectAsState(initial = null)
    val paid by vm.observeBorrowingPaid(id).collectAsState(initial = 0L)
    val interestPaid by vm.observeBorrowingInterest(id).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "ধার নেওয়ার বিস্তারিত", onBack = onBack) {
        val b = borrowing ?: run { EmptyState("রেকর্ড পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("ব্যক্তি", vm.contactName(b.contactId))
            KeyValueRow("আসল টাকা", Money.format(b.principalPaisa, digit))
            KeyValueRow("তারিখ", Dates.format(b.dateMillis, digit))
            if (b.dueDateMillis != null) KeyValueRow("মেয়াদ", Dates.format(b.dueDateMillis!!, digit))
            if (b.purpose.isNotBlank()) KeyValueRow("উদ্দেশ্য", b.purpose)
            KeyValueRow("সুদের নিয়ম", interestPolicyLabel(b.interestPolicy))
            KeyValueRow("বাকি আসল", Money.format(b.principalPaisa - paid, digit), if (b.principalPaisa - paid > 0) ExpenseRed else IncomeGreen)
            KeyValueRow("পরিশোধিত সুদ", Money.format(interestPaid, digit))
            if (b.notes.isNotBlank()) KeyValueRow("নোট", b.notes)
            KeyValueRow(
                "অবস্থা",
                when {
                    paid >= b.principalPaisa -> "সম্পূর্ণ পরিশোধিত"
                    paid > 0 -> "আংশিক পরিশোধিত"
                    else -> "অপরিশোধিত"
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("পরিশোধ যোগ করুন") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ObligationPaymentScreen(kind: String, id: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var amount by remember { mutableStateOf("") }
    var principalPart by remember { mutableStateOf("") }
    var interestPart by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var maxAmount by remember { mutableStateOf(0L) }
    var remainingPrincipal by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        if (accountId == null) accountId = accounts.firstOrNull()?.id
        when (kind) {
            "LENDING" -> {
                val l = vm.getLending(id)
                val paid = vm.observeLendingPaid(id).first()
                maxAmount = (l?.principalPaisa ?: 0L) - paid
                remainingPrincipal = maxAmount
            }
            "BORROWING" -> {
                val b = vm.getBorrowing(id)
                val paid = vm.observeBorrowingPaid(id).first()
                maxAmount = (b?.principalPaisa ?: 0L) - paid
                remainingPrincipal = maxAmount
            }
            "RECEIVABLE" -> {
                val r = vm.getReceivable(id)
                val collected = vm.observeReceivableCollected(id).first()
                maxAmount = (r?.amountPaisa ?: 0L) - collected
            }
            "PAYABLE" -> {
                val p = vm.getPayable(id)
                val paid = vm.observePayablePaid(id).first()
                maxAmount = (p?.amountPaisa ?: 0L) - paid
            }
        }
    }

    val title = when (kind) {
        "LENDING" -> "ধারের টাকা আদায়"
        "BORROWING" -> "ধার পরিশোধ"
        "RECEIVABLE" -> "পাওনা আদায়"
        else -> "দেনা পরিশোধ"
    }

    HisaabScaffold(title = title, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        SectionCard {
            KeyValueRow("বাকি পরিমাণ", Money.format(maxAmount, digit), WarningAmber)
        }
        Spacer(Modifier.height(10.dp))
        MoneyField("পরিমাণ", amount, { amount = it }, digit)
        if (kind == "LENDING" || kind == "BORROWING") {
            Spacer(Modifier.height(8.dp))
            MoneyField("আসলের অংশ", principalPart, { principalPart = it }, digit)
            Spacer(Modifier.height(8.dp))
            MoneyField("সুদের অংশ", interestPart, { interestPart = it }, digit)
            Text(
                "আসল/সুদ আলাদা না জানালে পুরো পরিমাণ আসল হিসেবে ধরা হবে। ${
                    com.shohankhan.hisaabdesk.core.formatting.Money.format(remainingPrincipal, digit)
                } আসল বাকি আছে।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
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
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", note, { note = it })
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val total = Money.parsePaisa(amount)
                if (total == null || total <= 0) { error = "সঠিক পরিমাণ লিখুন"; return@Button }
                if (accountId == null) { error = "অ্যাকাউন্ট নির্বাচন করুন"; return@Button }
                when (kind) {
                    "LENDING", "BORROWING" -> {
                        val pPart = if (principalPart.isBlank()) total else Money.parsePaisa(principalPart) ?: -1
                        val iPart = if (interestPart.isBlank()) 0L else Money.parsePaisa(interestPart) ?: -1
                        if (pPart < 0 || iPart < 0) { error = "আসল/সুদের অংশ সঠিকভাবে লিখুন"; return@Button }
                        if (pPart + iPart != total) { error = "আসল ও সুদের যোগফল মোট পরিমাণের সমান হতে হবে"; return@Button }
                        error = null
                        if (kind == "LENDING") {
                            vm.payLending(id, total, pPart, iPart, accountId, method, Dates.millisOf(date), note) { err ->
                                if (err == null) onDone() else error = err
                            }
                        } else {
                            vm.payBorrowing(id, total, pPart, iPart, accountId, method, Dates.millisOf(date), note) { err ->
                                if (err == null) onDone() else error = err
                            }
                        }
                    }
                    "RECEIVABLE" -> vm.collectReceivable(id, total, accountId, method, Dates.millisOf(date), note) { err ->
                        if (err == null) onDone() else error = err
                    }
                    else -> vm.payPayable(id, total, accountId, method, Dates.millisOf(date), note) { err ->
                        if (err == null) onDone() else error = err
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ReceivableListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val receivables by vm.receivables.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    ReceivablePayableList(
        title = "পাওনা",
        onBack = onBack,
        onAdd = onAdd,
        items = receivables.map {
            ObligationRowData(it.id, vm.contactName(it.contactId), it.amountPaisa, it.dueDateMillis, it.reason, it.dateMillis)
        },
        paidLabel = "আদায়কৃত",
        digit = digit,
        onOpen = onOpen
    )
}

@Composable
fun PayableListScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val payables by vm.payables.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    ReceivablePayableList(
        title = "দেনা",
        onBack = onBack,
        onAdd = onAdd,
        items = payables.map {
            ObligationRowData(it.id, vm.contactName(it.contactId), it.amountPaisa, it.dueDateMillis, it.reason, it.dateMillis)
        },
        paidLabel = "পরিশোধকৃত",
        digit = digit,
        onOpen = onOpen
    )
}

data class ObligationRowData(
    val id: Long,
    val contactName: String,
    val amount: Long,
    val dueDate: Long?,
    val reason: String,
    val date: Long
)

@Composable
private fun ReceivablePayableList(
    title: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    items: List<ObligationRowData>,
    paidLabel: String,
    digit: Money.DigitStyle,
    onOpen: (Long) -> Unit
) {
    val now = System.currentTimeMillis()
    HisaabScaffold(title = title, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("মোট", Money.format(items.sumOf { it.amount }, digit))
            KeyValueRow("রেকর্ড সংখ্যা", Money.toBengaliDigits(items.size.toString()))
            KeyValueRow("মেয়াদোত্তীর্ণ", Money.toBengaliDigits(items.count { it.dueDate != null && it.dueDate < now }.toString()))
        }
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            EmptyState("কোনো রেকর্ড নেই", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(row.id) }.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.contactName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    row.reason.takeIf { it.isNotBlank() },
                                    Dates.format(row.date, digit)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            Money.format(row.amount, digit),
                            color = if (row.dueDate != null && row.dueDate < now) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ReceivableFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contacts by vm.contacts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var contactId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getReceivable(id)?.let {
                contactId = it.contactId
                amount = Money.format(it.amountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                date = Dates.localDateOf(it.dateMillis)
                dueDate = it.dueDateMillis?.let { d -> Dates.localDateOf(d) }
                reason = it.reason
                notes = it.notes
            }
        }
        if (contactId == null) contactId = contacts.firstOrNull()?.id
    }

    HisaabScaffold(title = if (id == 0L) "পাওনা যোগ করুন" else "পাওনা সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        DropdownField(
            label = "কার কাছে পাওনা",
            options = contacts,
            selected = contacts.firstOrNull { it.id == contactId },
            optionLabel = { it.name },
            onSelected = { contactId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("মূল পরিমাণ", amount, { amount = it }, digit)
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DateField("পরিশোধের মেয়াদ (ঐচ্ছিক)", dueDate, { dueDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("কারণ", reason, { reason = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "টাকা কোথা থেকে গেছে (ঐচ্ছিক)",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "অ্যাকাউন্ট নির্বাচন করলে ওই অ্যাকাউন্ট থেকে টাকা যাওয়ার হিসাব হবে; না করলে শুধু পাওনা হিসেবে থাকবে।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    contactId == null -> error = "ব্যক্তি নির্বাচন করুন"
                    else -> {
                        error = null
                        vm.saveReceivable(id, contactId, paisa, Dates.millisOf(date), dueDate?.let { Dates.millisOf(it) }, reason, notes, accountId, method) { err ->
                            if (err == null) onSaved() else error = err
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
fun PayableFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val contacts by vm.contacts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var contactId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.getPayable(id)?.let {
                contactId = it.contactId
                amount = Money.format(it.amountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                date = Dates.localDateOf(it.dateMillis)
                dueDate = it.dueDateMillis?.let { d -> Dates.localDateOf(d) }
                reason = it.reason
                notes = it.notes
            }
        }
        if (contactId == null) contactId = contacts.firstOrNull()?.id
    }

    HisaabScaffold(title = if (id == 0L) "দেনা যোগ করুন" else "দেনা সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        DropdownField(
            label = "কাকে দেনা",
            options = contacts,
            selected = contacts.firstOrNull { it.id == contactId },
            optionLabel = { it.name },
            onSelected = { contactId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("মূল পরিমাণ", amount, { amount = it }, digit)
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DateField("পরিশোধের মেয়াদ (ঐচ্ছিক)", dueDate, { dueDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("কারণ", reason, { reason = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "টাকা কোথায় এসেছে (ঐচ্ছিক)",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val paisa = Money.parsePaisa(amount)
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    contactId == null -> error = "ব্যক্তি নির্বাচন করুন"
                    else -> {
                        error = null
                        vm.savePayable(id, contactId, paisa, Dates.millisOf(date), dueDate?.let { Dates.millisOf(it) }, reason, notes, accountId, method) { err ->
                            if (err == null) onSaved() else error = err
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
fun ReceivableDetailScreen(id: Long, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val item by vm.observeReceivable(id).collectAsState(initial = null)
    val collected by vm.observeReceivableCollected(id).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "পাওনার বিস্তারিত", onBack = onBack) {
        val r = item ?: run { EmptyState("পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("ব্যক্তি", vm.contactName(r.contactId))
            KeyValueRow("মূল পরিমাণ", Money.format(r.amountPaisa, digit))
            KeyValueRow("তারিখ", Dates.format(r.dateMillis, digit))
            if (r.dueDateMillis != null) KeyValueRow("মেয়াদ", Dates.format(r.dueDateMillis!!, digit))
            if (r.reason.isNotBlank()) KeyValueRow("কারণ", r.reason)
            KeyValueRow("আদায়কৃত", Money.format(collected, digit), IncomeGreen)
            KeyValueRow("বাকি", Money.format((r.amountPaisa - collected).coerceAtLeast(0), digit), WarningAmber)
            KeyValueRow(
                "অবস্থা",
                when {
                    collected >= r.amountPaisa -> "সম্পূর্ণ পরিশোধিত"
                    collected > 0 -> "আংশিক পরিশোধিত"
                    else -> "অপরিশোধিত"
                }
            )
            if (r.notes.isNotBlank()) KeyValueRow("নোট", r.notes)
        }
        Spacer(Modifier.height(12.dp))
        if (collected < r.amountPaisa) {
            Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("আদায় যোগ করুন") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PayableDetailScreen(id: Long, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val vm: DebtViewModel = appViewModel { DebtViewModel(it.container) }
    val item by vm.observePayable(id).collectAsState(initial = null)
    val paid by vm.observePayablePaid(id).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = "দেনার বিস্তারিত", onBack = onBack) {
        val p = item ?: run { EmptyState("পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("ব্যক্তি/প্রতিষ্ঠান", vm.contactName(p.contactId))
            KeyValueRow("মূল পরিমাণ", Money.format(p.amountPaisa, digit))
            KeyValueRow("তারিখ", Dates.format(p.dateMillis, digit))
            if (p.dueDateMillis != null) KeyValueRow("মেয়াদ", Dates.format(p.dueDateMillis!!, digit))
            if (p.reason.isNotBlank()) KeyValueRow("কারণ", p.reason)
            KeyValueRow("পরিশোধকৃত", Money.format(paid, digit), IncomeGreen)
            KeyValueRow("বাকি", Money.format((p.amountPaisa - paid).coerceAtLeast(0), digit), ExpenseRed)
            KeyValueRow(
                "অবস্থা",
                when {
                    paid >= p.amountPaisa -> "সম্পূর্ণ পরিশোধিত"
                    paid > 0 -> "আংশিক পরিশোধিত"
                    else -> "অপরিশোধিত"
                }
            )
            if (p.notes.isNotBlank()) KeyValueRow("নোট", p.notes)
        }
        Spacer(Modifier.height(12.dp))
        if (paid < p.amountPaisa) {
            Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("পরিশোধ করুন") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
