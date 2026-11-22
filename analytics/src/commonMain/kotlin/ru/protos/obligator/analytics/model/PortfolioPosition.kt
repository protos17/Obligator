package ru.protos.obligator.analytics.model

data class PortfolioPosition(
    val secid: String,
    val shortName: String,
    val quantity: Int,
    val addedAt: Long
)
