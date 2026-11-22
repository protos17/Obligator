package ru.protos.obligator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BondDao {
    @Query("SELECT * FROM bond")
    fun getAllBonds(): Flow<List<BondEntity>>

    @Query("SELECT * FROM bond WHERE secid = :secid")
    suspend fun getBondById(secid: String): BondEntity?

    @Query("DELETE FROM bond")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bonds: List<BondEntity>)

    @Query("DELETE FROM update_info")
    suspend fun clearUpdateInfo()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdateInfo(info: UpdateInfoEntity)

    @Transaction
    suspend fun replaceAllWithUpdateInfo(bonds: List<BondEntity>, info: UpdateInfoEntity) {
        clearAll()
        insertAll(bonds)
        clearUpdateInfo()
        insertUpdateInfo(info)
    }
}
