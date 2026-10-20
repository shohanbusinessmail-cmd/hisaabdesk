package com.shohankhan.hisaabdesk.data.repository

import androidx.room.withTransaction
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.BudgetDao
import com.shohankhan.hisaabdesk.data.local.dao.GoalDao
import com.shohankhan.hisaabdesk.data.local.dao.RecurringDao
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetPeriod
import com.shohankhan.hisaabdesk.data.local.entity.GoalEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.RecurringItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringOccurrenceEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PlanningRepository(
    private val db: HisaabDatabase,
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val txnRepository: TransactionRepository
) {
    class ValidationException(message: String) : Exception(message)

    // ---------------- goals ----------------
    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeActive()
    fun observeGoal(id: Long): Flow<GoalEntity?> = goalDao.observeById(id)
    suspend fun getGoal(id: Long): GoalEntity? = goalDao.getById(id)
    fun observeGoalContributions(goalId: Long): Flow<Long> = db.txnDao().observeGoalContributions(goalId)

    suspend fun saveGoal(goal: GoalEntity): Long =
        if (goal.id == 0L) {
            goalDao.insert(goal.copy(dayKey = goal.dayKey.takeIf { it != 0 } ?: Dates.dayKey(goal.startDateMillis)))
        } else {
            goalDao.update(goal)
            goal.id
        }

    suspend fun setGoalArchived(id: Long, archived: Boolean) {
        goalDao.getById(id)?.let {
            goalDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * A goal contribution moves money from a cash account into the linked savings
     * account (SAVINGS_DEPOSIT with goalId). It is never counted as income and
     * never double-counted as new savings on top of the account balance.
     */
    suspend fun addContribution(
        goalId: Long,
        amountPaisa: Long,
        fromAccountId: Long,
        dateMillis: Long,
        note: String
    ): Long = db.withTransaction {
        val goal = goalDao.getById(goalId) ?: throw ValidationException("লক্ষ্য পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        val savingsAccountId = goal.linkedAccountId
            ?: throw ValidationException("লক্ষ্যের সাথে একটি সঞ্চয় অ্যাকাউন্ট যুক্ত করুন")
        val now = System.currentTimeMillis()
        txnRepository.insert(
            TxnEntity(
                type = TxnType.SAVINGS_DEPOSIT,
                flow = TxnFlow.TRANSFER,
                amountPaisa = amountPaisa,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = fromAccountId,
                counterAccountId = savingsAccountId,
                goalId = goalId,
                paymentMethod = PaymentMethod.CASH,
                description = "লক্ষ্যে অবদান: ${goal.name}",
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    // ---------------- budgets ----------------
    data class BudgetUsage(
        val budget: BudgetEntity,
        val spentPaisa: Long,
        val remainingPaisa: Long,
        val percentUsed: Int,
        val overBudget: Boolean
    )

    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeActive()
    fun observeBudget(id: Long): Flow<BudgetEntity?> = budgetDao.observeById(id)
    suspend fun getBudget(id: Long): BudgetEntity? = budgetDao.getById(id)

    suspend fun saveBudget(budget: BudgetEntity): Long =
        if (budget.id == 0L) budgetDao.insert(budget) else {
            budgetDao.update(budget)
            budget.id
        }

    suspend fun setBudgetArchived(id: Long, archived: Boolean) {
        budgetDao.getById(id)?.let {
            budgetDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Period range for a budget, relative to [today]. */
    fun budgetRange(budget: BudgetEntity, today: LocalDate): Pair<Int, Int> {
        return when (budget.period) {
            BudgetPeriod.DAILY -> {
                val k = Dates.dayKey(today)
                k to k
            }
            BudgetPeriod.WEEKLY -> {
                val s = Dates.startOfWeek(today)
                Dates.dayKey(s) to Dates.dayKey(Dates.endOfWeek(today))
            }
            BudgetPeriod.MONTHLY -> {
                val s = Dates.startOfMonth(today)
                Dates.dayKey(s) to Dates.dayKey(Dates.endOfMonth(today))
            }
            BudgetPeriod.CUSTOM -> {
                val from = Dates.localDateOf(budget.startDateMillis)
                val to = budget.endDateMillis?.let { Dates.localDateOf(it) } ?: today
                Dates.dayKey(from) to Dates.dayKey(to)
            }
        }
    }

    suspend fun usage(budget: BudgetEntity, today: LocalDate = LocalDate.now()): BudgetUsage {
        val (from, to) = budgetRange(budget, today)
        val expenseSpent = db.txnDao().sumExpenseForBudget(from, to, budget.categoryId, budget.accountId)
        val purchaseSpent = if (budget.categoryId == null && budget.accountId == null) {
            db.shoppingDao().totalPurchaseExpenseBetween(from, to)
        } else if (budget.categoryId != null && budget.accountId == null) {
            db.shoppingDao().totalPurchaseExpenseForCategory(budget.categoryId, from, to)
        } else {
            0L
        }
        val spent = expenseSpent + purchaseSpent
        val remaining = budget.limitPaisa - spent
        val percent = if (budget.limitPaisa > 0) ((spent * 100) / budget.limitPaisa).toInt() else 0
        return BudgetUsage(
            budget = budget,
            spentPaisa = spent,
            remainingPaisa = remaining,
            percentUsed = percent,
            overBudget = spent > budget.limitPaisa
        )
    }

    // ---------------- recurring / bills ----------------
    fun observeRecurring(): Flow<List<RecurringItemEntity>> = recurringDao.observeActive()
    fun observeRecurringBetween(fromDay: Int, toDay: Int) = recurringDao.observeBetween(fromDay, toDay)
    suspend fun getRecurring(id: Long): RecurringItemEntity? = recurringDao.getById(id)
    fun observeOccurrences(recurringId: Long) = recurringDao.observeOccurrences(recurringId)

    suspend fun saveRecurring(item: RecurringItemEntity): Long =
        if (item.id == 0L) recurringDao.insert(item) else {
            recurringDao.update(item)
            item.id
        }

    suspend fun setRecurringPaused(id: Long, paused: Boolean) {
        recurringDao.getById(id)?.let {
            recurringDao.update(it.copy(isPaused = paused, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun setRecurringArchived(id: Long, archived: Boolean) {
        recurringDao.getById(id)?.let {
            recurringDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun dueItems(dayKey: Int): List<RecurringItemEntity> = recurringDao.dueOnOrBefore(dayKey)

    suspend fun needingReminder(fromDay: Int, toDay: Int): List<RecurringItemEntity> =
        recurringDao.needingReminder(fromDay, toDay)

    /**
     * Marks a scheduled occurrence as paid — this is the ONLY way a bill becomes an
     * actual transaction. Reminders alone never create financial records.
     * Idempotent: the same occurrenceKey is recorded at most once.
     */
    suspend fun markPaid(
        recurringId: Long,
        dateMillis: Long,
        accountId: Long?,
        amountOverridePaisa: Long? = null
    ): Long = db.withTransaction {
        val item = recurringDao.getById(recurringId) ?: throw ValidationException("তালিকায় নেই")
        val occurrenceKey = "${item.id}:${Dates.dayKey(dateMillis)}"
        if (recurringDao.countOccurrence(item.id, occurrenceKey) > 0) {
            throw ValidationException("এই তারিখের পরিশোধ আগেই রেকর্ড হয়েছে")
        }
        val amount = amountOverridePaisa ?: item.amountPaisa
        val now = System.currentTimeMillis()
        val txnId = txnRepository.insert(
            TxnEntity(
                type = item.txnType,
                flow = AccountingFlow.flowFor(item.txnType),
                amountPaisa = amount,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId ?: item.accountId,
                categoryId = item.categoryId,
                description = item.name,
                paymentMethod = PaymentMethod.CASH,
                idempotencyKey = "recurring:$occurrenceKey",
                createdAt = now,
                updatedAt = now
            )
        )
        recurringDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringId = item.id,
                occurrenceKey = occurrenceKey,
                dateMillis = dateMillis,
                txnId = txnId,
                createdAt = now
            )
        )
        // advance next date
        val nextDate = nextDateAfter(item, Dates.localDateOf(dateMillis))
        recurringDao.update(
            item.copy(
                nextDateMillis = Dates.millisOf(nextDate),
                nextDayKey = Dates.dayKey(nextDate),
                lastOccurrenceKey = occurrenceKey,
                updatedAt = now
            )
        )
        txnId
    }

    fun nextDateAfter(item: RecurringItemEntity, after: LocalDate): LocalDate {
        var next = Dates.localDateOf(item.startDateMillis)
        val freq = when (item.frequency) {
            "WEEKLY" -> Dates.Frequency.WEEKLY
            "CUSTOM_DAYS" -> Dates.Frequency.CUSTOM_DAYS
            else -> Dates.Frequency.MONTHLY
        }
        var guard = 0
        while (!next.isAfter(after) && guard < 10000) {
            next = when (freq) {
                Dates.Frequency.MONTHLY -> Dates.addMonths(next, 1)
                Dates.Frequency.WEEKLY -> Dates.addWeeks(next, 1)
                Dates.Frequency.CUSTOM_DAYS -> Dates.addDays(next, item.intervalDays.coerceAtLeast(1).toLong())
            }
            guard++
        }
        if (item.endDateMillis != null && next.isAfter(Dates.localDateOf(item.endDateMillis))) {
            return Dates.localDateOf(item.endDateMillis)
        }
        return next
    }
}

/** Local alias to avoid import cycles in this file. */
private object AccountingFlow {
    fun flowFor(type: TxnType): TxnFlow = com.shohankhan.hisaabdesk.domain.accounting.AccountingRules.flowFor(type)
}
