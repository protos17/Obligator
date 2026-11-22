package ru.protos.obligator.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.protos.obligator.analytics.model.PortfolioPosition
import ru.protos.obligator.analytics.repository.PortfolioRepository
import ru.protos.obligator.data.db.AppDatabase
import ru.protos.obligator.data.db.PortfolioEntity
import ru.protos.obligator.data.db.toEntity
import ru.protos.obligator.data.iss.IssApiClient

class PortfolioRepositoryImpl(
    private val db: AppDatabase,
    private val api: IssApiClient
) : PortfolioRepository {

    private fun getTodayIso(): String {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date.toString()
    }

    override fun getPositions(): Flow<List<PortfolioPosition>> {
        return db.portfolioDao().getPositions().map { list ->
            list.map { PortfolioPosition(it.secid, it.shortName, it.quantity, it.addedAt) }
        }
    }

    /** Сохраняет позицию портфеля и обновляет её будущие выплаты. */
    override suspend fun addOrUpdatePosition(secid: String, shortName: String, quantity: Int) {
        val entity = PortfolioEntity(
            secid = secid,
            shortName = shortName,
            quantity = quantity,
            addedAt = Clock.System.now().toEpochMilliseconds()
        )
        db.portfolioDao().insertOrUpdate(entity)

        try {
            val payments = api.fetchBondPayments(secid)
            val today = getTodayIso()
            val futurePayments = payments.filter { it.date >= today }

            db.paymentDao().deleteForSecid(secid)
            db.paymentDao().insertAll(futurePayments.map { it.toEntity() })
        } catch (_: Exception) {
        }
    }

    /** Удаляет позицию портфеля и связанные с ней выплаты. */
    override suspend fun removePosition(secid: String) {
        db.portfolioDao().delete(secid)
        db.paymentDao().deleteForSecid(secid)
    }
}
