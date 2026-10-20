package com.shohankhan.hisaabdesk.core.backup

import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.AttachmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.AuditEntryEntity
import com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetPeriod
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryKind
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.GoalEntity
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentStatus
import com.shohankhan.hisaabdesk.data.local.entity.InterestMethod
import com.shohankhan.hisaabdesk.data.local.entity.InterestPolicy
import com.shohankhan.hisaabdesk.data.local.entity.LendingEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanKind
import com.shohankhan.hisaabdesk.data.local.entity.PayableEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringKind
import com.shohankhan.hisaabdesk.data.local.entity.RecurringOccurrenceEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnStatus
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.core.formatting.Money
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned JSON backup payload (inside the encrypted container).
 *
 * schemaVersion 1 covers every persisted table plus settings. Validation is
 * strict: unknown versions are refused, duplicate IDs and broken references are
 * rejected BEFORE anything touches the live database.
 */
data class BackupData(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAtMillis: Long,
    val currency: String,
    val settingsJson: String,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val contacts: List<ContactEntity>,
    val shops: List<ShopEntity>,
    val purchases: List<PurchaseEntity>,
    val purchaseItems: List<PurchaseItemEntity>,
    val txns: List<TxnEntity>,
    val lendings: List<LendingEntity>,
    val borrowings: List<BorrowingEntity>,
    val receivables: List<ReceivableEntity>,
    val payables: List<PayableEntity>,
    val loans: List<LoanEntity>,
    val installments: List<InstallmentEntity>,
    val goals: List<GoalEntity>,
    val budgets: List<BudgetEntity>,
    val recurringItems: List<RecurringItemEntity>,
    val recurringOccurrences: List<RecurringOccurrenceEntity>,
    val attachments: List<AttachmentEntity>,
    val auditLog: List<AuditEntryEntity>
)

data class BackupValidationResult(
    val ok: Boolean,
    val errors: List<String>,
    val summary: BackupSummary
)

data class BackupSummary(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAtMillis: Long,
    val accountCount: Int,
    val txnCount: Int,
    val purchaseCount: Int,
    val loanCount: Int,
    val contactCount: Int
)

object BackupSerializer {
    const val SCHEMA_VERSION = 1
    const val FORMAT = "hisaabdesk-backup"

    private const val MAX_ENTITY_ROWS = 500_000
    private const val MAX_STRING = 10_000

    // ------------------------------------------------------------------
    // Serialize
    // ------------------------------------------------------------------
    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("schemaVersion", data.schemaVersion)
        root.put("appVersion", data.appVersion)
        root.put("createdAtMillis", data.createdAtMillis)
        root.put("currency", data.currency)
        root.put("settings", JSONObject(data.settingsJson.ifEmpty { "{}" }))

