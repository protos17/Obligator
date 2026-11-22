package ru.protos.obligator.di

import org.koin.dsl.module
import ru.protos.obligator.presentation.card.BondCardViewModel
import ru.protos.obligator.presentation.compare.CompareSelection
import ru.protos.obligator.presentation.compare.CompareViewModel
import ru.protos.obligator.presentation.favorites.FavoritesViewModel
import ru.protos.obligator.presentation.portfolio.PortfolioViewModel
import ru.protos.obligator.presentation.screener.ScreenerViewModel

val presentationModule = module {
    single { CompareSelection() }

    single { ScreenerViewModel(get(), get(), get()) }
    single { BondCardViewModel(get(), get(), get()) }
    single { CompareViewModel(get(), get()) }
    single { FavoritesViewModel(get(), get(), get()) }
    single { PortfolioViewModel(get(), get(), get()) }
}
