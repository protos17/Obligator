package ru.protos.obligator.presentation.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.protos.obligator.analytics.calc.compareBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.CompareResult
import ru.protos.obligator.analytics.repository.BondRepository

data class CompareUiState(
    val selectedSecids: List<String> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val result: CompareResult = CompareResult(emptyList()),
    val isLoading: Boolean = false
)

class CompareViewModel(
    private val bondRepository: BondRepository,
    val compareSelection: CompareSelection
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        observeSelection()
    }

    private fun observeSelection() {
        compareSelection.selectedSecids.onEach { secids ->
            val secidList = secids.toList()
            _uiState.value = _uiState.value.copy(selectedSecids = secidList, isLoading = true)
            loadBondsForSecids(secidList)
        }.launchIn(viewModelScope)
    }

    private fun loadBondsForSecids(secids: List<String>) {
        viewModelScope.launch {
            val loadedBonds = mutableListOf<Bond>()
            for (secid in secids) {
                val bond = bondRepository.getBondDetails(secid)
                if (bond != null) {
                    loadedBonds.add(bond)
                }
            }
            val compareRes = compareBonds(loadedBonds)
            _uiState.value = _uiState.value.copy(
                bonds = loadedBonds,
                result = compareRes,
                isLoading = false
            )
        }
    }

    fun removeSecid(secid: String) {
        compareSelection.remove(secid)
    }
}
