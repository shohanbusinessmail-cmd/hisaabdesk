package com.shohankhan.hisaabdesk.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * HisaabDesk ledger model
 * -----------------------
 * Every money movement is recorded as one Txn row with exactly one primary account
 * effect (INFLOW / OUTFLOW) or a paired TRANSFER. Balances are NEVER stored as
 * editable caches — they are always recomputed from this ledger (see DAOs).
 *
 * Classification policy (docs/FINANCIAL_RULES.md):
 *  - INCOME/EXPENSE/SAVINGS_INTEREST count toward income/expense totals.
 *  - TRANSFER, SAVINGS_DEPOSIT/WITHDRAWAL, LEND/BORROW, loan disbursements and
 *    principal repayments, receivable/payable movements and shop payments are
 *    NOT income/expense. Interest and fee components are.
 *  - Shopping purchases are expense-recognized at purchase time (accrual) via
 *    Purchase.totalPaisa; their payments are cash movements only.
 * Amounts are integer minor units (paisa).
 */

enum class AccountType { CASH, BANK, MFS, WALLET, SAVINGS, OTHER }

enum class CategoryKind {
    INCOME, EXPENSE, SAVINGS, BORROWING, LENDING, RECEIVABLE, PAYABLE,
    LOAN, SHOPPING, BILL, OTHER
}

enum class TxnType {
    INCOME,
    EXPENSE,
    TRANSFER,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL,
    SAVINGS_INTEREST,
    LEND,
    LEND_COLLECT,
    BORROW,
    BORROW_REPAY,
    LOAN_DISBURSE,
    LOAN_INSTALLMENT,
    RECEIVABLE_CREATE,
    RECEIVABLE_COLLECT,
    PAYABLE_CREATE,
    PAYABLE_PAY,
    SHOP_PAYMENT,
    REFUND,
    ADJUSTMENT
}

enum class TxnStatus { ACTIVE, REVERSED, ARCHIVED, DELETED }

/** Direction of the primary account effect of a txn. */
enum class TxnFlow { INFLOW, OUTFLOW, TRANSFER }

enum class PaymentMethod { CASH, BANK, MFS, CARD, CHEQUE, OTHER }

enum class InterestPolicy { NONE, FIXED, SIMPLE, EXPLICIT_TOTAL }

enum class InterestMethod { NONE, FLAT, REDUCING_MODEL, SIMPLE_MODEL, CONTRACT_TOTAL }

enum class LoanKind { NGO, BANK }

enum class InstallmentStatus { UNPAID, PARTIAL, PAID }

enum class BudgetPeriod { DAILY, WEEKLY, MONTHLY, CUSTOM }

enum class RecurringKind { BILL, RECURRING_TXN }

