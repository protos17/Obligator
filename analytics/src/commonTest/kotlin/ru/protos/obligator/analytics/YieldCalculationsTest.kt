package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.calculateCurrentYield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YieldCalculationsTest {

    @Test
    fun testNormalCase() {
        val yield = calculateCurrentYield(couponPercent = 12.0, price = 100.0)
        assertEquals(12.0, yield!!, 0.001)
    }

    @Test
    fun testZeroCoupon() {
        val yield = calculateCurrentYield(couponPercent = 0.0, price = 100.0)
        assertNull(yield)
    }

    @Test
    fun testNullInputs() {
        assertNull(calculateCurrentYield(couponPercent = null, price = 100.0))
        assertNull(calculateCurrentYield(couponPercent = 10.0, price = null))
        assertNull(calculateCurrentYield(couponPercent = null, price = null))
    }
}
