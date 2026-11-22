package ru.protos.obligator.analytics.calc

import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder

/** Сравнивает два значения с учётом порядка, помещая null в конец. */
fun <T : Comparable<T>> compareNullsLast(v1: T?, v2: T?, order: SortOrder): Int {
    if (v1 == null && v2 == null) return 0
    if (v1 == null) return 1
    if (v2 == null) return -1
    return if (order == SortOrder.ASC) v1.compareTo(v2) else v2.compareTo(v1)
}

/** Сортирует облигации по выбранному полю, помещая null в конец. */
fun sortBonds(
    bonds: List<Bond>,
    field: SortField,
    order: SortOrder,
    currentDateStr: String? = null
): List<Bond> {
    return bonds.sortedWith { b1, b2 ->
        when (field) {
            SortField.NAME -> {
                val cmp = b1.shortName.compareTo(b2.shortName, ignoreCase = true)
                if (order == SortOrder.ASC) cmp else -cmp
            }
            SortField.YIELD_TO_MATURITY -> compareNullsLast(b1.yieldToMaturity, b2.yieldToMaturity, order)
            SortField.CURRENT_YIELD -> {
                val y1 = calculateCurrentYield(b1.couponPercent, b1.price)
                val y2 = calculateCurrentYield(b2.couponPercent, b2.price)
                compareNullsLast(y1, y2, order)
            }
            SortField.TERM -> {
                val t1 = if (currentDateStr != null) calculateYearsToMaturity(b1.matDate, currentDateStr) else null
                val t2 = if (currentDateStr != null) calculateYearsToMaturity(b2.matDate, currentDateStr) else null
                compareNullsLast(t1, t2, order)
            }
            SortField.DURATION -> compareNullsLast(b1.duration, b2.duration, order)
            SortField.PRICE -> compareNullsLast(b1.price, b2.price, order)
        }
    }
}
