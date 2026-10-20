package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY kind, sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY kind, sortOrder, name COLLATE NOCASE")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE kind = :kind AND isArchived = 0 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeByKind(kind: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeChildren(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("UPDATE categories SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)
}

@Dao
interface ContactDao {
    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun observeById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts ORDER BY isArchived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' COLLATE NOCASE OR phone LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM contacts WHERE id != :excludeId AND name = :name COLLATE NOCASE")
    suspend fun countByName(name: String, excludeId: Long): Int
}

@Dao
interface ShopDao {
    @Insert
    suspend fun insert(shop: ShopEntity): Long

    @Update
    suspend fun update(shop: ShopEntity)

    @Delete
    suspend fun delete(shop: ShopEntity)

    @Query("SELECT * FROM shops WHERE id = :id")
    suspend fun getById(id: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE id = :id")
    fun observeById(id: Long): Flow<ShopEntity?>

    @Query("SELECT * FROM shops ORDER BY isArchived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ShopEntity>>

    @Query("SELECT COUNT(*) FROM shops WHERE id != :excludeId AND name = :name COLLATE NOCASE")
    suspend fun countByName(name: String, excludeId: Long): Int
}
