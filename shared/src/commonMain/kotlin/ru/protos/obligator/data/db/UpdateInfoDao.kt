package ru.protos.obligator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateInfoDao {
    @Query("SELECT * FROM update_info WHERE id = 1")
    fun getUpdateInfo(): Flow<UpdateInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUpdateInfo(info: UpdateInfoEntity)

    @Query("DELETE FROM update_info")
    suspend fun clear()
}