        root.put("accounts", JSONArray(data.accounts.map { it.toJson() }))
        root.put("categories", JSONArray(data.categories.map { it.toJson() }))
        root.put("contacts", JSONArray(data.contacts.map { it.toJson() }))
        root.put("shops", JSONArray(data.shops.map { it.toJson() }))
        root.put("purchases", JSONArray(data.purchases.map { it.toJson() }))
        root.put("purchaseItems", JSONArray(data.purchaseItems.map { it.toJson() }))
        root.put("txns", JSONArray(data.txns.map { it.toJson() }))
        root.put("lendings", JSONArray(data.lendings.map { it.toJson() }))
        root.put("borrowings", JSONArray(data.borrowings.map { it.toJson() }))
        root.put("receivables", JSONArray(data.receivables.map { it.toJson() }))
        root.put("payables", JSONArray(data.payables.map { it.toJson() }))
        root.put("loans", JSONArray(data.loans.map { it.toJson() }))
        root.put("installments", JSONArray(data.installments.map { it.toJson() }))
        root.put("goals", JSONArray(data.goals.map { it.toJson() }))
        root.put("budgets", JSONArray(data.budgets.map { it.toJson() }))
        root.put("recurringItems", JSONArray(data.recurringItems.map { it.toJson() }))
        root.put("recurringOccurrences", JSONArray(data.recurringOccurrences.map { it.toJson() }))
        root.put("attachments", JSONArray(data.attachments.map { it.toJson() }))
        root.put("auditLog", JSONArray(data.auditLog.map { it.toJson() }))
        return root.toString()
    }

    // ------------------------------------------------------------------
    // Deserialize + validate
    // ------------------------------------------------------------------
    fun parseAndValidate(jsonText: String): Pair<BackupData?, BackupValidationResult> {
        val errors = mutableListOf<String>()
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            return null to BackupValidationResult(false, listOf("invalid JSON"), emptySummary())
        }

        if (root.optString("format") != FORMAT) {
            errors.add("unrecognized backup format")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != SCHEMA_VERSION) {
            return null to BackupValidationResult(
                false,
                listOf("unsupported schema version: $schemaVersion"),
                emptySummary()
            )
        }

        fun <T> list(name: String, parse: (JSONObject) -> T): List<T> {
            val arr = root.optJSONArray(name) ?: return emptyList()
            if (arr.length() > MAX_ENTITY_ROWS) {
                errors.add("$name has too many rows")
                return emptyList()
            }
            val out = ArrayList<T>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: run { errors.add("$name[$i] invalid"); continue }
                try {
                    out.add(parse(o))
                } catch (e: Exception) {
                    errors.add("$name[$i] parse error: ${e.message}")
                }
            }
            return out
        }

        val accounts = list("accounts") { it.toAccount() }
        val categories = list("categories") { it.toCategory() }
        val contacts = list("contacts") { it.toContact() }
        val shops = list("shops") { it.toShop() }
        val purchases = list("purchases") { it.toPurchase() }
        val purchaseItems = list("purchaseItems") { it.toPurchaseItem() }
        val txns = list("txns") { it.toTxn() }
        val lendings = list("lendings") { it.toLending() }
        val borrowings = list("borrowings") { it.toBorrowing() }
        val receivables = list("receivables") { it.toReceivable() }
        val payables = list("payables") { it.toPayable() }
        val loans = list("loans") { it.toLoan() }
        val installments = list("installments") { it.toInstallment() }
        val goals = list("goals") { it.toGoal() }
        val budgets = list("budgets") { it.toBudget() }
        val recurringItems = list("recurringItems") { it.toRecurringItem() }
        val recurringOccurrences = list("recurringOccurrences") { it.toOccurrence() }
        val attachments = list("attachments") { it.toAttachment() }
        val auditLog = list("auditLog") { it.toAudit() }

        val summary = BackupSummary(
            schemaVersion = schemaVersion,
            appVersion = root.optString("appVersion", ""),
            createdAtMillis = root.optLong("createdAtMillis", 0),
            accountCount = accounts.size,
            txnCount = txns.size,
            purchaseCount = purchases.size,
            loanCount = loans.size,
            contactCount = contacts.size
        )

        if (errors.isNotEmpty()) {
            return null to BackupValidationResult(false, errors, summary)
        }

        // ---- structural validation ----
        fun checkUniqueIds(values: List<Long>, label: String) {
            if (values.size != values.toSet().size) errors.add("duplicate ids in $label")
        }
        checkUniqueIds(accounts.map { it.id }, "accounts")
        checkUniqueIds(categories.map { it.id }, "categories")
        checkUniqueIds(contacts.map { it.id }, "contacts")
        checkUniqueIds(shops.map { it.id }, "shops")
        checkUniqueIds(purchases.map { it.id }, "purchases")
        checkUniqueIds(txns.map { it.id }, "txns")
        checkUniqueIds(lendings.map { it.id }, "lendings")
        checkUniqueIds(borrowings.map { it.id }, "borrowings")
        checkUniqueIds(receivables.map { it.id }, "receivables")
        checkUniqueIds(payables.map { it.id }, "payables")
        checkUniqueIds(loans.map { it.id }, "loans")
        checkUniqueIds(installments.map { it.id }, "installments")
        checkUniqueIds(goals.map { it.id }, "goals")
        checkUniqueIds(budgets.map { it.id }, "budgets")
        checkUniqueIds(recurringItems.map { it.id }, "recurringItems")

        val accountIds = accounts.map { it.id }.toSet()
        val categoryIds = categories.map { it.id }.toSet()
        val contactIds = contacts.map { it.id }.toSet()
        val shopIds = shops.map { it.id }.toSet()
        val purchaseIds = purchases.map { it.id }.toSet()
        val lendingIds = lendings.map { it.id }.toSet()
        val borrowingIds = borrowings.map { it.id }.toSet()
        val receivableIds = receivables.map { it.id }.toSet()
        val payableIds = payables.map { it.id }.toSet()
        val loanIds = loans.map { it.id }.toSet()
        val installmentIds = installments.map { it.id }.toSet()
        val goalIds = goals.map { it.id }.toSet()

        fun validRef(id: Long?, ids: Set<Long>, label: String, row: String) {
            if (id != null && id !in ids) errors.add("$row references missing $label id=$id")
        }

        for (p in purchases) {
            if (p.shopId !in shopIds) errors.add("purchase ${p.id} references missing shop ${p.shopId}")
            validRef(p.categoryId, categoryIds, "category", "purchase ${p.id}")
            checkMoney(p.totalPaisa, "purchase ${p.id} total", errors)
        }
        for (item in purchaseItems) {
            if (item.purchaseId !in purchaseIds) errors.add("purchaseItem ${item.id} references missing purchase ${item.purchaseId}")
        }
        for (t in txns) {
            validRef(t.accountId, accountIds, "account", "txn ${t.id}")
            validRef(t.counterAccountId, accountIds, "account", "txn ${t.id}")
            validRef(t.categoryId, categoryIds, "category", "txn ${t.id}")
            validRef(t.contactId, contactIds, "contact", "txn ${t.id}")
            validRef(t.shopId, shopIds, "shop", "txn ${t.id}")
            validRef(t.purchaseId, purchaseIds, "purchase", "txn ${t.id}")
            validRef(t.lendingId, lendingIds, "lending", "txn ${t.id}")
            validRef(t.borrowingId, borrowingIds, "borrowing", "txn ${t.id}")
            validRef(t.receivableId, receivableIds, "receivable", "txn ${t.id}")
            validRef(t.payableId, payableIds, "payable", "txn ${t.id}")
            validRef(t.loanId, loanIds, "loan", "txn ${t.id}")
            validRef(t.installmentId, installmentIds, "installment", "txn ${t.id}")
            validRef(t.goalId, goalIds, "goal", "txn ${t.id}")
            checkMoney(t.amountPaisa, "txn ${t.id} amount", errors)
            if (t.amountPaisa <= 0) errors.add("txn ${t.id} amount must be positive")
            if (t.principalPaisa + t.interestPaisa + t.feePaisa != 0L &&
                t.principalPaisa + t.interestPaisa + t.feePaisa != t.amountPaisa
            ) {
                errors.add("txn ${t.id} components do not sum to amount")
            }
            if (t.flow == TxnFlow.TRANSFER && t.counterAccountId == null) {
                errors.add("txn ${t.id} transfer without destination")
            }
        }
        for (l in lendings) validRef(l.contactId, contactIds, "contact", "lending ${l.id}")
        for (b in borrowings) validRef(b.contactId, contactIds, "contact", "borrowing ${b.id}")
        for (r in receivables) validRef(r.contactId, contactIds, "contact", "receivable ${r.id}")
        for (p in payables) validRef(p.contactId, contactIds, "contact", "payable ${p.id}")
        for (i in installments) {
            if (i.loanId !in loanIds) errors.add("installment ${i.id} references missing loan ${i.loanId}")
        }
        for (o in recurringOccurrences) {
            if (o.recurringId !in recurringItems.map { it.id }.toSet()) {
                errors.add("occurrence ${o.id} references missing recurring item")
            }
        }
        for (a in attachments) {
            validRef(a.txnId, txns.map { it.id }.toSet(), "txn", "attachment ${a.id}")
            validRef(a.purchaseId, purchaseIds, "purchase", "attachment ${a.id}")
        }

        if (errors.isNotEmpty()) {
            return null to BackupValidationResult(false, errors, summary)
        }

        val data = BackupData(
            schemaVersion = schemaVersion,
            appVersion = summary.appVersion,
            createdAtMillis = summary.createdAtMillis,
            currency = root.optString("currency", "BDT"),
            settingsJson = root.optJSONObject("settings")?.toString() ?: "{}",
            accounts = accounts,
            categories = categories,
            contacts = contacts,
            shops = shops,
            purchases = purchases,
            purchaseItems = purchaseItems,
            txns = txns,
            lendings = lendings,
            borrowings = borrowings,
            receivables = receivables,
            payables = payables,
            loans = loans,
            installments = installments,
            goals = goals,
            budgets = budgets,
            recurringItems = recurringItems,
            recurringOccurrences = recurringOccurrences,
            attachments = attachments,
            auditLog = auditLog
        )
        return data to BackupValidationResult(true, emptyList(), summary)
    }

    private fun emptySummary() = BackupSummary(0, "", 0, 0, 0, 0, 0, 0)

    private fun checkMoney(paisa: Long, label: String, errors: MutableList<String>) {
        if (kotlin.math.abs(paisa) > Money.MAX_ABS_PAISA) errors.add("$label out of bounds")
    }

    private fun JSONObject.s(key: String): String {
        val v = optString(key, "")
        return if (v.length > MAX_STRING) v.substring(0, MAX_STRING) else v
    }

    private fun JSONObject.l(key: String): Long = optLong(key, 0)
    private fun JSONObject.lOrNull(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null
    private fun JSONObject.b(key: String, def: Boolean = false): Boolean = optBoolean(key, def)
    private fun JSONObject.i(key: String, def: Int = 0): Int = optInt(key, def)
    private fun JSONObject.d(key: String, def: Double = 0.0): Double = optDouble(key, def)

    // ---- entity -> json ----
    private fun AccountEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("type", type.name)
        put("openingBalancePaisa", openingBalancePaisa); put("notes", notes)
        put("allowNegative", allowNegative); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toAccount() = AccountEntity(
        id = l("id"), name = s("name"), type = AccountType.valueOf(s("type")),
        openingBalancePaisa = l("openingBalancePaisa"), notes = s("notes"),
        allowNegative = b("allowNegative"), isArchived = b("isArchived"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun CategoryEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("kind", kind.name)
        put("colorArgb", colorArgb); put("iconKey", iconKey)
        put("parentId", parentId ?: JSONObject.NULL); put("sortOrder", sortOrder)
        put("isArchived", isArchived); put("isSystemSuggestion", isSystemSuggestion)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toCategory() = CategoryEntity(
        id = l("id"), name = s("name"), kind = CategoryKind.valueOf(s("kind")),
        colorArgb = l("colorArgb"), iconKey = s("iconKey").ifEmpty { "category" },
        parentId = lOrNull("parentId"), sortOrder = i("sortOrder"),
        isArchived = b("isArchived"), isSystemSuggestion = b("isSystemSuggestion"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun ContactEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("phone", phone); put("address", address)
        put("relationship", relationship); put("idNotes", idNotes); put("notes", notes)
        put("isArchived", isArchived); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toContact() = ContactEntity(
        id = l("id"), name = s("name"), phone = s("phone"), address = s("address"),
        relationship = s("relationship"), idNotes = s("idNotes"), notes = s("notes"),
        isArchived = b("isArchived"), createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun ShopEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("shopkeeperName", shopkeeperName)
        put("phone", phone); put("address", address); put("notes", notes)
        put("isArchived", isArchived); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toShop() = ShopEntity(
        id = l("id"), name = s("name"), shopkeeperName = s("shopkeeperName"),
        phone = s("phone"), address = s("address"), notes = s("notes"),
        isArchived = b("isArchived"), createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun PurchaseEntity.toJson() = JSONObject().apply {
        put("id", id); put("shopId", shopId); put("categoryId", categoryId ?: JSONObject.NULL)
        put("dateMillis", dateMillis); put("dayKey", dayKey)
        put("description", description); put("grossPaisa", grossPaisa)
        put("discountPaisa", discountPaisa); put("chargesPaisa", chargesPaisa)
        put("totalPaisa", totalPaisa); put("dueDateMillis", dueDateMillis ?: JSONObject.NULL)
        put("receiptNo", receiptNo); put("note", note); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toPurchase() = PurchaseEntity(
        id = l("id"), shopId = l("shopId"), categoryId = lOrNull("categoryId"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"),
        description = s("description"), grossPaisa = l("grossPaisa"),
        discountPaisa = l("discountPaisa"), chargesPaisa = l("chargesPaisa"),
        totalPaisa = l("totalPaisa"), dueDateMillis = lOrNull("dueDateMillis"),
        receiptNo = s("receiptNo"), note = s("note"), isArchived = b("isArchived"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun PurchaseItemEntity.toJson() = JSONObject().apply {
        put("id", id); put("purchaseId", purchaseId); put("name", name)
        put("quantityMilli", quantityMilli); put("unit", unit)
        put("unitPricePaisa", unitPricePaisa); put("lineTotalPaisa", lineTotalPaisa)
        put("discountPaisa", discountPaisa); put("chargePaisa", chargePaisa)
        put("note", note); put("sortOrder", sortOrder)
    }

    private fun JSONObject.toPurchaseItem() = PurchaseItemEntity(
        id = l("id"), purchaseId = l("purchaseId"), name = s("name"),
        quantityMilli = l("quantityMilli"), unit = s("unit"),
        unitPricePaisa = l("unitPricePaisa"), lineTotalPaisa = l("lineTotalPaisa"),
        discountPaisa = l("discountPaisa"), chargePaisa = l("chargePaisa"),
        note = s("note"), sortOrder = i("sortOrder")
    )

    private fun TxnEntity.toJson() = JSONObject().apply {
        put("id", id); put("type", type.name); put("flow", flow.name)
        put("amountPaisa", amountPaisa); put("principalPaisa", principalPaisa)
        put("interestPaisa", interestPaisa); put("feePaisa", feePaisa)
        put("dateMillis", dateMillis); put("dayKey", dayKey)
        put("accountId", accountId ?: JSONObject.NULL)
        put("counterAccountId", counterAccountId ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("contactId", contactId ?: JSONObject.NULL)
        put("shopId", shopId ?: JSONObject.NULL)
        put("purchaseId", purchaseId ?: JSONObject.NULL)
        put("lendingId", lendingId ?: JSONObject.NULL)
        put("borrowingId", borrowingId ?: JSONObject.NULL)
        put("receivableId", receivableId ?: JSONObject.NULL)
        put("payableId", payableId ?: JSONObject.NULL)
        put("loanId", loanId ?: JSONObject.NULL)
        put("installmentId", installmentId ?: JSONObject.NULL)
        put("goalId", goalId ?: JSONObject.NULL)
        put("paymentMethod", paymentMethod.name)
        put("description", description); put("note", note); put("receiptNo", receiptNo)
        put("status", status.name)
        put("reversalOfId", reversalOfId ?: JSONObject.NULL)
        put("idempotencyKey", idempotencyKey ?: JSONObject.NULL)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toTxn() = TxnEntity(
        id = l("id"), type = TxnType.valueOf(s("type")), flow = TxnFlow.valueOf(s("flow")),
        amountPaisa = l("amountPaisa"), principalPaisa = l("principalPaisa"),
        interestPaisa = l("interestPaisa"), feePaisa = l("feePaisa"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"),
        accountId = lOrNull("accountId"), counterAccountId = lOrNull("counterAccountId"),
        categoryId = lOrNull("categoryId"), contactId = lOrNull("contactId"),
        shopId = lOrNull("shopId"), purchaseId = lOrNull("purchaseId"),
        lendingId = lOrNull("lendingId"), borrowingId = lOrNull("borrowingId"),
        receivableId = lOrNull("receivableId"), payableId = lOrNull("payableId"),
        loanId = lOrNull("loanId"), installmentId = lOrNull("installmentId"),
        goalId = lOrNull("goalId"),
        paymentMethod = PaymentMethod.valueOf(s("paymentMethod").ifEmpty { "CASH" }),
        description = s("description"), note = s("note"), receiptNo = s("receiptNo"),
        status = TxnStatus.valueOf(s("status").ifEmpty { "ACTIVE" }),
        reversalOfId = lOrNull("reversalOfId"), idempotencyKey = if (isNull("idempotencyKey")) null else s("idempotencyKey"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun LendingEntity.toJson() = JSONObject().apply {
        put("id", id); put("contactId", contactId); put("principalPaisa", principalPaisa)
        put("dateMillis", dateMillis); put("dayKey", dayKey); put("purpose", purpose)
        put("dueDateMillis", dueDateMillis ?: JSONObject.NULL)
        put("interestPolicy", interestPolicy.name)
        put("interestAmountPaisa", interestAmountPaisa); put("ratePercent", ratePercent)
        put("notes", notes); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toLending() = LendingEntity(
        id = l("id"), contactId = l("contactId"), principalPaisa = l("principalPaisa"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"), purpose = s("purpose"),
        dueDateMillis = lOrNull("dueDateMillis"),
        interestPolicy = InterestPolicy.valueOf(s("interestPolicy").ifEmpty { "NONE" }),
        interestAmountPaisa = l("interestAmountPaisa"), ratePercent = d("ratePercent"),
        notes = s("notes"), isArchived = b("isArchived"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun BorrowingEntity.toJson() = JSONObject().apply {
        put("id", id); put("contactId", contactId); put("principalPaisa", principalPaisa)
        put("dateMillis", dateMillis); put("dayKey", dayKey); put("purpose", purpose)
        put("dueDateMillis", dueDateMillis ?: JSONObject.NULL)
        put("interestPolicy", interestPolicy.name)
        put("interestAmountPaisa", interestAmountPaisa); put("ratePercent", ratePercent)
        put("notes", notes); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toBorrowing() = BorrowingEntity(
        id = l("id"), contactId = l("contactId"), principalPaisa = l("principalPaisa"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"), purpose = s("purpose"),
        dueDateMillis = lOrNull("dueDateMillis"),
        interestPolicy = InterestPolicy.valueOf(s("interestPolicy").ifEmpty { "NONE" }),
        interestAmountPaisa = l("interestAmountPaisa"), ratePercent = d("ratePercent"),
        notes = s("notes"), isArchived = b("isArchived"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun ReceivableEntity.toJson() = JSONObject().apply {
        put("id", id); put("contactId", contactId); put("amountPaisa", amountPaisa)
        put("dateMillis", dateMillis); put("dayKey", dayKey)
        put("dueDateMillis", dueDateMillis ?: JSONObject.NULL)
        put("reason", reason); put("notes", notes); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toReceivable() = ReceivableEntity(
        id = l("id"), contactId = l("contactId"), amountPaisa = l("amountPaisa"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"),
        dueDateMillis = lOrNull("dueDateMillis"), reason = s("reason"), notes = s("notes"),
        isArchived = b("isArchived"), createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun PayableEntity.toJson() = JSONObject().apply {
        put("id", id); put("contactId", contactId); put("amountPaisa", amountPaisa)
        put("dateMillis", dateMillis); put("dayKey", dayKey)
        put("dueDateMillis", dueDateMillis ?: JSONObject.NULL)
        put("reason", reason); put("notes", notes); put("isArchived", isArchived)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toPayable() = PayableEntity(
        id = l("id"), contactId = l("contactId"), amountPaisa = l("amountPaisa"),
        dateMillis = l("dateMillis"), dayKey = i("dayKey"),
        dueDateMillis = lOrNull("dueDateMillis"), reason = s("reason"), notes = s("notes"),
        isArchived = b("isArchived"), createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun LoanEntity.toJson() = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("institution", institution)
        put("branch", branch); put("accountRef", accountRef); put("loanTypeName", loanTypeName)
        put("startDateMillis", startDateMillis); put("dayKey", dayKey)
        put("principalPaisa", principalPaisa); put("interestMethod", interestMethod.name)
        put("ratePercent", ratePercent)
        put("totalScheduledPaisa", totalScheduledPaisa ?: JSONObject.NULL)
        put("frequency", frequency); put("intervalDays", intervalDays)
        put("installmentAmountPaisa", installmentAmountPaisa)
        put("installmentStartMillis", installmentStartMillis)
        put("expectedInstallments", expectedInstallments)
        put("notes", notes); put("isArchived", isArchived); put("isCompleted", isCompleted)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toLoan() = LoanEntity(
        id = l("id"), kind = LoanKind.valueOf(s("kind")), institution = s("institution"),
        branch = s("branch"), accountRef = s("accountRef"), loanTypeName = s("loanTypeName"),
        startDateMillis = l("startDateMillis"), dayKey = i("dayKey"),
        principalPaisa = l("principalPaisa"),
        interestMethod = InterestMethod.valueOf(s("interestMethod").ifEmpty { "NONE" }),
        ratePercent = d("ratePercent"), totalScheduledPaisa = lOrNull("totalScheduledPaisa"),
        frequency = s("frequency").ifEmpty { "MONTHLY" }, intervalDays = i("intervalDays"),
        installmentAmountPaisa = l("installmentAmountPaisa"),
        installmentStartMillis = l("installmentStartMillis"),
        expectedInstallments = i("expectedInstallments"),
        notes = s("notes"), isArchived = b("isArchived"), isCompleted = b("isCompleted"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun InstallmentEntity.toJson() = JSONObject().apply {
        put("id", id); put("loanId", loanId); put("seq", seq)
        put("dueDateMillis", dueDateMillis); put("dueDayKey", dueDayKey)
        put("scheduledPaisa", scheduledPaisa); put("principalPaisa", principalPaisa)
        put("interestPaisa", interestPaisa); put("feePaisa", feePaisa)
        put("paidPaisa", paidPaisa); put("status", status.name); put("notes", notes)
    }

    private fun JSONObject.toInstallment() = InstallmentEntity(
        id = l("id"), loanId = l("loanId"), seq = i("seq"),
        dueDateMillis = l("dueDateMillis"), dueDayKey = i("dueDayKey"),
        scheduledPaisa = l("scheduledPaisa"), principalPaisa = l("principalPaisa"),
        interestPaisa = l("interestPaisa"), feePaisa = l("feePaisa"),
        paidPaisa = l("paidPaisa"),
        status = InstallmentStatus.valueOf(s("status").ifEmpty { "UNPAID" }),
        notes = s("notes")
    )

    private fun GoalEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("targetPaisa", targetPaisa)
        put("startDateMillis", startDateMillis); put("dayKey", dayKey)
        put("targetDateMillis", targetDateMillis ?: JSONObject.NULL)
        put("linkedAccountId", linkedAccountId ?: JSONObject.NULL)
        put("notes", notes); put("isArchived", isArchived); put("isCompleted", isCompleted)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toGoal() = GoalEntity(
        id = l("id"), name = s("name"), targetPaisa = l("targetPaisa"),
        startDateMillis = l("startDateMillis"), dayKey = i("dayKey"),
        targetDateMillis = lOrNull("targetDateMillis"),
        linkedAccountId = lOrNull("linkedAccountId"), notes = s("notes"),
        isArchived = b("isArchived"), isCompleted = b("isCompleted"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun BudgetEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("limitPaisa", limitPaisa)
        put("period", period.name); put("startDateMillis", startDateMillis)
        put("endDateMillis", endDateMillis ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("accountId", accountId ?: JSONObject.NULL)
        put("warnThresholdPercent", warnThresholdPercent)
        put("isArchived", isArchived); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toBudget() = BudgetEntity(
        id = l("id"), name = s("name"), limitPaisa = l("limitPaisa"),
        period = BudgetPeriod.valueOf(s("period").ifEmpty { "MONTHLY" }),
        startDateMillis = l("startDateMillis"), endDateMillis = lOrNull("endDateMillis"),
        categoryId = lOrNull("categoryId"), accountId = lOrNull("accountId"),
        warnThresholdPercent = i("warnThresholdPercent", 80),
        isArchived = b("isArchived"), createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun RecurringItemEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("kind", kind.name); put("txnType", txnType.name)
        put("amountPaisa", amountPaisa)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("accountId", accountId ?: JSONObject.NULL)
        put("frequency", frequency); put("intervalDays", intervalDays)
        put("startDateMillis", startDateMillis)
        put("endDateMillis", endDateMillis ?: JSONObject.NULL)
        put("nextDateMillis", nextDateMillis); put("nextDayKey", nextDayKey)
        put("remindDaysBefore", remindDaysBefore); put("isPaused", isPaused)
        put("isArchived", isArchived); put("lastOccurrenceKey", lastOccurrenceKey)
        put("notes", notes); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toRecurringItem() = RecurringItemEntity(
        id = l("id"), name = s("name"),
        kind = RecurringKind.valueOf(s("kind").ifEmpty { "BILL" }),
        txnType = TxnType.valueOf(s("txnType").ifEmpty { "EXPENSE" }),
        amountPaisa = l("amountPaisa"), categoryId = lOrNull("categoryId"),
        accountId = lOrNull("accountId"), frequency = s("frequency").ifEmpty { "MONTHLY" },
        intervalDays = i("intervalDays"), startDateMillis = l("startDateMillis"),
        endDateMillis = lOrNull("endDateMillis"), nextDateMillis = l("nextDateMillis"),
        nextDayKey = i("nextDayKey"), remindDaysBefore = i("remindDaysBefore", 1),
        isPaused = b("isPaused"), isArchived = b("isArchived"),
        lastOccurrenceKey = s("lastOccurrenceKey"), notes = s("notes"),
        createdAt = l("createdAt"), updatedAt = l("updatedAt")
    )

    private fun RecurringOccurrenceEntity.toJson() = JSONObject().apply {
        put("id", id); put("recurringId", recurringId); put("occurrenceKey", occurrenceKey)
        put("dateMillis", dateMillis); put("txnId", txnId ?: JSONObject.NULL)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toOccurrence() = RecurringOccurrenceEntity(
        id = l("id"), recurringId = l("recurringId"), occurrenceKey = s("occurrenceKey"),
        dateMillis = l("dateMillis"), txnId = lOrNull("txnId"), createdAt = l("createdAt")
    )

    private fun AttachmentEntity.toJson() = JSONObject().apply {
        put("id", id); put("txnId", txnId ?: JSONObject.NULL)
        put("purchaseId", purchaseId ?: JSONObject.NULL)
        put("displayName", displayName); put("mimeType", mimeType)
        put("sizeBytes", sizeBytes); put("storedPath", storedPath); put("createdAt", createdAt)
    }

    private fun JSONObject.toAttachment() = AttachmentEntity(
        id = l("id"), txnId = lOrNull("txnId"), purchaseId = lOrNull("purchaseId"),
        displayName = s("displayName"), mimeType = s("mimeType"),
        sizeBytes = l("sizeBytes"), storedPath = s("storedPath"), createdAt = l("createdAt")
    )

    private fun AuditEntryEntity.toJson() = JSONObject().apply {
        put("id", id); put("entityType", entityType); put("entityId", entityId)
        put("action", action); put("beforeJson", beforeJson); put("afterJson", afterJson)
        put("atMillis", atMillis)
    }

    private fun JSONObject.toAudit() = AuditEntryEntity(
        id = l("id"), entityType = s("entityType"), entityId = l("entityId"),
        action = s("action"), beforeJson = s("beforeJson"), afterJson = s("afterJson"),
        atMillis = l("atMillis")
    )
}
