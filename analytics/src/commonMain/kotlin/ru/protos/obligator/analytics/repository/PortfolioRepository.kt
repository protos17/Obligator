package ru.protos.obligator.analytics.repository

import kotlinx.coroutines.flow.Flow
import ru.protos.obligator.analytics.model.PortfolioPosition

interface PortfolioRepository {
    fun getPositions(): Flow<List<PortfolioPosition>>
    suspend fun addOrUpdatePosition(secid: String, shortName: String, quantity: Int)
    suspend fun removePosition(secid: String)
}
