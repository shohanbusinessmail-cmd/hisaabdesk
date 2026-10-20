package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Cross-table outstanding aggregates. Reactive to both obligation rows and ledger payments. */
@Dao
interface SummaryDao {

    @Query(
        """
        SELECT COALESCE(SUM(l.principalPaisa - COALESCE(
            (SELECT SUM(t.principalPaisa) FROM txns t WHERE t.lendingId = l.id AND t.status = 'ACTIVE'), 0
        )), 0)
        FROM lendings l WHERE l.isArchived = 0
        """
    )
    fun observeLendingOutstanding(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(b.principalPaisa - COALESCE(
            (SELECT SUM(t.principalPaisa) FROM txns t WHERE t.borrowingId = b.id AND t.status = 'ACTIVE'), 0
        )), 0)
        FROM borrowings b WHERE b.isArchived = 0
        """
    )
    fun observeBorrowingOutstanding(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(r.amountPaisa - COALESCE(
            (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.receivableId = r.id AND t.status = 'ACTIVE' AND t.type = 'RECEIVABLE_COLLECT'), 0
        )), 0)
        FROM receivables r WHERE r.isArchived = 0
        """
    )
    fun observeReceivableRemaining(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(p.amountPaisa - COALESCE(
            (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.payableId = p.id AND t.status = 'ACTIVE' AND t.type = 'PAYABLE_PAY'), 0
        )), 0)
        FROM payables p WHERE p.isArchived = 0
        """
    )
    fun observePayableRemaining(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(l.principalPaisa - COALESCE(
            (SELECT SUM(t.principalPaisa) FROM txns t WHERE t.loanId = l.id AND t.status = 'ACTIVE'), 0
        )), 0)
        FROM loans l WHERE l.kind = :kind AND l.isArchived = 0
        """
    )
    fun observeLoanPrincipalOutstanding(kind: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(openingBalancePaisa), 0) FROM accounts WHERE type = 'SAVINGS'")
    fun observeSavingsOpening(): Flow<Long>

    @Query("SELECT COUNT(*) FROM txns")
    fun observeTxnCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM purchases")
    fun observePurchaseCount(): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(totalPaisa - COALESCE(
            (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0
        )), 0)
        FROM purchases p WHERE p.isArchived = 0
        """
    )
    fun observeShopOutstanding(): Flow<Long>

    /** Per-obligation remaining rows for lists. */
    @Query(
        """
        SELECT l.id AS id, l.principalPaisa AS amount, COALESCE(
            (SELECT SUM(t.principalPaisa) FROM txns t WHERE t.lendingId = l.id AND t.status = 'ACTIVE'), 0
        ) AS settled
        FROM lendings l WHERE l.isArchived = 0
        """
    )
    fun observeLendingRows(): Flow<List<IdAmountSettled>>

    @Query(
        """
        SELECT b.id AS id, b.principalPaisa AS amount, COALESCE(
            (SELECT SUM(t.principalPaisa) FROM txns t WHERE t.borrowingId = b.id AND t.status = 'ACTIVE'), 0
        ) AS settled
        FROM borrowings b WHERE b.isArchived = 0
        """
    )
    fun observeBorrowingRows(): Flow<List<IdAmountSettled>>

    @Query(
        """
        SELECT r.id AS id, r.amountPaisa AS amount, COALESCE(
            (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.receivableId = r.id AND t.status = 'ACTIVE' AND t.type = 'RECEIVABLE_COLLECT'), 0
        ) AS settled
        FROM receivables r WHERE r.isArchived = 0
        """
    )
    fun observeReceivableRows(): Flow<List<IdAmountSettled>>

    @Query(
        """
        SELECT p.id AS id, p.amountPaisa AS amount, COALESCE(
            (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.payableId = p.id AND t.status = 'ACTIVE' AND t.type = 'PAYABLE_PAY'), 0
        ) AS settled
        FROM payables p WHERE p.isArchived = 0
        """
    )
    fun observePayableRows(): Flow<List<IdAmountSettled>>
}

data class IdAmountSettled(val id: Long, val amount: Long, val settled: Long)
