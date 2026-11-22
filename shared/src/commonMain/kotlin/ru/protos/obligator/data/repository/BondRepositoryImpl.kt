package ru.protos.obligator.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.UpdateInfo
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.data.db.AppDatabase
import ru.protos.obligator.data.db.UpdateInfoEntity
import ru.protos.obligator.data.db.toDomain
import ru.protos.obligator.data.db.toEntity
import ru.protos.obligator.data.iss.IssApiClient

class BondRepositoryImpl(
    private val db: AppDatabase,
    private val api: IssApiClient
) : BondRepository {

    override fun getBonds(): Flow<List<Bond>> {
        return db.bondDao().getAllBonds().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUpdateInfo(): Flow<UpdateInfo?> {
        return db.updateInfoDao().getUpdateInfo().map { it?.toDomain() }
    }

    /** Загружает облигации с MOEX, обновляет БД и выплаты позиций портфеля. */
    override suspend fun refreshBonds() {
        val refreshStart = TimeSource.Monotonic.markNow()
        val (bonds, priceDate) = api.fetchAllBonds()
        if (bonds.isNotEmpty()) {
            val loadedAt = Clock.System.now().toEpochMilliseconds()
            val entities = bonds.map { it.toEntity() }
            val roomStart = TimeSource.Monotonic.markNow()
            db.bondDao().replaceAllWithUpdateInfo(
                bonds = entities,
                info = UpdateInfoEntity(1, loadedAt, priceDate)
            )
            val roomMs = roomStart.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
            println("[BENCH_STEP] stage=room_write count=${entities.size} ms=$roomMs")
            val totalMs = refreshStart.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
            println("[BENCH_STEP] stage=refresh_total count=${entities.size} ms=$totalMs")

            val positions = db.portfolioDao().getPositions().first()
            val today = getTodayIso()
            for (pos in positions) {
                try {
                    val payments = api.fetchBondPayments(pos.secid)
                    val futurePayments = payments.filter { it.date >= today }
                    db.paymentDao().deleteForSecid(pos.secid)
                    db.paymentDao().insertAll(futurePayments.map { it.toEntity() })
                } catch (e: Exception) {
                    println("[BondRepositoryImpl] Failed to refresh payments for ${pos.secid}: ${e.message}")
                }
            }
        }
    }

    override suspend fun getBondDetails(secid: String): Bond? {
        return db.bondDao().getBondById(secid)?.toDomain()
    }

    override suspend fun fetchPayments(secid: String): List<Payment> {
        return api.fetchBondPayments(secid)
    }

    override suspend fun fetchIsQualified(secid: String): Boolean {
        return api.fetchIsQualified(secid)
    }

    private fun getTodayIso(): String {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date.toString()
    }
}
