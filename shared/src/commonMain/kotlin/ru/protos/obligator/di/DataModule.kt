package ru.protos.obligator.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.protos.obligator.analytics.repository.BondRepository
import ru.protos.obligator.analytics.repository.FavoriteRepository
import ru.protos.obligator.analytics.repository.PortfolioRepository
import ru.protos.obligator.data.db.AppDatabase
import ru.protos.obligator.data.db.getRoomDatabase
import ru.protos.obligator.data.iss.IssApiClient
import ru.protos.obligator.data.iss.IssParser
import ru.protos.obligator.data.repository.BondRepositoryImpl
import ru.protos.obligator.data.repository.FavoriteRepositoryImpl
import ru.protos.obligator.data.repository.PortfolioRepositoryImpl

val dataModule = module {
    single {
        HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000L
                requestTimeoutMillis = 30_000L
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single { IssParser() }
    single { IssApiClient(get(), get()) }

    single { getRoomDatabase(get()) }
    single { get<AppDatabase>().bondDao() }
    single { get<AppDatabase>().updateInfoDao() }
    single { get<AppDatabase>().favoriteDao() }
    single { get<AppDatabase>().portfolioDao() }
    single { get<AppDatabase>().paymentDao() }

    single<BondRepository> { BondRepositoryImpl(get(), get()) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<PortfolioRepository> { PortfolioRepositoryImpl(get(), get()) }
}
