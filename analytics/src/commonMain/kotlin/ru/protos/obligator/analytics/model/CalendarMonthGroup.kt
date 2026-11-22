package ru.protos.obligator.analytics.model

data class CalendarPaymentItem(
    val secid: String,
    val shortName: String,
    val date: String,
    val kind: PaymentKind,
    val totalAmount: Double?
)

data class CalendarMonthGroup(
    val monthKey: String,
    val couponTotal: Double,
    val repaymentTotal: Double,
    val items: List<CalendarPaymentItem>
)
