package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.sortBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class SortCalculationsTest {

    @Test
    fun testSortWithNullAtEndInBothDirections() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "Bond 1", faceValue = 1000.0, yieldToMaturity = 10.0)
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "Bond 2", faceValue = 1000.0, yieldToMaturity = null)
        val b3 = Bond(secid = "B3", boardId = "TQOB", shortName = "Bond 3", faceValue = 1000.0, yieldToMaturity = 15.0)

        val bonds = listOf(b1, b2, b3)

        val ascResult = sortBonds(bonds, SortField.YIELD_TO_MATURITY, SortOrder.ASC)
        assertEquals(listOf("B1", "B3", "B2"), ascResult.map { it.secid })

        val descResult = sortBonds(bonds, SortField.YIELD_TO_MATURITY, SortOrder.DESC)
        assertEquals(listOf("B3", "B1", "B2"), descResult.map { it.secid })
    }
}
