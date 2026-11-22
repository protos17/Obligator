package ru.protos.obligator.analytics.calc

import ru.protos.obligator.analytics.model.CalendarMonthGroup
import ru.protos.obligator.analytics.model.CalendarPaymentItem
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PaymentKind
import ru.protos.obligator.analytics.model.PortfolioPosition

/** Группирует будущие выплаты по месяцам с учётом количества бумаг. */
fun calculateCouponCalendar(
    payments: List<Payment>,
    positions: List<PortfolioPosition>,
    currentDateStr: String
): List<CalendarMonthGroup> {
    val posMap = positions.associateBy { it.secid }
    val futurePayments = payments.filter { it.date >= currentDateStr && posMap.containsKey(it.secid) }

    val items = futurePayments.map { payment ->
        val pos = posMap[payment.secid]!!
        val totalAmt = payment.amount?.let { it * pos.quantity }
        CalendarPaymentItem(
            secid = payment.secid,
            shortName = pos.shortName,
            date = payment.date,
            kind = payment.kind,
            totalAmount = totalAmt
        )
    }.sortedBy { it.date }

    val groupedByMonth = items.groupBy { it.date.take(7) }

    return groupedByMonth.map { (monthKey, monthItems) ->
        var couponSum = 0.0
        var repaymentSum = 0.0

        for (item in monthItems) {
            if (item.totalAmount != null) {
                when (item.kind) {
                    PaymentKind.COUPON -> couponSum += item.totalAmount
                    PaymentKind.REPAYMENT -> repaymentSum += item.totalAmount
                }
            }
        }

        CalendarMonthGroup(
            monthKey = monthKey,
            couponTotal = couponSum,
            repaymentTotal = repaymentSum,
            items = monthItems
        )
    }.sortedBy { it.monthKey }
}
