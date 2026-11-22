package ru.protos.obligator.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.FavoriteItem
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.presentation.compare.CompareSelection

data class FavoritesUiState(
    val favorites: List<FavoriteItem> = emptyList(),
    val bondsMap: Map<String, Bond> = emptyMap(),
    val selectedCompareSecids: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val bondRepository: BondRepository,
    val compareSelection: CompareSelection
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        combine(
            favoriteRepository.getFavorites(),
            bondRepository.getBonds(),
            compareSelection.selectedSecids
        ) { favorites, bonds, compareSecids ->
            val bMap = bonds.associateBy { it.secid }
            FavoritesUiState(
                favorites = favorites,
                bondsMap = bMap,
                selectedCompareSecids = compareSecids,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun removeFavorite(secid: String) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(secid)
        }
    }

    fun toggleCompare(secid: String) {
        compareSelection.toggle(secid)
    }
}
