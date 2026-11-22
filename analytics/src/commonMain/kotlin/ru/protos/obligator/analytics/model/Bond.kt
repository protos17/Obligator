package ru.protos.obligator.analytics.model

/** Доменная модель облигации Московской биржи. */
data class Bond(
    val secid: String,
    val boardId: String,
    val isin: String? = null,
    val shortName: String,
    val secType: String? = null,
    val faceValue: Double,
    val price: Double? = null,
    val accruedInt: Double? = null,
    val yieldToMaturity: Double? = null,
    val duration: Int? = null,
    val couponPercent: Double? = null,
    val couponValue: Double? = null,
    val couponPeriod: Int? = null,
    val nextCoupon: String? = null,
    val matDate: String? = null,
    val offerDate: String? = null,
    val yieldKind: String? = null,
    val couponDetails: String? = null,
    val bondType: String? = null,
    val listLevel: Int? = null
)
