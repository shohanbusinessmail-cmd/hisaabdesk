package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.BorrowingEntity
import com.shohankhan.hisaabdesk.data.local.entity.LendingEntity
import com.shohankhan.hisaabdesk.data.local.entity.PayableEntity
import com.shohankhan.hisaabdesk.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LendingDao {
    @Insert
    suspend fun insert(item: LendingEntity): Long

    @Update
    suspend fun update(item: LendingEntity)

    @Delete
    suspend fun delete(item: LendingEntity)

    @Query("SELECT * FROM lendings WHERE id = :id")
    suspend fun getById(id: Long): LendingEntity?

    @Query("SELECT * FROM lendings WHERE id = :id")
    fun observeById(id: Long): Flow<LendingEntity?>

    @Query("SELECT * FROM lendings WHERE isArchived = 0 ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<LendingEntity>>

    @Query("SELECT * FROM lendings WHERE contactId = :contactId AND isArchived = 0 ORDER BY dateMillis DESC")
    fun observeForContact(contactId: Long): Flow<List<LendingEntity>>

    @Query("SELECT * FROM lendings ORDER BY isArchived, dateMillis DESC")
    fun observeAll(): Flow<List<LendingEntity>>
}

@Dao
interface BorrowingDao {
    @Insert
    suspend fun insert(item: BorrowingEntity): Long

    @Update
    suspend fun update(item: BorrowingEntity)

    @Delete
    suspend fun delete(item: BorrowingEntity)

    @Query("SELECT * FROM borrowings WHERE id = :id")
    suspend fun getById(id: Long): BorrowingEntity?

    @Query("SELECT * FROM borrowings WHERE id = :id")
    fun observeById(id: Long): Flow<BorrowingEntity?>

    @Query("SELECT * FROM borrowings WHERE isArchived = 0 ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings WHERE contactId = :contactId AND isArchived = 0 ORDER BY dateMillis DESC")
    fun observeForContact(contactId: Long): Flow<List<BorrowingEntity>>

    @Query("SELECT * FROM borrowings ORDER BY isArchived, dateMillis DESC")
    fun observeAll(): Flow<List<BorrowingEntity>>
}

@Dao
interface ReceivableDao {
    @Insert
    suspend fun insert(item: ReceivableEntity): Long

    @Update
    suspend fun update(item: ReceivableEntity)

    @Delete
    suspend fun delete(item: ReceivableEntity)

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getById(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE id = :id")
    fun observeById(id: Long): Flow<ReceivableEntity?>

    @Query("SELECT * FROM receivables WHERE isArchived = 0 ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE contactId = :contactId AND isArchived = 0 ORDER BY dateMillis DESC")
    fun observeForContact(contactId: Long): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables ORDER BY isArchived, dateMillis DESC")
    fun observeAll(): Flow<List<ReceivableEntity>>
}

@Dao
interface PayableDao {
    @Insert
    suspend fun insert(item: PayableEntity): Long

    @Update
    suspend fun update(item: PayableEntity)

    @Delete
    suspend fun delete(item: PayableEntity)

    @Query("SELECT * FROM payables WHERE id = :id")
    suspend fun getById(id: Long): PayableEntity?

    @Query("SELECT * FROM payables WHERE id = :id")
    fun observeById(id: Long): Flow<PayableEntity?>

    @Query("SELECT * FROM payables WHERE isArchived = 0 ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables WHERE contactId = :contactId AND isArchived = 0 ORDER BY dateMillis DESC")
    fun observeForContact(contactId: Long): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables ORDER BY isArchived, dateMillis DESC")
    fun observeAll(): Flow<List<PayableEntity>>
}
