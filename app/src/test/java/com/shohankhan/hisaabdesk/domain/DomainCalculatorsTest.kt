package com.shohankhan.hisaabdesk.domain

import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.domain.accounting.AccountingRules
import com.shohankhan.hisaabdesk.domain.loan.LoanCalculator
import com.shohankhan.hisaabdesk.domain.shopping.PurchaseCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PurchaseCalculatorTest {

    @Test
    fun `line total multiplies decimal quantity exactly`() {
        // 2.5 kg × ৳120.00 = ৳300.00
        assertEquals(30_000L, PurchaseCalculator.lineTotal(2_500, 12_000))
        // 0.333 kg × ৳100 = ৳33.30 (rounded half-up from 33.3)
        assertEquals(3_330L, PurchaseCalculator.lineTotal(333, 10_000))
    }

    @Test
    fun `totals apply line discounts charges and order level adjustments`() {
        val totals = PurchaseCalculator.totals(
            lines = listOf(
                PurchaseCalculator.LineInput(1_000, 10_000), // 100.00
                PurchaseCalculator.LineInput(2_000, 5_050, discountPaisa = 100, chargePaisa = 50) // 101.00 -1.00 +0.50
            ),
            orderDiscountPaisa = 200,
            orderChargesPaisa = 300
        )
        assertEquals(20_100L, totals.grossPaisa)
        assertEquals(300L, totals.discountPaisa)
        assertEquals(350L, totals.chargesPaisa)
        assertEquals(20_150L, totals.payablePaisa)
    }

    @Test
    fun `rejects discount larger than line total`() {
        var threw = false
        try {
            PurchaseCalculator.totals(listOf(PurchaseCalculator.LineInput(1_000, 100, discountPaisa = 200)))
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `rejects negative payable`() {
        var threw = false
        try {
            PurchaseCalculator.totals(
                listOf(PurchaseCalculator.LineInput(1_000, 100)),
                orderDiscountPaisa = 500
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `outstanding never allows overpayment`() {
        assertEquals(4_000L, PurchaseCalculator.outstanding(10_000, 6_000))
        var threw = false
        try {
            PurchaseCalculator.outstanding(10_000, 10_001)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }
}

class LoanCalculatorTest {

    @Test
    fun `interest free schedule splits principal evenly with remainder in last`() {
        val schedule = LoanCalculator.generateSchedule(
            principalPaisa = 10_000,
            mode = LoanCalculator.ScheduleMode.NONE,
            ratePercent = 0.0,
            startDate = LocalDate.of(2024, 1, 10),
            count = 3,
            frequency = Dates.Frequency.MONTHLY,
            intervalDays = 0
        )
        assertEquals(3, schedule.size)
        assertEquals(10_000L, schedule.sumOf { it.scheduledPaisa })
        assertEquals(0L, schedule.sumOf { it.interestPaisa })
        assertEquals(3_334L, schedule[0].scheduledPaisa)
        assertEquals(3_332L, schedule[2].scheduledPaisa)
    }

    @Test
    fun `flat interest adds rate over term`() {
        val schedule = LoanCalculator.generateSchedule(
            principalPaisa = 120_000, // ৳1200
            mode = LoanCalculator.ScheduleMode.FLAT,
            ratePercent = 12.0,
            startDate = LocalDate.of(2024, 1, 1),
            count = 12,
            frequency = Dates.Frequency.MONTHLY,
            intervalDays = 0
        )
        // 12% flat for 1 year = 14,400 paisa interest → total 134,400
        assertEquals(134_400L, schedule.sumOf { it.scheduledPaisa })
        assertEquals(14_400L, schedule.sumOf { it.interestPaisa })
        // per-installment proportional rounding may drift by at most 1 paisa per row
        assertEquals(120_000L, schedule.sumOf { it.principalPaisa }.toDouble(), 12.0)
    }

    @Test
    fun `reducing balance total is between principal and flat`() {
        val reducing = LoanCalculator.annuityTotal(100_000, 12.0, Dates.Frequency.MONTHLY, 0, 12)
        assertTrue(reducing > 100_000)
        val flat = 100_000 + 12_000
        assertTrue("reducing ($reducing) should be below flat ($flat)", reducing < flat)
    }

    @Test
    fun `contract total schedule uses provided repayment total`() {
        val schedule = LoanCalculator.generateSchedule(
            principalPaisa = 58_000,
            mode = LoanCalculator.ScheduleMode.CONTRACT_TOTAL,
            ratePercent = 0.0,
            startDate = LocalDate.of(2024, 3, 15),
            count = 20,
            frequency = Dates.Frequency.WEEKLY,
            intervalDays = 0,
            contractualTotalPaisa = 64_000
        )
        assertEquals(64_000L, schedule.sumOf { it.scheduledPaisa })
        assertEquals(20, schedule.size)
        assertEquals(LocalDate.of(2024, 3, 15), schedule.first().dueDate)
        assertEquals(LocalDate.of(2024, 3, 22), schedule[1].dueDate)
    }

    @Test
    fun `schedule rejects zero principal`() {
        var threw = false
        try {
            LoanCalculator.generateSchedule(0, LoanCalculator.ScheduleMode.NONE, 0.0, LocalDate.now(), 3, Dates.Frequency.MONTHLY, 0)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `payment split defaults to principal and validates components`() {
        val split = LoanCalculator.splitPayment(5_000, 0, 0, 0)
        assertEquals(5_000L, split.principalPaisa)
        val explicit = LoanCalculator.splitPayment(5_000, 4_000, 1_000, 0)
        assertEquals(4_000L, explicit.principalPaisa)
        var threw = false
        try {
            LoanCalculator.splitPayment(5_000, 4_000, 500, 0)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `simple interest estimate uses 365 day basis`() {
        // ৳10,000 at 10% for 365 days = ৳1,000
        assertEquals(100_000L, LoanCalculator.simpleInterest(1_000_000, 10.0, 365))
        assertEquals(49_863L, LoanCalculator.simpleInterest(1_000_000, 10.0, 182))
    }
}

class AccountingRulesTest {

    @Test
    fun `transfer types never count as income or expense`() {
        assertFalse(AccountingRules.isIncome(TxnType.TRANSFER))
        assertFalse(AccountingRules.isExpense(TxnType.TRANSFER))
        assertFalse(AccountingRules.isIncome(TxnType.SAVINGS_WITHDRAWAL))
        assertFalse(AccountingRules.isExpense(TxnType.SAVINGS_DEPOSIT))
        assertEquals(TxnFlow.TRANSFER, AccountingRules.flowFor(TxnType.SAVINGS_DEPOSIT))
        assertEquals(TxnFlow.TRANSFER, AccountingRules.flowFor(TxnType.SAVINGS_WITHDRAWAL))
    }

    @Test
    fun `loan proceeds and principal repayments are not income or expense`() {
        assertFalse(AccountingRules.isIncome(TxnType.LOAN_DISBURSE))
        assertFalse(AccountingRules.isExpense(TxnType.LOAN_INSTALLMENT))
        assertFalse(AccountingRules.isIncome(TxnType.LEND_COLLECT))
        assertFalse(AccountingRules.isExpense(TxnType.BORROW_REPAY))
        assertTrue(AccountingRules.hasInterestComponent(TxnType.BORROW_REPAY))
    }

    @Test
    fun `components must sum to amount when present`() {
        assertTrue(AccountingRules.componentsConsistent(1000, 0, 0, 0))
        assertTrue(AccountingRules.componentsConsistent(1000, 700, 200, 100))
        assertFalse(AccountingRules.componentsConsistent(1000, 700, 200, 200))
    }

    @Test
    fun `account requirements per type`() {
        assertFalse(AccountingRules.requiresAccount(TxnType.RECEIVABLE_CREATE))
        assertTrue(AccountingRules.requiresAccount(TxnType.INCOME))
        assertTrue(AccountingRules.requiresCounterAccount(TxnType.TRANSFER))
        assertFalse(AccountingRules.requiresCounterAccount(TxnType.EXPENSE))
    }

    @Test
    fun `net worth subtracts liabilities from assets`() {
        assertEquals(60_000L, AccountingRules.netWorth(100_000, 40_000))
        assertEquals(-5_000L, AccountingRules.netWorth(10_000, 15_000))
    }
}
