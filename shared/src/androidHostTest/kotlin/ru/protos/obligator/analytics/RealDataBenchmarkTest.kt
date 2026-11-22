package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.filterBonds
import ru.protos.obligator.analytics.calc.searchBonds
import ru.protos.obligator.analytics.calc.sortBonds
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class RealDataBenchmarkTest {

    private fun loadRealBondsFromResource(): List<Bond> {
        val stream = requireNotNull(
            this::class.java.classLoader?.getResourceAsStream("obligator_2870.tsv")
        )
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() }.map { line ->
                val cols = line.split('\t')
                fun str(i: Int): String? = cols.getOrNull(i)?.takeIf { it.isNotEmpty() }
                Bond(
                    secid = cols[0],
                    boardId = cols[1],
                    isin = str(2),
                    shortName = cols[3],
                    secType = str(4),
                    faceValue = cols[5].toDouble(),
                    price = str(6)?.toDoubleOrNull(),
                    accruedInt = str(7)?.toDoubleOrNull(),
                    yieldToMaturity = str(8)?.toDoubleOrNull(),
                    duration = str(9)?.toIntOrNull(),
                    couponPercent = str(10)?.toDoubleOrNull(),
                    couponValue = str(11)?.toDoubleOrNull(),
                    couponPeriod = str(12)?.toIntOrNull(),
                    nextCoupon = str(13),
                    matDate = str(14),
                    offerDate = str(15),
                    yieldKind = str(16),
                    couponDetails = str(17),
                    bondType = str(18),
                    listLevel = str(19)?.toIntOrNull()
                )
            }.toList()
        }
    }

    @Test
    fun benchmarkFilterAndSortReal2870() {
        val bonds = loadRealBondsFromResource()
        assertEquals(2870, bonds.size)

        val filter = BondFilter(minYieldToMat = 12.0, maxYieldToMat = 25.0, listLevels = setOf(1, 2))
        val warmupRuns = 5
        val measuredRuns = 10
        val priceDate = "2026-11-20"

        repeat(warmupRuns) {
            val searched = searchBonds(bonds, "")
            val filtered = filterBonds(searched, filter, currentDateStr = priceDate)
            sortBonds(filtered, SortField.YIELD_TO_MATURITY, SortOrder.DESC, currentDateStr = priceDate)
        }

        val times = mutableListOf<Double>()
        var lastCount = 0
        repeat(measuredRuns) {
            val mark = TimeSource.Monotonic.markNow()
            val searched = searchBonds(bonds, "")
            val filtered = filterBonds(searched, filter, currentDateStr = priceDate)
            val sorted = sortBonds(filtered, SortField.YIELD_TO_MATURITY, SortOrder.DESC, currentDateStr = priceDate)
            val durationMs = mark.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
            times.add(durationMs)
            lastCount = sorted.size
        }

        val sortedTimes = times.sorted()
        val median = (sortedTimes[4] + sortedTimes[5]) / 2.0
        val minT = sortedTimes.first()
        val maxT = sortedTimes.last()
        val mean = times.average()
        val variance = times.sumOf { (it - mean) * (it - mean) } / times.size
        val stdDev = sqrt(variance)

        println("BENCHMARK_REAL_2870: n=${bonds.size}, matched=$lastCount, warmup=$warmupRuns, runs=$measuredRuns, median=${median}ms, min=${minT}ms, max=${maxT}ms, stdDev=${stdDev}ms")
        assertEquals(measuredRuns, times.size)
        assertTrue(lastCount > 0)
        assertTrue(median < 1000.0)
    }
}
