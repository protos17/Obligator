package ru.protos.obligator.analytics.repository

import kotlinx.coroutines.flow.Flow
import ru.protos.obligator.analytics.model.FavoriteItem

interface FavoriteRepository {
    fun getFavorites(): Flow<List<FavoriteItem>>
    suspend fun addFavorite(secid: String, shortName: String)
    suspend fun removeFavorite(secid: String)
    fun isFavorite(secid: String): Flow<Boolean>
}
