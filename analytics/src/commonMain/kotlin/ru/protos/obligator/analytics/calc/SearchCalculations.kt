package ru.protos.obligator.analytics.calc

import ru.protos.obligator.analytics.model.Bond

/** Ищет облигации по подстроке в названии, тикере или ISIN. */
fun searchBonds(bonds: List<Bond>, query: String): List<Bond> {
    if (query.isBlank()) return bonds
    val trimmed = query.trim()
    return bonds.filter { bond ->
        bond.shortName.contains(trimmed, ignoreCase = true) ||
                bond.secid.contains(trimmed, ignoreCase = true) ||
                (bond.isin?.contains(trimmed, ignoreCase = true) == true)
    }
}
