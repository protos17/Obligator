package ru.protos.obligator.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.protos.obligator.analytics.model.Payment
import ru.protos.obligator.analytics.model.PaymentKind

@Entity(
    tableName = "payment",
    indices = [Index(value = ["secid", "payDate"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val secid: String,
    val payDate: String,
    val kind: String,
    val amount: Double?
)

fun PaymentEntity.toDomain() = Payment(
    id = id,
    secid = secid,
    date = payDate,
    kind = if (kind == "REPAYMENT") PaymentKind.REPAYMENT else PaymentKind.COUPON,
    amount = amount
)

fun Payment.toEntity() = PaymentEntity(
    id = id,
    secid = secid,
    payDate = date,
    kind = kind.name,
    amount = amount
)
