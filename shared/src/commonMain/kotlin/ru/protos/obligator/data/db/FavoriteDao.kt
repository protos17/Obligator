package ru.protos.obligator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE secid = :secid")
    suspend fun delete(secid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE secid = :secid)")
    fun isFavorite(secid: String): Flow<Boolean>
}
