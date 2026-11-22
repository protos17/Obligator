package ru.protos.obligator.presentation.screener

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import ru.protos.obligator.analytics.calc.filterBonds
import ru.protos.obligator.analytics.calc.searchBonds
import ru.protos.obligator.analytics.calc.sortBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder
import ru.protos.obligator.analytics.model.UpdateInfo
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.presentation.compare.CompareSelection

data class ScreenerUiState(
    val rawBonds: List<Bond> = emptyList(),
    val displayedBonds: List<Bond> = emptyList(),
    val searchQuery: String = "",
    val filter: BondFilter = BondFilter(),
    val sortField: SortField = SortField.NAME,
    val sortOrder: SortOrder = SortOrder.ASC,
    val updateInfo: UpdateInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedCompareSecids: Set<String> = emptySet(),
    val favoriteSecids: Set<String> = emptySet()
)

class ScreenerViewModel(
    private val bondRepository: BondRepository,
    private val favoriteRepository: FavoriteRepository,
    val compareSelection: CompareSelection
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenerUiState())
    val uiState: StateFlow<ScreenerUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        combine(
            bondRepository.getBonds(),
            bondRepository.getUpdateInfo(),
            favoriteRepository.getFavorites(),
            compareSelection.selectedSecids
        ) { bonds, updateInfo, favorites, compareSecids ->
            val favSet = favorites.map { it.secid }.toSet()
            val current = _uiState.value
            val processed = processBonds(
                bonds = bonds,
                query = current.searchQuery,
                filter = current.filter,
                sortField = current.sortField,
                sortOrder = current.sortOrder,
                priceDate = updateInfo?.priceDate
            )
            current.copy(
                rawBonds = bonds,
                displayedBonds = processed,
                updateInfo = updateInfo,
                favoriteSecids = favSet,
                selectedCompareSecids = compareSecids
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    private fun processBonds(
        bonds: List<Bond>,
        query: String,
        filter: BondFilter,
        sortField: SortField,
        sortOrder: SortOrder,
        priceDate: String?
    ): List<Bond> {
        val start = TimeSource.Monotonic.markNow()
        val searched = searchBonds(bonds, query)
        val filtered = filterBonds(searched, filter, priceDate)
        val sorted = sortBonds(filtered, sortField, sortOrder, priceDate)
        val ms = start.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        println("[BENCH_STEP] stage=process_bonds input=${bonds.size} output=${sorted.size} ms=$ms")
        return sorted
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        val processed = processBonds(
            bonds = current.rawBonds,
            query = query,
            filter = current.filter,
            sortField = current.sortField,
            sortOrder = current.sortOrder,
            priceDate = current.updateInfo?.priceDate
        )
        _uiState.value = current.copy(searchQuery = query, displayedBonds = processed)
    }

    /** Применяет новые фильтры и сбрасывает выбор для сравнения. */
    fun onFilterChanged(filter: BondFilter) {
        compareSelection.clear()
        val current = _uiState.value
        val processed = processBonds(
            bonds = current.rawBonds,
            query = current.searchQuery,
            filter = filter,
            sortField = current.sortField,
            sortOrder = current.sortOrder,
            priceDate = current.updateInfo?.priceDate
        )
        _uiState.value = current.copy(filter = filter, displayedBonds = processed)
    }

    fun onSortChanged(field: SortField, order: SortOrder) {
        val current = _uiState.value
        val processed = processBonds(
            bonds = current.rawBonds,
            query = current.searchQuery,
            filter = current.filter,
            sortField = field,
            sortOrder = order,
            priceDate = current.updateInfo?.priceDate
        )
        _uiState.value = current.copy(sortField = field, sortOrder = order, displayedBonds = processed)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                bondRepository.refreshBonds()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to refresh bonds"
                )
            }
        }
    }

    fun toggleFavorite(bond: Bond) {
        viewModelScope.launch {
            if (_uiState.value.favoriteSecids.contains(bond.secid)) {
                favoriteRepository.removeFavorite(bond.secid)
            } else {
                favoriteRepository.addFavorite(bond.secid, bond.shortName)
            }
        }
    }

    fun toggleCompare(secid: String) {
        compareSelection.toggle(secid)
    }
}
