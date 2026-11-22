package ru.protos.obligator.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.protos.obligator.analytics.calc.calculateCouponCalendar
import ru.protos.obligator.analytics.calc.calculatePortfolioTotalValue
import ru.protos.obligator.analytics.calc.searchBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.CalendarMonthGroup
import ru.protos.obligator.analytics.model.PortfolioPosition
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.PortfolioRepository
import ru.protos.obligator.data.db.AppDatabase
import ru.protos.obligator.data.db.toDomain

data class PortfolioUiState(
    val positions: List<PortfolioPosition> = emptyList(),
    val bondsMap: Map<String, Bond> = emptyMap(),
    val totalValue: Double = 0.0,
    val calendarGroups: List<CalendarMonthGroup> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Bond> = emptyList(),
    val rawBonds: List<Bond> = emptyList()
)

class PortfolioViewModel(
    private val portfolioRepository: PortfolioRepository,
    private val bondRepository: BondRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        observePortfolio()
    }

    private fun getTodayIso(): String {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date.toString()
    }

    private fun observePortfolio() {
        val today = getTodayIso()
        combine(
            portfolioRepository.getPositions(),
            bondRepository.getBonds(),
            db.paymentDao().getFuturePayments(today)
        ) { positions, bonds, paymentEntities ->
            val bMap = bonds.associateBy { it.secid }
            val total = calculatePortfolioTotalValue(positions, bMap)
            val payments = paymentEntities.map { it.toDomain() }
            val calendar = calculateCouponCalendar(payments, positions, today)

            val current = _uiState.value
            val searchRes = if (current.searchQuery.isNotBlank()) searchBonds(bonds, current.searchQuery) else emptyList()

            current.copy(
                positions = positions,
                bondsMap = bMap,
                totalValue = total,
                calendarGroups = calendar,
                rawBonds = bonds,
                searchResults = searchRes
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        val searchRes = if (query.isNotBlank()) searchBonds(current.rawBonds, query) else emptyList()
        _uiState.value = current.copy(searchQuery = query, searchResults = searchRes)
    }

    /** Добавляет, обновляет или удаляет позицию портфеля. */
    fun addOrUpdatePosition(secid: String, shortName: String, quantity: Int) {
        viewModelScope.launch {
            if (quantity <= 0) {
                portfolioRepository.removePosition(secid)
            } else {
                portfolioRepository.addOrUpdatePosition(secid, shortName, quantity)
            }
            _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList())
        }
    }

    fun removePosition(secid: String) {
        viewModelScope.launch {
            portfolioRepository.removePosition(secid)
        }
    }
}
