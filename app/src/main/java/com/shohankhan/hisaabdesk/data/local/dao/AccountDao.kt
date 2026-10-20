package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

data class AccountBalanceRow(val id: Long, val balance: Long)

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE type = 'SAVINGS' AND isArchived = 0")
    suspend fun countSavingsAccounts(): Int

    /**
     * Balance is always recomputed from the ledger — never stored as an editable cache.
     * delta rules (see docs/FINANCIAL_RULES.md):
     *  - INFLOW to account: +amount
     *  - OUTFLOW from account: -amount
     *  - TRANSFER: -amount on source, +amount on counter account
     */
    @Query(
        """
        SELECT a.id AS id,
               a.openingBalancePaisa + COALESCE(SUM(
                   CASE
                       WHEN t.accountId = a.id AND t.flow = 'INFLOW' THEN t.amountPaisa
                       WHEN t.accountId = a.id AND t.flow = 'OUTFLOW' THEN -t.amountPaisa
                       WHEN t.accountId = a.id AND t.flow = 'TRANSFER' THEN -t.amountPaisa
                       WHEN t.counterAccountId = a.id AND t.flow = 'TRANSFER' THEN t.amountPaisa
                       ELSE 0
                   END
               ), 0) AS balance
        FROM accounts a
        LEFT JOIN txns t
            ON t.status = 'ACTIVE'
            AND (t.accountId = a.id OR t.counterAccountId = a.id)
        WHERE a.id = :accountId
        GROUP BY a.id
        """
    )
    suspend fun balanceOf(accountId: Long): AccountBalanceRow?

    @Query(
        """
        SELECT a.id AS id,
               a.openingBalancePaisa + COALESCE(SUM(
                   CASE
                       WHEN t.accountId = a.id AND t.flow = 'INFLOW' THEN t.amountPaisa
                       WHEN t.accountId = a.id AND t.flow = 'OUTFLOW' THEN -t.amountPaisa
                       WHEN t.accountId = a.id AND t.flow = 'TRANSFER' THEN -t.amountPaisa
                       WHEN t.counterAccountId = a.id AND t.flow = 'TRANSFER' THEN t.amountPaisa
                       ELSE 0
                   END
               ), 0) AS balance
        FROM accounts a
        LEFT JOIN txns t
            ON t.status = 'ACTIVE'
            AND (t.accountId = a.id OR t.counterAccountId = a.id)
        GROUP BY a.id
        """
    )
    fun observeBalances(): Flow<List<AccountBalanceRow>>

    @Query("SELECT openingBalancePaisa FROM accounts WHERE id = :accountId")
    suspend fun openingBalance(accountId: Long): Long?

    /** Sum of all non-savings account balances (cash & equivalents) + optional savings. */
    @Query(
        """
        SELECT COALESCE(SUM(openingBalancePaisa), 0) FROM accounts WHERE isArchived = 0
        """
    )
    suspend fun sumOpeningBalances(): Long
}
