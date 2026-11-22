package ru.protos.obligator.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import ru.protos.obligator.analytics.model.PaymentKind
import ru.protos.obligator.data.iss.IssApiClient
import ru.protos.obligator.data.iss.IssParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IssApiClientMockTest {

    @Test
    fun testFetchAllBondsWithMockEngine() = runTest {
        val requestedUrls = mutableListOf<String>()
        val tqobJson = """
        {
          "securities": {
            "columns": ["SECID", "BOARDID", "SHORTNAME", "PREVWAPRICE", "YIELDATPREVWAPRICE", "COUPONVALUE", "NEXTCOUPON", "ACCRUEDINT", "FACEVALUE", "MATDATE", "COUPONPERIOD", "PREVDATE", "FACEUNIT", "ISIN", "LISTLEVEL", "SECTYPE", "COUPONPERCENT", "OFFERDATE", "SETTLEDATE", "BONDTYPE", "COUPON_DETAILS"],
            "data": [
              ["SU26238RMFS4", "TQOB", "ОФЗ 26238", 53.08, 16.5, 35.4, "2026-11-18", 1.5, 1000.0, "2041-05-15", 182, "2026-11-20", "SUR", "RU000A1038V6", 1, "3", 7.1, null, "2026-11-23", "ОФЗ-ПД", "Фиксированный"]
            ]
          },
          "marketdata": {
            "columns": ["SECID", "DURATION"],
            "data": [
              ["SU26238RMFS4", 2845]
            ]
          },
          "marketdata_yields": {
            "columns": ["SECID", "YIELDDATE", "YIELDDATETYPE", "DURATION", "DURATIONWAPRICE"],
            "data": [
              ["SU26238RMFS4", "2041-05-15", "maturity", 2845, 2840]
            ]
          }
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            requestedUrls.add(request.url.toString())
            respond(
                content = tqobJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val api = IssApiClient(client, IssParser())

        val (bonds, priceDate) = api.fetchAllBonds()
        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.all { it.contains("iss.only=securities,marketdata,marketdata_yields") })
        assertTrue(requestedUrls.all { it.contains("marketdata_yields.columns=SECID,YIELDDATE,YIELDDATETYPE,DURATION,DURATIONWAPRICE") })
        assertTrue(requestedUrls.none { it.contains("BONDSUBTYPE") })
        assertEquals(2, bonds.size)
        assertEquals("2026-11-20", priceDate)
        assertEquals("SU26238RMFS4", bonds[0].secid)
        assertEquals(2845, bonds[0].duration)
        assertEquals("к погашению", bonds[0].yieldKind)
    }

    @Test
    fun testFetchBondPaymentsWithMockEngine() = runTest {
        val paymentsJson = """
        {
          "coupons": {
            "columns": ["coupondate", "value_rub"],
            "data": [
              ["2026-12-20", 67.68],
              ["2027-06-20", 0.0]
            ]
          },
          "coupons.cursor": {
            "columns": ["INDEX", "TOTAL", "PAGESIZE"],
            "data": [[0, 2, 100]]
          },
          "amortizations": {
            "columns": ["amortdate", "value_rub"],
            "data": [
              ["2027-12-20", 1000.0]
            ]
          },
          "amortizations.cursor": {
            "columns": ["INDEX", "TOTAL", "PAGESIZE"],
            "data": [[0, 1, 100]]
          }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = paymentsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val api = IssApiClient(client, IssParser())

        val payments = api.fetchBondPayments("RU000A105C15")
        assertEquals(3, payments.size)
        val zeroCoupon = payments.find { it.date == "2027-06-20" }
        assertEquals(null, zeroCoupon?.amount)
    }

    @Test
    fun testFetchBondPaymentsWithoutCursorBlockTreatedAsSinglePage() = runTest {
        val requestedUrls = mutableListOf<String>()
        val noCursorJson = """
        {
          "coupons": {
            "columns": ["coupondate", "value_rub"],
            "data": [
              ["2026-11-04", 42.38],
              ["2027-05-05", 42.38]
            ]
          },
          "amortizations": {
            "columns": ["amortdate", "value_rub"],
            "data": [
              ["2031-09-17", 1000.0]
            ]
          }
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            requestedUrls.add(request.url.toString())
            respond(
                content = noCursorJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = IssApiClient(HttpClient(mockEngine), IssParser())
        val payments = api.fetchBondPayments("SU26218RMFS6")

        assertEquals(1, requestedUrls.size)
        assertTrue(requestedUrls[0].contains("coupons.cursor"))
        assertTrue(requestedUrls[0].contains("amortizations.cursor"))
        assertEquals(3, payments.size)
        assertEquals(2, payments.count { it.kind == PaymentKind.COUPON })
        assertEquals(1, payments.count { it.kind == PaymentKind.REPAYMENT })
    }

    @Test
    fun testFetchBondPaymentsMultiPagePagination() = runTest {
        val requestedUrls = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            requestedUrls.add(url)
            val start = request.url.parameters["start"]?.toIntOrNull() ?: 0
            val only = request.url.parameters["iss.only"] ?: ""
            val body = when {
                start == 0 -> """
                {
                  "coupons": {
                    "columns": ["coupondate", "value_rub"],
                    "data": [
                      ["2025-06-01", 35.0],
                      ["2025-12-01", 35.0]
                    ]
                  },
                  "coupons.cursor": {
                    "columns": ["INDEX", "TOTAL", "PAGESIZE"],
                    "data": [[0, 4, 2]]
                  },
                  "amortizations": {
                    "columns": ["amortdate", "value_rub"],
                    "data": [
                      ["2026-06-01", 500.0]
                    ]
                  },
                  "amortizations.cursor": {
                    "columns": ["INDEX", "TOTAL", "PAGESIZE"],
                    "data": [[0, 2, 1]]
                  }
                }
                """.trimIndent()
                only.startsWith("coupons") && start == 2 -> """
                {
                  "coupons": {
                    "columns": ["coupondate", "value_rub"],
                    "data": [
                      ["2026-06-01", 35.0],
                      ["2026-12-01", 35.0]
                    ]
                  },
                  "coupons.cursor": {
                    "columns": ["INDEX", "TOTAL", "PAGESIZE"],
                    "data": [[2, 4, 2]]
                  }
                }
                """.trimIndent()
                only.startsWith("amortizations") && start == 1 -> """
                {
                  "amortizations": {
                    "columns": ["amortdate", "value_rub"],
                    "data": [
                      ["2026-12-01", 500.0]
                    ]
                  },
                  "amortizations.cursor": {
                    "columns": ["INDEX", "TOTAL", "PAGESIZE"],
                    "data": [[1, 2, 1]]
                  }
                }
                """.trimIndent()
                else -> "{}"
            }
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = IssApiClient(HttpClient(mockEngine), IssParser())
        val payments = api.fetchBondPayments("SU26238RMFS4")

        assertEquals(3, requestedUrls.size)
        assertEquals(6, payments.size)
        assertEquals(4, payments.count { it.kind == PaymentKind.COUPON })
        assertEquals(2, payments.count { it.kind == PaymentKind.REPAYMENT })
        assertEquals("2025-06-01", payments.first().date)
        assertEquals("2026-12-01", payments.last().date)
    }

    @Test
    fun testFetchBondPaymentsStopsOnStuckCursor() = runTest {
        var requests = 0
        val stuckJson = """
        {
          "coupons": {
            "columns": ["coupondate", "value_rub"],
            "data": [
              ["2026-06-01", 35.0]
            ]
          },
          "coupons.cursor": {
            "columns": ["INDEX", "TOTAL", "PAGESIZE"],
            "data": [[0, 1000, 1]]
          }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            requests++
            respond(
                content = stuckJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = IssApiClient(HttpClient(mockEngine), IssParser())
        val payments = api.fetchBondPayments("SU26238RMFS4")

        assertEquals(2, requests)
        assertEquals(2, payments.size)
    }

    @Test
    fun testFetchIsQualifiedWithMockEngine() = runTest {
        val descJson = """
        {
          "description": {
            "columns": ["name", "title", "value"],
            "data": [
              ["SECID", "Код", "RU000A105C15"],
              ["ISQUALIFIEDINVESTORS", "Квалы", "1"]
            ]
          }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = descJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val api = IssApiClient(client, IssParser())

        val isQual = api.fetchIsQualified("RU000A105C15")
        assertTrue(isQual)
    }
}
