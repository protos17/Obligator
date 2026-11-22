package ru.protos.obligator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.protos.obligator.analytics.model.UpdateInfo

@Entity(tableName = "update_info")
data class UpdateInfoEntity(
    @PrimaryKey val id: Int = 1,
    val loadedAt: Long,
    val priceDate: String?
)

fun UpdateInfoEntity.toDomain() = UpdateInfo(
    loadedAt = loadedAt,
    priceDate = priceDate
)

fun UpdateInfo.toEntity() = UpdateInfoEntity(
    id = 1,
    loadedAt = loadedAt,
    priceDate = priceDate
)
