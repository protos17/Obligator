package ru.protos.obligator.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        BondEntity::class,
        UpdateInfoEntity::class,
        FavoriteEntity::class,
        PortfolioEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bondDao(): BondDao
    abstract fun updateInfoDao(): UpdateInfoDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun paymentDao(): PaymentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
