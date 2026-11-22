package ru.protos.obligator.analytics

import ru.protos.obligator.analytics.calc.filterBonds
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

class BenchmarkTest {

    private fun generateSyntheticBonds(count: Int): List<Bond> {
        return (1..count).map { i ->
            Bond(
                secid = "SYNTH_$i",
                boardId = if (i % 2 == 0) "TQOB" else "TQCB",
                shortName = "Bond $i",
                secType = if (i % 3 == 0) "3" else if (i % 3 == 1) "6" else "4",
                faceValue = 1000.0,
                price = 90.0 + (i % 20),
                yieldToMaturity = 10.0 + (i % 15),
                matDate = "2030-12-31",
                listLevel = (i % 3) + 1
            )
        }
    }

    @Test
    fun benchmarkFilterAndSortSynthetic4000() {
        val bonds = generateSyntheticBonds(4000)
        val filter = BondFilter(minYieldToMat = 12.0, maxYieldToMat = 20.0, listLevels = setOf(1, 2))
        val warmupRuns = 5
        val measuredRuns = 10

        repeat(warmupRuns) {
            val filtered = filterBonds(bonds, filter, currentDateStr = "2026-11-20")
            sortBonds(filtered, SortField.YIELD_TO_MATURITY, SortOrder.DESC, currentDateStr = "2026-11-20")
        }

        val times = mutableListOf<Double>()
        var lastCount = 0
        repeat(measuredRuns) {
            val mark = TimeSource.Monotonic.markNow()
            val filtered = filterBonds(bonds, filter, currentDateStr = "2026-11-20")
            val sorted = sortBonds(filtered, SortField.YIELD_TO_MATURITY, SortOrder.DESC, currentDateStr = "2026-11-20")
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

        println("BENCHMARK_SYNTH_4000: n=${bonds.size}, matched=$lastCount, warmup=$warmupRuns, runs=$measuredRuns, median=${median}ms, min=${minT}ms, max=${maxT}ms, stdDev=${stdDev}ms")
        assertEquals(measuredRuns, times.size)
        assertTrue(lastCount > 0)
        assertTrue(median < 1000.0)
    }
}
