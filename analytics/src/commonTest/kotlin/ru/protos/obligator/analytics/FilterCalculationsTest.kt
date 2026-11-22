package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.filterBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterCalculationsTest {

    @Test
    fun testFilterWithNullValues() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "Bond 1", faceValue = 1000.0, yieldToMaturity = 15.0, matDate = "2026-06-01")
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "Bond 2", faceValue = 1000.0, yieldToMaturity = null, matDate = "2026-06-01")
        val b3 = Bond(secid = "B3", boardId = "TQOB", shortName = "Bond 3", faceValue = 1000.0, yieldToMaturity = 20.0, matDate = null)

        val bonds = listOf(b1, b2, b3)

        val filterYield = BondFilter(minYieldToMat = 10.0)
        val resultYield = filterBonds(bonds, filterYield)
        assertEquals(listOf(b1, b3), resultYield)

        val filterTerm = BondFilter(minYearsToMat = 0.5)
        val resultTerm = filterBonds(bonds, filterTerm, currentDateStr = "2025-01-01")
        assertEquals(listOf(b1, b2), resultTerm)
    }
}
