package com.shohankhan.hisaabdesk

import android.content.Context
import com.shohankhan.hisaabdesk.core.security.PinManager
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.repository.AccountRepository
import com.shohankhan.hisaabdesk.data.repository.BackupRepository
import com.shohankhan.hisaabdesk.data.repository.CategoryRepository
import com.shohankhan.hisaabdesk.data.repository.ContactRepository
import com.shohankhan.hisaabdesk.data.repository.LoanRepository
import com.shohankhan.hisaabdesk.data.repository.ObligationRepository
import com.shohankhan.hisaabdesk.data.repository.PlanningRepository
import com.shohankhan.hisaabdesk.data.repository.ReportRepository
import com.shohankhan.hisaabdesk.data.repository.SettingsRepository
import com.shohankhan.hisaabdesk.data.repository.ShopRepository
import com.shohankhan.hisaabdesk.data.repository.ShoppingRepository
import com.shohankhan.hisaabdesk.data.repository.TransactionRepository

/** Manual dependency container (no reflection-based DI — keeps R8/backup simple). */
class AppContainer(private val context: Context) {
    val database: HisaabDatabase by lazy { HisaabDatabase.build(context) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val pinManager: PinManager by lazy { PinManager(context) }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(database.accountDao(), database.txnDao())
    }
    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }
    val contactRepository: ContactRepository by lazy {
        ContactRepository(database.contactDao())
    }
    val shopRepository: ShopRepository by lazy {
        ShopRepository(database.shopDao())
    }
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database, database.txnDao(), database.accountDao(), database.auditDao())
    }
    val shoppingRepository: ShoppingRepository by lazy {
        ShoppingRepository(database, database.shoppingDao(), transactionRepository)
    }
    val obligationRepository: ObligationRepository by lazy {
        ObligationRepository(
            database,
            database.lendingDao(),
            database.borrowingDao(),
            database.receivableDao(),
            database.payableDao(),
            transactionRepository
        )
    }
    val loanRepository: LoanRepository by lazy {
        LoanRepository(database, database.loanDao(), transactionRepository)
    }
    val planningRepository: PlanningRepository by lazy {
        PlanningRepository(database, database.goalDao(), database.budgetDao(), database.recurringDao(), transactionRepository)
    }
    val reportRepository: ReportRepository by lazy {
        ReportRepository(database.txnDao(), database.shoppingDao(), database.loanDao())
    }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(context, database)
    }
}
