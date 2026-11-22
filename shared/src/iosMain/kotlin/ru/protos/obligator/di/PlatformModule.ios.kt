package ru.protos.obligator.di

import org.koin.dsl.module
import ru.protos.obligator.data.db.getIosDatabaseBuilder

actual val platformModule = module {
    single { getIosDatabaseBuilder() }
}
