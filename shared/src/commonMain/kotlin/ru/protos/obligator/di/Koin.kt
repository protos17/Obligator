package ru.protos.obligator.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, dataModule, presentationModule)
}

class KoinHelper {
    fun doInit() = initKoin {}
}
