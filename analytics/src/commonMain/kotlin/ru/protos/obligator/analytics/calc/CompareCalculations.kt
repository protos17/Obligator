package ru.protos.obligator.analytics.calc

import ru.protos.obligator.analytics.model.BestRule
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.CompareResult
import ru.protos.obligator.analytics.model.CompareRowModel
import kotlin.math.round

/** Возвращает индексы лучших значений с учётом округления до сотых. */
fun bestIndexes(values: List<Double?>, rule: BestRule): Set<Int> {
    val validIndexed = values.mapIndexedNotNull { index, valD ->
        if (valD != null) index to valD else null
    }
    if (validIndexed.size < 2) return emptySet()

    val roundedValues = validIndexed.map { (index, v) ->
        index to (round(v * 100.0) / 100.0)
    }

    val targetValue = when (rule) {
        BestRule.HIGHEST -> roundedValues.maxOf { it.second }
        BestRule.LOWEST -> roundedValues.minOf { it.second }
    }

    return roundedValues.filter { it.second == targetValue }.map { it.first }.toSet()
}

/** Формирует таблицу сравнения облигаций с подсветкой лучших показателей. */
fun compareBonds(bonds: List<Bond>): CompareResult {
    if (bonds.isEmpty()) return CompareResult(bonds = emptyList())

    val bondsWithYtm = bonds.filter { it.yieldToMaturity != null }
    val ytmKinds = bondsWithYtm.mapNotNull { it.yieldKind?.trim() }.distinct()
    val hasDifferentYieldKinds = bondsWithYtm.size >= 2 && ytmKinds.size > 1

    val ytmHighlights = if (hasDifferentYieldKinds) {
        emptySet()
    } else {
        bestIndexes(bonds.map { it.yieldToMaturity }, BestRule.HIGHEST)
    }

    val currentYieldValues = bonds.map { calculateCurrentYield(it.couponPercent, it.price) }
    val currentYieldHighlights = bestIndexes(currentYieldValues, BestRule.HIGHEST)

    val listLevelValues = bonds.map { it.listLevel?.toDouble() }
    val listLevelHighlights = bestIndexes(listLevelValues, BestRule.LOWEST)

    fun formatDouble(value: Double?): String {
        if (value == null) return "нет данных"
        val rounded = (round(value * 100.0) / 100.0)
        val isNegative = rounded < 0
        val absVal = if (isNegative) -rounded else rounded
        val intPart = absVal.toLong()
        val fracPart = round((absVal - intPart) * 100).toLong()
        val fracStr = if (fracPart < 10) "0$fracPart" else "$fracPart"
        val signStr = if (isNegative) "-" else ""
        return "$signStr$intPart,$fracStr"
    }

    fun formatPercent(value: Double?): String = if (value == null) "нет данных" else "${formatDouble(value)}%"
    fun formatRubles(value: Double?): String = if (value == null) "нет данных" else "${formatDouble(value)} ₽"
    fun formatDuration(value: Int?): String = if (value == null) "нет данных" else "$value дн."
    fun formatString(value: String?): String = value ?: "нет данных"
    fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank() || dateStr == "0000-00-00") return "нет данных"
        val parts = dateStr.split("-")
        if (parts.size == 3 && parts[0].length == 4 && parts[1].length == 2 && parts[2].length == 2) {
            return "${parts[2]}.${parts[1]}.${parts[0]}"
        }
        return dateStr
    }

    val rows = listOf(
        CompareRowModel("shortName", "Выпуск", bonds.map { it.shortName }, isHeader = true),
        CompareRowModel("isin", "ISIN", bonds.map { formatString(it.isin) }),
        CompareRowModel("secType", "Группа эмитента", bonds.map { getIssuerGroupRu(it.secType) }),
        CompareRowModel("price", "Цена (% ном.)", bonds.map { formatPercent(it.price) }),
        CompareRowModel("accruedInt", "НКД", bonds.map { formatRubles(it.accruedInt) }),
        CompareRowModel(
            "yieldToMaturity",
            "Дох. к погашению",
            bonds.map {
                if (it.yieldToMaturity == null) {
                    formatPercent(null)
                } else {
                    "${formatPercent(it.yieldToMaturity)} ${it.yieldKind ?: ""}".trim()
                }
            },
            highlighted = ytmHighlights
        ),
        CompareRowModel("currentYield", "Текущая доходность", currentYieldValues.map { formatPercent(it) }, highlighted = currentYieldHighlights),
        CompareRowModel("duration", "Дюрация", bonds.map { formatDuration(it.duration) }),
        CompareRowModel("couponPercent", "Ставка купона", bonds.map { formatPercent(it.couponPercent) }),
        CompareRowModel("couponValue", "Сумма купона", bonds.map { formatRubles(it.couponValue) }),
        CompareRowModel("couponPeriod", "Период купона", bonds.map { formatDuration(it.couponPeriod) }),
        CompareRowModel("nextCoupon", "Ближайший купон", bonds.map { formatDate(it.nextCoupon) }),
        CompareRowModel("matDate", "Дата погашения", bonds.map { formatDate(it.matDate) }),
        CompareRowModel("offerDate", "Оферта", bonds.map { formatDate(it.offerDate) }),
        CompareRowModel("couponType", "Тип купона", bonds.map { getCouponTypeRu(it) }),
        CompareRowModel("listLevel", "Уровень листинга", bonds.map { formatString(it.listLevel?.toString()) }, highlighted = listLevelHighlights),
        CompareRowModel("faceValue", "Номинал", bonds.map { formatRubles(it.faceValue) })
    )

    return CompareResult(
        bonds = bonds,
        rows = rows,
        hasDifferentYieldKinds = hasDifferentYieldKinds
    )
}