@Entity(
    tableName = "accounts",
    indices = [Index("isArchived")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    @ColumnInfo(defaultValue = "0") val openingBalancePaisa: Long = 0,
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val allowNegative: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "categories",
    indices = [Index("kind"), Index("parentId"), Index("isArchived")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    @ColumnInfo(defaultValue = "0") val colorArgb: Long = 0xFF18794E,
    val iconKey: String = "category",
    val parentId: Long? = null,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSystemSuggestion: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "contacts",
    indices = [Index("name"), Index("isArchived")]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val relationship: String = "",
    val idNotes: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "shops",
    indices = [Index("name"), Index("isArchived")]
)
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shopkeeperName: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("shopId"), Index("dayKey"), Index("isArchived"), Index("categoryId")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val categoryId: Long? = null,
    val dateMillis: Long,
    val dayKey: Int,
    val description: String = "",
    val grossPaisa: Long,
    val discountPaisa: Long,
    val chargesPaisa: Long,
    val totalPaisa: Long,
    val dueDateMillis: Long? = null,
    val receiptNo: String = "",
    val note: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("purchaseId")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val name: String,
    /** Quantity × 1000 for exact decimal quantities (e.g. 2.5 kg = 2500). */
    val quantityMilli: Long,
    val unit: String = "",
    val unitPricePaisa: Long,
    val lineTotalPaisa: Long,
    val discountPaisa: Long = 0,
    val chargePaisa: Long = 0,
    val note: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "txns",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["counterAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ShopEntity::class, parentColumns = ["id"], childColumns = ["shopId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LendingEntity::class, parentColumns = ["id"], childColumns = ["lendingId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = BorrowingEntity::class, parentColumns = ["id"], childColumns = ["borrowingId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ReceivableEntity::class, parentColumns = ["id"], childColumns = ["receivableId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = PayableEntity::class, parentColumns = ["id"], childColumns = ["payableId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LoanEntity::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = InstallmentEntity::class, parentColumns = ["id"], childColumns = ["installmentId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("type"),
        Index("dateMillis"),
        Index("dayKey"),
        Index("accountId"),
        Index("counterAccountId"),
        Index("categoryId"),
        Index("contactId"),
        Index("shopId"),
        Index("purchaseId"),
        Index("lendingId"),
        Index("borrowingId"),
        Index("receivableId"),
        Index("payableId"),
        Index("loanId"),
        Index("installmentId"),
        Index("goalId"),
        Index("status"),
        Index(value = ["idempotencyKey"], unique = true),
        Index("description"),
        Index("receiptNo")
    ]
)
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxnType,
    /** Always positive; direction is defined by [type]. */
    val amountPaisa: Long,
    val principalPaisa: Long = 0,
    val interestPaisa: Long = 0,
    val feePaisa: Long = 0,
    val dateMillis: Long,
    val dayKey: Int,
    /** Primary account affected; null only for on-account obligation creation. */
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val flow: TxnFlow,
    val categoryId: Long? = null,
    val contactId: Long? = null,
    val shopId: Long? = null,
    val purchaseId: Long? = null,
    val lendingId: Long? = null,
    val borrowingId: Long? = null,
    val receivableId: Long? = null,
    val payableId: Long? = null,
    val loanId: Long? = null,
    val installmentId: Long? = null,
    val goalId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val description: String = "",
    val note: String = "",
    val receiptNo: String = "",
    val status: TxnStatus = TxnStatus.ACTIVE,
    val reversalOfId: Long? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "lendings",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("contactId"), Index("dayKey"), Index("isArchived")]
)
data class LendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val principalPaisa: Long,
    val dateMillis: Long,
    val dayKey: Int,
    val purpose: String = "",
    val dueDateMillis: Long? = null,
    val interestPolicy: InterestPolicy = InterestPolicy.NONE,
    val interestAmountPaisa: Long = 0,
    val ratePercent: Double = 0.0,
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "borrowings",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("contactId"), Index("dayKey"), Index("isArchived")]
)
data class BorrowingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val principalPaisa: Long,
    val dateMillis: Long,
    val dayKey: Int,
    val purpose: String = "",
    val dueDateMillis: Long? = null,
    val interestPolicy: InterestPolicy = InterestPolicy.NONE,
    val interestAmountPaisa: Long = 0,
    val ratePercent: Double = 0.0,
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "receivables",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("contactId"), Index("dayKey"), Index("isArchived")]
)
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val amountPaisa: Long,
    val dateMillis: Long,
    val dayKey: Int,
    val dueDateMillis: Long? = null,
    val reason: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "payables",
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("contactId"), Index("dayKey"), Index("isArchived")]
)
data class PayableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val amountPaisa: Long,
    val dateMillis: Long,
    val dayKey: Int,
    val dueDateMillis: Long? = null,
    val reason: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "loans",
    indices = [Index("kind"), Index("isArchived")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: LoanKind,
    val institution: String,
    val branch: String = "",
    val accountRef: String = "",
    val loanTypeName: String = "",
    val startDateMillis: Long,
    val dayKey: Int,
    val principalPaisa: Long,
    val interestMethod: InterestMethod = InterestMethod.NONE,
    val ratePercent: Double = 0.0,
    /** Contractual total repayment when known; null when modeled from rate. */
    val totalScheduledPaisa: Long? = null,
    val frequency: String = "MONTHLY",
    val intervalDays: Int = 0,
    val installmentAmountPaisa: Long = 0,
    val installmentStartMillis: Long = 0,
    val expectedInstallments: Int = 0,
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(entity = LoanEntity::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("loanId"), Index("dueDateMillis"), Index("status")]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val seq: Int,
    val dueDateMillis: Long,
    val dueDayKey: Int,
    val scheduledPaisa: Long,
    val principalPaisa: Long = 0,
    val interestPaisa: Long = 0,
    val feePaisa: Long = 0,
    val paidPaisa: Long = 0,
    val status: InstallmentStatus = InstallmentStatus.UNPAID,
    val notes: String = ""
)

@Entity(
    tableName = "goals",
    indices = [Index("isArchived")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetPaisa: Long,
    val startDateMillis: Long,
    val dayKey: Int,
    val targetDateMillis: Long? = null,
    val linkedAccountId: Long? = null,
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "budgets",
    indices = [Index("isArchived"), Index("period")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitPaisa: Long,
    val period: BudgetPeriod,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    @ColumnInfo(defaultValue = "80") val warnThresholdPercent: Int = 80,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "recurring_items",
    indices = [Index("nextDayKey"), Index("isArchived"), Index("isPaused")]
)
data class RecurringItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: RecurringKind = RecurringKind.BILL,
    val txnType: TxnType = TxnType.EXPENSE,
    val amountPaisa: Long,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val frequency: String = "MONTHLY",
    val intervalDays: Int = 0,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val nextDateMillis: Long,
    val nextDayKey: Int,
    /** Days before due date to remind; negative disables reminders. */
    @ColumnInfo(defaultValue = "1") val remindDaysBefore: Int = 1,
    @ColumnInfo(defaultValue = "0") val isPaused: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    /** Idempotency guard: last occurrence key that was materialized. */
    val lastOccurrenceKey: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = TxnEntity::class, parentColumns = ["id"], childColumns = ["txnId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("txnId"), Index("purchaseId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txnId: Long? = null,
    val purchaseId: Long? = null,
    val displayName: String,
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    /** Path relative to app files dir under attachments/. */
    val storedPath: String,
    val createdAt: Long
)

@Entity(
    tableName = "audit_log",
    indices = [Index("entityType"), Index("entityId"), Index("atMillis")]
)
data class AuditEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val action: String,
    val beforeJson: String = "",
    val afterJson: String = "",
    val atMillis: Long
)

@Entity(
    tableName = "recurring_occurrences",
    indices = [Index(value = ["recurringId", "occurrenceKey"], unique = true)]
)
data class RecurringOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringId: Long,
    val occurrenceKey: String,
    val dateMillis: Long,
    val txnId: Long? = null,
    val createdAt: Long
)
