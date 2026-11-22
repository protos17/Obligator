package ru.protos.obligator.analytics.model

enum class PaymentKind {
    COUPON,
    REPAYMENT
}

/** Запланированная выплата купона или погашения по облигации. */
data class Payment(
    val id: Long = 0,
    val secid: String,
    val date: String,
    val kind: PaymentKind,
    val amount: Double? = null
)
