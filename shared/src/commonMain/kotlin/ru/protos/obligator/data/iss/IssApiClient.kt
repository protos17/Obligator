package ru.protos.obligator.data.iss

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment

class IssApiClient(
    private val httpClient: HttpClient,
    private val parser: IssParser = IssParser()
) {

    companion object {
        private const val BONDIZATION_LIMIT = 100
        private const val MAX_BONDIZATION_PAGES = 50
    }

    /** Загружает и разбирает список облигаций для указанного режима торгов. */
    suspend fun fetchBoardBonds(board: String): Pair<List<Bond>, String?> {
        val url = "https://iss.moex.com/iss/engines/stock/markets/bonds/boards/$board/securities.json" +
                "?iss.meta=off&iss.only=securities,marketdata,marketdata_yields" +
                "&securities.columns=SECID,BOARDID,SHORTNAME,PREVWAPRICE,YIELDATPREVWAPRICE,COUPONVALUE,NEXTCOUPON,ACCRUEDINT,FACEVALUE,MATDATE,COUPONPERIOD,PREVDATE,FACEUNIT,ISIN,LISTLEVEL,SECTYPE,COUPONPERCENT,OFFERDATE,SETTLEDATE,BONDTYPE,COUPON_DETAILS" +
                "&marketdata.columns=SECID,DURATION" +
                "&marketdata_yields.columns=SECID,YIELDDATE,YIELDDATETYPE,DURATION,DURATIONWAPRICE"
        val networkStart = TimeSource.Monotonic.markNow()
        val responseText = httpClient.get(url).bodyAsText()
        val networkMs = networkStart.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        println("[BENCH_STEP] stage=network board=$board chars=${responseText.length} ms=$networkMs")

        val parseStart = TimeSource.Monotonic.markNow()
        val parsed = parser.parseBondsResponse(responseText)
        val parseMs = parseStart.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        println("[BENCH_STEP] stage=parse board=$board count=${parsed.first.size} ms=$parseMs")
        return parsed
    }

    /** Параллельно загружает облигации с режимов TQOB и TQCB. */
    suspend fun fetchAllBonds(): Pair<List<Bond>, String?> = coroutineScope {
        val totalStart = TimeSource.Monotonic.markNow()
        val tqobDeferred = async { fetchBoardBonds("TQOB") }
        val tqcbDeferred = async { fetchBoardBonds("TQCB") }

        val (tqobBonds, tqobDate) = tqobDeferred.await()
        val (tqcbBonds, tqcbDate) = tqcbDeferred.await()

        val allBonds = tqobBonds + tqcbBonds
        val priceDate = tqobDate ?: tqcbDate
        val totalMs = totalStart.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        println("[BENCH_STEP] stage=fetch_all_total count=${allBonds.size} ms=$totalMs")
        Pair(allBonds, priceDate)
    }

    /** Загружает все страницы купонов и амортизаций по облигации. */
    suspend fun fetchBondPayments(secid: String): List<Payment> {
        val firstUrl = "https://iss.moex.com/iss/securities/$secid/bondization.json" +
                "?iss.meta=off&iss.only=coupons,amortizations,coupons.cursor,amortizations.cursor" +
                "&start=0&limit=$BONDIZATION_LIMIT"
        val firstPage = parser.parseBondizationPage(httpClient.get(firstUrl).bodyAsText(), secid)

        val allPayments = mutableListOf<Payment>()
        allPayments.addAll(firstPage.coupons)
        allPayments.addAll(firstPage.amortizations)

        fetchRemainingBlockPages(
            secid = secid,
            blockName = "coupons",
            initialCursor = firstPage.couponsCursor,
            initialCount = firstPage.coupons.size,
            extractItems = { it.coupons },
            extractCursor = { it.couponsCursor },
            sink = allPayments
        )

        fetchRemainingBlockPages(
            secid = secid,
            blockName = "amortizations",
            initialCursor = firstPage.amortizationsCursor,
            initialCount = firstPage.amortizations.size,
            extractItems = { it.amortizations },
            extractCursor = { it.amortizationsCursor },
            sink = allPayments
        )

        return allPayments.sortedBy { it.date }
    }

    private suspend fun fetchRemainingBlockPages(
        secid: String,
        blockName: String,
        initialCursor: IssCursor?,
        initialCount: Int,
        extractItems: (BondizationPage) -> List<Payment>,
        extractCursor: (BondizationPage) -> IssCursor?,
        sink: MutableList<Payment>
    ) {
        var cursor = initialCursor ?: return
        if (initialCount == 0) return

        val visitedStarts = mutableSetOf(cursor.index)
        var pagesFetched = 1

        while (pagesFetched < MAX_BONDIZATION_PAGES) {
            if (cursor.pageSize <= 0) break
            val nextStart = cursor.index + cursor.pageSize
            if (nextStart >= cursor.total) break
            if (!visitedStarts.add(nextStart)) break

            val url = "https://iss.moex.com/iss/securities/$secid/bondization.json" +
                    "?iss.meta=off&iss.only=$blockName,$blockName.cursor" +
                    "&start=$nextStart&limit=$BONDIZATION_LIMIT"
            val page = parser.parseBondizationPage(httpClient.get(url).bodyAsText(), secid)
            val items = extractItems(page)
            if (items.isEmpty()) break

            sink.addAll(items)
            pagesFetched++

            val nextCursor = extractCursor(page) ?: break
            if (nextCursor.index < nextStart) break
            cursor = nextCursor
        }
    }

    suspend fun fetchIsQualified(secid: String): Boolean {
        val url = "https://iss.moex.com/iss/securities/$secid.json?iss.meta=off&iss.only=description"
        val responseText = httpClient.get(url).bodyAsText()
        return parser.parseIsQualified(responseText)
    }
}
