package ru.protos.obligator.di

import org.koin.dsl.module
import ru.protos.obligator.data.db.getAndroidDatabaseBuilder

actual val platformModule = module {
    single { getAndroidDatabaseBuilder(get()) }
}
