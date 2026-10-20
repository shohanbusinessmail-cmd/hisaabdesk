package com.shohankhan.hisaabdesk.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.data.local.entity.AccountEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.ContactEntity
import com.shohankhan.hisaabdesk.data.local.entity.PaymentMethod
import com.shohankhan.hisaabdesk.data.local.entity.TxnEntity
import com.shohankhan.hisaabdesk.data.local.entity.TxnFlow
import com.shohankhan.hisaabdesk.data.local.entity.TxnStatus
import com.shohankhan.hisaabdesk.data.local.entity.TxnType
import com.shohankhan.hisaabdesk.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TxnFilterState(
    val query: String = "",
    val type: TxnType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val contactId: Long? = null,
    val fromDay: Int? = null,
    val toDay: Int? = null,
    val minPaisa: Long? = null,
    val maxPaisa: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModel(private val container: AppContainer) : ViewModel() {

    val digitStyle = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.shohankhan.hisaabdesk.core.formatting.Money.DigitStyle.BENGALI)

    private val _filter = MutableStateFlow(TxnFilterState())
    val filter: StateFlow<TxnFilterState> = _filter.asStateFlow()

    private val _page = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = container.accountRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<CategoryEntity>> = container.categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val txns: StateFlow<List<TxnEntity>> = combine(_filter, _page) { f, p -> f to p }
        .flatMapLatest { (f, p) ->
            container.transactionRepository.observeFiltered(
                query = f.query,
                type = f.type,
                accountId = f.accountId,
                categoryId = f.categoryId,
                contactId = f.contactId,
                fromDay = f.fromDay,
                toDay = f.toDay,
                minPaisa = f.minPaisa,
                maxPaisa = f.maxPaisa,
                limit = PAGE_SIZE,
                offset = p * PAGE_SIZE
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateFilter(transform: (TxnFilterState) -> TxnFilterState) {
        _filter.value = transform(_filter.value)
        _page.value = 0
    }

    fun clearFilters() {
        _filter.value = TxnFilterState()
        _page.value = 0
    }

    fun nextPage() { _page.value = _page.value + 1 }
    fun prevPage() { _page.value = (_page.value - 1).coerceAtLeast(0) }

    companion object { const val PAGE_SIZE = 40 }
}

sealed class FormMode {
    data object Income : FormMode()
    data object Expense : FormMode()
    data object Transfer : FormMode()
    data class Edit(val id: Long) : FormMode()
}

class TransactionFormViewModel(private val container: AppContainer) : ViewModel() {

    data class FormState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val saved: Boolean = false,
        val accounts: List<AccountEntity> = emptyList(),
        val categories: List<CategoryEntity> = emptyList(),
        val contacts: List<ContactEntity> = emptyList(),
        val initial: TxnEntity? = null
    )

    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    fun load(mode: FormMode) {
        viewModelScope.launch {
            val accList = container.accountRepository.observeAll().first()
            val catList = container.categoryRepository.observeActive().first()
            val conList = container.contactRepository.observeActive().first()
            val initial = if (mode is FormMode.Edit) container.transactionRepository.get(mode.id) else null
            _state.value = FormState(
                loading = false,
                accounts = accList.filter { !it.isArchived },
                categories = catList,
                contacts = conList,
                initial = initial
            )
        }
    }

    fun save(
        mode: FormMode,
        amountPaisa: Long,
        dateMillis: Long,
        accountId: Long?,
        counterAccountId: Long?,
        categoryId: Long?,
        contactId: Long?,
        paymentMethod: PaymentMethod,
        description: String,
        note: String,
        receiptNo: String
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                val now = System.currentTimeMillis()
                when (mode) {
                    FormMode.Income -> container.transactionRepository.insert(
                        TxnEntity(
                            type = TxnType.INCOME,
                            flow = TxnFlow.INFLOW,
                            amountPaisa = amountPaisa,
                            dateMillis = dateMillis,
                            dayKey = Dates.dayKey(dateMillis),
                            accountId = accountId,
                            categoryId = categoryId,
                            contactId = contactId,
                            paymentMethod = paymentMethod,
                            description = description,
                            note = note,
                            receiptNo = receiptNo,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    FormMode.Expense -> container.transactionRepository.insert(
                        TxnEntity(
                            type = TxnType.EXPENSE,
                            flow = TxnFlow.OUTFLOW,
                            amountPaisa = amountPaisa,
                            dateMillis = dateMillis,
                            dayKey = Dates.dayKey(dateMillis),
                            accountId = accountId,
                            categoryId = categoryId,
                            contactId = contactId,
                            paymentMethod = paymentMethod,
                            description = description,
                            note = note,
                            receiptNo = receiptNo,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    FormMode.Transfer -> container.transactionRepository.insert(
                        TxnEntity(
                            type = TxnType.TRANSFER,
                            flow = TxnFlow.TRANSFER,
                            amountPaisa = amountPaisa,
                            dateMillis = dateMillis,
                            dayKey = Dates.dayKey(dateMillis),
                            accountId = accountId,
                            counterAccountId = counterAccountId,
                            paymentMethod = paymentMethod,
                            description = description.ifBlank { "টাকা স্থানান্তর" },
                            note = note,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    is FormMode.Edit -> {
                        val old = _state.value.initial ?: error("missing")
                        container.transactionRepository.update(
                            old.copy(
                                amountPaisa = amountPaisa,
                                dateMillis = dateMillis,
                                dayKey = Dates.dayKey(dateMillis),
                                accountId = accountId,
                                counterAccountId = counterAccountId,
                                categoryId = categoryId,
                                contactId = contactId,
                                paymentMethod = paymentMethod,
                                description = description,
                                note = note,
                                receiptNo = receiptNo,
                                updatedAt = now
                            )
                        )
                    }
                }
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (e: TransactionRepository.ValidationException) {
                _state.value = _state.value.copy(saving = false, error = e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = "সংরক্ষণ করা যায়নি")
            }
        }
    }
}

class TransactionDetailViewModel(private val container: AppContainer) : ViewModel() {

    data class DetailState(
        val txn: TxnEntity? = null,
        val loading: Boolean = true,
        val error: String? = null,
        val message: String? = null
    )

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    val digitStyle = container.settingsRepository.settings
        .map { it.digitStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.shohankhan.hisaabdesk.core.formatting.Money.DigitStyle.BENGALI)

    fun load(id: Long) {
        viewModelScope.launch {
            container.transactionRepository.observeById(id).collect { txn ->
                _state.value = _state.value.copy(txn = txn, loading = false)
            }
        }
    }

    fun reverse(id: Long, reason: String) {
        viewModelScope.launch {
            try {
                container.transactionRepository.reverse(id, reason)
                _state.value = _state.value.copy(message = "বাতিল করার সুরক্ষিত রেকর্ড তৈরি হয়েছে", error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "বাতিল করা যায়নি", message = null)
            }
        }
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            container.transactionRepository.softDelete(id)
            _state.value = _state.value.copy(message = "লেনদেন ট্র্যাশে সরানো হয়েছে")
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            container.transactionRepository.restore(id)
            _state.value = _state.value.copy(message = "ফিরিয়ে আনা হয়েছে")
        }
    }
}
