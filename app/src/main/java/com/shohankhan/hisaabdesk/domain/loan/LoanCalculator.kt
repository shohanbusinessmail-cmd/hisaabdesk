package com.shohankhan.hisaabdesk.domain.loan

import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Loan schedule generation and payment allocation.
 *
 * IMPORTANT honesty policy (docs/FINANCIAL_RULES.md):
 *  - Schedules generated from a rate are MODELS. They are labelled as estimates.
 *  - When the exact bank contract is unknown, the user enters the contractual
 *    total or the actual installment amount/count — we never invent a contract.
 *  - No compound-interest assumption is made unless the user explicitly selects
 *    a reducing-balance model.
 *  - Month arithmetic is calendar-aware (Jan 31 → Feb 28/29), never +30 days.
 */
object LoanCalculator {

    data class ScheduledInstallment(
        val seq: Int,
        val dueDate: LocalDate,
        val scheduledPaisa: Long,
        val principalPaisa: Long,
        val interestPaisa: Long
    )

    enum class ScheduleMode {
        /** No interest: equal (or given) installments of principal only. */
        NONE,
        /** Flat interest: total interest = principal × rate × years, spread equally. */
        FLAT,
        /** Reducing-balance annuity model (labelled as model). */
        REDUCING_MODEL,
        /** Simple interest on original principal (labelled as model). */
        SIMPLE_MODEL,
        /** User provides contractual total repayment; spread across installments. */
        CONTRACT_TOTAL
    }

    /**
     * Generates an installment schedule from explicit inputs.
     *
     * @param installmentAmount when > 0, used as the installment amount (last
     *        installment absorbs the rounding remainder); when 0 the amount is
     *        derived from the mode.
     */
    fun generateSchedule(
        principalPaisa: Long,
        mode: ScheduleMode,
        ratePercent: Double,
        startDate: LocalDate,
        count: Int,
        frequency: Dates.Frequency,
        intervalDays: Int,
        contractualTotalPaisa: Long? = null,
        installmentAmountPaisa: Long = 0
    ): List<ScheduledInstallment> {
        require(principalPaisa > 0) { "principal must be > 0" }
        require(count in 1..1000) { "installment count out of range" }
        require(ratePercent >= 0) { "rate must be >= 0" }

        val dueDates = Dates.generateDueDates(startDate, count, frequency, intervalDays)
        val totalRepayment: Long = when (mode) {
            ScheduleMode.NONE -> principalPaisa
            ScheduleMode.CONTRACT_TOTAL -> {
                requireNotNull(contractualTotalPaisa) { "contractual total required" }
                require(contractualTotalPaisa >= principalPaisa) { "total repayment below principal" }
                contractualTotalPaisa
            }
            ScheduleMode.FLAT -> {
                // totalInterest = principal * rate/100 * years ; years = count * periodYears
                val periodYears = when (frequency) {
                    Dates.Frequency.WEEKLY -> 7.0 / 365.0
                    Dates.Frequency.MONTHLY -> 1.0 / 12.0
                    Dates.Frequency.CUSTOM_DAYS -> intervalDays.coerceAtLeast(1) / 365.0
                }
                val years = count * periodYears
                val interest = java.math.BigDecimal(principalPaisa)
                    .multiply(java.math.BigDecimal(ratePercent))
                    .multiply(java.math.BigDecimal(years))
                    .divide(java.math.BigDecimal(100), 0, RoundingMode.HALF_UP)
                    .longValueExact()
                Money.safeAdd(principalPaisa, interest)
            }
            ScheduleMode.SIMPLE_MODEL -> {
                val periodYears = when (frequency) {
                    Dates.Frequency.WEEKLY -> 7.0 / 365.0
                    Dates.Frequency.MONTHLY -> 1.0 / 12.0
                    Dates.Frequency.CUSTOM_DAYS -> intervalDays.coerceAtLeast(1) / 365.0
                }
                // Simple interest: each installment carries interest on the original
                // principal for the periods it still covers (declining slice).
                var totalInterest = 0L
                for (i in 0 until count) {
                    val remainingPeriods = count - i
                    val interestSlice = java.math.BigDecimal(principalPaisa)
                        .multiply(java.math.BigDecimal(ratePercent))
                        .multiply(java.math.BigDecimal(periodYears))
                        .multiply(java.math.BigDecimal(remainingPeriods))
                        .divide(java.math.BigDecimal(count), 0, RoundingMode.HALF_UP)
                        .divide(java.math.BigDecimal(100), 0, RoundingMode.HALF_UP)
                        .longValueExact()
                    totalInterest = Money.safeAdd(totalInterest, interestSlice)
                }
                Money.safeAdd(principalPaisa, totalInterest)
            }
            ScheduleMode.REDUCING_MODEL -> annuityTotal(principalPaisa, ratePercent, frequency, intervalDays, count)
        }

        val baseInstallment = if (installmentAmountPaisa > 0) {
            installmentAmountPaisa
        } else {
            BigDecimal(totalRepayment).divide(BigDecimal(count), 0, RoundingMode.CEILING).longValueExact()
        }

        val result = ArrayList<ScheduledInstallment>(count)
        var allocated = 0L
        for (i in 0 until count) {
            val isLast = i == count - 1
            val scheduled = if (isLast) {
                Money.safeSub(totalRepayment, allocated)
            } else {
                val amt = minOf(baseInstallment, Money.safeSub(totalRepayment, allocated))
                amt
            }
            allocated = Money.safeAdd(allocated, scheduled)

            // principal/interest split
            val principalPart: Long
            val interestPart: Long
            when (mode) {
                ScheduleMode.NONE -> {
                    principalPart = scheduled
                    interestPart = 0
                }
                ScheduleMode.CONTRACT_TOTAL -> {
                    // Proportional split of the contractual spread
                    val principalSlice = BigDecimal(scheduled)
                        .multiply(BigDecimal(principalPaisa))
                        .divide(BigDecimal(totalRepayment), 0, RoundingMode.HALF_UP)
                        .longValueExact()
                    principalPart = principalSlice
                    interestPart = Money.safeSub(scheduled, principalSlice)
                }
                ScheduleMode.REDUCING_MODEL -> {
                    val r = reducingSplit(principalPaisa, ratePercent, frequency, intervalDays, count, i)
                    principalPart = r.first
                    interestPart = r.second
                }
                ScheduleMode.FLAT, ScheduleMode.SIMPLE_MODEL -> {
                    val principalSlice = BigDecimal(scheduled)
                        .multiply(BigDecimal(principalPaisa))
                        .divide(BigDecimal(totalRepayment), 0, RoundingMode.HALF_UP)
                        .longValueExact()
                    principalPart = principalSlice
                    interestPart = Money.safeSub(scheduled, principalSlice)
                }
            }
            result.add(
                ScheduledInstallment(
                    seq = i + 1,
                    dueDate = dueDates[i],
                    scheduledPaisa = scheduled,
                    principalPaisa = principalPart,
                    interestPaisa = interestPart
                )
            )
        }
        return result
    }

