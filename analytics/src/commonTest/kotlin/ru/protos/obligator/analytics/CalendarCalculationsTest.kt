package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.calculateCouponCalendar
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PaymentKind
import ru.protos.obligator.analytics.model.PortfolioPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarCalculationsTest {

    @Test
    fun testCalendarGroupingAndEmptyAmount() {
        val pos = PortfolioPosition(secid = "B1", shortName = "Bond 1", quantity = 10, addedAt = 0)

        val p1 = Payment(id = 1, secid = "B1", date = "2025-04-15", kind = PaymentKind.COUPON, amount = 30.0)
        val p2 = Payment(id = 2, secid = "B1", date = "2025-04-20", kind = PaymentKind.COUPON, amount = null)
        val p3 = Payment(id = 3, secid = "B1", date = "2025-05-10", kind = PaymentKind.REPAYMENT, amount = 1000.0)

        val calendar = calculateCouponCalendar(
            payments = listOf(p1, p2, p3),
            positions = listOf(pos),
            currentDateStr = "2025-01-01"
        )

        assertEquals(2, calendar.size)

        val aprilGroup = calendar[0]
        assertEquals("2025-04", aprilGroup.monthKey)
        assertEquals(300.0, aprilGroup.couponTotal, 0.001)
        assertEquals(2, aprilGroup.items.size)

        val mayGroup = calendar[1]
        assertEquals("2025-05", mayGroup.monthKey)
        assertEquals(10000.0, mayGroup.repaymentTotal, 0.001)
    }
}
