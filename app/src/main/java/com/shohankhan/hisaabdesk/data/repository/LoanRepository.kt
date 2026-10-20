package com.shohankhan.hisaabdesk.data.repository

import androidx.room.withTransaction
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.dao.LoanDao
import com.shohankhan.hisaabdesk.data.local.db.HisaabDatabase
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentEntity
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentStatus
import com.shohankhan.hisaabdesk.data.local.entity.LoanEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.domain.loan.LoanCalculator
import kotlinx.coroutines.flow.Flow

class LoanRepository(
    private val db: HisaabDatabase,
    private val loanDao: LoanDao,
    private val txnRepository: TransactionRepository
) {
    class ValidationException(message: String) : Exception(message)

    fun observeLoans(kind: String) = loanDao.observeByKind(kind)
    fun observeLoan(id: Long) = loanDao.observeById(id)
    fun observeInstallments(loanId: Long) = loanDao.observeInstallments(loanId)
    fun observeUpcomingInstallments(until: Long, limit: Int) = loanDao.observeUpcomingInstallments(until, limit)
    fun observeOverdueInstallments(now: Long, limit: Int) = loanDao.observeOverdueInstallments(now, limit)
    fun observeOverdueScheduled(now: Long) = loanDao.observeOverdueScheduled(now)
    fun observeLoanPayments(loanId: Long) = db.txnDao().observeLoanPayments(loanId)
    fun observeLoanPrincipalPaid(loanId: Long) = db.txnDao().observeLoanPrincipalPaid(loanId)
    fun observeLoanInterestPaid(loanId: Long) = db.txnDao().observeLoanInterestPaid(loanId)
    fun observeLoanFeesPaid(loanId: Long) = db.txnDao().observeLoanFeesPaid(loanId)
    suspend fun getLoan(id: Long) = loanDao.getById(id)
    suspend fun getInstallments(loanId: Long) = loanDao.getInstallments(loanId)

    fun observeTotalPrincipalByKind(kind: String): Flow<Long> = loanDao.observePrincipalByKind(kind)

    /** Total outstanding principal estimate = principal − principal components paid. */
    suspend fun outstandingPrincipal(loanId: Long): Long {
        val loan = loanDao.getById(loanId) ?: return 0
        val paid = db.txnDao().sumLoanPrincipalPaid(loanId)
        return (loan.principalPaisa - paid).coerceAtLeast(0)
    }

    data class ScheduleInput(
        val mode: LoanCalculator.ScheduleMode,
        val frequency: Dates.Frequency,
        val intervalDays: Int,
        val installmentAmountPaisa: Long,
        val installmentStartMillis: Long,
        val expectedInstallments: Int,
        val contractualTotalPaisa: Long?
    )

    /**
     * Creates/updates a loan and (re)generates its installment schedule from
     * explicit inputs. Schedule figures produced from a rate are model estimates.
     */
    suspend fun saveLoan(
        loan: LoanEntity,
        scheduleInput: ScheduleInput,
        disburseAccountId: Long?,
        method: PaymentMethod,
        regenerateSchedule: Boolean
    ): Long = db.withTransaction {
        if (loan.principalPaisa <= 0) throw ValidationException("মূল ঋণ শূন্যের বেশি হতে হবে")
        if (scheduleInput.expectedInstallments !in 1..1000) {
            throw ValidationException("কিস্তির সংখ্যা ১ থেকে ১০০০ এর মধ্যে হতে হবে")
        }
        val now = System.currentTimeMillis()
        val loanId = if (loan.id == 0L) {
            loanDao.insert(loan.copy(dayKey = loan.dayKey.takeIf { it != 0 } ?: Dates.dayKey(loan.startDateMillis), createdAt = now, updatedAt = now))
        } else {
            loanDao.update(loan.copy(updatedAt = now))
            loan.id
        }

        if (regenerateSchedule) {
            val existingPayments = db.txnDao().sumLoanPayments(loanId)
            if (existingPayments > 0) {
                throw ValidationException("পরিশোধের ইতিহাস থাকায় কিস্তির সূচি নতুন করে তৈরি করা যায় না")
            }
            val schedule = LoanCalculator.generateSchedule(
                principalPaisa = loan.principalPaisa,
                mode = scheduleInput.mode,
                ratePercent = loan.ratePercent,
                startDate = Dates.localDateOf(scheduleInput.installmentStartMillis),
                count = scheduleInput.expectedInstallments,
                frequency = scheduleInput.frequency,
                intervalDays = scheduleInput.intervalDays,
                contractualTotalPaisa = scheduleInput.contractualTotalPaisa,
                installmentAmountPaisa = scheduleInput.installmentAmountPaisa
            )
            loanDao.deleteInstallmentsFor(loanId)
            loanDao.insertInstallments(
                schedule.map {
                    InstallmentEntity(
                        loanId = loanId,
                        seq = it.seq,
                        dueDateMillis = Dates.millisOf(it.dueDate),
                        dueDayKey = Dates.dayKey(it.dueDate),
                        scheduledPaisa = it.scheduledPaisa,
                        principalPaisa = it.principalPaisa,
                        interestPaisa = it.interestPaisa
                    )
                }
            )
        }

        if (loan.id == 0L && disburseAccountId != null) {
            txnRepository.insert(
                TxnEntity(
                    type = TxnType.LOAN_DISBURSE,
                    flow = TxnFlow.INFLOW,
                    amountPaisa = loan.principalPaisa,
                    dateMillis = loan.startDateMillis,
                    dayKey = Dates.dayKey(loan.startDateMillis),
                    accountId = disburseAccountId,
                    loanId = loanId,
                    paymentMethod = method,
                    description = "ঋণ অর্থ গ্রহণ: ${loan.institution}",
                    note = loan.notes,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        loanId
    }

    /**
     * Records an installment payment. When [installmentId] is null the payment is
     * applied to the oldest unpaid installment (FIFO) for reporting purposes.
     * Overpayment of a specific installment is rejected.
     */
    suspend fun addPayment(
        loanId: Long,
        installmentId: Long?,
        amountPaisa: Long,
        principalPart: Long,
        interestPart: Long,
        feePart: Long,
        accountId: Long?,
        method: PaymentMethod,
        dateMillis: Long,
        receiptNo: String,
        note: String
    ): Long = db.withTransaction {
        val loan = loanDao.getById(loanId) ?: throw ValidationException("ঋণ পাওয়া যায়নি")
        if (amountPaisa <= 0) throw ValidationException("পরিমাণ শূন্যের বেশি হতে হবে")
        if (accountId == null) throw ValidationException("হিসাবের অ্যাকাউন্ট নির্বাচন করুন")
        val split = LoanCalculator.splitPayment(amountPaisa, principalPart, interestPart, feePart)

        val target = installmentId?.let { loanDao.getInstallment(it) }
            ?: loanDao.getInstallments(loanId).firstOrNull { it.status != InstallmentStatus.PAID }
        if (target != null && target.loanId != loanId) {
            throw ValidationException("কিস্তিটি এই ঋণের নয়")
        }
        if (target != null && amountPaisa > target.scheduledPaisa - target.paidPaisa) {
            throw ValidationException("কিস্তির বাকির চেয়ে বেশি পরিশোধ — অতিরিক্ত পরিশোধ হিসেবে আলাদা লিখুন")
        }

        val now = System.currentTimeMillis()
        val txnId = txnRepository.insert(
            TxnEntity(
                type = TxnType.LOAN_INSTALLMENT,
                flow = TxnFlow.OUTFLOW,
                amountPaisa = amountPaisa,
                principalPaisa = split.principalPaisa,
                interestPaisa = split.interestPaisa,
                feePaisa = split.feePaisa,
                dateMillis = dateMillis,
                dayKey = Dates.dayKey(dateMillis),
                accountId = accountId,
                loanId = loanId,
                installmentId = target?.id,
                paymentMethod = method,
                description = "${loan.institution} ঋণের কিস্তি",
                receiptNo = receiptNo,
                note = note,
                createdAt = now,
                updatedAt = now
            )
        )
        if (target != null) {
            val paid = target.paidPaisa + amountPaisa
            val status = when {
                paid >= target.scheduledPaisa -> InstallmentStatus.PAID
                paid > 0 -> InstallmentStatus.PARTIAL
                else -> InstallmentStatus.UNPAID
            }
            loanDao.updateInstallment(target.copy(paidPaisa = paid, status = status))
        }
        maybeMarkCompleted(loanId, now)
        txnId
    }

    private suspend fun maybeMarkCompleted(loanId: Long, now: Long) {
        val loan = loanDao.getById(loanId) ?: return
        val principalPaid = db.txnDao().sumLoanPrincipalPaid(loanId)
        val installments = loanDao.getInstallments(loanId)
        val allPaid = installments.isNotEmpty() && installments.all { it.status == InstallmentStatus.PAID }
        // A loan is only completed when principal is repaid AND every scheduled
        // installment is settled — principal-only zero is NOT enough.
        val completed = allPaid && principalPaid >= loan.principalPaisa
        loanDao.setCompleted(loanId, completed, now)
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        loanDao.getById(id)?.let { loanDao.update(it.copy(isArchived = archived, updatedAt = System.currentTimeMillis())) }
    }
}
