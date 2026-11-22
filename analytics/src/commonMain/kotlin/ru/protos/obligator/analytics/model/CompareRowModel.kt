package ru.protos.obligator.analytics.model

data class CompareRowModel(
    val id: String,
    val label: String,
    val values: List<String>,
    val highlighted: Set<Int> = emptySet(),
    val isHeader: Boolean = false
)
