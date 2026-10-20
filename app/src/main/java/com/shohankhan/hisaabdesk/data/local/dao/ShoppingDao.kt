package com.shohankhan.hisaabdesk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseEntity
import com.shohankhan.hisaabdesk.data.local.entity.PurchaseItemEntity
import com.shohankhan.hisaabdesk.data.local.entity.ShopEntity
import kotlinx.coroutines.flow.Flow

data class ShopTotalsRow(
    val shopId: Long,
    val totalPurchases: Long,
    val totalPaid: Long,
    val purchaseCount: Int
)

data class PurchaseWithPaidRow(
    val id: Long,
    val shopId: Long,
    val dateMillis: Long,
    val dayKey: Int,
    val description: String,
    val totalPaisa: Long,
    val dueDateMillis: Long?,
    val receiptNo: String,
    val isArchived: Boolean,
    val paidPaisa: Long
)

@Dao
interface ShoppingDao {

    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsFor(purchaseId: Long)

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchase(id: Long): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    fun observePurchase(id: Long): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY sortOrder, id")
    fun observeItems(purchaseId: Long): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY sortOrder, id")
    suspend fun getItems(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchases WHERE isArchived = 0 ORDER BY dateMillis DESC, id DESC LIMIT :limit OFFSET :offset")
    fun observePurchases(limit: Int, offset: Int): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * FROM purchases
        WHERE isArchived = 0
          AND (:shopId IS NULL OR shopId = :shopId)
          AND (:fromDay IS NULL OR dayKey >= :fromDay)
          AND (:toDay IS NULL OR dayKey <= :toDay)
          AND (:query IS NULL OR :query = '' OR description LIKE '%' || :query || '%' COLLATE NOCASE OR receiptNo LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY dateMillis DESC, id DESC LIMIT :limit OFFSET :offset
        """
    )
    fun observePurchasesFiltered(
        shopId: Long?,
        fromDay: Int?,
        toDay: Int?,
        query: String?,
        limit: Int,
        offset: Int
    ): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND isArchived = 0 ORDER BY dateMillis DESC, id DESC")
    fun observePurchasesForShop(shopId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchases WHERE shopId = :shopId AND isArchived = 0")
    fun observeShopTotalPurchases(shopId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchases WHERE shopId = :shopId AND isArchived = 0 AND :fromDay <= dayKey AND dayKey <= :toDay")
    suspend fun shopPurchasesBetween(shopId: Long, fromDay: Int, toDay: Int): Long

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchases WHERE isArchived = 0 AND dayKey BETWEEN :fromDay AND :toDay")
    suspend fun totalPurchaseExpenseBetween(fromDay: Int, toDay: Int): Long

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchases WHERE isArchived = 0 AND categoryId = :categoryId AND dayKey BETWEEN :fromDay AND :toDay")
    suspend fun totalPurchaseExpenseForCategory(categoryId: Long, fromDay: Int, toDay: Int): Long

    @Query("SELECT COALESCE(SUM(totalPaisa), 0) FROM purchases WHERE isArchived = 0 AND dayKey BETWEEN :fromDay AND :toDay")
    fun observeTotalPurchaseExpenseBetween(fromDay: Int, toDay: Int): Flow<Long>

    @Query(
        """
        SELECT p.id AS id, p.shopId AS shopId, p.dateMillis AS dateMillis, p.dayKey AS dayKey,
               p.description AS description, p.totalPaisa AS totalPaisa, p.dueDateMillis AS dueDateMillis,
               p.receiptNo AS receiptNo, p.isArchived AS isArchived,
               COALESCE((SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0) AS paidPaisa
        FROM purchases p
        WHERE p.shopId = :shopId AND p.isArchived = 0
        ORDER BY p.dateMillis DESC, p.id DESC
        """
    )
    fun observeShopPurchasesWithPaid(shopId: Long): Flow<List<PurchaseWithPaidRow>>

    @Query(
        """
        SELECT shopId,
               COALESCE(SUM(totalPaisa), 0) AS totalPurchases,
               COALESCE((SELECT SUM(t.amountPaisa) FROM txns t JOIN purchases p2 ON t.purchaseId = p2.id
                         WHERE p2.shopId = s.shopId AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0) AS totalPaid,
               COUNT(*) AS purchaseCount
        FROM purchases s
        WHERE s.isArchived = 0
        GROUP BY shopId
        """
    )
    fun observeShopTotals(): Flow<List<ShopTotalsRow>>

    @Query("SELECT * FROM shops WHERE id IN (SELECT DISTINCT shopId FROM purchases WHERE isArchived = 0)")
    fun observeShopsWithPurchases(): Flow<List<ShopEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(p.totalPaisa - COALESCE((SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0)), 0)
        FROM purchases p WHERE p.isArchived = 0
        """
    )
    fun observeTotalShopOutstanding(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(p.totalPaisa - COALESCE((SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0)), 0)
        FROM purchases p WHERE p.isArchived = 0
        """
    )
    suspend fun totalOutstandingSuspend(): Long

    @Query(
        """
        SELECT COUNT(*) FROM purchases p
        WHERE (SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT') > p.totalPaisa
        """
    )
    suspend fun purchasesOverpaidCount(): Int

    @Query(
        """
        SELECT COALESCE(SUM(p.totalPaisa - COALESCE((SELECT SUM(t.amountPaisa) FROM txns t WHERE t.purchaseId = p.id AND t.status = 'ACTIVE' AND t.type = 'SHOP_PAYMENT'), 0)), 0)
        FROM purchases p
        WHERE p.isArchived = 0 AND p.dueDateMillis IS NOT NULL AND p.dueDateMillis < :now
        """
    )
    fun observeTotalShopOverdue(now: Long): Flow<Long>

    @Transaction
    suspend fun replaceItems(purchaseId: Long, items: List<PurchaseItemEntity>) {
        deleteItemsFor(purchaseId)
        insertItems(items.map { it.copy(purchaseId = purchaseId) })
    }
}
