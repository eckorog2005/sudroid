/*
 *  Copyright 2026 Roger Lamb
 *
 *  This file is part of Sudroid.
 *
 *  Sudroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Sudroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Sudroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rlamb.android.sudroid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.rlamb.android.sudroid.game.GameRepository
import com.rlamb.android.sudroid.game.GameRequest
import com.rlamb.android.sudroid.game.GameScreen
import com.rlamb.android.sudroid.game.GameViewModel
import com.rlamb.android.sudroid.menu.MenuScreen
import com.rlamb.android.sudroid.results.ResultsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data object Menu : NavKey

@Serializable
data class Game(val request: GameRequest) : NavKey

@Serializable
data class Results(val elapsedSeconds: Int) : NavKey

@Composable
fun SudroidApp(repository: GameRepository, saveScope: CoroutineScope) {
    val backStack = rememberNavBackStack(Menu)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Menu> {
                val hasSavedGame by remember { repository.hasSavedGame() }.collectAsStateWithLifecycle(false)
                MenuScreen(hasSavedGame, onStart = { request -> backStack.add(Game(request)) })
            }
            entry<Game> { key ->
                GameScreen(
                    viewModel = viewModel { GameViewModel(repository, key.request, saveScope, createSavedStateHandle()) },
                    onSolved = { elapsedSeconds ->
                        // Replacing the game keeps Back from returning to a finished board.
                        if (backStack.lastOrNull() == key) backStack[backStack.lastIndex] = Results(elapsedSeconds)
                    },
                )
            }
            entry<Results> { key ->
                ResultsScreen(key.elapsedSeconds, onBackToMenu = { backStack.removeLastOrNull() })
            }
        },
    )
}
