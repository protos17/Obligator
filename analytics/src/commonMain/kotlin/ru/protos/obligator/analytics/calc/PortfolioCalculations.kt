package ru.protos.obligator.analytics.calc

import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.PortfolioPosition

/** Вычисляет стоимость позиции в рублях с учётом НКД и количества. */
fun calculatePositionValue(
    price: Double?,
    faceValue: Double,
    accruedInt: Double?,
    quantity: Int
): Double? {
    if (price == null || accruedInt == null) return null
    return (price / 100.0 * faceValue + accruedInt) * quantity.toDouble()
}

/** Вычисляет общую стоимость портфеля по всем валидным позициям. */
fun calculatePortfolioTotalValue(
    positions: List<PortfolioPosition>,
    bondsMap: Map<String, Bond>
): Double {
    var total = 0.0
    for (pos in positions) {
        val bond = bondsMap[pos.secid] ?: continue
        val valRub = calculatePositionValue(bond.price, bond.faceValue, bond.accruedInt, pos.quantity)
        if (valRub != null) {
            total += valRub
        }
    }
    return total
}
