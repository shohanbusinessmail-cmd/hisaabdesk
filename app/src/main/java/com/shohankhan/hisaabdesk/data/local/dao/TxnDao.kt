package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import kotlinx.coroutines.flow.Flow

data class SumRow(val total: Long)
data class MonthSumRow(val monthKey: Int, val total: Long)
data class CategorySumRow(val categoryId: Long?, val total: Long)
data class DaySumRow(val dayKey: Int, val total: Long)

@Dao
interface TxnDao {

    @Insert
    suspend fun insert(txn: TxnEntity): Long

    @Insert
    suspend fun insertAll(txns: List<TxnEntity>)

    @Update
    suspend fun update(txn: TxnEntity)

    @Delete
    suspend fun delete(txn: TxnEntity)

    @Query("SELECT * FROM txns WHERE id = :id")
    suspend fun getById(id: Long): TxnEntity?

    @Query("SELECT * FROM txns WHERE id = :id")
    fun observeById(id: Long): Flow<TxnEntity?>

    @Query("SELECT COUNT(*) FROM txns WHERE idempotencyKey = :key")
    suspend fun countByIdempotencyKey(key: String): Int

    @Query("SELECT * FROM txns WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): TxnEntity?

    // ------------------------------------------------------------------
    // Listing with filters (pagination by limit/offset)
    // ------------------------------------------------------------------
    @Query(
        """
        SELECT * FROM txns
        WHERE status != 'DELETED'
          AND (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR accountId = :accountId OR counterAccountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:contactId IS NULL OR contactId = :contactId)
          AND (:shopId IS NULL OR shopId = :shopId)
          AND (:fromDay IS NULL OR dayKey >= :fromDay)
          AND (:toDay IS NULL OR dayKey <= :toDay)
          AND (:minPaisa IS NULL OR amountPaisa >= :minPaisa)
          AND (:maxPaisa IS NULL OR amountPaisa <= :maxPaisa)
          AND (
              :query IS NULL OR :query = '' OR
              description LIKE '%' || :query || '%' COLLATE NOCASE OR
              note LIKE '%' || :query || '%' COLLATE NOCASE OR
              receiptNo LIKE '%' || :query || '%' COLLATE NOCASE
          )
        ORDER BY dateMillis DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeFiltered(
        query: String?,
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        contactId: Long?,
        shopId: Long?,
        fromDay: Int?,
        toDay: Int?,
        minPaisa: Long?,
        maxPaisa: Long?,
        limit: Int,
        offset: Int
    ): Flow<List<TxnEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM txns
        WHERE status != 'DELETED'
          AND (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR accountId = :accountId OR counterAccountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:contactId IS NULL OR contactId = :contactId)
          AND (:shopId IS NULL OR shopId = :shopId)
          AND (:fromDay IS NULL OR dayKey >= :fromDay)
          AND (:toDay IS NULL OR dayKey <= :toDay)
          AND (:minPaisa IS NULL OR amountPaisa >= :minPaisa)
          AND (:maxPaisa IS NULL OR amountPaisa <= :maxPaisa)
          AND (
              :query IS NULL OR :query = '' OR
              description LIKE '%' || :query || '%' COLLATE NOCASE OR
              note LIKE '%' || :query || '%' COLLATE NOCASE OR
              receiptNo LIKE '%' || :query || '%' COLLATE NOCASE
          )
        """
    )
    suspend fun countFiltered(
        query: String?,
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        contactId: Long?,
        shopId: Long?,
        fromDay: Int?,
        toDay: Int?,
        minPaisa: Long?,
        maxPaisa: Long?
    ): Int

    @Query("SELECT * FROM txns WHERE status != 'DELETED' ORDER BY dateMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE accountId = :accountId AND status != 'DELETED' ORDER BY dateMillis DESC, id DESC LIMIT :limit OFFSET :offset")
    fun observeForAccount(accountId: Long, limit: Int, offset: Int): Flow<List<TxnEntity>>

    // ------------------------------------------------------------------
    // Aggregates — all figures come from the ledger, day-inclusive ranges
    // ------------------------------------------------------------------
    @Query(
        """
        SELECT COALESCE(SUM(amountPaisa), 0) FROM txns
        WHERE status = 'ACTIVE'
          AND type IN ('INCOME', 'SAVINGS_INTEREST')
          AND dayKey BETWEEN :fromDay AND :toDay
        """
    )
    fun observeIncomeBetween(fromDay: Int, toDay: Int): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amountPaisa), 0) FROM txns
        WHERE status = 'ACTIVE'
          AND type IN ('INCOME', 'SAVINGS_INTEREST')
          AND dayKey BETWEEN :fromDay AND :toDay
        """
    )
    suspend fun incomeBetween(fromDay: Int, toDay: Int): Long

    /**
     * Ordinary expense + interest/fee components of debt & loan repayments.
     * Principal repayments, transfers and savings movements are excluded.
     */
    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'EXPENSE' THEN amountPaisa
                WHEN type IN ('BORROW_REPAY', 'LOAN_INSTALLMENT') THEN interestPaisa + feePaisa
                ELSE 0
            END
        ), 0) FROM txns
        WHERE status = 'ACTIVE'
          AND dayKey BETWEEN :fromDay AND :toDay
        """
    )
    suspend fun expenseBetween(fromDay: Int, toDay: Int): Long

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'EXPENSE' THEN amountPaisa
                WHEN type IN ('BORROW_REPAY', 'LOAN_INSTALLMENT') THEN interestPaisa + feePaisa
                ELSE 0
            END
        ), 0) FROM txns
        WHERE status = 'ACTIVE'
          AND dayKey BETWEEN :fromDay AND :toDay
        """
    )
    fun observeExpenseBetween(fromDay: Int, toDay: Int): Flow<Long>

    @Query(
        """
        SELECT (dayKey / 100) AS monthKey, COALESCE(SUM(amountPaisa), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE'
          AND type IN ('INCOME', 'SAVINGS_INTEREST')
          AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY monthKey
        ORDER BY monthKey
        """
    )
    fun observeIncomeByMonth(fromDay: Int, toDay: Int): Flow<List<MonthSumRow>>

    @Query(
        """
        SELECT (dayKey / 100) AS monthKey, COALESCE(SUM(
            CASE
                WHEN type = 'EXPENSE' THEN amountPaisa
                WHEN type IN ('BORROW_REPAY', 'LOAN_INSTALLMENT') THEN interestPaisa + feePaisa
                ELSE 0
            END
        ), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE'
          AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY monthKey
        ORDER BY monthKey
        """
    )
    fun observeExpenseByMonth(fromDay: Int, toDay: Int): Flow<List<MonthSumRow>>

    @Query(
        """
        SELECT categoryId, COALESCE(SUM(amountPaisa), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE' AND type = 'EXPENSE' AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeExpenseByCategory(fromDay: Int, toDay: Int): Flow<List<CategorySumRow>>

    @Query(
        """
        SELECT categoryId, COALESCE(SUM(amountPaisa), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE' AND type = 'INCOME' AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeIncomeByCategory(fromDay: Int, toDay: Int): Flow<List<CategorySumRow>>

    @Query(
        """
        SELECT dayKey, COALESCE(SUM(amountPaisa), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE' AND type = 'INCOME' AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY dayKey ORDER BY dayKey
        """
    )
    fun observeIncomeByDay(fromDay: Int, toDay: Int): Flow<List<DaySumRow>>

    @Query(
        """
        SELECT dayKey, COALESCE(SUM(amountPaisa), 0) AS total
        FROM txns
        WHERE status = 'ACTIVE' AND type = 'EXPENSE' AND dayKey BETWEEN :fromDay AND :toDay
        GROUP BY dayKey ORDER BY dayKey
        """
    )
    fun observeExpenseByDay(fromDay: Int, toDay: Int): Flow<List<DaySumRow>>

    @Query(
        """
        SELECT COALESCE(SUM(amountPaisa), 0) FROM txns
        WHERE status = 'ACTIVE' AND type = :type AND dayKey BETWEEN :fromDay AND :toDay
        """
    )
    suspend fun sumTypeBetween(type: String, fromDay: Int, toDay: Int): Long

    @Query(
        """
        SELECT COALESCE(SUM(amountPaisa), 0) FROM txns
        WHERE status = 'ACTIVE' AND type = :type
        """
    )
    suspend fun sumTypeAll(type: String): Long

    // ------------------------------------------------------------------
    // Linked-record payment sums (reconciliation + detail screens)
    // ------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND purchaseId = :purchaseId AND type = 'SHOP_PAYMENT'")
    suspend fun sumPurchasePayments(purchaseId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND lendingId = :lendingId AND type = 'LEND_COLLECT'")
    fun observeLendingCollected(lendingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND lendingId = :lendingId")
    fun observeLendingPrincipalPaid(lendingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND lendingId = :lendingId")
    suspend fun sumLendingPrincipalPaid(lendingId: Long): Long

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND borrowingId = :borrowingId")
    suspend fun sumBorrowingPrincipalPaid(borrowingId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND receivableId = :receivableId AND type = 'RECEIVABLE_COLLECT'")
    suspend fun sumReceivableCollected(receivableId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND payableId = :payableId AND type = 'PAYABLE_PAY'")
    suspend fun sumPayablePaid(payableId: Long): Long

    @Query("SELECT COALESCE(SUM(interestPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND lendingId = :lendingId")
    fun observeLendingInterestPaid(lendingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND borrowingId = :borrowingId AND type = 'BORROW_REPAY'")
    fun observeBorrowingRepaid(borrowingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND borrowingId = :borrowingId")
    fun observeBorrowingPrincipalPaid(borrowingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(interestPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND borrowingId = :borrowingId")
    fun observeBorrowingInterestPaid(borrowingId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND receivableId = :receivableId AND type = 'RECEIVABLE_COLLECT'")
    fun observeReceivableCollected(receivableId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND payableId = :payableId AND type = 'PAYABLE_PAY'")
    fun observePayablePaid(payableId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    fun observeLoanPayments(loanId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    fun observeLoanPrincipalPaid(loanId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(interestPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    fun observeLoanInterestPaid(loanId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(feePaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    fun observeLoanFeesPaid(loanId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    suspend fun sumLoanPayments(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(principalPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND loanId = :loanId")
    suspend fun sumLoanPrincipalPaid(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND goalId = :goalId AND type = 'SAVINGS_DEPOSIT'")
    fun observeGoalContributions(goalId: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amountPaisa), 0) FROM txns
        WHERE status = 'ACTIVE' AND type = 'EXPENSE' AND dayKey BETWEEN :fromDay AND :toDay
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:accountId IS NULL OR accountId = :accountId)
        """
    )
    suspend fun sumExpenseForBudget(fromDay: Int, toDay: Int, categoryId: Long?, accountId: Long?): Long

    // ------------------------------------------------------------------
    // Reconciliation support
    // ------------------------------------------------------------------
    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND flow = 'TRANSFER' AND accountId = :accountId")
    suspend fun sumTransfersOut(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaisa), 0) FROM txns WHERE status = 'ACTIVE' AND flow = 'TRANSFER' AND counterAccountId = :accountId")
    suspend fun sumTransfersIn(accountId: Long): Long

    @Query("SELECT COUNT(*) FROM txns WHERE status = 'ACTIVE' AND flow = 'TRANSFER' AND (counterAccountId IS NULL OR counterAccountId = accountId)")
    suspend fun countBrokenTransfers(): Int

    @Query("SELECT COUNT(*) FROM txns WHERE status = 'ACTIVE' AND principalPaisa + interestPaisa + feePaisa != 0 AND principalPaisa + interestPaisa + feePaisa != amountPaisa")
    suspend fun countComponentMismatch(): Int

    @Query("SELECT * FROM txns WHERE status = 'DELETED' ORDER BY updatedAt DESC")
    fun observeTrash(): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE status = 'ARCHIVED' ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<TxnEntity>>

    @Query("UPDATE txns SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, now: Long)

    @Query("SELECT COUNT(*) FROM txns WHERE status = 'ACTIVE' AND accountId = :accountId")
    suspend fun countActiveForAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM txns WHERE status = 'ACTIVE' AND (accountId = :accountId OR counterAccountId = :accountId)")
    suspend fun countAllForAccount(accountId: Long): Int
}
