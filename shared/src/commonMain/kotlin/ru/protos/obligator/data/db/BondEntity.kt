package ru.protos.obligator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.protos.obligator.analytics.model.Bond

@Entity(tableName = "bond")
data class BondEntity(
    @PrimaryKey val secid: String,
    val boardId: String,
    val isin: String?,
    val shortName: String,
    val secType: String?,
    val faceValue: Double,
    val price: Double?,
    val accruedInt: Double?,
    val yieldToMaturity: Double?,
    val duration: Int?,
    val couponPercent: Double?,
    val couponValue: Double?,
    val couponPeriod: Int?,
    val nextCoupon: String?,
    val matDate: String?,
    val offerDate: String?,
    val yieldKind: String?,
    val couponDetails: String?,
    val bondType: String?,
    val listLevel: Int?
)

fun BondEntity.toDomain() = Bond(
    secid = secid,
    boardId = boardId,
    isin = isin,
    shortName = shortName,
    secType = secType,
    faceValue = faceValue,
    price = price,
    accruedInt = accruedInt,
    yieldToMaturity = yieldToMaturity,
    duration = duration,
    couponPercent = couponPercent,
    couponValue = couponValue,
    couponPeriod = couponPeriod,
    nextCoupon = nextCoupon,
    matDate = matDate,
    offerDate = offerDate,
    yieldKind = yieldKind,
    couponDetails = couponDetails,
    bondType = bondType,
    listLevel = listLevel
)

fun Bond.toEntity() = BondEntity(
    secid = secid,
    boardId = boardId,
    isin = isin,
    shortName = shortName,
    secType = secType,
    faceValue = faceValue,
    price = price,
    accruedInt = accruedInt,
    yieldToMaturity = yieldToMaturity,
    duration = duration,
    couponPercent = couponPercent,
    couponValue = couponValue,
    couponPeriod = couponPeriod,
    nextCoupon = nextCoupon,
    matDate = matDate,
    offerDate = offerDate,
    yieldKind = yieldKind,
    couponDetails = couponDetails,
    bondType = bondType,
    listLevel = listLevel
)
