package ru.protos.obligator.analytics.model

data class BondFilter(
    val issuerGroups: Set<String>? = null,
    val minYearsToMat: Double? = null,
    val maxYearsToMat: Double? = null,
    val minYieldToMat: Double? = null,
    val maxYieldToMat: Double? = null,
    val couponType: CouponType? = null,
    val listLevels: Set<Int>? = null
)
