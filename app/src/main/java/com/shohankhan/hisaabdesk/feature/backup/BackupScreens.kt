package com.shohankhan.hisaabdesk.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.BuildConfig
import com.shohankhan.hisaabdesk.core.backup.BackupSummary
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.ConfirmDialog
import com.shohankhan.hisaabdesk.ui.components.ErrorBanner
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.KeyValueRow
import com.shohankhan.hisaabdesk.ui.components.LabeledField
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SuccessBanner
import com.shohankhan.hisaabdesk.ui.theme.ExpenseRed
import com.shohankhan.hisaabdesk.ui.theme.WarningAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class BackupViewModel(private val container: AppContainer) : ViewModel() {

    data class State(
        val working: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val pendingRestoreUri: Uri? = null,
        val summary: BackupSummary? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun export(uri: Uri, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, error = null, message = null)
            try {
                container.backupRepository.exportTo(uri, password.toCharArray(), BuildConfig.VERSION_NAME)
                _state.value = _state.value.copy(working = false, message = "ব্যাকআপ তৈরি হয়েছে")
            } catch (e: Exception) {
                _state.value = _state.value.copy(working = false, error = e.message ?: "ব্যাকআপ তৈরি করা যায়নি")
            }
        }
    }

    fun inspect(uri: Uri, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, error = null, message = null)
            val (data, result) = withContextIo { container.backupRepository.peek(uri, password.toCharArray()) }
            _state.value = if (data != null && result.ok) {
                _state.value.copy(working = false, pendingRestoreUri = uri, summary = result.summary, error = null)
            } else {
                _state.value.copy(
                    working = false,
                    pendingRestoreUri = null,
                    summary = null,
                    error = when {
                        result.errors.any { it.contains("wrong password") || it.contains("corrupted") } -> "পাসওয়ার্ড সঠিক নয় বা ফাইল ক্ষতিগ্রস্ত"
                        result.errors.any { it.contains("unsupported schema") } -> "এই সংস্করণের ব্যাকআপ সমর্থিত নয়"
                        result.errors.any { it.contains("not a HisaabDesk") } -> "ফাইলটি সঠিক হিসাবডেস্ক ব্যাকআপ নয়"
                        else -> "তথ্য যাচাই ব্যর্থ: " + (result.errors.firstOrNull() ?: "অজানা ত্রুটি")
                    }
                )
            }
        }
    }

    fun confirmRestore(password: String) {
        val uri = _state.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true, error = null)
            try {
                val summary = container.backupRepository.restore(uri, password.toCharArray())
                _state.value = _state.value.copy(
                    working = false,
                    pendingRestoreUri = null,
                    summary = summary,
                    message = "রিস্টোর সফল হয়েছে — ${DateFmt(summary.createdAtMillis)}"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    working = false,
                    pendingRestoreUri = null,
                    error = "রিস্টোর ব্যর্থ হয়েছে। বর্তমান তথ্য অক্ষত আছে। (${e.message})"
                )
            }
        }
    }

    fun cancelRestore() {
        _state.value = _state.value.copy(pendingRestoreUri = null, summary = null)
    }

    private fun DateFmt(millis: Long): String = com.shohankhan.hisaabdesk.core.formatting.Dates.format(
        millis, com.shohankhan.hisaabdesk.core.formatting.Money.DigitStyle.BENGALI
    )

    private suspend fun <T> withContextIo(block: () -> T): T =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, message = null)
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm: BackupViewModel = appViewModel { BackupViewModel(it.container) }
    val state by vm.state.collectAsState()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var confirmRestorePrompt by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && password.isNotEmpty()) vm.export(uri, password)
    }
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (restorePassword.isBlank()) localError = "ব্যাকআপ পাসওয়ার্ড দিন"
            else vm.inspect(uri, restorePassword)
        }
    }

    HisaabScaffold(title = "ব্যাকআপ ও রিস্টোর", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        val err = state.error ?: localError
        err?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        state.message?.let { SuccessBanner(it); Spacer(Modifier.height(8.dp)) }
        if (state.working) Text("প্রক্রিয়া চলছে…", color = WarningAmber)

        SectionCard(title = "এনক্রিপ্টেড ব্যাকআপ তৈরি") {
            Text(
                "সব হিসাব একটি সুরক্ষিত .hisaabdesk ফাইলে সংরক্ষণ হবে। ফাইলটি AES-256-GCM দিয়ে এনক্রিপ্ট হয়।",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            LabeledField(
                label = "ব্যাকআপ পাসওয়ার্ড",
                value = password,
                onValueChange = { password = it },
                supportingText = "কমপক্ষে ৮ অক্ষরের শক্তিশালী পাসওয়ার্ড দিন"
            )
            Spacer(Modifier.height(8.dp))
            LabeledField(
                label = "পাসওয়ার্ড নিশ্চিত করুন",
                value = confirm,
                onValueChange = { confirm = it }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "পাসওয়ার্ড ভুলে গেলে ব্যাকআপ ফাইল আর খোলা যাবে না। পাসওয়ার্ড নিরাপদ জায়গায় রাখুন।",
                style = MaterialTheme.typography.bodySmall,
                color = ExpenseRed
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    when {
                        password.length < 8 -> localError = "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে"
                        password != confirm -> localError = "পাসওয়ার্ড দুটি মিলছে না"
                        else -> {
                            localError = null
                            val name = "hisaabdesk-backup-${LocalDate.now()}.hisaabdesk"
                            createLauncher.launch(name)
                        }
                    }
                },
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("ফাইল সংরক্ষণ করুন") }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard(title = "ব্যাকআপ রিস্টোর") {
            Text(
                "সিস্টেম ফাইল পিকার থেকে .hisaabdesk ফাইল নির্বাচন করুন। রিস্টোর করলে বর্তমান তথ্য প্রতিস্থাপিত হবে।",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            LabeledField(
                label = "ব্যাকআপ পাসওয়ার্ড",
                value = restorePassword,
                onValueChange = { restorePassword = it }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    localError = null
                    openLauncher.launch(arrayOf("*/*"))
                },
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("ফাইল নির্বাচন করুন") }
        }

        state.summary?.let { summary ->
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "ব্যাকআপের সারসংক্ষেপ") {
                KeyValueRow("ব্যাকআপ তৈরি", Dates.format(summary.createdAtMillis, Money.DigitStyle.BENGALI))
                KeyValueRow("অ্যাপ সংস্করণ", summary.appVersion)
                KeyValueRow("অ্যাকাউন্ট", Money.toBengaliDigits(summary.accountCount.toString()))
                KeyValueRow("লেনদেন", Money.toBengaliDigits(summary.txnCount.toString()))
                KeyValueRow("কেনাকাটা", Money.toBengaliDigits(summary.purchaseCount.toString()))
                KeyValueRow("ঋণ", Money.toBengaliDigits(summary.loanCount.toString()))
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { confirmRestorePrompt = true },
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("এই ব্যাকআপ রিস্টোর করুন") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmRestorePrompt) {
        ConfirmDialog(
            title = "বর্তমান তথ্য প্রতিস্থাপিত হবে",
            body = "রিস্টোর করলে বর্তমান সব তথ্য মুছে ব্যাকআপের তথ্য বসবে। এটি ফেরানো যাবে না। আগে একটি ব্যাকআপ তৈরি করার পরামর্শ দেওয়া হচ্ছে।",
            confirmLabel = "রিস্টোর করুন",
            onConfirm = { vm.confirmRestore(restorePassword) },
            onDismiss = { confirmRestorePrompt = false }
        )
    }
}
