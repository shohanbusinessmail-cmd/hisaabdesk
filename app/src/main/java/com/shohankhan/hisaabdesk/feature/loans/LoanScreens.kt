package com.shohankhan.hisaabdesk.feature.loans

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
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentStatus
import com.shohankhan.hisaabdesk.data.local.entity.InterestMethod
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanKind
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.repository.LoanRepository
import com.shohankhan.hisaabdesk.domain.loan.LoanCalculator
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

fun interestMethodLabel(m: InterestMethod): String = when (m) {
    InterestMethod.NONE -> "সুদ নেই"
    InterestMethod.FLAT -> "ফ্ল্যাট সুদ"
    InterestMethod.REDUCING_MODEL -> "ক্রমহ্রাসমান ব্যালেন্স (মডেল)"
    InterestMethod.SIMPLE_MODEL -> "সরল সুদ (মডেল)"
    InterestMethod.CONTRACT_TOTAL -> "চুক্তির মোট পরিশোধ নিজে দেব"
}

fun frequencyLabel(f: String): String = when (f) {
    "WEEKLY" -> "সাপ্তাহিক"
    "CUSTOM_DAYS" -> "নির্দিষ্ট দিন পরপর"
    else -> "মাসিক"
}

class LoanViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)
    val accounts = container.accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun observeLoans(kind: String) = container.loanRepository.observeLoans(kind)
    fun observeLoan(id: Long) = container.loanRepository.observeLoan(id)
    fun observeInstallments(id: Long) = container.loanRepository.observeInstallments(id)
    fun observePayments(id: Long) = container.loanRepository.observeLoanPayments(id)
    fun observePrincipalPaid(id: Long) = container.loanRepository.observeLoanPrincipalPaid(id)
    fun observeInterestPaid(id: Long) = container.loanRepository.observeLoanInterestPaid(id)
    fun observeFeesPaid(id: Long) = container.loanRepository.observeLoanFeesPaid(id)
    suspend fun getLoan(id: Long) = container.loanRepository.getLoan(id)
    suspend fun getInstallments(id: Long) = container.loanRepository.getInstallments(id)

    fun saveLoan(
        kind: LoanKind,
        id: Long,
        institution: String,
        branch: String,
        accountRef: String,
        loanTypeName: String,
        startDate: LocalDate,
        principal: Long,
        method: InterestMethod,
        rate: Double,
        contractualTotal: Long?,
        installmentAmount: Long,
        installmentStart: LocalDate,
        count: Int,
        frequency: String,
        intervalDays: Int,
        notes: String,
        disburseAccountId: Long?,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val existing = if (id != 0L) getLoan(id) else null
                val loan = (existing ?: LoanEntity(
                    kind = kind,
                    institution = institution,
                    startDateMillis = Dates.millisOf(startDate),
                    dayKey = Dates.dayKey(startDate),
                    principalPaisa = principal,
                    createdAt = now,
                    updatedAt = now
                )).copy(
                    kind = kind,
                    institution = institution,
                    branch = branch,
                    accountRef = accountRef,
                    loanTypeName = loanTypeName,
                    startDateMillis = Dates.millisOf(startDate),
                    dayKey = Dates.dayKey(startDate),
                    principalPaisa = principal,
                    interestMethod = method,
                    ratePercent = rate,
                    totalScheduledPaisa = contractualTotal,
                    frequency = frequency,
                    intervalDays = intervalDays,
                    installmentAmountPaisa = installmentAmount,
                    installmentStartMillis = Dates.millisOf(installmentStart),
                    expectedInstallments = count,
                    notes = notes,
                    updatedAt = now
                )
                val scheduleMode = when (method) {
                    InterestMethod.NONE -> LoanCalculator.ScheduleMode.NONE
                    InterestMethod.FLAT -> LoanCalculator.ScheduleMode.FLAT
                    InterestMethod.REDUCING_MODEL -> LoanCalculator.ScheduleMode.REDUCING_MODEL
                    InterestMethod.SIMPLE_MODEL -> LoanCalculator.ScheduleMode.SIMPLE_MODEL
                    InterestMethod.CONTRACT_TOTAL -> LoanCalculator.ScheduleMode.CONTRACT_TOTAL
                }
                val freq = when (frequency) {
                    "WEEKLY" -> Dates.Frequency.WEEKLY
                    "CUSTOM_DAYS" -> Dates.Frequency.CUSTOM_DAYS
                    else -> Dates.Frequency.MONTHLY
                }
                container.loanRepository.saveLoan(
                    loan = loan,
                    scheduleInput = LoanRepository.ScheduleInput(
                        mode = scheduleMode,
                        frequency = freq,
                        intervalDays = intervalDays,
                        installmentAmountPaisa = installmentAmount,
                        installmentStartMillis = Dates.millisOf(installmentStart),
                        expectedInstallments = count,
                        contractualTotalPaisa = contractualTotal
                    ),
                    disburseAccountId = if (id == 0L) disburseAccountId else null,
                    method = paymentMethod,
                    regenerateSchedule = id == 0L
                )
                onResult(null)
            } catch (e: LoanRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult(e.message ?: "সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun addPayment(
        loanId: Long,
        installmentId: Long?,
        amount: Long,
        principalPart: Long,
        interestPart: Long,
        feePart: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        receiptNo: String,
        note: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                container.loanRepository.addPayment(
                    loanId, installmentId, amount, principalPart, interestPart, feePart,
                    accountId, method, dateMillis, receiptNo, note
                )
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "পরিশোধ সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { container.loanRepository.setArchived(id, archived) }
    }
}

