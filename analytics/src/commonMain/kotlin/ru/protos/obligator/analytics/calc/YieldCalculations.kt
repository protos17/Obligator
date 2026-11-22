package ru.protos.obligator.analytics.calc

/** Вычисляет текущую доходность в процентах по ставке купона и цене. */
fun calculateCurrentYield(couponPercent: Double?, price: Double?): Double? {
    if (couponPercent == null || price == null || couponPercent <= 0.0 || price <= 0.0) {
        return null
    }
    return couponPercent / price * 100.0
}
