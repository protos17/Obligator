package ru.protos.obligator.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val secid: String,
    val shortName: String,
    val addedAt: Long
)
