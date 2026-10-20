package com.shohankhan.hisaabdesk.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.DropdownField
import com.shohankhan.hisaabdesk.ui.components.ErrorBanner
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.MoneyField
import kotlinx.coroutines.launch

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    var error: String? = null
        private set

    fun createFirstAccount(
        name: String,
        type: AccountType,
        openingPaisa: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                container.accountRepository.createWithOpeningBalance(
                    AccountEntity(
                        name = name,
                        type = type,
                        openingBalancePaisa = openingPaisa,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                onDone()
            } catch (e: Exception) {
                error = "অ্যাকাউন্ট তৈরি করা যায়নি"
            }
        }
    }

    fun seedCategories(onDone: () -> Unit) {
        viewModelScope.launch {
            container.categoryRepository.seedSuggestionsIfEmpty()
            onDone()
        }
    }

    fun skipToDone(onDone: () -> Unit) = onDone()
}

@Composable
fun OnboardingWelcomeScreen(onNext: () -> Unit) {
    HisaabScaffold(title = "স্বাগতম") {
        Spacer(Modifier.height(20.dp))
        Text("হিসাবডেস্ক", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text("ব্যক্তিগত অর্থ ও হিসাব ব্যবস্থাপনা", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "আপনার আয়-ব্যয়, ধার-দেনা, সঞ্চয় ও ঋণের পূর্ণ হিসাব রাখুন এক জায়গায়। সব তথ্য থাকে আপনার নিজের ফোনে — ইন্টারনেট বা ক্লাউড অ্যাকাউন্টের প্রয়োজন নেই।",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("শুরু করুন") }
    }
}

@Composable
fun OnboardingPrivacyScreen(onNext: () -> Unit, onBack: () -> Unit) {
    HisaabScaffold(title = "গোপনীয়তা", onBack = onBack) {
        Spacer(Modifier.height(16.dp))
        Text(
            "হিসাবডেস্ক অফলাইনে কাজ করে। আপনার আর্থিক তথ্য আপনার ডিভাইস ছাড়া কোথাও পাঠানো হয় না। কোনো অ্যানালিটিক্স, বিজ্ঞাপন বা ট্র্যাকিং নেই।",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "অ্যাপ খোলার জন্য একটি নিরাপদ পিন তৈরি করবেন। পিন কোথাও সাধারণ টেক্সট হিসেবে জমা থাকে না — এটি একটি শক্তিশালী হ্যাশ হিসেবে সুরক্ষিত থাকে।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "নিরাপত্তার জন্য পিন ভুলে গেলে কোনো গোপন উপায়ে অ্যাপ খোলা সম্ভব নয়। তাই নিয়মিত এনক্রিপ্টেড ব্যাকআপ তৈরি করে নিরাপদ জায়গায় রাখুন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("পরবর্তী") }
    }
}

@Composable
fun OnboardingAccountScreen(onNext: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    val vm: OnboardingViewModel = appViewModel { OnboardingViewModel(it.container) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var opening by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    HisaabScaffold(title = "প্রথম অ্যাকাউন্ট", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Text(
            "যে অ্যাকাউন্ট বা মানিব্যাগে টাকা রাখেন তা যোগ করুন। প্রারম্ভিক ব্যালেন্স শুরুর ব্যালেন্স হিসেবে সংরক্ষিত হবে — কোনো কৃত্রিম আয় তৈরি হবে না।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        showError?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("অ্যাকাউন্টের নাম") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        DropdownField(
            label = "অ্যাকাউন্টের ধরন",
            options = AccountType.entries,
            selected = type,
            optionLabel = { accountTypeLabel(it) },
            onSelected = { type = it }
        )
        Spacer(Modifier.height(10.dp))
        MoneyField(
            label = "শুরুর ব্যালেন্স (ঐচ্ছিক)",
            value = opening,
            onValueChange = { opening = it },
            digitStyle = Money.DigitStyle.BENGALI,
            supportingText = "শুরুর ব্যালেন্স সংরক্ষিত হবে, আয় হিসেবে গণনা হবে না"
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val paisa = if (opening.isBlank()) 0L else Money.parsePaisa(opening)
                if (name.isBlank()) {
                    showError = "অ্যাকাউন্টের নাম দিন"
                } else if (paisa == null) {
                    showError = "সঠিক পরিমাণ লিখুন"
                } else {
                    showError = null
                    vm.createFirstAccount(name.trim(), type, paisa) { onNext() }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ ও পরবর্তী") }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("এখন বাদ দিন") }
    }
}

@Composable
fun OnboardingCategoriesScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val vm: OnboardingViewModel = appViewModel { OnboardingViewModel(it.container) }
    var useSuggestions by remember { mutableStateOf(true) }

    HisaabScaffold(title = "ক্যাটাগরি", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Text(
            "প্রস্তাবিত ক্যাটাগরিগুলো (বেতন, খাবার, যাতায়াত ইত্যাদি) এডিটেবল সাজেশন হিসেবে তৈরি হতে পারে। এগুলো কোনো লেনদেন নয় এবং পরে যেকোনো সময় পরিবর্তন করা যাবে।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Switch(checked = useSuggestions, onCheckedChange = { useSuggestions = it })
            Spacer(Modifier.height(0.dp))
            Text("প্রস্তাবিত ক্যাটাগরি তৈরি করুন", modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (useSuggestions) vm.seedCategories { onNext() } else onNext()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("পরবর্তী") }
    }
}

@Composable
fun OnboardingDoneScreen(onFinish: () -> Unit) {
    HisaabScaffold(title = "সম্পন্ন") {
        Spacer(Modifier.height(24.dp))
        Text("সব তৈরি!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "আপনার হিসাবডেস্ক প্রস্তুত। এখন থেকে আয়-ব্যয়, ধার-দেনা, সঞ্চয় ও সব আর্থিক হিসাব রাখা শুরু করুন।",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("ড্যাশবোর্ডে যান") }
    }
}

fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "নগদ"
    AccountType.BANK -> "ব্যাংক অ্যাকাউন্ট"
    AccountType.MFS -> "মোবাইল ফাইন্যান্সিয়াল সার্ভিস"
    AccountType.WALLET -> "ডিজিটাল ওয়ালেট"
    AccountType.SAVINGS -> "সঞ্চয় অ্যাকাউন্ট"
    AccountType.OTHER -> "অন্যান্য"
}
