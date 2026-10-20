package com.shohankhan.hisaabdesk.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shohankhan.hisaabdesk.data.local.dao.AccountDao
import com.shohankhan.hisaabdesk.data.local.dao.AttachmentDao
import com.shohankhan.hisaabdesk.data.local.dao.AuditDao
import com.shohankhan.hisaabdesk.data.local.dao.BorrowingDao
import com.shohankhan.hisaabdesk.data.local.dao.BudgetDao
import com.shohankhan.hisaabdesk.data.local.dao.CategoryDao
import com.shohankhan.hisaabdesk.data.local.dao.ContactDao
import com.shohankhan.hisaabdesk.data.local.dao.GoalDao
import com.shohankhan.hisaabdesk.data.local.dao.LendingDao
import com.shohankhan.hisaabdesk.data.local.dao.LoanDao
import com.shohankhan.hisaabdesk.data.local.dao.PayableDao
import com.shohankhan.hisaabdesk.data.local.dao.PurgeDao
import com.shohankhan.hisaabdesk.data.local.dao.ReceivableDao
import com.shohankhan.hisaabdesk.data.local.dao.RecurringDao
import com.shohankhan.hisaabdesk.data.local.dao.ShopDao
import com.shohankhan.hisaabdesk.data.local.dao.SummaryDao
import com.shohankhan.hisaabdesk.data.local.dao.ShoppingDao
import com.shohankhan.hisaabdesk.data.local.dao.TxnDao
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.AttachmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.AuditEntryEntity
import com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.GoalEntity
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.LendingEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import com.shohankhan.hisaabdesk.data.local.entity.PayableEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringOccurrenceEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        ContactEntity::class,
        ShopEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        TxnEntity::class,
        LendingEntity::class,
        BorrowingEntity::class,
        ReceivableEntity::class,
        PayableEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        GoalEntity::class,
        BudgetEntity::class,
        RecurringItemEntity::class,
        AttachmentEntity::class,
        AuditEntryEntity::class,
        RecurringOccurrenceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HisaabDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun contactDao(): ContactDao
    abstract fun shopDao(): ShopDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun txnDao(): TxnDao
    abstract fun lendingDao(): LendingDao
    abstract fun borrowingDao(): BorrowingDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun payableDao(): PayableDao
    abstract fun loanDao(): LoanDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun auditDao(): AuditDao
    abstract fun purgeDao(): PurgeDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        const val DB_NAME = "hisaabdesk.db"

        /**
         * Migration policy (docs/PROJECT_STATUS.md):
         * - Every schema change ships an explicit [Migration]. fallbackToDestructiveMigration
         *   is NEVER enabled — user data must not be destroyed by an upgrade.
         * - Migration tests live in androidTest (instrumented) because they need the
         *   real SQLite engine across schema versions.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun build(context: Context): HisaabDatabase =
            Room.databaseBuilder(context.applicationContext, HisaabDatabase::class.java, DB_NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
