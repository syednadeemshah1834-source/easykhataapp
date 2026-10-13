package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BusinessEntity
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.AppBackupData
import com.example.data.model.DashboardStats
import com.example.data.model.DateRangeFilter
import com.example.data.model.DateRangePreset
import com.example.data.model.LedgerItem
import com.example.data.model.PartyType
import com.example.data.model.PartyWithBalance
import com.example.data.model.TransactionType
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val _selectedBusinessId = MutableStateFlow<Long?>(null)
    val selectedBusinessId: StateFlow<Long?> = _selectedBusinessId.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val businesses: StateFlow<List<BusinessEntity>> = repository.allBusinesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBusiness: StateFlow<BusinessEntity?> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id != null) repository.getBusiness(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboardStats: StateFlow<DashboardStats> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id != null) repository.getDashboardStats(id)
            else flowOf(DashboardStats())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val customers: StateFlow<List<PartyWithBalance>> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id != null) repository.getPartiesWithBalances(id, PartyType.CUSTOMER)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<PartyWithBalance>> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id != null) repository.getPartiesWithBalances(id, PartyType.SUPPLIER)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Party for Ledger View
    private val _activePartyId = MutableStateFlow<Long?>(null)
    val activePartyId: StateFlow<Long?> = _activePartyId.asStateFlow()

    val activePartyWithBalance: StateFlow<PartyWithBalance?> = _activePartyId
        .flatMapLatest { id ->
            if (id != null) repository.getPartyWithBalance(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePartyLedger: StateFlow<List<LedgerItem>> = _activePartyId
        .flatMapLatest { id ->
            if (id != null) repository.getLedgerForParty(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date range filter for Reports
    private val _reportsDateFilter = MutableStateFlow(createDefaultDateFilter())
    val reportsDateFilter: StateFlow<DateRangeFilter> = _reportsDateFilter.asStateFlow()

    val allBusinessTransactions: StateFlow<List<TransactionEntity>> = _selectedBusinessId
        .flatMapLatest { id ->
            if (id != null) repository.getTransactionsForBusiness(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val defaultId = repository.ensureDefaultBusinessExists()
            _selectedBusinessId.value = defaultId
        }
    }

    private fun createDefaultDateFilter(): DateRangeFilter {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startMillis = cal.timeInMillis

        val calEnd = Calendar.getInstance()
        calEnd.set(Calendar.HOUR_OF_DAY, 23)
        calEnd.set(Calendar.MINUTE, 59)
        calEnd.set(Calendar.SECOND, 59)
        val endMillis = calEnd.timeInMillis

        return DateRangeFilter(
            preset = DateRangePreset.THIS_MONTH,
            startMillis = startMillis,
            endMillis = endMillis
        )
    }

    fun selectBusiness(id: Long) {
        _selectedBusinessId.value = id
    }

    fun createBusiness(
        name: String,
        type: String,
        owner: String,
        phone: String,
        address: String,
        currency: String
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _snackbarMessage.emit("Business name cannot be empty.")
                return@launch
            }
            val newId = repository.createBusiness(
                name = name.trim(),
                businessType = type.trim(),
                ownerName = owner.trim(),
                phone = phone.trim(),
                address = address.trim(),
                currencySymbol = currency.trim()
            )
            _selectedBusinessId.value = newId
            _snackbarMessage.emit("Created business: $name")
        }
    }

    fun updateBusiness(business: BusinessEntity) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            _snackbarMessage.emit("Business updated.")
        }
    }

    fun deleteBusiness(id: Long) {
        viewModelScope.launch {
            repository.deleteBusiness(id)
            val remaining = businesses.value.filter { it.id != id }
            if (remaining.isNotEmpty()) {
                _selectedBusinessId.value = remaining.first().id
            } else {
                val newId = repository.ensureDefaultBusinessExists()
                _selectedBusinessId.value = newId
            }
            _snackbarMessage.emit("Business removed.")
        }
    }

    fun selectParty(partyId: Long?) {
        _activePartyId.value = partyId
    }

    fun addParty(
        name: String,
        phone: String,
        address: String,
        notes: String,
        openingBalance: Double,
        type: PartyType
    ) {
        val bizId = _selectedBusinessId.value ?: return
        viewModelScope.launch {
            if (name.isBlank()) {
                _snackbarMessage.emit("Please enter a valid name.")
                return@launch
            }
            val party = PartyEntity(
                businessId = bizId,
                type = type.name,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim(),
                openingBalance = openingBalance
            )
            val newId = repository.createParty(party)
            _snackbarMessage.emit("Added ${type.name.lowercase().replaceFirstChar { it.uppercase() }}: $name")
        }
    }

    fun updateParty(party: PartyEntity) {
        viewModelScope.launch {
            repository.updateParty(party.copy(updatedAt = System.currentTimeMillis()))
            _snackbarMessage.emit("Saved changes for ${party.name}")
        }
    }

    fun deleteParty(partyId: Long) {
        viewModelScope.launch {
            repository.deleteParty(partyId)
            if (_activePartyId.value == partyId) {
                _activePartyId.value = null
            }
            _snackbarMessage.emit("Party removed.")
        }
    }

    fun recordTransaction(
        partyId: Long,
        partyType: PartyType,
        transactionType: TransactionType,
        amount: Double,
        date: Long,
        referenceNo: String,
        description: String,
        notes: String
    ) {
        val bizId = _selectedBusinessId.value ?: return
        viewModelScope.launch {
            if (amount <= 0) {
                _snackbarMessage.emit("Please enter an amount greater than 0.")
                return@launch
            }
            val tx = TransactionEntity(
                businessId = bizId,
                partyId = partyId,
                partyType = partyType.name,
                transactionType = transactionType.name,
                amount = amount,
                date = date,
                referenceNo = referenceNo.trim(),
                description = description.trim(),
                notes = notes.trim()
            )
            repository.recordTransaction(tx)
            _snackbarMessage.emit("Recorded ${transactionType.title} of $amount")
        }
    }

    /**
     * Reverses or corrects a transaction as requested:
     * Creates a new, clearly marked transaction that negates the original,
     * referencing original ID and reason, while keeping original in history marked reversed.
     */
    fun reverseTransaction(
        originalTransactionId: Long,
        reason: String,
        reversalDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (reason.isBlank()) {
                _snackbarMessage.emit("Please provide a reason for the correction/reversal.")
                return@launch
            }
            try {
                repository.reverseTransaction(originalTransactionId, reason.trim(), reversalDate)
                _snackbarMessage.emit("Transaction successfully reversed & correction logged.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Reversal failed: ${e.message}")
            }
        }
    }

    fun seedDemoData() {
        val bizId = _selectedBusinessId.value ?: return
        viewModelScope.launch {
            repository.seedDemoData(bizId)
            _snackbarMessage.emit("Demo business data loaded successfully!")
        }
    }

    suspend fun exportBackupJson(): String {
        val data = repository.exportBackupData()
        return data.toJsonString()
    }

    fun restoreBackupFromJson(jsonString: String, overwriteExisting: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = AppBackupData.fromJsonString(jsonString)
                if (data.businesses.isEmpty()) {
                    onResult(false, "Invalid backup: No business data found.")
                    return@launch
                }
                repository.restoreBackup(data, overwriteExisting)
                _selectedBusinessId.value = data.businesses.first().id
                _snackbarMessage.emit("Restored ${data.businesses.size} businesses, ${data.parties.size} parties, and ${data.transactions.size} transactions.")
                onResult(true, "Restore successful!")
            } catch (e: Exception) {
                onResult(false, "Restore failed: ${e.message}")
            }
        }
    }

    fun setReportsFilter(preset: DateRangePreset, customStart: Long = 0, customEnd: Long = 0) {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        val (start, end) = when (preset) {
            DateRangePreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(s, cal.timeInMillis)
            }
            DateRangePreset.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(s, cal.timeInMillis)
            }
            DateRangePreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                Pair(s, now)
            }
            DateRangePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                Pair(s, now)
            }
            DateRangePreset.LAST_30_DAYS -> {
                Pair(now - (30L * 86_400_000L), now)
            }
            DateRangePreset.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
            DateRangePreset.CUSTOM -> {
                Pair(customStart, customEnd)
            }
        }

        _reportsDateFilter.value = DateRangeFilter(preset, start, end)
    }
}
