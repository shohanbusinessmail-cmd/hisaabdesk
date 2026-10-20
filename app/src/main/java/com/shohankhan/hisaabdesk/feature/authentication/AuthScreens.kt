package com.shohankhan.hisaabdesk.feature.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabdesk.core.formatting.Money
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.ErrorBanner
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.SuccessBanner

@Composable
fun PinCreateScreen(
    title: String,
    body: String,
    onBack: (() -> Unit)? = null,
    onDone: () -> Unit
) {
    val vm: SecurityViewModel = appViewModel { SecurityViewModel(it.container) }
    val ui by vm.ui.collectAsState()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    HisaabScaffold(title = title, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        if (ui.error != null) {
            ErrorBanner(ui.error!!)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("নতুন পিন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirm = it },
            label = { Text("পিন নিশ্চিত করুন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { vm.createPin(pin, confirm) { onDone() } },
            enabled = pin.length >= 4 && confirm.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
    }
}

@Composable
fun PinChangeScreen(onBack: () -> Unit) {
    val vm: SecurityViewModel = appViewModel { SecurityViewModel(it.container) }
    val ui by vm.ui.collectAsState()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    HisaabScaffold(title = "পিন পরিবর্তন", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (ui.error != null) {
            ErrorBanner(ui.error!!)
            Spacer(Modifier.height(8.dp))
        }
        if (ui.message != null) {
            SuccessBanner(ui.message!!)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = current,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) current = it },
            label = { Text("বর্তমান পিন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = new,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) new = it },
            label = { Text("নতুন পিন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirm = it },
            label = { Text("নতুন পিন নিশ্চিত করুন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { vm.changePin(current, new, confirm) { current = ""; new = ""; confirm = "" } },
            enabled = current.isNotEmpty() && new.length >= 4 && confirm.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text("পিন পরিবর্তন করুন") }
    }
}

@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val vm: SecurityViewModel = appViewModel { SecurityViewModel(it.container) }
    val ui by vm.ui.collectAsState()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("হিসাবডেস্ক", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text("পিন দিয়ে প্রবেশ করুন", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        if (ui.error != null) {
            ErrorBanner(ui.error!!)
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("পিন লিখুন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.verify(pin) { pin = ""; onUnlocked() } },
            enabled = pin.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) { Text("প্রবেশ করুন") }
    }
}
