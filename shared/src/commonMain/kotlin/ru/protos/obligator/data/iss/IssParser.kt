package ru.protos.obligator.data.iss

import kotlinx.serialization.json.Json
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PaymentKind

data class IssCursor(
    val index: Int,
    val total: Int,
    val pageSize: Int
)

data class BondizationPage(
    val coupons: List<Payment>,
    val amortizations: List<Payment>,
    val couponsCursor: IssCursor?,
    val amortizationsCursor: IssCursor?
)

class IssParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    /** Разбирает JSON-ответ MOEX ISS со списком облигаций и датой цен. */
    fun parseBondsResponse(jsonStr: String): Pair<List<Bond>, String?> {
        val dto = json.decodeFromString<IssSecuritiesResponseDto>(jsonStr)
        val secTable = dto.securities ?: return Pair(emptyList(), null)
        val mktTable = dto.marketdata

        val secIdIdx = secTable.getColumnIndex("SECID")
        val boardIdIdx = secTable.getColumnIndex("BOARDID")
        val shortNameIdx = secTable.getColumnIndex("SHORTNAME")
        val prevPriceIdx = secTable.getColumnIndex("PREVWAPRICE")
        val yieldIdx = secTable.getColumnIndex("YIELDATPREVWAPRICE")
        val couponValueIdx = secTable.getColumnIndex("COUPONVALUE")
        val nextCouponIdx = secTable.getColumnIndex("NEXTCOUPON")
        val accruedIntIdx = secTable.getColumnIndex("ACCRUEDINT")
        val faceValueIdx = secTable.getColumnIndex("FACEVALUE")
        val matDateIdx = secTable.getColumnIndex("MATDATE")
        val couponPeriodIdx = secTable.getColumnIndex("COUPONPERIOD")
        val prevDateIdx = secTable.getColumnIndex("PREVDATE")
        val faceUnitIdx = secTable.getColumnIndex("FACEUNIT")
        val isinIdx = secTable.getColumnIndex("ISIN")
        val listLevelIdx = secTable.getColumnIndex("LISTLEVEL")
        val secTypeIdx = secTable.getColumnIndex("SECTYPE")
        val couponPercentIdx = secTable.getColumnIndex("COUPONPERCENT")
        val offerDateIdx = secTable.getColumnIndex("OFFERDATE")
        val couponDetailsIdx = secTable.getColumnIndex("COUPON_DETAILS")
        val bondTypeIdx = secTable.getColumnIndex("BONDTYPE")

        val durationMap = mutableMapOf<String, Int?>()
        if (mktTable != null) {
            val mktSecIdIdx = mktTable.getColumnIndex("SECID")
            val durIdx = mktTable.getColumnIndex("DURATION")
            if (mktSecIdIdx >= 0) {
                for (row in mktTable.data) {
                    val secid = row.getString(mktSecIdIdx) ?: continue
                    val durRaw = if (durIdx >= 0) row.getInt(durIdx) else null
                    val dur = if (durRaw != null && durRaw == 0) null else durRaw
                    durationMap[secid] = dur
                }
            }
        }

        val yieldKindMap = mutableMapOf<String, Pair<String, String?>>()
        val yieldsTable = dto.marketdataYields
        if (yieldsTable != null) {
            val mySecIdIdx = yieldsTable.getColumnIndex("SECID")
            val yieldDateIdx = yieldsTable.getColumnIndex("YIELDDATE")
            val yieldDateTypeIdx = yieldsTable.getColumnIndex("YIELDDATETYPE")
            if (mySecIdIdx >= 0 && yieldDateTypeIdx >= 0) {
                for (row in yieldsTable.data) {
                    val secid = row.getString(mySecIdIdx) ?: continue
                    val mappedKind = when (row.getString(yieldDateTypeIdx)?.trim()?.lowercase()) {
                        "maturity" -> "к погашению"
                        "offer" -> "к оферте"
                        else -> null
                    } ?: continue
                    val yieldDate = row.getString(yieldDateIdx)
                    val existing = yieldKindMap[secid]
                    val existingDate = existing?.second
                    if (existing == null || (yieldDate != null && (existingDate == null || yieldDate < existingDate))) {
                        yieldKindMap[secid] = Pair(mappedKind, yieldDate)
                    }
                }
            }
        }

        var priceDate: String? = null
        val bonds = mutableListOf<Bond>()

        for (row in secTable.data) {
            val faceUnit = row.getString(faceUnitIdx)
            if (faceUnit != "SUR") continue

            val secid = row.getString(secIdIdx) ?: continue
            val boardId = row.getString(boardIdIdx) ?: ""
            val shortName = row.getString(shortNameIdx) ?: secid
            val faceValue = row.getDouble(faceValueIdx) ?: continue

            if (priceDate == null && prevDateIdx >= 0) {
                priceDate = row.getString(prevDateIdx)
            }

            val bond = Bond(
                secid = secid,
                boardId = boardId,
                isin = row.getString(isinIdx),
                shortName = shortName,
                secType = row.getString(secTypeIdx),
                faceValue = faceValue,
                price = row.getDouble(prevPriceIdx, zeroAsNull = true),
                accruedInt = row.getDouble(accruedIntIdx, zeroAsNull = false),
                yieldToMaturity = row.getDouble(yieldIdx, zeroAsNull = true),
                duration = durationMap[secid],
                couponPercent = row.getDouble(couponPercentIdx, zeroAsNull = true),
                couponValue = row.getDouble(couponValueIdx, zeroAsNull = true),
                couponPeriod = row.getInt(couponPeriodIdx),
                nextCoupon = row.getString(nextCouponIdx),
                matDate = row.getString(matDateIdx),
                offerDate = row.getString(offerDateIdx),
                yieldKind = yieldKindMap[secid]?.first,
                couponDetails = if (couponDetailsIdx >= 0) row.getString(couponDetailsIdx) else null,
                bondType = if (bondTypeIdx >= 0) row.getString(bondTypeIdx) else null,
                listLevel = row.getInt(listLevelIdx)
            )
            bonds.add(bond)
        }

        return Pair(bonds, priceDate)
    }

    /** Разбирает страницу купонов, амортизаций и курсоров пагинации. */
    fun parseBondizationPage(jsonStr: String, secid: String): BondizationPage {
        val dto = json.decodeFromString<IssBondizationResponseDto>(jsonStr)
        val coupons = mutableListOf<Payment>()
        val amortizations = mutableListOf<Payment>()

        val couponsTable = dto.coupons
        if (couponsTable != null) {
            val dateIdx = couponsTable.getColumnIndex("coupondate")
            val valIdx = couponsTable.getColumnIndex("value_rub")
            for (row in couponsTable.data) {
                val date = row.getString(dateIdx) ?: continue
                val amt = row.getDouble(valIdx, zeroAsNull = true)
                coupons.add(Payment(secid = secid, date = date, kind = PaymentKind.COUPON, amount = amt))
            }
        }

        val amortTable = dto.amortizations
        if (amortTable != null) {
            val dateIdx = amortTable.getColumnIndex("amortdate")
            val valIdx = amortTable.getColumnIndex("value_rub")
            for (row in amortTable.data) {
                val date = row.getString(dateIdx) ?: continue
                val amt = row.getDouble(valIdx, zeroAsNull = true)
                amortizations.add(Payment(secid = secid, date = date, kind = PaymentKind.REPAYMENT, amount = amt))
            }
        }

        return BondizationPage(
            coupons = coupons,
            amortizations = amortizations,
            couponsCursor = parseCursor(dto.couponsCursor),
            amortizationsCursor = parseCursor(dto.amortizationsCursor)
        )
    }

    private fun parseCursor(table: IssTableDto?): IssCursor? {
        if (table == null || table.data.isEmpty()) return null
        val row = table.data.first()
        val indexIdx = table.getColumnIndex("INDEX")
        val totalIdx = table.getColumnIndex("TOTAL")
        val pageSizeIdx = table.getColumnIndex("PAGESIZE")
        val index = row.getInt(indexIdx) ?: return null
        val total = row.getInt(totalIdx) ?: return null
        val pageSize = row.getInt(pageSizeIdx) ?: return null
        return IssCursor(index = index, total = total, pageSize = pageSize)
    }

    /** Разбирает ответ с выплатами и сортирует их по дате. */
    fun parsePaymentsResponse(jsonStr: String, secid: String): List<Payment> {
        val page = parseBondizationPage(jsonStr, secid)
        return (page.coupons + page.amortizations).sortedBy { it.date }
    }

    /** Извлекает из описания бумаги флаг для квалифицированных инвесторов. */
    fun parseIsQualified(jsonStr: String): Boolean {
        val dto = json.decodeFromString<IssDescriptionResponseDto>(jsonStr)
        val descTable = dto.description ?: return false
        val nameIdx = descTable.getColumnIndex("name")
        val valIdx = descTable.getColumnIndex("value")
        if (nameIdx < 0 || valIdx < 0) return false

        for (row in descTable.data) {
            val name = row.getString(nameIdx)
            if (name == "ISQUALIFIEDINVESTORS") {
                val value = row.getString(valIdx)
                return value == "1"
            }
        }
        return false
    }
}
