package ru.protos.obligator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_position")
data class PortfolioEntity(
    @PrimaryKey val secid: String,
    val shortName: String,
    val quantity: Int,
    val addedAt: Long
)
