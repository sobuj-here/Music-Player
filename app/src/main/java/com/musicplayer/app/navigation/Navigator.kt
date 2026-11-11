package com.musicplayer.app.navigation

import androidx.compose.runtime.mutableStateListOf

class Navigator(initialScreen: Screen = Screen.Library) {
    private val backStack = mutableStateListOf<Screen>(initialScreen)

    val currentScreen: Screen
        get() = backStack.lastOrNull() ?: Screen.Library

    val canPop: Boolean
        get() = backStack.size > 1

    fun navigateTo(screen: Screen) {
        // Avoid duplicate consecutive destinations
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    fun pop(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            true
        } else {
            false
        }
    }

    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun replace(screen: Screen) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.size - 1)
        }
        backStack.add(screen)
    }
}
