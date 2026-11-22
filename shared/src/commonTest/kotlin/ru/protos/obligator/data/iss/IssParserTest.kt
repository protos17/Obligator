package ru.protos.obligator.data.iss

import ru.protos.obligator.analytics.calc.compareBonds
import ru.protos.obligator.analytics.calc.getCouponType
import ru.protos.obligator.analytics.calc.getIssuerGroup
import ru.protos.obligator.analytics.model.CouponType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IssParserTest {

    private val parser = IssParser()

    @Test
    fun testParseBondsResponseEdgeCases() {
        val json = """
        {
          "securities": {
            "columns": [
              "SECID", "BOARDID", "SHORTNAME", "PREVWAPRICE", "YIELDATPREVWAPRICE",
              "COUPONVALUE", "NEXTCOUPON", "ACCRUEDINT", "FACEVALUE", "MATDATE",
              "COUPONPERIOD", "PREVDATE", "FACEUNIT", "ISIN", "LISTLEVEL",
              "SECTYPE", "COUPONPERCENT", "OFFERDATE", "SETTLEDATE", "BONDTYPE",
              "COUPON_DETAILS"
            ],
            "data": [
              [
                "SU29014RMFS6", "TQOB", "ОФЗ 29014", 95.5, 12.5,
                35.0, "2025-06-01", 10.2, 1000.0, "2029-03-25",
                182, "2025-02-28", "SUR", "RU000A0JXTS2", 1,
                "3", 14.0, null, "2025-03-03", "ОФЗ с плавающим купоном",
                "Плавающий купон"
              ],
              [
                "USD_BOND", "TQCB", "US Dollar Bond", 101.0, 5.0,
                25.0, "2025-07-01", 1.0, 1000.0, "2030-01-01",
                182, "2025-02-28", "USD", "US1234567890", 1,
                "6", 5.0, null, "2025-03-03", "Корпоративная",
                "Фиксированный"
              ],
              [
                "NO_PRICE_BOND", "TQCB", "No Price Corp", 0.0, 0.0,
                30.0, "2025-08-01", 5.0, 1000.0, "2027-05-15",
                91, "2025-02-28", "SUR", "RU000NOPRICE1", 2,
                "6", 12.0, null, "2025-03-03", "Корпоративная",
                "Фиксированный"
              ],
              [
                "NO_MAT_BOND", "TQCB", "Perpetual Bond", 99.0, 15.0,
                40.0, "2025-09-01", 8.0, 1000.0, "0000-00-00",
                182, "2025-02-28", "SUR", "RU000NOMATD1", 1,
                "6", 16.0, null, "2025-03-03", "Корпоративная",
                "Фиксированный"
              ],
              [
                "AMORT_BOND", "TQCB", "Amortized Muni", 98.0, 13.0,
                20.0, "2025-05-10", 3.0, 750.0, "2026-11-20",
                91, "2025-02-28", "SUR", "RU000AMORT01", 1,
                "4", 11.0, "2026-01-15", "2025-03-03", "Амортизируемый выпуск",
                "Фиксированный"
              ]
            ]
          },
          "marketdata": {
            "columns": ["SECID", "DURATION"],
            "data": [
              ["SU29014RMFS6", 1200],
              ["USD_BOND", 1500],
              ["NO_PRICE_BOND", 600],
              ["NO_MAT_BOND", null],
              ["AMORT_BOND", 400]
            ]
          }
        }
        """.trimIndent()

        val (bonds, priceDate) = parser.parseBondsResponse(json)

        assertEquals("2025-02-28", priceDate)
        assertEquals(4, bonds.size)

        val floatingOfz = bonds.first { it.secid == "SU29014RMFS6" }
        assertEquals("GOVT", getIssuerGroup(floatingOfz.secType))
        assertEquals(CouponType.FLOATING, getCouponType(floatingOfz))
        assertEquals(1200, floatingOfz.duration)

        val noPriceBond = bonds.first { it.secid == "NO_PRICE_BOND" }
        assertNull(noPriceBond.price)
        assertNull(noPriceBond.yieldToMaturity)

        val noMatBond = bonds.first { it.secid == "NO_MAT_BOND" }
        assertNull(noMatBond.matDate)

        val amortBond = bonds.first { it.secid == "AMORT_BOND" }
        assertEquals(750.0, amortBond.faceValue)
        assertEquals("2026-01-15", amortBond.offerDate)
        assertEquals("MUNI", getIssuerGroup(amortBond.secType))
    }

    @Test
    fun testParseMarketdataYieldsAndCompareWarning() {
        val json = """
        {
          "securities": {
            "columns": ["SECID", "BOARDID", "SHORTNAME", "PREVWAPRICE", "YIELDATPREVWAPRICE", "FACEVALUE", "FACEUNIT", "YIELD_KIND"],
            "data": [
              ["B_MAT", "TQOB", "ОФЗ 26238", 50.5, 16.5, 1000.0, "SUR", "ignored"],
              ["B_OFFER", "TQCB", "Корп Оферта", 98.0, 18.2, 1000.0, "SUR", "ignored"],
              ["B_MBS", "TQCB", "Ипотечная", 99.0, 14.0, 1000.0, "SUR", "ignored"],
              ["B_OTHER", "TQCB", "Другая", 97.0, 15.0, 1000.0, "SUR", "ignored"],
              ["B_MULTI", "TQCB", "Несколько строк", 96.0, 19.0, 1000.0, "SUR", "ignored"]
            ]
          },
          "marketdata": {
            "columns": ["SECID", "DURATION"],
            "data": [
              ["B_MAT", 2412],
              ["B_OFFER", 365],
              ["B_MBS", 500],
              ["B_OTHER", 400],
              ["B_MULTI", 300]
            ]
          },
          "marketdata_yields": {
            "columns": ["SECID", "YIELDDATE", "YIELDDATETYPE", "DURATION", "DURATIONWAPRICE"],
            "data": [
              ["B_MAT", "2041-05-15", "maturity", 2412, 2410],
              ["B_OFFER", "2027-06-10", "offer", 365, 360],
              ["B_MBS", "2030-01-01", "MBS", 500, 495],
              ["B_OTHER", "2028-01-01", "", 400, 398],
              ["B_OTHER", "2029-01-01", "custom_type", 400, 398],
              ["B_MULTI", "2026-01-01", "MBS", 100, 100],
              ["B_MULTI", "2030-05-15", "maturity", 900, 895],
              ["B_MULTI", "2027-03-20", "offer", 300, 298]
            ]
          }
        }
        """.trimIndent()

        val (bonds, _) = parser.parseBondsResponse(json)
        assertEquals(5, bonds.size)

        val matBond = bonds.first { it.secid == "B_MAT" }
        val offerBond = bonds.first { it.secid == "B_OFFER" }
        val mbsBond = bonds.first { it.secid == "B_MBS" }
        val otherBond = bonds.first { it.secid == "B_OTHER" }
        val multiBond = bonds.first { it.secid == "B_MULTI" }

        assertEquals("к погашению", matBond.yieldKind)
        assertEquals("к оферте", offerBond.yieldKind)
        assertNull(mbsBond.yieldKind)
        assertNull(otherBond.yieldKind)
        assertEquals("к оферте", multiBond.yieldKind)

        val compareResult = compareBonds(listOf(matBond, offerBond))
        assertTrue(compareResult.hasDifferentYieldKinds)
        val ytmRow = compareResult.rows.first { it.id == "yieldToMaturity" }
        assertEquals(listOf("16,50% к погашению", "18,20% к оферте"), ytmRow.values)
        assertEquals(emptySet(), ytmRow.highlighted)
    }

    @Test
    fun testParsePaymentsResponse() {
        val json = """
        {
          "coupons": {
            "columns": ["coupondate", "value_rub"],
            "data": [
              ["2025-06-01", 35.0],
              ["2025-12-01", 35.0]
            ]
          },
          "amortizations": {
            "columns": ["amortdate", "value_rub"],
            "data": [
              ["2025-12-01", 250.0]
            ]
          }
        }
        """.trimIndent()

        val payments = parser.parsePaymentsResponse(json, "SU29014RMFS6")
        assertEquals(3, payments.size)
        assertEquals("2025-06-01", payments[0].date)
        assertEquals(35.0, payments[0].amount)
        assertEquals("2025-12-01", payments[1].date)
        assertEquals("2025-12-01", payments[2].date)
    }

    @Test
    fun testParseIsQualified() {
        val jsonQualified = """
        {
          "description": {
            "columns": ["name", "title", "value"],
            "data": [
              ["SECID", "Код ценной бумаги", "SU29014RMFS6"],
              ["ISQUALIFIEDINVESTORS", "Только для квад. инвесторов", "1"]
            ]
          }
        }
        """.trimIndent()

        val jsonNotQualified = """
        {
          "description": {
            "columns": ["name", "title", "value"],
            "data": [
              ["SECID", "Код ценной бумаги", "SU29014RMFS6"],
              ["ISQUALIFIEDINVESTORS", "Только для квад. инвесторов", "0"]
            ]
          }
        }
        """.trimIndent()

        assertTrue(parser.parseIsQualified(jsonQualified))
        assertFalse(parser.parseIsQualified(jsonNotQualified))
    }

    @Test
    fun testDurationZeroParsedAsNull() {
        val json = """
        {
          "securities": {
            "columns": ["SECID", "BOARDID", "SHORTNAME", "FACEVALUE", "FACEUNIT"],
            "data": [
              ["SU29007", "TQOB", "ОФЗ 29007", 1000.0, "SUR"]
            ]
          },
          "marketdata": {
            "columns": ["SECID", "DURATION"],
            "data": [
              ["SU29007", 0]
            ]
          }
        }
        """.trimIndent()

        val (bonds, _) = parser.parseBondsResponse(json)
        assertEquals(1, bonds.size)
        assertNull(bonds[0].duration)
    }

    @Test
    fun testBondizationZeroAmountParsedAsNull() {
        val json = """
        {
          "coupons": {
            "columns": ["coupondate", "value_rub"],
            "data": [
              ["2025-06-01", 0.0],
              ["2025-12-01", 35.0]
            ]
          }
        }
        """.trimIndent()

        val payments = parser.parsePaymentsResponse(json, "TEST")
        assertEquals(2, payments.size)
        assertNull(payments[0].amount)
        assertEquals(35.0, payments[1].amount)
    }
}
