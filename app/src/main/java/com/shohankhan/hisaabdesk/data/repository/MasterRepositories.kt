package com.shohankhan.hisaabdesk.data.repository

import com.shohankhan.hisaabdesk.data.local.dao.AccountDao
import com.shohankhan.hisaabdesk.data.local.dao.CategoryDao
import com.shohankhan.hisaabdesk.data.local.dao.ContactDao
import com.shohankhan.hisaabdesk.data.local.dao.ShopDao
import com.shohankhan.hisaabdesk.data.local.dao.TxnDao
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryKind
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val accountDao: AccountDao,
    private val txnDao: TxnDao
) {
    fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()
    fun observeActive(): Flow<List<AccountEntity>> = accountDao.observeActive()
    fun observeById(id: Long): Flow<AccountEntity?> = accountDao.observeById(id)
    fun observeBalances() = accountDao.observeBalances()
    suspend fun balanceOf(id: Long): Long = accountDao.balanceOf(id)?.balance ?: 0L
    suspend fun get(id: Long): AccountEntity? = accountDao.getById(id)

    suspend fun save(account: AccountEntity): Long =
        if (account.id == 0L) accountDao.insert(account) else {
            accountDao.update(account)
            account.id
        }

    /** Deleting is only allowed when no ledger rows reference the account. */
    suspend fun canDelete(id: Long): Boolean = txnDao.countAllForAccount(id) == 0

    suspend fun delete(account: AccountEntity): Boolean {
        if (!canDelete(account.id)) return false
        accountDao.delete(account)
        return true
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val existing = accountDao.getById(id) ?: return
        accountDao.update(existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun createWithOpeningBalance(account: AccountEntity): Long {
        // Opening balances are stored on the account itself — never as income txns.
        return accountDao.insert(account)
    }
}

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()
    fun observeByKind(kind: String): Flow<List<CategoryEntity>> = categoryDao.observeByKind(kind)
    suspend fun get(id: Long): CategoryEntity? = categoryDao.getById(id)
    suspend fun count(): Int = categoryDao.count()

    suspend fun save(category: CategoryEntity): Long =
        if (category.id == 0L) categoryDao.insert(category) else {
            categoryDao.update(category)
            category.id
        }

    suspend fun setArchived(id: Long, archived: Boolean) =
        categoryDao.setArchived(id, archived, System.currentTimeMillis())

    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)

    suspend fun move(category: CategoryEntity, up: Boolean, siblings: List<CategoryEntity>) {
        val index = siblings.indexOfFirst { it.id == category.id }
        if (index < 0) return
        val otherIndex = if (up) index - 1 else index + 1
        if (otherIndex !in siblings.indices) return
        val other = siblings[otherIndex]
        categoryDao.updateSortOrder(category.id, other.sortOrder)
        categoryDao.updateSortOrder(other.id, category.sortOrder)
    }

    /**
     * Optional, editable system-suggestion categories created at first launch.
     * These are NOT financial records and are never mandatory.
     */
    suspend fun seedSuggestionsIfEmpty() {
        if (categoryDao.count() > 0) return
        val now = System.currentTimeMillis()
        val suggestions = listOf(
            "বেতন" to CategoryKind.INCOME,
            "ব্যবসায়িক আয়" to CategoryKind.INCOME,
            "উপহার" to CategoryKind.INCOME,
            "খাবার" to CategoryKind.EXPENSE,
            "বাসা ভাড়া" to CategoryKind.EXPENSE,
            "যাতায়াত" to CategoryKind.EXPENSE,
            "চিকিৎসা" to CategoryKind.EXPENSE,
            "শিক্ষা" to CategoryKind.EXPENSE,
            "উপযোগিতা বিল" to CategoryKind.EXPENSE,
            "কেনাকাটা" to CategoryKind.SHOPPING
        )
        categoryDao.insertAll(
            suggestions.mapIndexed { index, (name, kind) ->
                CategoryEntity(
                    name = name,
                    kind = kind,
                    sortOrder = index,
                    isSystemSuggestion = true,
                    createdAt = now,
                    updatedAt = now
                )
            }
        )
    }
}

class ContactRepository(private val contactDao: ContactDao) {
    fun observeAll(): Flow<List<ContactEntity>> = contactDao.observeAll()
    fun observeActive(): Flow<List<ContactEntity>> = contactDao.observeActive()
    fun observeById(id: Long): Flow<ContactEntity?> = contactDao.observeById(id)
    fun search(query: String): Flow<List<ContactEntity>> = contactDao.search(query)
    suspend fun get(id: Long): ContactEntity? = contactDao.getById(id)

    suspend fun isDuplicateName(name: String, excludeId: Long = 0): Boolean =
        contactDao.countByName(name.trim(), excludeId) > 0

    suspend fun save(contact: ContactEntity): Long =
        if (contact.id == 0L) contactDao.insert(contact) else {
            contactDao.update(contact)
            contact.id
        }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val existing = contactDao.getById(id) ?: return
        contactDao.update(existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(contact: ContactEntity) = contactDao.delete(contact)
}

class ShopRepository(private val shopDao: ShopDao) {
    fun observeAll(): Flow<List<ShopEntity>> = shopDao.observeAll()
    fun observeActive(): Flow<List<ShopEntity>> = shopDao.observeActive()
    fun observeById(id: Long): Flow<ShopEntity?> = shopDao.observeById(id)
    suspend fun get(id: Long): ShopEntity? = shopDao.getById(id)

    suspend fun isDuplicateName(name: String, excludeId: Long = 0): Boolean =
        shopDao.countByName(name.trim(), excludeId) > 0

    suspend fun save(shop: ShopEntity): Long =
        if (shop.id == 0L) shopDao.insert(shop) else {
            shopDao.update(shop)
            shop.id
        }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val existing = shopDao.getById(id) ?: return
        shopDao.update(existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(shop: ShopEntity) = shopDao.delete(shop)
}
