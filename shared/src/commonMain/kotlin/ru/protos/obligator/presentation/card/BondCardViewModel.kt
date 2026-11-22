package ru.protos.obligator.presentation.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.analytics.repository.PortfolioRepository

data class BondCardUiState(
    val secid: String = "",
    val bond: Bond? = null,
    val fallbackShortName: String? = null,
    val isMissingFromExchange: Boolean = false,
    val isQualified: Boolean? = null,
    val payments: List<Payment> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false,
    val portfolioQuantity: Int = 0
)

class BondCardViewModel(
    private val bondRepository: BondRepository,
    private val favoriteRepository: FavoriteRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BondCardUiState())
    val uiState: StateFlow<BondCardUiState> = _uiState.asStateFlow()

    /** Загружает данные облигации, график выплат и статус в избранном и портфеле. */
    fun loadBond(secid: String) {
        _uiState.value = BondCardUiState(secid = secid, isLoading = true)
        viewModelScope.launch {
            val bond = bondRepository.getBondDetails(secid)
            val isFav = favoriteRepository.isFavorite(secid).first()
            val pos = portfolioRepository.getPositions().first().find { it.secid == secid }
            val favItem = favoriteRepository.getFavorites().first().find { it.secid == secid }
            val fallbackName = pos?.shortName ?: favItem?.shortName

            _uiState.value = _uiState.value.copy(
                bond = bond,
                fallbackShortName = fallbackName,
                isMissingFromExchange = bond == null,
                isFavorite = isFav,
                portfolioQuantity = pos?.quantity ?: 0
            )

            try {
                val allPayments = bondRepository.fetchPayments(secid)
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                val futurePayments = allPayments.filter { it.date >= today }
                val isQual = bondRepository.fetchIsQualified(secid)
                _uiState.value = _uiState.value.copy(
                    payments = futurePayments,
                    isQualified = isQual,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load details"
                )
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val secid = state.secid
        val shortName = state.bond?.shortName ?: state.fallbackShortName ?: return
        viewModelScope.launch {
            if (state.isFavorite) {
                favoriteRepository.removeFavorite(secid)
                _uiState.value = _uiState.value.copy(isFavorite = false)
            } else {
                favoriteRepository.addFavorite(secid, shortName)
                _uiState.value = _uiState.value.copy(isFavorite = true)
            }
        }
    }

    /** Добавляет, обновляет или удаляет позицию текущей облигации в портфеле. */
    fun addToPortfolio(quantity: Int) {
        val state = _uiState.value
        val secid = state.secid
        val shortName = state.bond?.shortName ?: state.fallbackShortName ?: return
        viewModelScope.launch {
            if (quantity <= 0) {
                portfolioRepository.removePosition(secid)
                _uiState.value = _uiState.value.copy(portfolioQuantity = 0)
            } else {
                portfolioRepository.addOrUpdatePosition(secid, shortName, quantity)
                _uiState.value = _uiState.value.copy(portfolioQuantity = quantity)
            }
        }
    }
}
