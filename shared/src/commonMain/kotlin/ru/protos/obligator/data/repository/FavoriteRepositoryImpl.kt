package ru.protos.obligator.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import ru.protos.obligator.analytics.model.FavoriteItem
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.data.db.AppDatabase
import ru.protos.obligator.data.db.FavoriteEntity

class FavoriteRepositoryImpl(
    private val db: AppDatabase
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<FavoriteItem>> {
        return db.favoriteDao().getFavorites().map { list ->
            list.map { FavoriteItem(it.secid, it.shortName, it.addedAt) }
        }
    }

    override suspend fun addFavorite(secid: String, shortName: String) {
        val entity = FavoriteEntity(
            secid = secid,
            shortName = shortName,
            addedAt = Clock.System.now().toEpochMilliseconds()
        )
        db.favoriteDao().insert(entity)
    }

    override suspend fun removeFavorite(secid: String) {
        db.favoriteDao().delete(secid)
    }

    override fun isFavorite(secid: String): Flow<Boolean> {
        return db.favoriteDao().isFavorite(secid)
    }
}
