package com.shohankhan.hisaabdesk.data.local.db

import androidx.room.TypeConverter
import com.shohankhan.hisaabdesk.data.local.entity.AccountType
import com.shohankhan.hisaabdesk.data.local.entity.BudgetPeriod
import com.shohankhan.hisaabdesk.data.local.entity.CategoryKind
import com.shohankhan.hisaabdesk.data.local.entity.InstallmentStatus
import com.shohankhan.hisaabdesk.data.local.entity.InterestMethod
import com.shohankhan.hisaabdesk.data.local.entity.InterestPolicy
import com.shohankhan.hisaabdesk.data.local.entity.LoanKind
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.RecurringKind
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnStatus
import com.shohankhan.hisaabdesk.data.local.entity.TxnType

class Converters {
    @TypeConverter fun accountTypeToString(v: AccountType): String = v.name
    @TypeConverter fun stringToAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun categoryKindToString(v: CategoryKind): String = v.name
    @TypeConverter fun stringToCategoryKind(v: String): CategoryKind = CategoryKind.valueOf(v)

    @TypeConverter fun txnTypeToString(v: TxnType): String = v.name
    @TypeConverter fun stringToTxnType(v: String): TxnType = TxnType.valueOf(v)

    @TypeConverter fun txnStatusToString(v: TxnStatus): String = v.name
    @TypeConverter fun stringToTxnStatus(v: String): TxnStatus = TxnStatus.valueOf(v)

    @TypeConverter fun txnFlowToString(v: TxnFlow): String = v.name
    @TypeConverter fun stringToTxnFlow(v: String): TxnFlow = TxnFlow.valueOf(v)

    @TypeConverter fun paymentMethodToString(v: PaymentMethod): String = v.name
    @TypeConverter fun stringToPaymentMethod(v: String): PaymentMethod = PaymentMethod.valueOf(v)

    @TypeConverter fun interestPolicyToString(v: InterestPolicy): String = v.name
    @TypeConverter fun stringToInterestPolicy(v: String): InterestPolicy = InterestPolicy.valueOf(v)

    @TypeConverter fun interestMethodToString(v: InterestMethod): String = v.name
    @TypeConverter fun stringToInterestMethod(v: String): InterestMethod = InterestMethod.valueOf(v)

    @TypeConverter fun loanKindToString(v: LoanKind): String = v.name
    @TypeConverter fun stringToLoanKind(v: String): LoanKind = LoanKind.valueOf(v)

    @TypeConverter fun installmentStatusToString(v: InstallmentStatus): String = v.name
    @TypeConverter fun stringToInstallmentStatus(v: String): InstallmentStatus = InstallmentStatus.valueOf(v)

    @TypeConverter fun budgetPeriodToString(v: BudgetPeriod): String = v.name
    @TypeConverter fun stringToBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun recurringKindToString(v: RecurringKind): String = v.name
    @TypeConverter fun stringToRecurringKind(v: String): RecurringKind = RecurringKind.valueOf(v)
}
