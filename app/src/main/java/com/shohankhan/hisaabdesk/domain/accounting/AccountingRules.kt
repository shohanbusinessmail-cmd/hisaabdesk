package com.shohankhan.hisaabdesk.domain.accounting

import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType

/**
 * Central accounting classification. UI and reports must never invent their own
 * rules — everything is derived from here so dashboard, reports and tests agree.
 */
object AccountingRules {

    /** Default account flow for a txn type. ADJUSTMENT/REFUND are caller-decided. */
    fun flowFor(type: TxnType): TxnFlow = when (type) {
        TxnType.INCOME -> TxnFlow.INFLOW
        TxnType.SAVINGS_INTEREST -> TxnFlow.INFLOW
        TxnType.LEND_COLLECT -> TxnFlow.INFLOW
        TxnType.BORROW -> TxnFlow.INFLOW
        TxnType.LOAN_DISBURSE -> TxnFlow.INFLOW
        TxnType.RECEIVABLE_COLLECT -> TxnFlow.INFLOW
        TxnType.RECEIVABLE_CREATE -> TxnFlow.OUTFLOW
        TxnType.PAYABLE_CREATE -> TxnFlow.INFLOW
        TxnType.REFUND -> TxnFlow.INFLOW
        TxnType.ADJUSTMENT -> TxnFlow.INFLOW
        TxnType.EXPENSE -> TxnFlow.OUTFLOW
        TxnType.LEND -> TxnFlow.OUTFLOW
        TxnType.BORROW_REPAY -> TxnFlow.OUTFLOW
        TxnType.LOAN_INSTALLMENT -> TxnFlow.OUTFLOW
        TxnType.PAYABLE_PAY -> TxnFlow.OUTFLOW
        TxnType.SHOP_PAYMENT -> TxnFlow.OUTFLOW
        TxnType.TRANSFER -> TxnFlow.TRANSFER
        TxnType.SAVINGS_DEPOSIT -> TxnFlow.TRANSFER
        TxnType.SAVINGS_WITHDRAWAL -> TxnFlow.TRANSFER
    }

    fun requiresAccount(type: TxnType): Boolean = when (type) {
        TxnType.RECEIVABLE_CREATE, TxnType.PAYABLE_CREATE -> false
        else -> true
    }

    fun requiresCounterAccount(type: TxnType): Boolean = when (type) {
        TxnType.TRANSFER, TxnType.SAVINGS_DEPOSIT, TxnType.SAVINGS_WITHDRAWAL -> true
        else -> false
    }

    /** True when the type counts toward ordinary income totals. */
    fun isIncome(type: TxnType): Boolean = when (type) {
        TxnType.INCOME, TxnType.SAVINGS_INTEREST -> true
        else -> false
    }

    /** True when the type (or its interest/fee component) counts as expense. */
    fun isExpense(type: TxnType): Boolean = type == TxnType.EXPENSE

    fun hasInterestComponent(type: TxnType): Boolean = when (type) {
        TxnType.BORROW_REPAY, TxnType.LEND_COLLECT, TxnType.LOAN_INSTALLMENT -> true
        else -> false
    }

    /**
     * Component integrity: whenever components are recorded they must sum to the
     * total amount. Enforced before persistence.
     */
    fun componentsConsistent(amountPaisa: Long, principal: Long, interest: Long, fee: Long): Boolean {
        val sum = principal + interest + fee
        return when {
            principal == 0L && interest == 0L && fee == 0L -> true
            else -> sum == amountPaisa
        }
    }

    /**
     * Net-worth classification (docs/FINANCIAL_RULES.md §Net worth):
     *  assets = account balances (incl. savings) + lending outstanding + receivable remaining
     *  liabilities = borrowing outstanding + payable remaining + shop-credit outstanding
     *               + loan outstanding principal (NGO + bank)
     * Internal transfers never change net worth because they only move value
     * between accounts.
     */
    fun netWorth(assets: Long, liabilities: Long): Long = assets - liabilities
}