@Composable
fun LoanListScreen(kind: String, onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm: LoanViewModel = appViewModel { LoanViewModel(it.container) }
    val loans by vm.observeLoans(kind).collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()

    HisaabScaffold(title = if (kind == "NGO") "এনজিও ঋণ" else "ব্যাংক ঋণ", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("মোট ঋণ", Money.format(loans.sumOf { it.principalPaisa }, digit), ExpenseRed)
            KeyValueRow("ঋণ সংখ্যা", Money.toBengaliDigits(loans.size.toString()))
            KeyValueRow("চলমান", Money.toBengaliDigits(loans.count { !it.isCompleted }.toString()))
        }
        Spacer(Modifier.height(10.dp))
        if (loans.isEmpty()) {
            EmptyState("কোনো ঋণ নেই", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(loans, key = { it.id }) { loan ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(loan.id) }.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(loan.institution, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${frequencyLabel(loan.frequency)} · ${Money.toBengaliDigits(loan.expectedInstallments.toString())} কিস্তি" +
                                    if (loan.isCompleted) " · সম্পন্ন" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(Money.format(loan.principalPaisa, digit), color = ExpenseRed)
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(if (kind == "NGO") "এনজিও ঋণ যোগ করুন" else "ব্যাংক ঋণ যোগ করুন")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun LoanFormScreen(kind: String, existingId: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: LoanViewModel = appViewModel { LoanViewModel(it.container) }
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()

    var institution by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var accountRef by remember { mutableStateOf("") }
    var loanTypeName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var principal by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(InterestMethod.NONE) }
    var rate by remember { mutableStateOf("") }
    var contractualTotal by remember { mutableStateOf("") }
    var installmentAmount by remember { mutableStateOf("") }
    var installmentStart by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var count by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var intervalDays by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingId) {
        if (existingId != 0L) {
            vm.getLoan(existingId)?.let {
                institution = it.institution
                branch = it.branch
                accountRef = it.accountRef
                loanTypeName = it.loanTypeName
                startDate = Dates.localDateOf(it.startDateMillis)
                principal = Money.format(it.principalPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                method = it.interestMethod
                rate = if (it.ratePercent > 0) it.ratePercent.toString() else ""
                contractualTotal = it.totalScheduledPaisa?.let { t -> Money.format(t, Money.DigitStyle.ENGLISH, withSymbol = false) } ?: ""
                installmentAmount = if (it.installmentAmountPaisa > 0) Money.format(it.installmentAmountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                installmentStart = Dates.localDateOf(it.installmentStartMillis)
                count = it.expectedInstallments.toString()
                frequency = it.frequency
                intervalDays = if (it.intervalDays > 0) it.intervalDays.toString() else ""
                notes = it.notes
            }
        }
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    HisaabScaffold(
        title = (if (existingId == 0L) "যোগ করুন: " else "সম্পাদনা: ") + (if (kind == "NGO") "এনজিও ঋণ" else "ব্যাংক ঋণ"),
        onBack = onBack
    ) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField(if (kind == "NGO") "এনজিওর নাম" else "ব্যাংকের নাম", institution, { institution = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("শাখা (ঐচ্ছিক)", branch, { branch = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("ঋণ হিসাব/চুক্তি নম্বর (ঐচ্ছিক)", accountRef, { accountRef = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("ঋণের ধরন (ঐচ্ছিক)", loanTypeName, { loanTypeName = it })
        Spacer(Modifier.height(8.dp))
        DateField("ঋণ শুরুর তারিখ", startDate, { startDate = it })
        Spacer(Modifier.height(8.dp))
        MoneyField("মূল ঋণ", principal, { principal = it }, digit)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "সুদ/চার্জের হিসাব পদ্ধতি",
            options = InterestMethod.entries,
            selected = method,
            optionLabel = { interestMethodLabel(it) },
            onSelected = { method = it }
        )
        if (method != InterestMethod.NONE && method != InterestMethod.CONTRACT_TOTAL) {
            Spacer(Modifier.height(8.dp))
            LabeledField("সুদের হার (%)", rate, { rate = it })
        }
        if (method == InterestMethod.CONTRACT_TOTAL) {
            Spacer(Modifier.height(8.dp))
            MoneyField("চুক্তির মোট পরিশোধ", contractualTotal, { contractualTotal = it }, digit)
        }
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "কিস্তির ব্যবধান",
            options = listOf("WEEKLY", "MONTHLY", "CUSTOM_DAYS"),
            selected = frequency,
            optionLabel = { frequencyLabel(it) },
            onSelected = { frequency = it }
        )
        if (frequency == "CUSTOM_DAYS") {
            Spacer(Modifier.height(8.dp))
            LabeledField("ব্যবধান (দিন)", intervalDays, { intervalDays = it })
        }
        Spacer(Modifier.height(8.dp))
        DateField("প্রথম কিস্তির তারিখ", installmentStart, { installmentStart = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("কিস্তির সংখ্যা", count, { count = it })
        Spacer(Modifier.height(8.dp))
        MoneyField("কিস্তির পরিমাণ (ঐচ্ছিক)", installmentAmount, { installmentAmount = it }, digit, supportingText = "খালি রাখলে হিসাব পদ্ধতি অনুযায়ী নির্ধারিত হবে")
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "ঋণ অর্থ যে অ্যাকাউন্টে",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = paymentMethod,
            optionLabel = { methodLabel(it) },
            onSelected = { paymentMethod = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", notes, { notes = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        Text(
            "মডেল অনুযায়ী হিসাব অনুমান; ব্যাংক/এনজিওর চুক্তির প্রকৃত সূচি জানা থাকলে চুক্তির মোট পরিশোধ বা কিস্তির পরিমাণ লিখে দিন।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val principalPaisa = Money.parsePaisa(principal)
                val countInt = count.toIntOrNull()
                val total = if (contractualTotal.isBlank()) null else Money.parsePaisa(contractualTotal)
                val instAmount = if (installmentAmount.isBlank()) 0L else Money.parsePaisa(installmentAmount)
                val rateValue = rate.toDoubleOrNull() ?: 0.0
                when {
                    institution.isBlank() -> error = "প্রতিষ্ঠানের নাম দিন"
                    principalPaisa == null || principalPaisa <= 0 -> error = "সঠিক মূল ঋণ লিখুন"
                    countInt == null || countInt <= 0 -> error = "কিস্তির সংখ্যা দিন"
                    method == InterestMethod.CONTRACT_TOTAL && total == null -> error = "চুক্তির মোট পরিশোধ দিন"
                    instAmount == null -> error = "কিস্তির পরিমাণ সঠিকভাবে লিখুন"
                    frequency == "CUSTOM_DAYS" && (intervalDays.toIntOrNull() ?: 0) <= 0 -> error = "ব্যবধান (দিন) দিন"
                    else -> {
                        error = null
                        vm.saveLoan(
                            kind = LoanKind.valueOf(kind),
                            id = existingId,
                            institution = institution.trim(),
                            branch = branch,
                            accountRef = accountRef,
                            loanTypeName = loanTypeName,
                            startDate = startDate,
                            principal = principalPaisa,
                            method = method,
                            rate = rateValue,
                            contractualTotal = total,
                            installmentAmount = instAmount,
                            installmentStart = installmentStart,
                            count = countInt,
                            frequency = frequency,
                            intervalDays = intervalDays.toIntOrNull() ?: 0,
                            notes = notes,
                            disburseAccountId = accountId,
                            paymentMethod = paymentMethod,
                            onResult = { err -> if (err == null) onSaved() else error = err }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun LoanDetailScreen(loanId: Long, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val vm: LoanViewModel = appViewModel { LoanViewModel(it.container) }
    val loan by vm.observeLoan(loanId).collectAsState(initial = null)
    val installments by vm.observeInstallments(loanId).collectAsState(initial = emptyList())
    val principalPaid by vm.observePrincipalPaid(loanId).collectAsState(initial = 0L)
    val interestPaid by vm.observeInterestPaid(loanId).collectAsState(initial = 0L)
    val feesPaid by vm.observeFeesPaid(loanId).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()
    val now = System.currentTimeMillis()

    val scheduled = installments.sumOf { it.scheduledPaisa }
    val next = installments.firstOrNull { it.status != InstallmentStatus.PAID }
    val overdue = installments.count { it.status != InstallmentStatus.PAID && it.dueDateMillis < now }

    HisaabScaffold(title = "ঋণের বিস্তারিত", onBack = onBack) {
        val l = loan ?: run { EmptyState("ঋণ পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard(title = l.institution) {
            if (l.branch.isNotBlank()) KeyValueRow("শাখা", l.branch)
            if (l.accountRef.isNotBlank()) KeyValueRow("হিসাব নম্বর", l.accountRef)
            if (l.loanTypeName.isNotBlank()) KeyValueRow("ধরন", l.loanTypeName)
            KeyValueRow("শুরুর তারিখ", Dates.format(l.startDateMillis, digit))
            KeyValueRow("মূল ঋণ", Money.format(l.principalPaisa, digit))
            KeyValueRow("হিসাব পদ্ধতি", interestMethodLabel(l.interestMethod))
            if (l.ratePercent > 0) KeyValueRow("সুদের হার", "${l.ratePercent}%")
            KeyValueRow("নির্ধারিত মোট পরিশোধ", Money.format(scheduled, digit))
            KeyValueRow("আসল পরিশোধিত", Money.format(principalPaid, digit), IncomeGreen)
            KeyValueRow("সুদ পরিশোধিত", Money.format(interestPaid, digit), IncomeGreen)
            KeyValueRow("ফি পরিশোধিত", Money.format(feesPaid, digit))
            KeyValueRow("বাকি আসল", Money.format((l.principalPaisa - principalPaid).coerceAtLeast(0), digit), ExpenseRed)
            if (next != null) {
                KeyValueRow("পরবর্তী কিস্তি", Dates.format(next.dueDateMillis, digit), WarningAmber)
                KeyValueRow("পরবর্তী কিস্তির বাকি", Money.format(next.scheduledPaisa - next.paidPaisa, digit))
            }
            KeyValueRow("মেয়াদোত্তীর্ণ কিস্তি", Money.toBengaliDigits(overdue.toString()), if (overdue > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface)
            KeyValueRow("অবস্থা", if (l.isCompleted) "পরিশোধ সম্পন্ন" else "চলমান")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("কিস্তির পরিশোধ যোগ করুন") }
        Spacer(Modifier.height(12.dp))
        SectionCard(title = "কিস্তির সূচি") {
            if (installments.isEmpty()) {
                Text("কোনো কিস্তি নেই", style = MaterialTheme.typography.bodyMedium)
            }
            installments.forEach { inst ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "কিস্তি ${Money.toBengaliDigits(inst.seq.toString())} · ${Dates.format(inst.dueDateMillis, digit)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            when (inst.status) {
                                InstallmentStatus.PAID -> "পরিশোধিত"
                                InstallmentStatus.PARTIAL -> "আংশিক"
                                InstallmentStatus.UNPAID -> if (inst.dueDateMillis < now) "মেয়াদ পেরিয়েছে" else "অপরিশোধিত"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (inst.status) {
                                InstallmentStatus.PAID -> IncomeGreen
                                InstallmentStatus.PARTIAL -> WarningAmber
                                InstallmentStatus.UNPAID -> if (inst.dueDateMillis < now) ExpenseRed else TextSecondary
                            }
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(Money.format(inst.scheduledPaisa, digit), style = MaterialTheme.typography.bodyMedium)
                        if (inst.paidPaisa > 0 && inst.paidPaisa < inst.scheduledPaisa) {
                            Text(
                                "বাকি " + Money.format(inst.scheduledPaisa - inst.paidPaisa, digit),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                SoftDivider()
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { vm.setArchived(loanId, !(loan?.isArchived ?: false)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loan?.isArchived == true) "সংরক্ষণাগার থেকে ফেরান" else "সংরক্ষণাগারে রাখুন")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun LoanPaymentScreen(loanId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: LoanViewModel = appViewModel { LoanViewModel(it.container) }
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val installments by vm.observeInstallments(loanId).collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<InstallmentEntity?>(null) }
    var amount by remember { mutableStateOf("") }
    var principalPart by remember { mutableStateOf("") }
    var interestPart by remember { mutableStateOf("") }
    var feePart by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var receipt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(installments) {
        if (selected == null) {
            selected = installments.firstOrNull { it.status != InstallmentStatus.PAID }
            selected?.let { inst ->
                amount = Money.format(inst.scheduledPaisa - inst.paidPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                principalPart = Money.format(inst.principalPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                interestPart = Money.format(inst.interestPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
            }
        }
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    HisaabScaffold(title = "কিস্তির পরিশোধ", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        val pending = installments.filter { it.status != InstallmentStatus.PAID }
        if (pending.isEmpty()) {
            EmptyState("সব কিস্তি পরিশোধিত")
            return@HisaabScaffold
        }
        DropdownField(
            label = "কিস্তি",
            options = pending,
            selected = selected,
            optionLabel = { "কিস্তি ${Money.toBengaliDigits(it.seq.toString())} · ${Money.format(it.scheduledPaisa - it.paidPaisa, digit)}" },
            onSelected = { inst ->
                selected = inst
                amount = Money.format(inst.scheduledPaisa - inst.paidPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                principalPart = Money.format(inst.principalPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
                interestPart = Money.format(inst.interestPaisa, Money.DigitStyle.ENGLISH, withSymbol = false)
            }
        )
        Spacer(Modifier.height(8.dp))
        MoneyField("পরিমাণ", amount, { amount = it }, digit)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { MoneyField("আসলের অংশ", principalPart, { principalPart = it }, digit) }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) { MoneyField("সুদের অংশ", interestPart, { interestPart = it }, digit) }
        }
        Spacer(Modifier.height(8.dp))
        MoneyField("বিলম্ব ফি (ঐচ্ছিক)", feePart, { feePart = it }, digit)
        Text(
            "পরিশোধের আসল/সুদ/ফি অংশ জানা থাকলে আলাদা করে লিখুন; না জানালে পুরো পরিশোধ আসল হিসেবে ধরা হবে।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
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
            label = "পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        DateField("তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("রসিদ নম্বর (ঐচ্ছিক)", receipt, { receipt = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", note, { note = it })
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val total = Money.parsePaisa(amount)
                if (total == null || total <= 0) { error = "সঠিক পরিমাণ লিখুন"; return@Button }
                if (accountId == null) { error = "অ্যাকাউন্ট নির্বাচন করুন"; return@Button }
                val p = if (principalPart.isBlank()) 0L else Money.parsePaisa(principalPart)
                val i = if (interestPart.isBlank()) 0L else Money.parsePaisa(interestPart)
                val f = if (feePart.isBlank()) 0L else Money.parsePaisa(feePart)
                if (p == null || i == null || f == null) { error = "অংশগুলো সঠিকভাবে লিখুন"; return@Button }
                if (p + i + f != 0L && p + i + f != total) { error = "আসল + সুদ + ফি = মোট পরিমাণ হতে হবে"; return@Button }
                error = null
                vm.addPayment(
                    loanId = loanId,
                    installmentId = selected?.id,
                    amount = total,
                    principalPart = p,
                    interestPart = i,
                    feePart = f,
                    accountId = accountId,
                    method = method,
                    dateMillis = Dates.millisOf(date),
                    receiptNo = receipt,
                    note = note
                ) { err -> if (err == null) onDone() else error = err }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("পরিশোধ সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}
