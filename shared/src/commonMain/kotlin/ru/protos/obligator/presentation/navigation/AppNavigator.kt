package ru.protos.obligator.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface Screen {
    data object Screener : Screen
    data object Favorites : Screen
    data object Portfolio : Screen
    data class BondCard(val secid: String) : Screen
    data object Compare : Screen
}

enum class BottomTab {
    SCREENER,
    FAVORITES,
    PORTFOLIO
}

class AppNavigator {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Screener))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    private val _activeTab = MutableStateFlow(BottomTab.SCREENER)
    val activeTab: StateFlow<BottomTab> = _activeTab.asStateFlow()

    /** Переключает активную вкладку и сбрасывает стек навигации на её корень. */
    fun navigateToTab(tab: BottomTab) {
        _activeTab.value = tab
        val rootScreen = when (tab) {
            BottomTab.SCREENER -> Screen.Screener
            BottomTab.FAVORITES -> Screen.Favorites
            BottomTab.PORTFOLIO -> Screen.Portfolio
        }
        _stack.value = listOf(rootScreen)
    }

    fun navigateTo(screen: Screen) {
        _stack.value = _stack.value + screen
    }

    /** Возвращает на предыдущий экран, если в стеке больше одного экрана. */
    fun pop(): Boolean {
        val currentStack = _stack.value
        if (currentStack.size > 1) {
            _stack.value = currentStack.dropLast(1)
            return true
        }
        return false
    }

    fun currentScreen(): Screen = _stack.value.last()
}
