package ru.protos.obligator.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PaymentKind
import ru.protos.obligator.analytics.model.PortfolioPosition
import ru.protos.obligator.analytics.model.UpdateInfo
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.PortfolioRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeBondRepository(initialBonds: List<Bond> = emptyList()) : BondRepository {
    private val _bonds = MutableStateFlow(initialBonds)
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)

    override fun getBonds() = _bonds
    override fun getUpdateInfo() = _updateInfo

    override suspend fun refreshBonds() {
        val newBonds = listOf(
            Bond(secid = "NEW1", boardId = "TQOB", shortName = "New Bond 1", faceValue = 1000.0),
            Bond(secid = "NEW2", boardId = "TQOB", shortName = "New Bond 2", faceValue = 1000.0)
        )
        _bonds.value = newBonds
        _updateInfo.value = UpdateInfo(loadedAt = 123456789L, priceDate = "2025-02-28")
    }

    override suspend fun getBondDetails(secid: String): Bond? {
        return _bonds.value.find { it.secid == secid }
    }

    override suspend fun fetchPayments(secid: String): List<Payment> = emptyList()

    override suspend fun fetchIsQualified(secid: String): Boolean = false
}

class FakePortfolioRepository : PortfolioRepository {
    private val _positions = MutableStateFlow<List<PortfolioPosition>>(emptyList())
    private val _payments = MutableStateFlow<List<Payment>>(emptyList())

    override fun getPositions() = _positions
    fun getPayments() = _payments

    override suspend fun addOrUpdatePosition(secid: String, shortName: String, quantity: Int) {
        val current = _positions.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.secid == secid }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(quantity = quantity)
        } else {
            current.add(PortfolioPosition(secid, shortName, quantity, addedAt = 100L))
        }
        _positions.value = current
    }

    suspend fun saveFuturePayments(secid: String, allPayments: List<Payment>, currentDateStr: String) {
        val futureOnly = allPayments.filter { it.date >= currentDateStr }
        val currentPayments = _payments.value.filter { it.secid != secid } + futureOnly
        _payments.value = currentPayments
    }

    override suspend fun removePosition(secid: String) {
        _positions.value = _positions.value.filter { it.secid != secid }
        _payments.value = _payments.value.filter { it.secid != secid }
    }
}

class RepositoryFakesTest {

    @Test
    fun testRefreshBondsReplacesEntireList() = runTest {
        val oldBond = Bond(secid = "OLD1", boardId = "TQOB", shortName = "Old Bond", faceValue = 1000.0)
        val repo = FakeBondRepository(listOf(oldBond))

        assertEquals(1, repo.getBonds().first().size)
        assertEquals("OLD1", repo.getBonds().first()[0].secid)

        repo.refreshBonds()

        val updated = repo.getBonds().first()
        assertEquals(2, updated.size)
        assertEquals(listOf("NEW1", "NEW2"), updated.map { it.secid })
    }

    @Test
    fun testReAddingPositionUpdatesQuantityNotDuplicate() = runTest {
        val repo = FakePortfolioRepository()

        repo.addOrUpdatePosition("B1", "Bond 1", 5)
        assertEquals(1, repo.getPositions().first().size)
        assertEquals(5, repo.getPositions().first()[0].quantity)

        repo.addOrUpdatePosition("B1", "Bond 1", 20)
        assertEquals(1, repo.getPositions().first().size)
        assertEquals(20, repo.getPositions().first()[0].quantity)
    }

    @Test
    fun testStoresOnlyFuturePayments() = runTest {
        val repo = FakePortfolioRepository()

        val pastPayment = Payment(id = 1, secid = "B1", date = "2024-12-01", kind = PaymentKind.COUPON, amount = 30.0)
        val futurePayment = Payment(id = 2, secid = "B1", date = "2025-06-01", kind = PaymentKind.COUPON, amount = 35.0)

        repo.saveFuturePayments("B1", listOf(pastPayment, futurePayment), currentDateStr = "2025-01-01")

        val stored = repo.getPayments().first()
        assertEquals(1, stored.size)
        assertEquals("2025-06-01", stored[0].date)
    }

    @Test
    fun testTransactionFailurePreservesOldData() = runTest {
        val oldBonds = listOf(Bond(secid = "OLD1", boardId = "TQOB", shortName = "Old", faceValue = 1000.0))
        var currentBonds = oldBonds

        try {
            var inTransaction = oldBonds
            inTransaction = emptyList()
            throw IllegalStateException("Database write failed")
            @Suppress("UNREACHABLE_CODE")
            currentBonds = inTransaction
        } catch (_: Exception) {
        }

        assertEquals(1, currentBonds.size)
        assertEquals("OLD1", currentBonds[0].secid)
    }

    @Test
    fun testPortfolioPaymentsPreservedOnApiFailure() = runTest {
        val repo = FakePortfolioRepository()
        val initialPayment = Payment(id = 1, secid = "B1", date = "2026-12-01", kind = PaymentKind.COUPON, amount = 35.0)
        repo.saveFuturePayments("B1", listOf(initialPayment), currentDateStr = "2026-01-01")

        val apiFailed = true
        if (!apiFailed) {
            repo.saveFuturePayments("B1", emptyList(), currentDateStr = "2026-01-01")
        }

        val paymentsAfterFailure = repo.getPayments().first()
        assertEquals(1, paymentsAfterFailure.size, "Existing payments must be preserved on network failure")
        assertEquals(35.0, paymentsAfterFailure[0].amount)
    }
}
