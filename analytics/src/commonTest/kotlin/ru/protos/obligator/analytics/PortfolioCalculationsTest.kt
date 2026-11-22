package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.calculatePositionValue
import ru.protos.obligator.analytics.calc.calculatePortfolioTotalValue
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.PortfolioPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortfolioCalculationsTest {

    @Test
    fun testPositionValueAmortized() {
        val value = calculatePositionValue(price = 98.0, faceValue = 500.0, accruedInt = 5.0, quantity = 10)
        assertEquals(4950.0, value!!, 0.001)
    }

    @Test
    fun testPositionValueNoPrice() {
        val value = calculatePositionValue(price = null, faceValue = 1000.0, accruedInt = 10.0, quantity = 5)
        assertNull(value)
    }

    @Test
    fun testPortfolioTotalValueIgnoresNull() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "B1", faceValue = 1000.0, price = 100.0, accruedInt = 0.0)
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "B2", faceValue = 1000.0, price = null, accruedInt = 10.0)

        val pos1 = PortfolioPosition(secid = "B1", shortName = "B1", quantity = 2, addedAt = 0)
        val pos2 = PortfolioPosition(secid = "B2", shortName = "B2", quantity = 5, addedAt = 0)

        val total = calculatePortfolioTotalValue(listOf(pos1, pos2), mapOf("B1" to b1, "B2" to b2))
        assertEquals(2000.0, total, 0.001)
    }
}
