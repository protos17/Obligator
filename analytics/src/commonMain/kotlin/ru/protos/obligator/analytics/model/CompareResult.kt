package ru.protos.obligator.analytics.model

data class CompareResult(
    val bonds: List<Bond>,
    val rows: List<CompareRowModel> = emptyList(),
    val hasDifferentYieldKinds: Boolean = false
)
