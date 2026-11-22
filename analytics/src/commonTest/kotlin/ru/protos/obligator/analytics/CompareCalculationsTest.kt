package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.bestIndexes
import ru.protos.obligator.analytics.calc.compareBonds
import ru.protos.obligator.analytics.model.BestRule
import ru.protos.obligator.analytics.model.Bond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompareCalculationsTest {

    @Test
    fun testBestIndexesNormalCase() {
        val values = listOf(10.0, 15.0, 12.0)
        assertEquals(setOf(1), bestIndexes(values, BestRule.HIGHEST))
        assertEquals(setOf(0), bestIndexes(values, BestRule.LOWEST))
    }

    @Test
    fun testBestIndexesEqualValues() {
        // Значения округляются до 15.00
        val values = listOf(15.001, 15.004, 10.0)
        assertEquals(setOf(0, 1), bestIndexes(values, BestRule.HIGHEST))
    }

    @Test
    fun testBestIndexesSingleValueAndAllNulls() {
        assertEquals(emptySet(), bestIndexes(listOf(10.0, null), BestRule.HIGHEST))
        assertEquals(emptySet(), bestIndexes(listOf(null, null, null), BestRule.HIGHEST))
    }

    @Test
    fun testCompareBondsDifferentYieldKind() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "Bond 1", faceValue = 1000.0, yieldToMaturity = 15.0, yieldKind = "к погашению", couponPercent = 10.0, price = 100.0)
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "Bond 2", faceValue = 1000.0, yieldToMaturity = 18.0, yieldKind = "к оферте", couponPercent = 12.0, price = 100.0)

        val result = compareBonds(listOf(b1, b2))

        assertTrue(result.hasDifferentYieldKinds)
        val ytmRow = result.rows.find { it.id == "yieldToMaturity" }
        assertEquals(emptySet(), ytmRow?.highlighted)

        val currentYieldRow = result.rows.find { it.id == "currentYield" }
        assertEquals(setOf(1), currentYieldRow?.highlighted)
    }

    @Test
    fun testCompareBondsMissingCurrentYield() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "Bond 1", faceValue = 1000.0, couponPercent = 10.0, price = null)
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "Bond 2", faceValue = 1000.0, couponPercent = 10.0, price = 100.0)

        val result = compareBonds(listOf(b1, b2))
        val currentYieldRow = result.rows.find { it.id == "currentYield" }
        assertEquals(emptySet(), currentYieldRow?.highlighted)
    }

    @Test
    fun testCompareBondsEqualListingLevel() {
        val b1 = Bond(secid = "B1", boardId = "TQOB", shortName = "Bond 1", faceValue = 1000.0, listLevel = 1)
        val b2 = Bond(secid = "B2", boardId = "TQOB", shortName = "Bond 2", faceValue = 1000.0, listLevel = 1)
        val b3 = Bond(secid = "B3", boardId = "TQOB", shortName = "Bond 3", faceValue = 1000.0, listLevel = 1)

        val result = compareBonds(listOf(b1, b2, b3))
        val listLevelRow = result.rows.find { it.id == "listLevel" }
        assertEquals(setOf(0, 1, 2), listLevelRow?.highlighted)
    }
}
