package com.shohankhan.hisaabdesk.feature.home

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SoftDivider
import com.shohankhan.hisaabdesk.ui.theme.TextSecondary

@Composable
fun MoreScreen(
    onShopping: () -> Unit,
    onContacts: () -> Unit,
    onDebts: () -> Unit,
    onReceivables: () -> Unit,
    onPayables: () -> Unit,
    onNgoLoans: () -> Unit,
    onBankLoans: () -> Unit,
    onSavings: () -> Unit,
    onBudgets: () -> Unit,
    onGoals: () -> Unit,
    onBills: () -> Unit,
    onCalendar: () -> Unit,
    onReports: () -> Unit,
    onCategories: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    HisaabScaffold(title = "আরও") {
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SectionCard(title = "কেনাকাটা ও মানুষ") {
                MoreLink("কেনাকাটা ও দোকান বাকি", onShopping)
                SoftDivider()
                MoreLink("ব্যক্তি/প্রতিষ্ঠান", onContacts)
                SoftDivider()
                MoreLink("ধার-দেনা", onDebts)
                SoftDivider()
                MoreLink("পাওনা", onReceivables)
                SoftDivider()
                MoreLink("দেনা", onPayables)
            }
            Spacer(Modifier.height(10.dp))
            SectionCard(title = "ঋণ ও সঞ্চয়") {
                MoreLink("এনজিও ঋণ", onNgoLoans)
                SoftDivider()
                MoreLink("ব্যাংক ঋণ", onBankLoans)
                SoftDivider()
                MoreLink("সঞ্চয় ও লক্ষ্য", onSavings)
            }
            Spacer(Modifier.height(10.dp))
            SectionCard(title = "পরিকল্পনা") {
                MoreLink("বাজেট", onBudgets)
                SoftDivider()
                MoreLink("আর্থিক লক্ষ্য", onGoals)
                SoftDivider()
                MoreLink("বিল ও নিয়মিত লেনদেন", onBills)
                SoftDivider()
                MoreLink("পেমেন্ট ক্যালেন্ডার", onCalendar)
            }
            Spacer(Modifier.height(10.dp))
            SectionCard(title = "রিপোর্ট ও সেটিংস") {
                MoreLink("রিপোর্ট", onReports)
                SoftDivider()
                MoreLink("ক্যাটাগরি", onCategories)
                SoftDivider()
                MoreLink("ব্যাকআপ ও রিস্টোর", onBackup)
                SoftDivider()
                MoreLink("সেটিংস", onSettings)
                SoftDivider()
                MoreLink("হিসাবডেস্ক সম্পর্কে", onAbout)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoreLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("→", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
    }
}
