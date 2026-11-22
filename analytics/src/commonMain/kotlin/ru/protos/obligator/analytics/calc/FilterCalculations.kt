package ru.protos.obligator.analytics.calc

import kotlinx.datetime.LocalDate
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.CouponType

/** Преобразует код типа бумаги в группу эмитента. */
fun getIssuerGroup(secType: String?): String? {
    return when (secType) {
        "3" -> "GOVT"
        "4", "C" -> "MUNI"
        "5" -> "CBR"
        "6", "7", "8" -> "CORP"
        else -> secType
    }
}

fun getIssuerGroupRu(secType: String?): String {
    return when (getIssuerGroup(secType)) {
        "GOVT" -> "ОФЗ"
        "MUNI" -> "Муниципальные"
        "CBR" -> "Банк России"
        "CORP" -> "Корпоративные"
        else -> secType ?: "нет данных"
    }
}

fun getCouponTypeRu(bond: Bond): String {
    return when (getCouponType(bond)) {
        CouponType.FIXED -> "Постоянный"
        CouponType.FLOATING -> "Плавающий"
        CouponType.OTHER -> "Другой"
    }
}

/** Определяет тип купона облигации по её метаданным. */
fun getCouponType(bond: Bond): CouponType {
    if (bond.bondType?.contains("плавающ", ignoreCase = true) == true) {
        return CouponType.FLOATING
    }
    if (bond.couponDetails?.startsWith("Фиксированный", ignoreCase = true) == true) {
        return CouponType.FIXED
    }
    return CouponType.OTHER
}

/** Вычисляет срок до погашения в годах от текущей даты. */
fun calculateYearsToMaturity(matDateStr: String?, currentDateStr: String): Double? {
    if (matDateStr == null) return null
    return try {
        val matDate = LocalDate.parse(matDateStr)
        val curDate = LocalDate.parse(currentDateStr)
        val days = (matDate.toEpochDays() - curDate.toEpochDays()).toDouble()
        days / 365.25
    } catch (_: Exception) {
        null
    }
}

/** Фильтрует список облигаций по заданным условиям фильтра. */
fun filterBonds(bonds: List<Bond>, filter: BondFilter, currentDateStr: String? = null): List<Bond> {
    return bonds.filter { bond ->
        if (filter.issuerGroups != null && filter.issuerGroups.isNotEmpty()) {
            val group = getIssuerGroup(bond.secType)
            if (group == null || !filter.issuerGroups.contains(group)) return@filter false
        }

        if (filter.minYieldToMat != null || filter.maxYieldToMat != null) {
            val yieldVal = bond.yieldToMaturity ?: return@filter false
            if (filter.minYieldToMat != null && yieldVal < filter.minYieldToMat) return@filter false
            if (filter.maxYieldToMat != null && yieldVal > filter.maxYieldToMat) return@filter false
        }

        if (filter.minYearsToMat != null || filter.maxYearsToMat != null) {
            if (currentDateStr == null) return@filter false
            val years = calculateYearsToMaturity(bond.matDate, currentDateStr) ?: return@filter false
            if (filter.minYearsToMat != null && years < filter.minYearsToMat) return@filter false
            if (filter.maxYearsToMat != null && years > filter.maxYearsToMat) return@filter false
        }

        if (filter.couponType != null) {
            val cType = getCouponType(bond)
            if (cType != filter.couponType) return@filter false
        }

        if (filter.listLevels != null && filter.listLevels.isNotEmpty()) {
            val level = bond.listLevel ?: return@filter false
            if (!filter.listLevels.contains(level)) return@filter false
        }

        true
    }
}
