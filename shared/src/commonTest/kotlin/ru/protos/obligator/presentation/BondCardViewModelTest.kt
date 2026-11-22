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
import ru.protos.obligator.analytics.model.FavoriteItem
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PortfolioPosition
import ru.protos.obligator.analytics.model.UpdateInfo
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.analytics.repository.PortfolioRepository
import ru.protos.obligator.presentation.card.BondCardViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BondCardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMissingBondFromExchangeDoesNotCreateFakeBond() = runTest(testDispatcher) {
        val bondRepo = object : BondRepository {
            override fun getBonds(): Flow<List<Bond>> = MutableStateFlow(emptyList())
            override fun getUpdateInfo(): Flow<UpdateInfo?> = MutableStateFlow(null)
            override suspend fun refreshBonds() {}
            override suspend fun getBondDetails(secid: String): Bond? = null
            override suspend fun fetchPayments(secid: String): List<Payment> = emptyList()
            override suspend fun fetchIsQualified(secid: String): Boolean = false
        }

        val favFlow = MutableStateFlow<List<FavoriteItem>>(emptyList())
        val favRepo = object : FavoriteRepository {
            override fun getFavorites(): Flow<List<FavoriteItem>> = favFlow
            override fun isFavorite(secid: String): Flow<Boolean> = favFlow.map { list -> list.any { it.secid == secid } }
            override suspend fun addFavorite(secid: String, shortName: String) {
                favFlow.value = favFlow.value + FavoriteItem(secid, shortName, 0L)
            }
            override suspend fun removeFavorite(secid: String) {
                favFlow.value = favFlow.value.filterNot { it.secid == secid }
            }
        }

        val posFlow = MutableStateFlow(
            listOf(PortfolioPosition(secid = "MATURED_BOND", shortName = "Погашенный выпуск", quantity = 5, addedAt = 1L))
        )
        val portfolioRepo = object : PortfolioRepository {
            override fun getPositions(): Flow<List<PortfolioPosition>> = posFlow
            override suspend fun addOrUpdatePosition(secid: String, shortName: String, quantity: Int) {
                posFlow.value = posFlow.value.filterNot { it.secid == secid } + PortfolioPosition(secid, shortName, quantity, 1L)
            }
            override suspend fun removePosition(secid: String) {
                posFlow.value = posFlow.value.filterNot { it.secid == secid }
            }
        }

        val vm = BondCardViewModel(bondRepo, favRepo, portfolioRepo)
        vm.loadBond("MATURED_BOND")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.bond)
        assertTrue(state.isMissingFromExchange)
        assertEquals("Погашенный выпуск", state.fallbackShortName)
        assertEquals(5, state.portfolioQuantity)

        vm.addToPortfolio(0)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.portfolioQuantity)
        assertTrue(posFlow.value.isEmpty())
    }
}
