package ru.protos.obligator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_position ORDER BY addedAt DESC")
    fun getPositions(): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(position: PortfolioEntity)

    @Query("DELETE FROM portfolio_position WHERE secid = :secid")
    suspend fun delete(secid: String)
}
