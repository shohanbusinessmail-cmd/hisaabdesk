package com.shohankhan.hisaabdesk.feature.shopping

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import com.shohankhan.hisaabdesk.data.repository.ShoppingRepository
import com.shohankhan.hisaabdesk.feature.transactions.methodLabel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShoppingViewModel(private val container: AppContainer) : ViewModel() {
    val digitStyle: StateFlow<Money.DigitStyle> = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Money.DigitStyle.BENGALI)

    val purchases = container.shoppingRepository.observePurchases(null, null, null, null, 100, 0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shops = container.shopRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = container.accountRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = container.categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun observePurchaseWithPaid(shopId: Long) = container.shoppingRepository.observeShopPurchasesWithPaid(shopId)
    fun observeShopTotal(shopId: Long) = container.database.shoppingDao().observeShopTotalPurchases(shopId)
    fun observePurchase(id: Long) = container.shoppingRepository.observePurchase(id)
    fun observeItems(id: Long) = container.shoppingRepository.observeItems(id)
    fun observePayments(purchaseId: Long) =
        container.transactionRepository.observeFiltered(shopId = null, limit = 50, offset = 0)
            .map { list -> list.filter { it.purchaseId == purchaseId } }

    suspend fun paymentsTotal(purchaseId: Long): Long =
        container.database.txnDao().sumPurchasePayments(purchaseId)

    fun observeShop(shopId: Long) = container.shopRepository.observeById(shopId)

    fun saveShop(name: String, keeper: String, phone: String, address: String, notes: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = container.shopRepository.save(
                ShopEntity(
                    name = name.trim(),
                    shopkeeperName = keeper,
                    phone = phone,
                    address = address,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now
                )
            )
            onDone(id)
        }
    }

    fun savePurchase(
        existingId: Long,
        shopId: Long,
        categoryId: Long?,
        dateMillis: Long,
        dueDateMillis: Long?,
        description: String,
        orderDiscount: Long,
        orderCharges: Long,
        items: List<PurchaseItemEntity>,
        paidAtPurchase: Long,
        paymentAccountId: Long?,
        method: PaymentMethod,
        receiptNo: String,
        note: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val purchase = PurchaseEntity(
                    id = existingId,
                    shopId = shopId,
                    categoryId = categoryId,
                    dateMillis = dateMillis,
                    dayKey = Dates.dayKey(dateMillis),
                    description = description,
                    grossPaisa = 0,
                    discountPaisa = orderDiscount,
                    chargesPaisa = orderCharges,
                    totalPaisa = 0,
                    dueDateMillis = dueDateMillis,
                    receiptNo = receiptNo,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )
                container.shoppingRepository.savePurchase(
                    purchase = purchase,
                    items = items,
                    paidAtPurchasePaisa = paidAtPurchase,
                    paymentAccountId = paymentAccountId,
                    paymentMethod = method,
                    existingId = existingId
                )
                onResult(null)
            } catch (e: ShoppingRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun addPayment(
        purchaseId: Long,
        amount: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        receiptNo: String,
        note: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                container.shoppingRepository.addPayment(purchaseId, amount, accountId, method, dateMillis, receiptNo, note)
                onResult(null)
            } catch (e: ShoppingRepository.ValidationException) {
                onResult(e.message)
            } catch (e: Exception) {
                onResult("পরিশোধ সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun setArchived(purchaseId: Long, archived: Boolean) {
        viewModelScope.launch { container.shoppingRepository.setArchived(purchaseId, archived) }
    }
}

@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    onNewPurchase: () -> Unit,
    onOpenPurchase: (Long) -> Unit,
    onOpenShops: () -> Unit
) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val purchases by vm.purchases.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    val shops by vm.shops.collectAsState()

    HisaabScaffold(title = "কেনাকাটা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNewPurchase, modifier = Modifier.weight(1f)) { Text("নতুন কেনাকাটা") }
            OutlinedButton(onClick = onOpenShops, modifier = Modifier.weight(1f)) { Text("দোকানসমূহ") }
        }
        Spacer(Modifier.height(10.dp))
        if (purchases.isEmpty()) {
            EmptyState("কোনো কেনাকাটা নেই", "পণ্যের তালিকাসহ কেনাকাটা যোগ করুন", actionLabel = "যোগ করুন", onAction = onNewPurchase)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(purchases, key = { it.id }) { p ->
                    val shopName = shops.firstOrNull { it.id == p.shopId }?.name ?: "দোকান"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPurchase(p.id) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(shopName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                Dates.format(p.dateMillis, digit) + if (p.description.isNotBlank()) " · ${p.description}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        Text(Money.format(p.totalPaisa, digit), style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

data class ItemDraft(
    val name: String,
    val quantity: String,
    val unit: String,
    val unitPrice: String,
    val discount: String,
    val charge: String
)

@Composable
fun PurchaseFormScreen(existingId: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val shops by vm.shops.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val digit by vm.digitStyle.collectAsState()

    var shopId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var description by remember { mutableStateOf("") }
    var orderDiscount by remember { mutableStateOf("") }
    var orderCharges by remember { mutableStateOf("") }
    var paidAtPurchase by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var receiptNo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(existingId != 0L) }
    var newShopName by remember { mutableStateOf("") }
    var showNewShop by remember { mutableStateOf(false) }

    val items = remember { mutableStateListOf(ItemDraft("", "1", "", "", "", "")) }

    LaunchedEffect(existingId) {
        if (existingId != 0L) {
            val p = vm.observePurchase(existingId).first()
            val its = vm.observeItems(existingId).first()
            if (p != null) {
                shopId = p.shopId
                categoryId = p.categoryId
                date = Dates.localDateOf(p.dateMillis)
                dueDate = p.dueDateMillis?.let { Dates.localDateOf(it) }
                description = p.description
                orderDiscount = if (p.discountPaisa > 0) Money.format(p.discountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                orderCharges = if (p.chargesPaisa > 0) Money.format(p.chargesPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                receiptNo = p.receiptNo
                note = p.note
                val paid = vm.paymentsTotal(existingId)
                paidAtPurchase = if (paid > 0) Money.format(paid, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
            }
            if (its.isNotEmpty()) {
                items.clear()
                its.forEach { item ->
                    items.add(
                        ItemDraft(
                            name = item.name,
                            quantity = Money.format(item.quantityMilli / 1000, Money.DigitStyle.ENGLISH, withSymbol = false),
                            unit = item.unit,
                            unitPrice = Money.format(item.unitPricePaisa, Money.DigitStyle.ENGLISH, withSymbol = false),
                            discount = if (item.discountPaisa > 0) Money.format(item.discountPaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else "",
                            charge = if (item.chargePaisa > 0) Money.format(item.chargePaisa, Money.DigitStyle.ENGLISH, withSymbol = false) else ""
                        )
                    )
                }
            }
            loading = false
        }
        if (shopId == null && shops.isNotEmpty()) shopId = shops.first().id
        if (accountId == null && accounts.isNotEmpty()) accountId = accounts.first().id
    }

    HisaabScaffold(title = if (existingId == 0L) "নতুন কেনাকাটা" else "কেনাকাটা সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (loading) { Text("লোড হচ্ছে…"); return@HisaabScaffold }
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }

        if (existingId != 0L) {
            SuccessBanner("এই কেনাকাটায় পরিশোধ আছে। মোট পরিবর্তন করলে বাকি পুনরায় হিসাব হবে।")
            Spacer(Modifier.height(8.dp))
        }

        DropdownField(
            label = "দোকান",
            options = shops,
            selected = shops.firstOrNull { it.id == shopId },
            optionLabel = { it.name },
            onSelected = { shopId = it.id }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showNewShop = !showNewShop }) { Text("+ নতুন দোকান যোগ করুন") }
        }
        if (showNewShop) {
            LabeledField("নতুন দোকানের নাম", newShopName, { newShopName = it })
            TextButton(onClick = {
                if (newShopName.isNotBlank()) {
                    vm.saveShop(newShopName, "", "", "", "") { newId ->
                        shopId = newId
                        newShopName = ""
                        showNewShop = false
                    }
                }
            }) { Text("দোকান সংরক্ষণ") }
        }
        Spacer(Modifier.height(6.dp))
        val shopCats = categories.filter { it.kind.name == "SHOPPING" || it.kind.name == "EXPENSE" }
        DropdownField(
            label = "ক্যাটাগরি (ঐচ্ছিক)",
            options = shopCats,
            selected = shopCats.firstOrNull { it.id == categoryId },
            optionLabel = { it.name },
            onSelected = { categoryId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DateField("কেনাকাটার তারিখ", date, { date = it })
        Spacer(Modifier.height(8.dp))
        DateField("বাকি পরিশোধের মেয়াদ (ঐচ্ছিক)", dueDate, { dueDate = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("বর্ণনা (ঐচ্ছিক)", description, { description = it })

        Spacer(Modifier.height(12.dp))
        Text("পণ্যের তালিকা", style = MaterialTheme.typography.titleSmall)
        items.forEachIndexed { index, draft ->
            SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("পণ্য ${Money.toBengaliDigits((index + 1).toString())}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    TextButton(onClick = { if (items.size > 1) items.removeAt(index) }) { Text("সরান") }
                }
                LabeledField("পণ্যের নাম", draft.name, { items[index] = draft.copy(name = it) })
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        LabeledField("পরিমাণ", draft.quantity, { items[index] = draft.copy(quantity = it) })
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        LabeledField("একক", draft.unit, { items[index] = draft.copy(unit = it) })
                    }
                }
                MoneyField("একক দাম", draft.unitPrice, { items[index] = draft.copy(unitPrice = it) }, digit)
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        MoneyField("ছাড়", draft.discount, { items[index] = draft.copy(discount = it) }, digit)
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        MoneyField("চার্জ", draft.charge, { items[index] = draft.copy(charge = it) }, digit)
                    }
                }
            }
        }
        TextButton(onClick = { items.add(ItemDraft("", "1", "", "", "", "")) }) { Text("+ পণ্য যোগ করুন") }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                MoneyField("পাইকারি ছাড়", orderDiscount, { orderDiscount = it }, digit)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                MoneyField("অতিরিক্ত চার্জ", orderCharges, { orderCharges = it }, digit)
            }
        }
        Spacer(Modifier.height(8.dp))
        MoneyField("কেনার সময় পরিশোধ", paidAtPurchase, { paidAtPurchase = it }, digit)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পরিশোধের অ্যাকাউন্ট",
            options = accounts,
            selected = accounts.firstOrNull { it.id == accountId },
            optionLabel = { it.name },
            onSelected = { accountId = it.id }
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "পরিশোধ পদ্ধতি",
            options = PaymentMethod.entries,
            selected = method,
            optionLabel = { methodLabel(it) },
            onSelected = { method = it }
        )
        Spacer(Modifier.height(8.dp))
        LabeledField("রসিদ নম্বর (ঐচ্ছিক)", receiptNo, { receiptNo = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("নোট (ঐচ্ছিক)", note, { note = it }, singleLine = false)
        Spacer(Modifier.height(8.dp))
        Text(
            "ছাড় প্রয়োগের নিয়ম: প্রতিটি পণ্যের ছাড় লাইন মোট থেকে বাদ পড়ে; পাইকারি ছাড় ও অতিরিক্ত চার্জ পরিশোধযোগ্য মোটে যোগ/বিয়োগ হয়।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (shopId == null) { error = "দোকান নির্বাচন করুন"; return@Button }
                val parsed = mutableListOf<PurchaseItemEntity>()
                var lineError: String? = null
                items.forEach { draft ->
                    if (draft.name.isBlank()) { lineError = "প্রতিটি পণ্যের নাম দিন"; return@forEach }
                    val qty = draft.quantity.replace(",", "").toBigDecimalOrNull()
                    val unitPrice = Money.parsePaisa(draft.unitPrice)
                    val lineDiscount = if (draft.discount.isBlank()) 0L else Money.parsePaisa(draft.discount)
                    val lineCharge = if (draft.charge.isBlank()) 0L else Money.parsePaisa(draft.charge)
                    if (qty == null || qty <= java.math.BigDecimal.ZERO) { lineError = "সঠিক পরিমাণ লিখুন"; return@forEach }
                    if (unitPrice == null) { lineError = "সঠিক একক দাম লিখুন"; return@forEach }
                    if (lineDiscount == null || lineCharge == null) { lineError = "সঠিক ছাড়/চার্জ লিখুন"; return@forEach }
                    val quantityMilli = qty.multiply(java.math.BigDecimal(1000)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    val lineTotal = com.shohankhan.hisaabdesk.domain.shopping.PurchaseCalculator.lineTotal(quantityMilli, unitPrice)
                    parsed.add(
                        PurchaseItemEntity(
                            purchaseId = existingId,
                            name = draft.name.trim(),
                            quantityMilli = quantityMilli,
                            unit = draft.unit,
                            unitPricePaisa = unitPrice,
                            lineTotalPaisa = lineTotal,
                            discountPaisa = lineDiscount,
                            chargePaisa = lineCharge
                        )
                    )
                }
                if (lineError != null) { error = lineError; return@Button }
                val totalDiscount = if (orderDiscount.isBlank()) 0L else Money.parsePaisa(orderDiscount)
                val totalCharges = if (orderCharges.isBlank()) 0L else Money.parsePaisa(orderCharges)
                val paid = if (paidAtPurchase.isBlank()) 0L else Money.parsePaisa(paidAtPurchase)
                if (totalDiscount == null || totalCharges == null || paid == null) {
                    error = "সঠিক পরিমাণ লিখুন"
                    return@Button
                }
                if (paid > 0 && accountId == null) { error = "পরিশোধের অ্যাকাউন্ট নির্বাচন করুন"; return@Button }
                if (paid > 0 && existingId != 0L) {
                    error = "এই কেনাকাটায় আগে থেকেই পরিশোধ আছে — নতুন পরিশোধ আলাদা ‘পরিশোধ যোগ করুন’ থেকে দিন"
                    return@Button
                }
                error = null
                vm.savePurchase(
                    existingId = existingId,
                    shopId = shopId!!,
                    categoryId = categoryId,
                    dateMillis = Dates.millisOf(date),
                    dueDateMillis = dueDate?.let { Dates.millisOf(it) },
                    description = description,
                    orderDiscount = totalDiscount,
                    orderCharges = totalCharges,
                    items = parsed,
                    paidAtPurchase = paid,
                    paymentAccountId = accountId,
                    method = method,
                    receiptNo = receiptNo,
                    note = note,
                    onResult = { err ->
                        if (err == null) onSaved() else error = err
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PurchaseDetailScreen(purchaseId: Long, onBack: () -> Unit, onEdit: () -> Unit, onAddPayment: () -> Unit) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val purchase by vm.observePurchase(purchaseId).collectAsState(initial = null)
    val items by vm.observeItems(purchaseId).collectAsState(initial = emptyList())
    val digit by vm.digitStyle.collectAsState()
    val shops by vm.shops.collectAsState()
    var paid by remember { mutableStateOf(0L) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(purchaseId) { paid = vm.paymentsTotal(purchaseId) }

    val p = purchase
    HisaabScaffold(title = "কেনাকাটার বিস্তারিত", onBack = onBack) {
        if (p == null) { EmptyState("কেনাকাটা পাওয়া যায়নি"); return@HisaabScaffold }
        Spacer(Modifier.height(8.dp))
        SectionCard(title = shops.firstOrNull { it.id == p.shopId }?.name ?: "দোকান") {
            KeyValueRow("তারিখ", Dates.format(p.dateMillis, digit))
            KeyValueRow("মোট দাম", Money.format(p.grossPaisa, digit))
            KeyValueRow("ছাড়", Money.format(p.discountPaisa, digit))
            KeyValueRow("অতিরিক্ত চার্জ", Money.format(p.chargesPaisa, digit))
            KeyValueRow("পরিশোধযোগ্য", Money.format(p.totalPaisa, digit), WarningAmber)
            KeyValueRow("পরিশোধিত", Money.format(paid, digit), IncomeGreen)
            KeyValueRow("বাকি", Money.format(p.totalPaisa - paid, digit), if (p.totalPaisa - paid > 0) ExpenseRed else IncomeGreen)
            if (p.dueDateMillis != null) KeyValueRow("বাকির মেয়াদ", Dates.format(p.dueDateMillis!!, digit))
            if (p.receiptNo.isNotBlank()) KeyValueRow("রসিদ", p.receiptNo)
            if (p.description.isNotBlank()) KeyValueRow("বর্ণনা", p.description)
            if (p.note.isNotBlank()) KeyValueRow("নোট", p.note)
            KeyValueRow(
                "অবস্থা",
                when {
                    paid >= p.totalPaisa -> "সম্পূর্ণ পরিশোধিত"
                    paid > 0 -> "আংশিক পরিশোধিত"
                    else -> "বাকিতে কেনা"
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        SectionCard(title = "পণ্যের তালিকা") {
            items.forEach { item ->
                KeyValueRow(
                    "${item.name} × ${Money.format(item.quantityMilli / 1000, Money.DigitStyle.ENGLISH, withSymbol = false)} ${item.unit}",
                    Money.format(item.lineTotalPaisa, digit)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (p.totalPaisa - paid > 0) {
            Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) { Text("পরিশোধ যোগ করুন") }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("সংরক্ষণাগারে রাখুন", color = ExpenseRed)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (showDelete) {
        ConfirmDialog(
            title = "কেনাকাটা সংরক্ষণাগারে রাখবেন?",
            body = "রিপোর্ট থেকে সরে যাবে, তবে রেকর্ড মুছে যাবে না।",
            onConfirm = { vm.setArchived(purchaseId, true); onBack() },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
fun PurchasePaymentScreen(purchaseId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val purchase by vm.observePurchase(purchaseId).collectAsState(initial = null)
    val accounts by vm.accounts.collectAsState()
    val digit by vm.digitStyle.collectAsState()
    var paid by remember { mutableStateOf(0L) }
    var amount by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var receipt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(purchaseId) {
        paid = vm.paymentsTotal(purchaseId)
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    val outstanding = (purchase?.totalPaisa ?: 0L) - paid

    HisaabScaffold(title = "পরিশোধ যোগ করুন", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        SectionCard {
            KeyValueRow("মোট", Money.format(purchase?.totalPaisa ?: 0L, digit))
            KeyValueRow("পরিশোধিত", Money.format(paid, digit))
            KeyValueRow("বাকি", Money.format(outstanding, digit), ExpenseRed)
        }
        Spacer(Modifier.height(10.dp))
        MoneyField("পরিশোধের পরিমাণ", amount, { amount = it }, digit)
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
                val paisa = Money.parsePaisa(amount)
                when {
                    paisa == null || paisa <= 0 -> error = "সঠিক পরিমাণ লিখুন"
                    paisa > outstanding -> error = "পরিশোধ বাকি পরিমাণের বেশি হতে পারে না"
                    accountId == null -> error = "অ্যাকাউন্ট নির্বাচন করুন"
                    else -> {
                        error = null
                        vm.addPayment(purchaseId, paisa, accountId, method, Dates.millisOf(date), receipt, note) { err ->
                            if (err == null) onDone() else error = err
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("পরিশোধ সংরক্ষণ") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ShopListScreen(onBack: () -> Unit, onOpenShop: (Long) -> Unit) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val shops by vm.shops.collectAsState()
    var newName by remember { mutableStateOf("") }

    HisaabScaffold(title = "দোকানসমূহ", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        if (shops.isEmpty()) {
            EmptyState("কোনো দোকান নেই", "কেনাকাটার সময় দোকান যোগ করুন")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shops, key = { it.id }) { shop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenShop(shop.id) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(shop.name, style = MaterialTheme.typography.titleSmall)
                            if (shop.shopkeeperName.isNotBlank()) {
                                Text(shop.shopkeeperName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Text("বিস্তারিত →", color = MaterialTheme.colorScheme.primary)
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LabeledField("নতুন দোকানের নাম", newName, { newName = it })
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (newName.isNotBlank()) {
                    vm.saveShop(newName, "", "", "", "") { newName = "" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("দোকান যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ShopDetailScreen(shopId: Long, onBack: () -> Unit, onOpenPurchase: (Long) -> Unit) {
    val vm: ShoppingViewModel = appViewModel { ShoppingViewModel(it.container) }
    val shop by vm.observeShop(shopId).collectAsState(initial = null)
    val purchases by vm.observePurchaseWithPaid(shopId).collectAsState(initial = emptyList())
    val totalPurchases by vm.observeShopTotal(shopId).collectAsState(initial = 0L)
    val digit by vm.digitStyle.collectAsState()

    val totalPaid = purchases.sumOf { it.paidPaisa }
    val outstanding = totalPurchases - totalPaid
    val now = System.currentTimeMillis()
    val overdue = purchases.filter { (it.totalPaisa - it.paidPaisa) > 0 && it.dueDateMillis != null && it.dueDateMillis < now }
        .sumOf { it.totalPaisa - it.paidPaisa }

    HisaabScaffold(title = shop?.name ?: "দোকান", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        SectionCard(title = "দোকানের সারসংক্ষেপ") {
            KeyValueRow("মোট কেনাকাটা", Money.format(totalPurchases, digit))
            KeyValueRow("মোট পরিশোধ", Money.format(totalPaid, digit), IncomeGreen)
            KeyValueRow("বাকি", Money.format(outstanding, digit), if (outstanding > 0) ExpenseRed else IncomeGreen)
            KeyValueRow("মেয়াদ পেরিয়ে যাওয়া বাকি", Money.format(overdue, digit), WarningAmber)
            KeyValueRow("কেনাকাটার সংখ্যা", Money.toBengaliDigits(purchases.size.toString()))
            KeyValueRow(
                "সর্বশেষ পরিশোধ",
                purchases.maxByOrNull { it.dateMillis }?.let { Dates.format(it.dateMillis, digit) } ?: "—"
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("কেনাকাটার ইতিহাস", style = MaterialTheme.typography.titleSmall)
        SoftDivider()
        purchases.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPurchase(row.id) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Dates.format(row.dateMillis, digit), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "বাকি " + Money.format((row.totalPaisa - row.paidPaisa).coerceAtLeast(0), digit),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(Money.format(row.totalPaisa, digit))
            }
        }
        if (purchases.isEmpty()) EmptyState("কোনো কেনাকাটা নেই")
        Spacer(Modifier.height(24.dp))
    }
}
