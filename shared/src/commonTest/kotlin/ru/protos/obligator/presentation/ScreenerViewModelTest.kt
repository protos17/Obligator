package ru.protos.obligator.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.FavoriteItem
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder
import ru.protos.obligator.analytics.model.UpdateInfo
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.presentation.compare.CompareSelection
import ru.protos.obligator.presentation.screener.ScreenerViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeFavoriteRepository(initial: List<FavoriteItem> = emptyList()) : FavoriteRepository {
    private val _favorites = MutableStateFlow(initial)
    override fun getFavorites(): Flow<List<FavoriteItem>> = _favorites
    override suspend fun addFavorite(secid: String, shortName: String) {
        _favorites.value = _favorites.value + FavoriteItem(secid, shortName, 100L)
    }
    override suspend fun removeFavorite(secid: String) {
        _favorites.value = _favorites.value.filter { it.secid != secid }
    }
    override fun isFavorite(secid: String): Flow<Boolean> =
        _favorites.map { list -> list.any { it.secid == secid } }
}

class TestBondRepository(private val bondsList: List<Bond>) : BondRepository {
    private val _bonds = MutableStateFlow(bondsList)
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(UpdateInfo(loadedAt = 1000L, priceDate = "2026-11-20"))

    override fun getBonds(): Flow<List<Bond>> = _bonds
    override fun getUpdateInfo(): Flow<UpdateInfo?> = _updateInfo
    override suspend fun refreshBonds() {}
    override suspend fun getBondDetails(secid: String): Bond? = _bonds.value.find { it.secid == secid }
    override suspend fun fetchPayments(secid: String): List<Payment> = emptyList()
    override suspend fun fetchIsQualified(secid: String): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleBonds = listOf(
        Bond(secid = "SU26238", boardId = "TQOB", shortName = "ОФЗ 26238", faceValue = 1000.0, price = 50.0, yieldToMaturity = 16.5, secType = "3", listLevel = 1),
        Bond(secid = "SU26240", boardId = "TQOB", shortName = "ОФЗ 26240", faceValue = 1000.0, price = 56.0, yieldToMaturity = 15.0, secType = "3", listLevel = 1),
        Bond(secid = "RU000A1", boardId = "TQCB", shortName = "Корп Выпуск", faceValue = 1000.0, price = 95.0, yieldToMaturity = 22.0, secType = "6", listLevel = 2)
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadingAndProcessing() = runTest(testDispatcher) {
        val bondRepo = TestBondRepository(sampleBonds)
        val favRepo = FakeFavoriteRepository()
        val compareSelection = CompareSelection()

        val viewModel = ScreenerViewModel(bondRepo, favRepo, compareSelection)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.rawBonds.size)
        assertEquals(3, state.displayedBonds.size)
        assertEquals("2026-11-20", state.updateInfo?.priceDate)
    }

    @Test
    fun testSearchQueryFiltering() = runTest(testDispatcher) {
        val bondRepo = TestBondRepository(sampleBonds)
        val favRepo = FakeFavoriteRepository()
        val compareSelection = CompareSelection()

        val viewModel = ScreenerViewModel(bondRepo, favRepo, compareSelection)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("26238")
        val state = viewModel.uiState.value

        assertEquals(1, state.displayedBonds.size)
        assertEquals("SU26238", state.displayedBonds[0].secid)
    }

    @Test
    fun testFilterApplication() = runTest(testDispatcher) {
        val bondRepo = TestBondRepository(sampleBonds)
        val favRepo = FakeFavoriteRepository()
        val compareSelection = CompareSelection()

        val viewModel = ScreenerViewModel(bondRepo, favRepo, compareSelection)
        advanceUntilIdle()

        viewModel.onFilterChanged(BondFilter(issuerGroups = setOf("GOVT"), minYieldToMat = 16.0))
        val state = viewModel.uiState.value

        assertEquals(1, state.displayedBonds.size)
        assertEquals("SU26238", state.displayedBonds[0].secid)
    }

    @Test
    fun testSortOrderAndFieldChange() = runTest(testDispatcher) {
        val bondRepo = TestBondRepository(sampleBonds)
        val favRepo = FakeFavoriteRepository()
        val compareSelection = CompareSelection()

        val viewModel = ScreenerViewModel(bondRepo, favRepo, compareSelection)
        advanceUntilIdle()

        viewModel.onSortChanged(SortField.YIELD_TO_MATURITY, SortOrder.DESC)
        val state = viewModel.uiState.value

        assertEquals("RU000A1", state.displayedBonds[0].secid)
        assertEquals("SU26238", state.displayedBonds[1].secid)
        assertEquals("SU26240", state.displayedBonds[2].secid)
    }

    @Test
    fun testCompareToggle() = runTest(testDispatcher) {
        val bondRepo = TestBondRepository(sampleBonds)
        val favRepo = FakeFavoriteRepository()
        val compareSelection = CompareSelection()

        val viewModel = ScreenerViewModel(bondRepo, favRepo, compareSelection)
        advanceUntilIdle()

        viewModel.toggleCompare("SU26238")
        assertTrue(compareSelection.isSelected("SU26238"))
    }
}
