package ru.protos.obligator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.tab_favorites
import obligator.shared.generated.resources.tab_portfolio
import obligator.shared.generated.resources.tab_screener
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ru.protos.obligator.presentation.card.BondCardScreen
import ru.protos.obligator.presentation.card.BondCardViewModel
import ru.protos.obligator.presentation.compare.CompareScreen
import ru.protos.obligator.presentation.compare.CompareViewModel
import ru.protos.obligator.presentation.favorites.FavoritesScreen
import ru.protos.obligator.presentation.favorites.FavoritesViewModel
import ru.protos.obligator.presentation.navigation.AppNavigator
import ru.protos.obligator.presentation.navigation.BottomTab
import ru.protos.obligator.presentation.navigation.Screen
import ru.protos.obligator.presentation.portfolio.PortfolioScreen
import ru.protos.obligator.presentation.portfolio.PortfolioViewModel
import ru.protos.obligator.presentation.screener.ScreenerScreen
import ru.protos.obligator.presentation.screener.ScreenerViewModel
import ru.protos.obligator.presentation.theme.AppTheme

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val navigator = remember { AppNavigator() }
    val stack by navigator.stack.collectAsState()
    val activeTab by navigator.activeTab.collectAsState()
    val currentScreen = stack.lastOrNull() ?: Screen.Screener

    val screenerViewModel = koinViewModel<ScreenerViewModel>()
    val bondCardViewModel = koinViewModel<BondCardViewModel>()
    val compareViewModel = koinViewModel<CompareViewModel>()
    val favoritesViewModel = koinViewModel<FavoritesViewModel>()
    val portfolioViewModel = koinViewModel<PortfolioViewModel>()

    val screenerListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()

    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    AppTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            bottomBar = {
                if (stack.size == 1) {
                    BottomNavigation {
                        val tabScreener = stringResource(Res.string.tab_screener)
                        val tabFavorites = stringResource(Res.string.tab_favorites)
                        val tabPortfolio = stringResource(Res.string.tab_portfolio)

                        BottomNavigationItem(
                            selected = activeTab == BottomTab.SCREENER,
                            onClick = { navigator.navigateToTab(BottomTab.SCREENER) },
                            icon = { Icon(Icons.Default.List, contentDescription = tabScreener) },
                            label = { Text(tabScreener) }
                        )
                        BottomNavigationItem(
                            selected = activeTab == BottomTab.FAVORITES,
                            onClick = { navigator.navigateToTab(BottomTab.FAVORITES) },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = tabFavorites) },
                            label = { Text(tabFavorites) }
                        )
                        BottomNavigationItem(
                            selected = activeTab == BottomTab.PORTFOLIO,
                            onClick = { navigator.navigateToTab(BottomTab.PORTFOLIO) },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = tabPortfolio) },
                            label = { Text(tabPortfolio) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (currentScreen) {
                    is Screen.Screener -> {
                        ScreenerScreen(
                            viewModel = screenerViewModel,
                            listState = screenerListState,
                            onBondClick = { secid -> navigator.navigateTo(Screen.BondCard(secid)) },
                            onCompareClick = { navigator.navigateTo(Screen.Compare) }
                        )
                    }
                    is Screen.Favorites -> {
                        FavoritesScreen(
                            viewModel = favoritesViewModel,
                            listState = favoritesListState,
                            onBondClick = { secid -> navigator.navigateTo(Screen.BondCard(secid)) },
                            onCompareClick = { navigator.navigateTo(Screen.Compare) }
                        )
                    }
                    is Screen.Portfolio -> {
                        PortfolioScreen(
                            viewModel = portfolioViewModel,
                            onBondClick = { secid -> navigator.navigateTo(Screen.BondCard(secid)) }
                        )
                    }
                    is Screen.BondCard -> {
                        BondCardScreen(
                            secid = currentScreen.secid,
                            viewModel = bondCardViewModel,
                            onBack = { navigator.pop() }
                        )
                    }
                    is Screen.Compare -> {
                        CompareScreen(
                            viewModel = compareViewModel,
                            onBack = { navigator.pop() }
                        )
                    }
                }
            }
        }
    }
}
