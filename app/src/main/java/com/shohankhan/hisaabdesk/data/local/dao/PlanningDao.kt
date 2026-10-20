package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.AttachmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.AuditEntryEntity
import com.shohankhan.hisaabdesk.data.local.entity.BudgetEntity
import com.shohankhan.hisaabdesk.data.local.entity.GoalEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.RecurringOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY targetDateMillis IS NULL, targetDateMillis, createdAt")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isArchived, createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>
}

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY isArchived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<BudgetEntity>>
}

@Dao
interface RecurringDao {
    @Insert
    suspend fun insert(item: RecurringItemEntity): Long

    @Update
    suspend fun update(item: RecurringItemEntity)

    @Delete
    suspend fun delete(item: RecurringItemEntity)

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    suspend fun getById(id: Long): RecurringItemEntity?

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    fun observeById(id: Long): Flow<RecurringItemEntity?>

    @Query("SELECT * FROM recurring_items WHERE isArchived = 0 ORDER BY nextDateMillis")
    fun observeActive(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isArchived = 0 AND isPaused = 0 AND nextDayKey <= :dayKey ORDER BY nextDateMillis")
    suspend fun dueOnOrBefore(dayKey: Int): List<RecurringItemEntity>

    @Query("SELECT * FROM recurring_items WHERE isArchived = 0 AND isPaused = 0 AND nextDayKey BETWEEN :fromDay AND :toDay ORDER BY nextDateMillis")
    fun observeBetween(fromDay: Int, toDay: Int): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isArchived = 0 AND remindDaysBefore >= 0 AND isPaused = 0 AND nextDayKey BETWEEN :fromDay AND :toDay")
    suspend fun needingReminder(fromDay: Int, toDay: Int): List<RecurringItemEntity>

    // Occurrences (idempotency for recurrence materialization)
    @Insert
    suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long

    @Query("SELECT COUNT(*) FROM recurring_occurrences WHERE recurringId = :recurringId AND occurrenceKey = :key")
    suspend fun countOccurrence(recurringId: Long, key: String): Int

    @Query("SELECT * FROM recurring_occurrences WHERE recurringId = :recurringId ORDER BY dateMillis DESC")
    fun observeOccurrences(recurringId: Long): Flow<List<RecurringOccurrenceEntity>>
}

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE txnId = :txnId")
    fun observeForTxn(txnId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE purchaseId = :purchaseId")
    fun observeForPurchase(purchaseId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): AttachmentEntity?

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entry: AuditEntryEntity): Long

    @Query("SELECT * FROM audit_log WHERE entityType = :entityType AND entityId = :entityId ORDER BY atMillis DESC")
    fun observeFor(entityType: String, entityId: Long): Flow<List<AuditEntryEntity>>

    @Query("SELECT * FROM audit_log ORDER BY atMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AuditEntryEntity>>
}
