package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/** Bulk delete/insert support for full backup restore inside one transaction. */
@Dao
interface PurgeDao {
    @Query("DELETE FROM audit_log") suspend fun deleteAudit()
    @Query("DELETE FROM attachments") suspend fun deleteAttachments()
    @Query("DELETE FROM recurring_occurrences") suspend fun deleteOccurrences()
    @Query("DELETE FROM recurring_items") suspend fun deleteRecurring()
    @Query("DELETE FROM budgets") suspend fun deleteBudgets()
    @Query("DELETE FROM goals") suspend fun deleteGoals()
    @Query("DELETE FROM installments") suspend fun deleteInstallments()
    @Query("DELETE FROM loans") suspend fun deleteLoans()
    @Query("DELETE FROM payables") suspend fun deletePayables()
    @Query("DELETE FROM receivables") suspend fun deleteReceivables()
    @Query("DELETE FROM borrowings") suspend fun deleteBorrowings()
    @Query("DELETE FROM lendings") suspend fun deleteLendings()
    @Query("DELETE FROM txns") suspend fun deleteTxns()
    @Query("DELETE FROM purchase_items") suspend fun deletePurchaseItems()
    @Query("DELETE FROM purchases") suspend fun deletePurchases()
    @Query("DELETE FROM shops") suspend fun deleteShops()
    @Query("DELETE FROM contacts") suspend fun deleteContacts()
    @Query("DELETE FROM categories") suspend fun deleteCategories()
    @Query("DELETE FROM accounts") suspend fun deleteAccounts()

    suspend fun deleteAll() {
        // Order respects foreign keys.
        deleteAudit()
        deleteAttachments()
        deleteOccurrences()
        deleteRecurring()
        deleteBudgets()
        deleteGoals()
        deleteTxns()
        deleteInstallments()
        deleteLoans()
        deletePayables()
        deleteReceivables()
        deleteBorrowings()
        deleteLendings()
        deletePurchaseItems()
        deletePurchases()
        deleteShops()
        deleteContacts()
        deleteCategories()
        deleteAccounts()
    }
}
