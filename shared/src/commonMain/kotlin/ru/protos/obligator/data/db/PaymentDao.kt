package ru.protos.obligator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment WHERE secid = :secid AND payDate >= :minDate")
    suspend fun getFuturePaymentsForSecid(secid: String, minDate: String): List<PaymentEntity>

    @Query("SELECT * FROM payment WHERE payDate >= :minDate")
    fun getFuturePayments(minDate: String): Flow<List<PaymentEntity>>

    @Query("DELETE FROM payment WHERE secid = :secid")
    suspend fun deleteForSecid(secid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)
}