    private fun periodRateFraction(ratePercent: Double, frequency: Dates.Frequency, intervalDays: Int): BigDecimal {
        val annual = BigDecimal(ratePercent).divide(BigDecimal(100), MathContext.DECIMAL128)
        return when (frequency) {
            Dates.Frequency.MONTHLY -> annual.divide(BigDecimal(12), MathContext.DECIMAL128)
            Dates.Frequency.WEEKLY -> annual.multiply(BigDecimal(7)).divide(BigDecimal(365), MathContext.DECIMAL128)
            Dates.Frequency.CUSTOM_DAYS -> annual.multiply(BigDecimal(intervalDays.coerceAtLeast(1))).divide(BigDecimal(365), MathContext.DECIMAL128)
        }
    }

    /** Level annuity payment. Returns total repayment. */
    fun annuityTotal(
        principalPaisa: Long,
        ratePercent: Double,
        frequency: Dates.Frequency,
        intervalDays: Int,
        count: Int
    ): Long {
        val r = periodRateFraction(ratePercent, frequency, intervalDays)
        if (ratePercent == 0.0) return principalPaisa
        val one = BigDecimal.ONE
        val growth = one.add(r).pow(count, MathContext.DECIMAL128)
        val payment = BigDecimal(principalPaisa)
            .multiply(r)
            .multiply(growth)
            .divide(growth.subtract(one), MathContext.DECIMAL128)
        return payment.multiply(BigDecimal(count)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    /** (principal, interest) for installment index i of a reducing-balance schedule. */
    fun reducingSplit(
        principalPaisa: Long,
        ratePercent: Double,
        frequency: Dates.Frequency,
        intervalDays: Int,
        count: Int,
        index: Int
    ): Pair<Long, Long> {
        val r = periodRateFraction(ratePercent, frequency, intervalDays)
        val payment = if (ratePercent == 0.0) {
            BigDecimal(principalPaisa).divide(BigDecimal(count), MathContext.DECIMAL128)
        } else {
            val growth = BigDecimal.ONE.add(r).pow(count, MathContext.DECIMAL128)
            BigDecimal(principalPaisa)
                .multiply(r)
                .multiply(growth)
                .divide(growth.subtract(BigDecimal.ONE), MathContext.DECIMAL128)
        }
        var balance = BigDecimal(principalPaisa)
        for (i in 0..index) {
            val interest = balance.multiply(r)
            var principalPart = payment.subtract(interest)
            if (i == count - 1) principalPart = balance
            if (i == index) {
                val p = principalPart.setScale(0, RoundingMode.HALF_UP).longValueExact()
                val pay = payment.setScale(0, RoundingMode.HALF_UP).longValueExact()
                val intPart = Money.safeSub(pay, p)
                return Pair(p, intPart.coerceAtLeast(0))
            }
            balance = balance.subtract(principalPart)
        }
        error("unreachable")
    }

    /**
     * Allocates a payment amount across principal/interest/fee. When the caller
     * does not know the split, the whole payment is applied to principal and the
     * UI says so (docs/FINANCIAL_RULES.md §Payment allocation).
     */
    data class PaymentSplit(val principalPaisa: Long, val interestPaisa: Long, val feePaisa: Long)

    fun splitPayment(amountPaisa: Long, principal: Long, interest: Long, fee: Long): PaymentSplit {
        require(amountPaisa > 0) { "amount must be > 0" }
        require(principal >= 0 && interest >= 0 && fee >= 0) { "negative component" }
        val sum = principal + interest + fee
        return if (sum == 0L) {
            PaymentSplit(amountPaisa, 0, 0)
        } else {
            require(sum == amountPaisa) { "components must sum to amount" }
            PaymentSplit(principal, interest, fee)
        }
    }

    /**
     * Simple interest estimate for personal borrowing/lending:
     * interest = principal × rate/100 × days/365
     */
    fun simpleInterest(principalPaisa: Long, ratePercent: Double, days: Long): Long {
        require(days >= 0) { "days must be >= 0" }
        return BigDecimal(principalPaisa)
            .multiply(BigDecimal(ratePercent))
            .multiply(BigDecimal(days))
            .divide(BigDecimal(36500), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
