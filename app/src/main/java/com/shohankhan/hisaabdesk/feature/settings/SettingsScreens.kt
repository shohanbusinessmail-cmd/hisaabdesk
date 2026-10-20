package com.shohankhan.hisaabdesk.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.BuildConfig
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.repository.UserSettings
import com.shohankhan.hisaabdesk.feature.transactions.txnTypeLabel
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.ConfirmDialog
import com.shohankhan.hisaabdesk.ui.components.ErrorBanner
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.KeyValueRow
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SoftDivider
import com.shohankhan.hisaabdesk.ui.components.SuccessBanner
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.IncomeGreen
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<UserSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    fun setDigitStyle(style: Money.DigitStyle) = viewModelScope.launch {
        container.settingsRepository.setDigitStyle(style)
    }

    fun setGrouping(grouping: Money.Grouping) = viewModelScope.launch {
        container.settingsRepository.setGrouping(grouping)
    }

    fun setIsoDates(iso: Boolean) = viewModelScope.launch {
        container.settingsRepository.setIsoDates(iso)
    }

    fun setLockTimeout(minutes: Int) = viewModelScope.launch {
        container.settingsRepository.setLockTimeoutMinutes(minutes)
    }

    fun setReminders(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setRemindersEnabled(enabled)
    }

    // trash
    fun observeTrash() = container.transactionRepository.observeTrash()
    fun observeArchived() = container.transactionRepository.observeArchived()
    fun restore(id: Long) = viewModelScope.launch { container.transactionRepository.restore(id) }
    fun permanentDelete(id: Long, onDone: () -> Unit) = viewModelScope.launch {
        try {
            container.transactionRepository.permanentDelete(id)
        } finally {
            onDone()
        }
    }

    // reconcile
    suspend fun reconcile() = container.reportRepository.reconcile()
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenReconcile: () -> Unit
) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.container) }
    val settings by vm.settings.collectAsState()

    HisaabScaffold(title = "সেটিংস", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard(title = "সংখ্যা ও মুদ্রা") {
            Text("সংখ্যা লেখার ধরন", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = settings.digitStyle == Money.DigitStyle.BENGALI,
                    onClick = { vm.setDigitStyle(Money.DigitStyle.BENGALI) },
                    label = { Text("বাংলা অঙ্ক (১২৩)") }
                )
                FilterChip(
                    selected = settings.digitStyle == Money.DigitStyle.ENGLISH,
                    onClick = { vm.setDigitStyle(Money.DigitStyle.ENGLISH) },
                    label = { Text("ইংরেজি অঙ্ক (123)") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("মুদ্রা: বাংলাদেশি টাকা (৳)", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("তারিখের ফরম্যাট", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !settings.isoDates,
                    onClick = { vm.setIsoDates(false) },
                    label = { Text("দিন/মাস/বছর") }
                )
                FilterChip(
                    selected = settings.isoDates,
                    onClick = { vm.setIsoDates(true) },
                    label = { Text("বছর-মাস-দিন") }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "নিরাপত্তা") {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenSecurity).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("নিরাপত্তা সেটিংস", style = MaterialTheme.typography.bodyLarge)
                    Text("পিন পরিবর্তন ও স্বয়ংক্রিয় লক", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text("→", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "রিমাইন্ডার") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = settings.remindersEnabled, onCheckedChange = { vm.setReminders(it) })
                Text("বিল ও কিস্তির রিমাইন্ডার চালু", modifier = Modifier.padding(start = 10.dp))
            }
            Text(
                "নোটিফিকেশনের অনুমতি না থাকলে রিমাইন্ডার দেখানো হবে না — অ্যাপের হিসাব ঠিক থাকবে।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "ব্যবস্থাপনা") {
            SettingsLink("ক্যাটাগরি ব্যবস্থাপনা", onOpenCategories)
            SoftDivider()
            SettingsLink("ব্যাকআপ ও রিস্টোর", onOpenBackup)
            SoftDivider()
            SettingsLink("তথ্য যাচাই (রিকনসিলিয়েশন)", onOpenReconcile)
            SoftDivider()
            SettingsLink("ট্র্যাশ ও সংরক্ষণাগার", onOpenTrash)
            SoftDivider()
            SettingsLink("হিসাবডেস্ক সম্পর্কে", onOpenAbout)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "হিসাবডেস্কের ভাষা বাংলা (বাংলাদেশ)। সব তথ্য আপনার ডিভাইসে সংরক্ষিত থাকে।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("→", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SecuritySettingsScreen(onBack: () -> Unit, onChangePin: () -> Unit) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.container) }
    val settings by vm.settings.collectAsState()

    HisaabScaffold(title = "নিরাপত্তা সেটিংস", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard(title = "পিন") {
            Button(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) { Text("পিন পরিবর্তন করুন") }
            Spacer(Modifier.height(8.dp))
            Text(
                "নিরাপত্তার জন্য কোনো গোপন পিন-রিসেট উপায় নেই। পিন ভুলে গেলে অ্যাপের ডেটা পুনরুদ্ধার করা যাবে না — তবে আগে তৈরি করা এনক্রিপ্টেড ব্যাকআপ ফাইল থেকে নতুন করে রিস্টোর করা যাবে।",
                style = MaterialTheme.typography.bodySmall,
                color = ExpenseRed
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "স্বয়ংক্রিয় লক") {
            Text("অ্যাপ ব্যাকগ্রাউন্ডে গেলে কত ক্ষণ পর পিন চাইবে", style = MaterialTheme.typography.bodyMedium)
            val options = listOf(
                0 to "সঙ্গে সঙ্গে",
                1 to "১ মিনিট",
                5 to "৫ মিনিট",
                15 to "১৫ মিনিট",
                -1 to "কখনো নয়"
            )
            Column {
                options.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = settings.lockTimeoutMinutes == value,
                            onClick = { vm.setLockTimeout(value) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    HisaabScaffold(title = "হিসাবডেস্ক সম্পর্কে", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard {
            KeyValueRow("অ্যাপের নাম", "হিসাবডেস্ক (HisaabDesk)")
            KeyValueRow("সংস্করণ", BuildConfig.VERSION_NAME)
            KeyValueRow("বিল্ড", BuildConfig.VERSION_CODE.toString())
            KeyValueRow("প্যাকেজ", BuildConfig.APPLICATION_ID)
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "মালিক ও যোগাযোগ") {
            KeyValueRow("মালিক", "শোহান খান")
            KeyValueRow("যোগাযোগ", "helloiamshohan@gmail.com")
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "প্রযুক্তি") {
            Text(
                "কোটলিন, জেটপ্যাক কম্পোজ, ম্যাটেরিয়াল ৩, Room (SQLite), WorkManager। সম্পূর্ণ অফলাইন — কোনো ইন্টারনেট অনুমতি নেই।",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "তৃতীয় পক্ষের লাইসেন্স") {
            Text(
                "AndroidX (Core, Lifecycle, Activity, Navigation, Room, WorkManager, DataStore, Compose UI, Material 3) — Apache License 2.0\n" +
                    "Kotlin & Kotlin Coroutines — Apache License 2.0\n" +
                    "JUnit 4 — Eclipse Public License 1.0\n" +
                    "Robolectric — MIT License\n" +
                    "Google Truth — Apache License 2.0\n" +
                    "Gradle wrapper — Apache License 2.0\n\n" +
                    "বিস্তারিত নোটিশ: রিপোজিটরির docs/THIRD_PARTY_LICENSES.md ফাইলে।",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(title = "গোপনীয়তা") {
            Text(
                "কোনো অ্যানালিটিক্স, বিজ্ঞাপন বা ট্র্যাকিং নেই। ইন্টারনেট অনুমতি নেই। সব তথ্য আপনার ডিভাইসে থাকে। ব্যাকআপ ফাইল শুধু আপনি নিজে তৈরি করলে এবং নির্বাচিত জায়গায় সংরক্ষিত হয়।",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.container) }
    val deleted by vm.observeTrash().collectAsState(initial = emptyList())
    val archived by vm.observeArchived().collectAsState(initial = emptyList())
    val settings by vm.settings.collectAsState()
    var confirmDelete by remember { mutableStateOf<TxnEntity?>(null) }

    HisaabScaffold(title = "ট্র্যাশ ও সংরক্ষণাগার", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Text("মুছে ফেলা রেকর্ড", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        if (deleted.isEmpty()) Text("কোনো রেকর্ড নেই", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        deleted.forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(txn.description.ifBlank { txnTypeLabel(txn.type) }, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        txnTypeLabel(txn.type) + " · " + Dates.format(txn.dateMillis, settings.digitStyle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row {
                    TextButton(onClick = { vm.restore(txn.id) }) { Text("ফিরিয়ে আনুন") }
                    TextButton(onClick = { confirmDelete = txn }) { Text("স্থায়ী", color = ExpenseRed) }
                }
            }
            SoftDivider()
        }
        Spacer(Modifier.height(14.dp))
        Text("সংরক্ষণাগারভুক্ত লেনদেন", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        if (archived.isEmpty()) Text("কোনো রেকর্ড নেই", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        archived.forEach { txn ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(txn.description.ifBlank { txnTypeLabel(txn.type) }, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Dates.format(txn.dateMillis, settings.digitStyle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = { vm.restore(txn.id) }) { Text("ফিরিয়ে আনুন") }
            }
            SoftDivider()
        }
        Spacer(Modifier.height(24.dp))
    }

    confirmDelete?.let { txn ->
        ConfirmDialog(
            title = "স্থায়ীভাবে মুছে ফেলবেন?",
            body = "এটি স্থায়ীভাবে মুছে যাবে এবং ফেরানো যাবে না। নিশ্চিত?",
            confirmLabel = "স্থায়ীভাবে মুছুন",
            onConfirm = { vm.permanentDelete(txn.id) {} },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
fun ReconcileScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.container) }
    var report by remember { mutableStateOf<com.shohankhan.hisaabdesk.data.repository.ReportRepository.ReconcileReport?>(null) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        report = vm.reconcile()
        running = false
    }

    HisaabScaffold(title = "তথ্য যাচাই", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Text(
            "অ্যাকাউন্ট ব্যালেন্স, ট্রান্সফার, দোকানের বাকি ও ঋণের পরিশোধ মিলিয়ে দেখা হয়। কোনো অমিল থাকলে তা এখানে দেখানো হবে — আপনা-আপনি ঠিক করা হবে না।",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        if (running) {
            Text("যাচাই চলছে…")
        } else {
            val r = report
            if (r == null) {
                ErrorBanner("যাচাই করা যায়নি")
            } else {
                if (r.allOk) SuccessBanner("সব হিসাব মিলছে") else ErrorBanner("অমিল পাওয়া গেছে")
                Spacer(Modifier.height(8.dp))
                r.checks.forEach { check ->
                    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(check.name, style = MaterialTheme.typography.titleSmall)
                            Text(if (check.ok) "ঠিক আছে" else "অমিল", color = if (check.ok) IncomeGreen else ExpenseRed)
                        }
                        Text(check.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
