package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

data class LoanKindSum(val total: Long)

@Dao
interface LoanDao {

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Delete
    suspend fun delete(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE kind = :kind AND isArchived = 0 ORDER BY startDateMillis DESC")
    fun observeByKind(kind: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY isArchived, startDateMillis DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("UPDATE loans SET isCompleted = :completed, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, now: Long)

    // Installments
    @Insert
    suspend fun insertInstallments(items: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(item: InstallmentEntity)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsFor(loanId: Long)

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY seq")
    fun observeInstallments(loanId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY seq")
    suspend fun getInstallments(loanId: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallment(id: Long): InstallmentEntity?

    @Query("SELECT * FROM installments WHERE dueDateMillis <= :until AND status != 'PAID' ORDER BY dueDateMillis LIMIT :limit")
    fun observeUpcomingInstallments(until: Long, limit: Int): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE dueDateMillis < :now AND status != 'PAID' ORDER BY dueDateMillis LIMIT :limit")
    fun observeOverdueInstallments(now: Long, limit: Int): Flow<List<InstallmentEntity>>

    @Query("SELECT COALESCE(SUM(scheduledPaisa), 0) FROM installments WHERE loanId = :loanId")
    suspend fun sumScheduled(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(scheduledPaisa - paidPaisa), 0) FROM installments WHERE loanId = :loanId AND status != 'PAID'")
    suspend fun sumRemainingScheduled(loanId: Long): Long

    @Query("SELECT COALESCE(SUM(scheduledPaisa - paidPaisa), 0) FROM installments WHERE dueDateMillis < :now AND status != 'PAID'")
    fun observeOverdueScheduled(now: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(l.principalPaisa), 0) FROM loans l
        WHERE l.kind = :kind AND l.isArchived = 0
        """
    )
    fun observePrincipalByKind(kind: String): Flow<Long>

    @Query("SELECT * FROM loans WHERE (:kind IS NULL OR kind = :kind)")
    suspend fun getLoansByKind(kind: String?): List<LoanEntity>

    @Query("SELECT COUNT(*) FROM loans")
    suspend fun count(): Int
}
