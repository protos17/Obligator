package ru.protos.obligator.data.db

import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomTransactionTest {

    private fun createInMemoryDb(): AppDatabase {
        return Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    private fun sampleBond(secid: String, shortName: String): BondEntity {
        return BondEntity(
            secid = secid,
            boardId = "TQOB",
            isin = secid,
            shortName = shortName,
            secType = "3",
            faceValue = 1000.0,
            price = 95.0,
            accruedInt = 10.0,
            yieldToMaturity = 15.0,
            duration = 500,
            couponPercent = 12.0,
            couponValue = 60.0,
            couponPeriod = 182,
            nextCoupon = "2027-01-01",
            matDate = "2030-01-01",
            offerDate = null,
            yieldKind = null,
            couponDetails = null,
            bondType = null,
            listLevel = 1
        )
    }

    @Test
    fun testReplaceAllWithUpdateInfoRollsBackBothTablesOnFailure() = runTest {
        val db = createInMemoryDb()
        try {
            val initialBonds = listOf(sampleBond("OLD_1", "Old Bond 1"), sampleBond("OLD_2", "Old Bond 2"))
            val initialInfo = UpdateInfoEntity(id = 1, loadedAt = 1000L, priceDate = "2026-11-19")
            db.bondDao().replaceAllWithUpdateInfo(initialBonds, initialInfo)

            assertEquals(listOf("OLD_1", "OLD_2"), db.bondDao().getAllBonds().first().map { it.secid }.sorted())
            assertEquals("2026-11-19", db.updateInfoDao().getUpdateInfo().first()?.priceDate)

            db.useWriterConnection { transactor ->
                transactor.execSQL(
                    "CREATE TRIGGER fail_update_info BEFORE INSERT ON update_info " +
                        "WHEN NEW.priceDate = 'FAIL' " +
                        "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END;"
                )
            }

            var failed = false
            try {
                db.bondDao().replaceAllWithUpdateInfo(
                    bonds = listOf(sampleBond("NEW_1", "New Bond 1")),
                    info = UpdateInfoEntity(id = 1, loadedAt = 2000L, priceDate = "FAIL")
                )
            } catch (_: Exception) {
                failed = true
            }

            assertTrue(failed)
            val bondsAfterFailure = db.bondDao().getAllBonds().first().map { it.secid }.sorted()
            val infoAfterFailure = db.updateInfoDao().getUpdateInfo().first()

            assertEquals(listOf("OLD_1", "OLD_2"), bondsAfterFailure)
            assertEquals(1000L, infoAfterFailure?.loadedAt)
            assertEquals("2026-11-19", infoAfterFailure?.priceDate)
        } finally {
            db.close()
        }
    }
}
