package com.shohankhan.hisaabdesk.domain.shopping

import com.shohankhan.hisaabdesk.core.formatting.Money

/**
 * Exact monetary arithmetic for itemized shopping purchases.
 *
 * Discount policy (docs/FINANCIAL_RULES.md):
 *  1. lineTotal = quantity × unitPrice  (quantity scaled by 1000, exact integer math)
 *  2. line net  = lineTotal − lineDiscount + lineCharge   (both optional)
 *  3. payable   = Σ line nets − orderDiscount + orderCharges
 * Discount and payment amounts may never push totals below zero.
 */
object PurchaseCalculator {

    const val QTY_SCALE = 1000L

    /** quantity (decimal) × unit price → line total, exact half-up to paisa. */
    fun lineTotal(quantityMilli: Long, unitPricePaisa: Long): Long {
        require(quantityMilli >= 0) { "quantity must be >= 0" }
        require(unitPricePaisa >= 0) { "unit price must be >= 0" }
        val product = java.math.BigDecimal(quantityMilli)
            .multiply(java.math.BigDecimal(unitPricePaisa))
            .divide(java.math.BigDecimal(QTY_SCALE), 0, java.math.RoundingMode.HALF_UP)
        return product.longValueExact().also {
            Money.safeAdd(it, 0) // bounds check
        }
    }

    data class LineInput(
        val quantityMilli: Long,
        val unitPricePaisa: Long,
        val discountPaisa: Long = 0,
        val chargePaisa: Long = 0
    )

    data class Totals(
        val grossPaisa: Long,
        val discountPaisa: Long,
        val chargesPaisa: Long,
        val payablePaisa: Long
    )

    fun totals(lines: List<LineInput>, orderDiscountPaisa: Long = 0, orderChargesPaisa: Long = 0): Totals {
        require(orderDiscountPaisa >= 0 && orderChargesPaisa >= 0) { "invalid order amounts" }
        var gross = 0L
        var discounts = orderDiscountPaisa
        var charges = orderChargesPaisa
        for (line in lines) {
            val lt = lineTotal(line.quantityMilli, line.unitPricePaisa)
            require(line.discountPaisa >= 0 && line.chargePaisa >= 0) { "invalid line amounts" }
            require(line.discountPaisa <= lt) { "line discount exceeds line total" }
            gross = Money.safeAdd(gross, lt)
            discounts = Money.safeAdd(discounts, line.discountPaisa)
            charges = Money.safeAdd(charges, line.chargePaisa)
        }
        require(discounts <= Money.safeAdd(gross, 0)) { "total discount exceeds gross" }
        val payable = Money.safeSub(Money.safeAdd(gross, charges), discounts)
        require(payable >= 0) { "payable cannot be negative" }
        return Totals(gross, discounts, charges, payable)
    }

    /** remaining outstanding = payable − paid, clamped policy: overpayment rejected upstream. */
    fun outstanding(totalPaisa: Long, paidPaisa: Long): Long {
        require(paidPaisa >= 0) { "paid must be >= 0" }
        require(paidPaisa <= totalPaisa) { "overpayment not allowed" }
        return Money.safeSub(totalPaisa, paidPaisa)
    }
}
