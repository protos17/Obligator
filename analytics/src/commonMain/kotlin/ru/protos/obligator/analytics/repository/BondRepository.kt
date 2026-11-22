package ru.protos.obligator.analytics.repository

import kotlinx.coroutines.flow.Flow
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.UpdateInfo

interface BondRepository {
    fun getBonds(): Flow<List<Bond>>
    fun getUpdateInfo(): Flow<UpdateInfo?>
    suspend fun refreshBonds()
    suspend fun getBondDetails(secid: String): Bond?
    suspend fun fetchPayments(secid: String): List<Payment>
    suspend fun fetchIsQualified(secid: String): Boolean
}
